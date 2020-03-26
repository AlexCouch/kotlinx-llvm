package parsing.vm

import ErrorResult
import OKResult
import parsing.hir.ToylangHIRElement
import Result
import WrappedResult
import com.couch.toylang.ToylangParser
import parsing.*
import parsing.ast.ToylangASTNode
import parsing.typeck.Type

data class VMValue(val type: Type, val value: Any?)

class VMParser: Parse<ToylangHIRElement, Result>(){
    val variables: HashMap<ToylangHIRElement.StatementNode.VariableNode, VMValue> = hashMapOf()
    override fun parseFile(node: ToylangHIRElement, context: Context): Result {
        if(node !is ToylangHIRElement.RootNode) return ParserErrorResult(ErrorResult("Attempted to run file without root node"), node.location)
        node.statements.forEach {
            this.parseStatement(it, context)
        }
        return OKResult
    }

    override fun parseVariableDeclaration(node: ToylangHIRElement, context: Context): Result {
        if(node !is ToylangHIRElement.StatementNode.VariableNode) return ParserErrorResult(ErrorResult("Attempted to evaluate variable declaration "), node.location)
        val assignment = when(val assignmentResult = this.parseAssignment(node.assignment, context)){
            is WrappedResult<*> -> {
                when(assignmentResult.t){
                    is VMValue -> {
                        if(assignmentResult.t.type.identifier == node.type?.typeName ?:
                                return ErrorResult("Variable ${node.identifier} does not have a type but tried to assign it to a value of type ${assignmentResult.t.type}")){
                            assignmentResult.t
                        }else{
                            return ErrorResult("Variable ${node.identifier} is of type ${node.type?.typeName} but its assignment was of type ${assignmentResult.t.type}")
                        }
                    }
                    else -> return ErrorResult("Unrecognized return value: ${assignmentResult.t}")
                }
            }
            is ErrorResult -> return ParserErrorResult(assignmentResult, node.location)
            is ParserErrorResult<*> -> return ErrorResult("An error occurred while parsing assignment for variable ${node.identifier}")
            else -> return ErrorResult("Unrecognized result: $assignmentResult")
        }
        this.variables[node] = assignment
        return OKResult
    }

    override fun parseStatement(node: ToylangHIRElement, context: Context): Result {
        return when(node){
            is ToylangHIRElement.StatementNode.VariableNode -> this.parseVariableDeclaration(node, context)
            is ToylangHIRElement.StatementNode.FunctionDeclNode -> this.parseFunctionDeclaration(node, context)
            is ToylangHIRElement.StatementNode.ExpressionNode -> this.parseExpression(node, context)
            is ToylangHIRElement.StatementNode.ReturnStatementNode -> this.parseReturnStatement(node, context)
            is ToylangHIRElement.StatementNode.AssignmentNode -> this.parseAssignment(node, context)
            else -> ErrorResult("Unrecognized statement: $node")
        }
    }

    override fun parseFunctionDeclaration(node: ToylangHIRElement, context: Context): Result {
        if(node !is ToylangHIRElement.StatementNode.FunctionDeclNode) return ParserErrorResult(ErrorResult("Attempted to evaluate function without function decl node"), node.location)
        node.codeblock.statements.forEach {
            when(it){
                is ToylangHIRElement.StatementNode.ReturnStatementNode -> {
                    return when(val returnResult = this.parseReturnStatement(it, node.context)){
                        is WrappedResult<*> -> {
                            when(returnResult.t){
                                is VMValue -> {
                                    if(returnResult.t.type.identifier == node.type.typeName){
                                        ErrorResult("Return statement value does not match return type of ${node.identifier} which is ${node.type.typeName}")
                                    }else{
                                        WrappedResult(returnResult.t.value)
                                    }
                                }
                                else -> ErrorResult("Unrecognized return result value: ${returnResult.t}")
                            }
                        }
                        is ErrorResult -> ParserErrorResult(returnResult, it.location)
                        is ParserErrorResult<*> -> ErrorResult("An error occurred while parsing return statement: $returnResult")
                        else -> ErrorResult("Unrecognized result: $returnResult")
                    }
                }
                else -> {
                    this.parseStatement(it, node.context)
                }
            }
        }
        return super.parseFunctionDeclaration(node, context)
    }

    override fun parseValueReference(node: ToylangHIRElement, context: Context): Result {
        if(node !is ToylangHIRElement.StatementNode.ExpressionNode.ValueReferenceNode) return ParserErrorResult(ErrorResult("Attempted to evaluate function call without function call node"), node.location)
        return when(val result = context.findIdentifier(node.identifier)){
            is WrappedResult<*> -> {
                when(result.t){
                    is ToylangHIRElement.StatementNode.VariableNode -> {
                        if(this.variables.containsKey(result.t)){
                            WrappedResult(variables[result.t])
                        }else{
                            ErrorResult("Value reference identifier ${node.identifier} does not reference a variable that has been initialized")
                        }
                    }
                    else -> {
                        ErrorResult("Value reference identifier ${node.identifier} does not reference a variable: ${result.t}")
                    }
                }
            }
            is ErrorResult -> ParserErrorResult(result, node.location)
            is ParserErrorResult<*> -> ErrorResult("An error occurred while parsing finding symbol for identifier ${node.identifier}: $result")
            else -> ErrorResult("Unrecognized result: $result")
        }
    }

    override fun parseFunctionCall(node: ToylangHIRElement, context: Context): Result {
        if(node !is ToylangHIRElement.StatementNode.ExpressionNode.FunctionCallNode) return ParserErrorResult(ErrorResult("Attempted to evaluate function call without function call node"), node.location)
        when(val findResult = context.findIdentifier(node.identifier)){
            is WrappedResult<*> -> {
                when(findResult.t){
                    is ToylangHIRElement.StatementNode.FunctionDeclNode -> {
                        return when(val callResult = this.parseFunctionDeclaration(findResult.t, findResult.t.context.parentContext)){
                            is WrappedResult<*> -> {
                                callResult
                            }
                            is ErrorResult -> ParserErrorResult(callResult, node.location)
                            is ParserErrorResult<*> -> ErrorResult("An error occurred while parsing function call: $callResult")
                            is OKResult -> WrappedResult(Type.NONE)
                            else -> ErrorResult("Unrecognized result: $callResult")
                        }
                    }
                    is ToylangHIRElement.StatementNode.VariableNode -> return ParserErrorResult(ErrorResult("Variable ${findResult.t.identifier} is not callable"), node.location)
                    is ToylangHIRElement.FunctionParamNode -> return ParserErrorResult(ErrorResult("Variable ${findResult.t.identifier} is not callable"), node.location)
                    else -> return ErrorResult("Unrecognized callable element: ${findResult.t}")
                }
            }
            is ErrorResult -> return ParserErrorResult(findResult, node.location)
            else -> return ErrorResult("Unrecognized result: $findResult")
        }
    }

    override fun parseStringLiteral(node: ToylangHIRElement, context: Context): Result {
        if(node !is ToylangHIRElement.StatementNode.ExpressionNode.StringLiteralExpression) return ParserErrorResult(ErrorResult("Attempted to evaluate string expression without string node"), node.location)
        val str = node.content.map {
            when(it){
                is ToylangHIRElement.StatementNode.ExpressionNode.StringLiteralContentNode.StringLiteralInterpolationNode -> when(val result = this.parseExpression(it.expression, context)){
                    is WrappedResult<*> -> result.t.toString()
                    is ErrorResult -> return ParserErrorResult(result, it.location)
                    is ParserErrorResult<*> -> return ErrorResult("An error occurred during while evaluated string interpolation: $result")
                    else -> return ErrorResult("Unrecognized result: $result")
                }
                is ToylangHIRElement.StatementNode.ExpressionNode.StringLiteralContentNode.StringLiteralRawNode -> it.content
            }
        }.joinToString(separator = "")
        return WrappedResult(VMValue(Type.STRING, str))
    }

    override fun parseBinaryOperation(node: ToylangHIRElement, context: Context): Result {
        if(node !is ToylangHIRElement.StatementNode.ExpressionNode.BinaryExpressionNode) return ParserErrorResult(ErrorResult("Attempted to evaluate left hand of binary operation without binary node"), node.location)
        val left = when(val result = this.parseExpression(node.left, context)){
            is WrappedResult<*> -> {
                when(result.t){
                    is VMValue -> {
                        result.t.value
                                ?: return ParserErrorResult(ErrorResult("Value of left hand of binary expression cannot be null"), node.left.location)
                    }
                    else -> return ErrorResult("Unrecognized return result value: ${result.t}")
                }
            }
            is ErrorResult -> ParserErrorResult(result, node.left.location)
            is ParserErrorResult<*> -> ErrorResult("An error occurred while parsing left hand of binary operation: $result")
            else -> ErrorResult("Unrecognized result: $result")
        }
        return super.parseBinaryOperation(node, context)
    }

    override fun parseExpression(node: ToylangHIRElement, context: Context): Result {
        return when(node){
            is ToylangHIRElement.StatementNode.ExpressionNode.BinaryExpressionNode -> this.parseBinaryOperation(node, context)
            is ToylangHIRElement.StatementNode.ExpressionNode.IntegerLiteralExpression -> WrappedResult(VMValue(Type.INTEGER, node.integer))
            is ToylangHIRElement.StatementNode.ExpressionNode.DecimalLiteralExpression -> WrappedResult(VMValue(Type.DECIMAL, node.decimal))
            is ToylangHIRElement.StatementNode.ExpressionNode.StringLiteralExpression -> this.parseStringLiteral(node, context)
            else -> ParserErrorResult(ErrorResult("Unrecognized expression: $node"), node.location)
        }
    }
}