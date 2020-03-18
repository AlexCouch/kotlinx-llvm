package com.couch.kotlinx.parsing.p1

import ErrorResult
import OKResult
import Result
import WrappedResult
import com.couch.kotlinx.ast.Location
import org.antlr.v4.kotlinruntime.ast.Point
import parsing.ParserErrorResult
import parsing.ToylangMainAST
import parsing.p1.ToylangP1ASTNode

interface Context{
    val parentContext: Context?
    fun findIdentifier(identifier: String): Result
}

interface ProvidesContext{
    val context: Context
}

data class GlobalContext(override val parentContext: Context? = null, val globalVariables: ArrayList<ToylangP1ASTNode.StatementNode.VariableNode.GlobalVariableNode> = arrayListOf(), val functions: ArrayList<ToylangP1ASTNode.StatementNode.FunctionDeclNode> = arrayListOf()): Context {
    override fun findIdentifier(identifier: String): Result {
        val found = this.globalVariables.find { it.identifier == identifier } ?: this.functions.find { it.identifier == identifier }
        return if(found != null) WrappedResult(found) else when(val parentFound = this.parentContext?.findIdentifier(identifier) ?: return ErrorResult("No parent context, and no symbol found in current context: $identifier")){
            is WrappedResult<*> -> parentFound
            is ErrorResult -> ErrorResult("Could not find symbol with identifier: $identifier", parentFound)
            else -> OKResult
        }
    }
}

data class FunctionContext(override val parentContext: Context, val params: ArrayList<ToylangP1ASTNode.FunctionParamNode> = arrayListOf(), val localVariables: ArrayList<ToylangP1ASTNode.StatementNode.VariableNode.LocalVariableNode> = arrayListOf()): Context{
    override fun findIdentifier(identifier: String): Result {
        val found = this.params.find { it.identifier == identifier } ?: this.localVariables.find { it.identifier == identifier }
        return if(found != null) WrappedResult(found) else when(val parentFound = this.parentContext.findIdentifier(identifier)){
            is WrappedResult<*> -> parentFound
            is ErrorResult -> ErrorResult("Could not find symbol with identifier: $identifier", parentFound)
            else -> ErrorResult("Unrecognized result: $parentFound")
        }
    }
}

class GeneralParsingStage {
    fun parseRootNode(rootNode: ToylangMainAST.RootNode): Result{
        val globalContext = GlobalContext()
        val statements = rootNode.statements.map{
            when (it) {
                is ToylangMainAST.StatementNode.LetNode -> {
                    when(val letNodeResult = this.parseLetNode(it, globalContext)){
                        is WrappedResult<*> -> {
                            when(letNodeResult.t){
                                is ToylangP1ASTNode.StatementNode.VariableNode.GlobalVariableNode -> letNodeResult.t
                                null -> return ErrorResult("Variable node came back null")
                                else -> return ErrorResult("Variable node came back as wrong type: ${letNodeResult.t::class.qualifiedName}")
                            }
                        }
                        is ErrorResult -> return ErrorResult("Could not parse let node into variable node", letNodeResult)
                        is ParserErrorResult<*> -> return ErrorResult("Parser error occurred while parsing let declaration: $letNodeResult")
                        else -> return ErrorResult("Unrecognized result: $letNodeResult")
                    }
                }
                is ToylangMainAST.StatementNode.FunctionDeclNode -> {
                    when(val functionDeclResult = this.parseFunctionDeclNode(it, globalContext)){
                        is WrappedResult<*> -> {
                            when(functionDeclResult.t){
                                is ToylangP1ASTNode.StatementNode.FunctionDeclNode -> functionDeclResult.t
                                null -> return ErrorResult("Function declaration node came back null")
                                else -> return ErrorResult("Function declaration node came back as wrong type: ${functionDeclResult.t::class.qualifiedName}")
                            }
                        }
                        is ErrorResult -> return ErrorResult("Could not parse function declaration node", functionDeclResult)
                        is ParserErrorResult<*> -> return ErrorResult("Parser error occurred while parsing function declaration: $functionDeclResult")
                        else -> return ErrorResult("Unrecognized result: $functionDeclResult")
                    }
                }
                else -> throw RuntimeException("Found non-statement node mixed with statement nodes: $it")
            }
        }
        val ret = ToylangP1ASTNode.RootNode(rootNode.location, statements, globalContext)
        ret.context.functions.add(
                ToylangP1ASTNode.StatementNode.FunctionDeclNode(
                        Location(Point(1, 1), Point(1, 1)),
                        "printf",
                        ToylangP1ASTNode.TypeAnnotation(Location(Point(1, 1), Point(1, 1)), "Int"),
                        ToylangP1ASTNode.CodeBlockNode(Location(Point(1, 1), Point(1, 1)), emptyList()),
                        FunctionContext(ret.context)
                )
        )
        return WrappedResult(ret)
    }

    fun parseStringLiteralExpression(expression: ToylangMainAST.StatementNode.ExpressionNode.StringLiteralNode, context: Context): Result =
            WrappedResult(ToylangP1ASTNode.StatementNode.ExpressionNode.StringLiteralExpression(expression.location, expression.content.map {
                when (it) {
                    is ToylangMainAST.StatementNode.ExpressionNode.StringLiteralContentNode.StringInterpolationNode -> {
                        when(val interpExpressionResult = this.parseExpression(it.interpolatedExpr, context)){
                            is WrappedResult<*> -> {
                                when(interpExpressionResult.t){
                                    is ToylangP1ASTNode.StatementNode.ExpressionNode -> {
                                        ToylangP1ASTNode.StatementNode.ExpressionNode.StringLiteralContentNode.StringLiteralInterpolationNode(expression.location, interpExpressionResult.t)
                                    }
                                    else -> return ErrorResult("Could not parse expression node for string interpolation")
                                }

                            }
                            is ErrorResult -> {
                                return ErrorResult("Could not parse expression node for string interpolation", interpExpressionResult)
                            }
                            is ParserErrorResult<*> -> return ErrorResult("Parser error occurred while parsing string interpolation: $interpExpressionResult")
                            else -> return ErrorResult("Could not parse expression node for string interpolation", ErrorResult("Unrecognized result: $interpExpressionResult"))
                        }
                    }
                    is ToylangMainAST.StatementNode.ExpressionNode.StringLiteralContentNode.RawStringLiteralContentNode -> {
                        ToylangP1ASTNode.StatementNode.ExpressionNode.StringLiteralContentNode.StringLiteralRawNode(expression.location, it.string)
                    }
                }
            }))

    fun parseBinaryOperation(expression: ToylangMainAST.StatementNode.ExpressionNode.BinaryOperation, context: Context): Result {
        val leftParseResult = this.parseExpression(expression.left, context)
        val rightParseResult = this.parseExpression(expression.right, context)

        val left: ToylangP1ASTNode.StatementNode.ExpressionNode
        when (leftParseResult) {
            is WrappedResult<*> -> {
                when (leftParseResult.t) {
                    is ToylangP1ASTNode.StatementNode.ExpressionNode -> left = leftParseResult.t
                    else -> {
                        return ErrorResult("Did not get expression node from parsing the left node of binary operation")
                    }
                }
            }
            is ErrorResult -> {
                return ErrorResult("Could not get wrapped parsed expression from phase 1 parser", cause = leftParseResult)
            }
            is ParserErrorResult<*> -> return ErrorResult("Parser error occurred while parsing left hand of binary operation: $leftParseResult")
            else -> {
                return ErrorResult("Could not get proper return result from expression parser for right expression of binary operation")
            }
        }
        val right: ToylangP1ASTNode.StatementNode.ExpressionNode
        when (rightParseResult) {
            is WrappedResult<*> -> {
                when (rightParseResult.t) {
                    is ToylangP1ASTNode.StatementNode.ExpressionNode -> right = rightParseResult.t
                    else -> {
                        return ErrorResult("Did not get expression node from parsing the left node of binary operation")
                    }
                }
            }
            is ErrorResult -> {
                return ErrorResult("Could not get wrapped parsed expression from phase 1 parser", cause = rightParseResult)
            }
            is ParserErrorResult<*> -> return ErrorResult("Parser error occurred while parsing right hand of binary operation: $rightParseResult")
            else -> {
                return ErrorResult("Could not get proper return result from expression parser for right expression of binary operation")
            }
        }

        return when (expression) {
            is ToylangMainAST.StatementNode.ExpressionNode.BinaryOperation.BinaryPlusOperation -> {
                WrappedResult(ToylangP1ASTNode.StatementNode.ExpressionNode.BinaryExpressionNode.PlusNode(expression.location, left, right))
            }
            is ToylangMainAST.StatementNode.ExpressionNode.BinaryOperation.BinaryMinusOperation -> {
                WrappedResult(ToylangP1ASTNode.StatementNode.ExpressionNode.BinaryExpressionNode.MinusNode(expression.location, left, right))
            }
            is ToylangMainAST.StatementNode.ExpressionNode.BinaryOperation.BinaryDivOperation -> {
                WrappedResult(ToylangP1ASTNode.StatementNode.ExpressionNode.BinaryExpressionNode.DivNode(expression.location, left, right))
            }
            is ToylangMainAST.StatementNode.ExpressionNode.BinaryOperation.BinaryMultOperation -> {
                WrappedResult(ToylangP1ASTNode.StatementNode.ExpressionNode.BinaryExpressionNode.MultiplyNode(expression.location, left, right))
            }
        }
    }

    fun parseFunctionCallNode(functionCallNode: ToylangMainAST.StatementNode.ExpressionNode.FunctionCallNode, context: Context): Result{
        val ident = functionCallNode.name
        val args = functionCallNode.args.map {
            when(val expressionResult = this.parseExpression(it, context)){
                is WrappedResult<*> -> {
                    when(expressionResult.t){
                        is ToylangP1ASTNode.StatementNode.ExpressionNode -> expressionResult.t
                        null -> return ErrorResult("Wrapped return result object was null")
                        else -> return ErrorResult("Function call argument expression node came back wrong type ${expressionResult.t::class::qualifiedName}")
                    }
                }
                is ErrorResult -> return ParserErrorResult(ErrorResult("Could not parse function call argument", expressionResult), functionCallNode.location)
                is ParserErrorResult<*> -> return ErrorResult("Parser error occurred while parsing function call: $expressionResult")
                else -> return ErrorResult("Unrecognized result: $expressionResult")
            }
        }
        return WrappedResult(ToylangP1ASTNode.StatementNode.ExpressionNode.FunctionCallNode(functionCallNode.location, ident, args))
    }

    fun parseExpression(expression: ToylangMainAST.StatementNode.ExpressionNode, context: Context): Result = when(expression){
            is ToylangMainAST.StatementNode.ExpressionNode.IntegerLiteralNode -> {
                WrappedResult(ToylangP1ASTNode.StatementNode.ExpressionNode.IntegerLiteralExpression(expression.location, expression.integer))
            }
            is ToylangMainAST.StatementNode.ExpressionNode.DecimalLiteralNode -> {
                WrappedResult(ToylangP1ASTNode.StatementNode.ExpressionNode.DecimalLiteralExpression(expression.location, expression.float))
            }
            is ToylangMainAST.StatementNode.ExpressionNode.StringLiteralNode -> {
                this.parseStringLiteralExpression(expression, context)
            }
            is ToylangMainAST.StatementNode.ExpressionNode.BinaryOperation -> {
                this.parseBinaryOperation(expression, context)
            }
            is ToylangMainAST.StatementNode.ExpressionNode.FunctionCallNode -> {
                this.parseFunctionCallNode(expression, context)
            }
            is ToylangMainAST.StatementNode.ExpressionNode.ValueReferenceNode -> {
                WrappedResult(ToylangP1ASTNode.StatementNode.ExpressionNode.ValueReferenceNode(expression.location, expression.ident.identifier))
            }
            else -> ParserErrorResult(ErrorResult("Could not parse expression: expression not recognized."), expression.location)
        }

    fun parseAssignment(assignmentNode: ToylangMainAST.StatementNode.AssignmentNode, context: Context): Result =
        when(val expressionResult = this.parseExpression(assignmentNode.expression, context)){
            is WrappedResult<*> -> {
                when(expressionResult.t){
                    is ToylangP1ASTNode.StatementNode.ExpressionNode -> {
                        WrappedResult(ToylangP1ASTNode.StatementNode.AssignmentNode(assignmentNode.location, expressionResult.t))
                    }
                    null -> ErrorResult("Wrapped return result object was null")
                    else -> ErrorResult("Expression node came back wrong type ${expressionResult.t::class::qualifiedName}")
                }
            }
            is ErrorResult -> {
                ParserErrorResult(ErrorResult("Could not parse expression for assignment node", cause = expressionResult), assignmentNode.location)
            }
            is ParserErrorResult<*> -> ErrorResult("Parser error occurred while parsing assignment: $expressionResult")
            else -> ErrorResult("Could not parse expression, got an unrecognized return result: $expressionResult")
        }

    fun parseLetNode(letNode: ToylangMainAST.StatementNode.LetNode, context: Context): Result{
        val ident = letNode.identifier.identifier
        val mutable = letNode.mutable
        val type = if(letNode.type == null) null else ToylangP1ASTNode.TypeAnnotation(letNode.location, letNode.type.identifier.identifier)
        return when(val assignmentResult = this.parseAssignment(letNode.assignment, context)){
            is WrappedResult<*> -> {
                when(assignmentResult.t){
                    is ToylangP1ASTNode.StatementNode.AssignmentNode -> {
                        when(context){
                            is GlobalContext -> {
                                val variable = ToylangP1ASTNode.StatementNode.VariableNode.GlobalVariableNode(letNode.location, ident, mutable, type, assignmentResult.t)
                                context.globalVariables.add(variable)
                                WrappedResult(variable)
                            }
                            is FunctionContext -> {
                                val variable = ToylangP1ASTNode.StatementNode.VariableNode.LocalVariableNode(letNode.location, ident, mutable, type, assignmentResult.t)
                                context.localVariables.add(variable)
                                WrappedResult(variable)
                            }
                            else -> ErrorResult("Unrecognized context: $context")
                        }
                    }
                    null -> ErrorResult("Wrapped return result object was null")
                    else -> ErrorResult("Assignment node came back wrong type ${assignmentResult.t::class::qualifiedName}")
                }
            }
            is ErrorResult -> {
                ParserErrorResult(ErrorResult("Could not get phase 1 parsed assignment node", assignmentResult), letNode.location)
            }
            is ParserErrorResult<*> -> return ErrorResult("Parser error occurred while parsing function call: $assignmentResult")
            else -> ErrorResult("Unrecognized result: $assignmentResult")
        }
    }

    fun parseReturnStatement(statementNode: ToylangMainAST.StatementNode.ReturnStatementNode, context: FunctionContext): Result{
        val expression = when(val expressionResult = this.parseExpression(statementNode.expression, context)){
            is WrappedResult<*> -> {
                when(expressionResult.t){
                    is ToylangP1ASTNode.StatementNode.ExpressionNode -> expressionResult.t
                    null -> return ErrorResult("Expression node came back null")
                    else -> return ErrorResult("Expression node came back wrong type: ${expressionResult.t::class.qualifiedName}")
                }
            }
            is ErrorResult -> return ParserErrorResult(
                    ErrorResult("Could not parse expression node", expressionResult),
                    statementNode.location
            )
            is ParserErrorResult<*> -> return ErrorResult("Parser error occurred while parsing return statement: $expressionResult")
            else -> return ErrorResult("Unrecognized result: $expressionResult")
        }
        return WrappedResult(ToylangP1ASTNode.StatementNode.ReturnStatementNode(statementNode.location, expression))
    }

    fun parseCodeblockStatement(statementNode: ToylangMainAST.StatementNode, context: FunctionContext): Result{
        return when(statementNode){
            is ToylangMainAST.StatementNode.AssignmentNode -> {
                this.parseAssignment(statementNode, context)
            }
            is ToylangMainAST.StatementNode.LetNode -> {
                this.parseLetNode(statementNode, context)
            }
            is ToylangMainAST.StatementNode.ReturnStatementNode -> {
                this.parseReturnStatement(statementNode, context)
            }
            is ToylangMainAST.StatementNode.ExpressionNode -> {
                this.parseExpression(statementNode, context)
            }
            else -> ParserErrorResult(
                    ErrorResult("Codeblock statement not recognized: ${statementNode::class.qualifiedName}"),
                    statementNode.location
            )
        }
    }

    fun parseStatement(statementNode: ToylangMainAST.StatementNode, context: Context): Result{
        return when(statementNode){
            is ToylangMainAST.StatementNode.ExpressionNode -> {
                this.parseExpression(statementNode, context)
            }
            is ToylangMainAST.StatementNode.AssignmentNode -> {
                this.parseAssignment(statementNode, context)
            }
            is ToylangMainAST.StatementNode.LetNode -> {
                this.parseLetNode(statementNode, context)
            }
            is ToylangMainAST.StatementNode.FunctionDeclNode -> {
                when(context){
                    is GlobalContext -> this.parseFunctionDeclNode(statementNode, context)
                    else -> ErrorResult("Function decl node found in wrong context")
                }

            }
            else -> ErrorResult("Statement node not recognized: ${statementNode::class.qualifiedName}")
        }
    }

    fun parseCodeblock(codeblock: ToylangMainAST.CodeblockNode, context: FunctionContext): Result{
        val statements = codeblock.statements.map {
            when(val codeblockStatementResult = this.parseCodeblockStatement(it, context)){
                is WrappedResult<*> -> {
                    when(codeblockStatementResult.t){
                        is ToylangP1ASTNode.StatementNode -> codeblockStatementResult.t
                        null -> return ErrorResult("Codeblock statement node came back null")
                        else -> return ErrorResult("Codeblock statement node came back as wrong type: ${codeblockStatementResult.t::class.qualifiedName}")
                    }
                }
                is ErrorResult -> return ParserErrorResult(ErrorResult("Could not parse codeblock statement", codeblockStatementResult), codeblock.location)
                is ParserErrorResult<*> -> return ErrorResult("Parser error occurred while parsing codeblock statement: $codeblockStatementResult")
                else -> return ErrorResult("Unrecognized result: $codeblockStatementResult")
            }
        }
        return WrappedResult(ToylangP1ASTNode.CodeBlockNode(codeblock.location, statements))
    }

    fun parseFunctionParamNode(paramNode: ToylangMainAST.FunctionParamNode, context: FunctionContext): Result{
        val ident = paramNode.identifier.identifier
        val type = paramNode.type.identifier.identifier
        return when(val symbol = context.findIdentifier(ident)){
             is WrappedResult<*> -> {
                 when(symbol.t){
                     is ToylangP1ASTNode.FunctionParamNode -> ErrorResult("Function param with identifier $ident already exists for current function")
                     else -> ParserErrorResult(ErrorResult("Function param with identifier $ident already exists"), paramNode.location)
                 }
             }
            else -> WrappedResult(ToylangP1ASTNode.FunctionParamNode(paramNode.location, ident, ToylangP1ASTNode.TypeAnnotation(paramNode.location, type)))
        }
    }

    fun parseFunctionDeclNode(functionDeclNode: ToylangMainAST.StatementNode.FunctionDeclNode, context: GlobalContext): Result{
        val ident = functionDeclNode.identifier.identifier
        val type = ToylangP1ASTNode.TypeAnnotation(functionDeclNode.location, functionDeclNode.returnType?.identifier?.identifier ?: "Unit")
        val oldcodeblock = functionDeclNode.codeBlock
        val localContext = FunctionContext(context)
        val localVariables = arrayListOf<ToylangP1ASTNode.StatementNode.VariableNode.LocalVariableNode>()
        localContext.params.addAll(functionDeclNode.params.map {
            when(val paramResult = this.parseFunctionParamNode(it, localContext)){
                is WrappedResult<*> -> {
                    when(paramResult.t){
                        is ToylangP1ASTNode.FunctionParamNode -> {
                            paramResult.t
                        }
                        null -> return ErrorResult("Function param node came back null")
                        else -> return ErrorResult("Function param node came back wrong type: ${paramResult.t::class.qualifiedName}")
                    }
                }
                is ErrorResult -> return ParserErrorResult(ErrorResult("Could not parse function param node", paramResult), functionDeclNode.location)
                is ParserErrorResult<*> -> return ErrorResult("Parser error occurred while parsing function parameter: $paramResult")
                else -> return ErrorResult("Unrecognized result: $paramResult")
            }
        })
        val newcodeblock = when(val codeblockParseResult = this.parseCodeblock(oldcodeblock, localContext)){
            is WrappedResult<*> -> {
                when(codeblockParseResult.t){
                    is ToylangP1ASTNode.CodeBlockNode -> codeblockParseResult.t
                    null -> return ErrorResult("Codeblock node came back null")
                    else -> return ErrorResult("Codeblock node came back as wrong type: ${codeblockParseResult.t::class.qualifiedName}")
                }
            }
            is ErrorResult -> return ParserErrorResult(ErrorResult("Could not parse codeblock", codeblockParseResult), functionDeclNode.location)
            is ParserErrorResult<*> -> return ErrorResult("Parser error occurred while parsing function call: $codeblockParseResult")
            else -> return ErrorResult("Unrecognized result: $codeblockParseResult")
        }.apply {
            this.statements.forEach {
                when(it){
                    is ToylangP1ASTNode.StatementNode.VariableNode.LocalVariableNode -> localVariables.add(it)
                }
            }
        }
        return WrappedResult(ToylangP1ASTNode.StatementNode.FunctionDeclNode(functionDeclNode.location, ident, type, newcodeblock, localContext))
    }
}