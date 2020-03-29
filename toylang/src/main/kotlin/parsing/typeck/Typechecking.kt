package parsing.typeck

/*
import ErrorResult
import OKResult
import Result
import WrappedResult
import com.strumenta.kolasu.model.walkAncestors
import com.strumenta.kolasu.model.walkChildren
import parsing.*
import parsing.hir.ToylangHIRElement

class TypecheckingParser: Parse<ToylangHIRElement, Result>() {
    override fun parseElement(node: ToylangHIRElement, context: Context): Result {
        return when(node){
            is ToylangHIRElement.StatementNode.ExpressionNode.StringLiteralExpression -> {
                WrappedResult(Type.STRING)
            }
            is ToylangHIRElement.StatementNode.ExpressionNode.IntegerLiteralExpression -> {
                WrappedResult(Type.INTEGER)
            }
            is ToylangHIRElement.StatementNode.ExpressionNode.DecimalLiteralExpression -> {
                WrappedResult(Type.DECIMAL)
            }
            else -> ParserErrorResult(ErrorResult("Could not recognize element during type checking: $node"), node.location)
        }
    }

    override fun parseFile(node: ToylangHIRElement, context: Context): Result {
        if(node !is ToylangHIRElement.RootNode) return ParserErrorResult(ErrorResult("Attempted to do type checking on HIR without a root node: $node"), node.location)
        return node.statements.map {
            this.parseStatement(it, context)
        }.let { results ->
            results.forEach{
                when(it){
                    is ErrorResult -> return@let ParserErrorResult(it, node.location)
                    is ParserErrorResult<*> -> return@let it
                }
            }
            OKResult
        }

    }

    override fun parseStatement(node: ToylangHIRElement, context: Context): Result {
        if(node !is ToylangHIRElement.StatementNode) return ParserErrorResult(ErrorResult("Attempted to type check node for statement but was not given a statement node"), node.location)
        return when(node){
            is ToylangHIRElement.StatementNode.VariableNode -> this.parseVariableDeclaration(node, context)
            is ToylangHIRElement.StatementNode.FunctionDeclNode -> this.parseFunctionDeclaration(node, context)
            is ToylangHIRElement.StatementNode.AssignmentNode -> this.parseAssignment(node, context)
            is ToylangHIRElement.StatementNode.ExpressionNode -> this.parseExpression(node, context)
            is ToylangHIRElement.StatementNode.ReturnStatementNode -> this.parseReturnStatement(node, context)
        }
    }

    override fun parseReturnStatement(node: ToylangHIRElement, context: Context): Result {
        if(node !is ToylangHIRElement.StatementNode.ReturnStatementNode) return ParserErrorResult(ErrorResult("Attempted to parse return statement without return statement node: $node"), node.location)
        val returnType = when(val expressionTypeResult = this.parseExpression(node.expression, context)){
            is WrappedResult<*> -> {
                when(expressionTypeResult.t){
                    is Type -> expressionTypeResult.t
                    else -> return ParserErrorResult(ErrorResult("Unrecognized type check result value: ${expressionTypeResult.t}"), node.location)
                }
            }
            is ErrorResult -> return ParserErrorResult(ErrorResult("An error during type checking expression: $expressionTypeResult"), node.location)
            is ParserErrorResult<*> -> return ErrorResult("An error occurred during type checking expression: $expressionTypeResult")
            else -> return ErrorResult("Unrecognized result: $expressionTypeResult")
        }
        return WrappedResult(returnType)
    }

    override fun parseVariableDeclaration(node: ToylangHIRElement, context: Context): Result {
        if(node !is ToylangHIRElement.StatementNode.VariableNode) return ParserErrorResult(ErrorResult("Attempted to perform type checking on variable without a variable node"), node.location)
        return when{
            node.type != null -> {
                WrappedResult(Type(node.type!!.typeName))
            }
            else -> {
                val type = when(val result = this.parseAssignment(node.assignment, context)){
                    is WrappedResult<*> -> {
                        when(result.t){
                            is Type -> result.t
                            else -> return ErrorResult("Parse return result came back abnormal: ${result.t}")
                        }
                    }
                    is ErrorResult -> return ParserErrorResult(result, node.location)
                    is ParserErrorResult<*> -> return ErrorResult("An error occurred during assignent parsing: $result")
                    else -> return ErrorResult("Unrecognized result: $result")
                }
                node.type = ToylangHIRElement.TypeAnnotation(node.location, type.identifier)
                WrappedResult(type)
            }
        }
    }

    override fun parseAssignment(node: ToylangHIRElement, context: Context): Result {
        if(node !is ToylangHIRElement.StatementNode.AssignmentNode) return ParserErrorResult(ErrorResult("Attempted to perform type checking on variable without a variable node"), node.location)
        return this.parseExpression(node.expression, context)
    }

    override fun parseExpression(node: ToylangHIRElement, context: Context): Result {
        if(node !is ToylangHIRElement.StatementNode.ExpressionNode) return ParserErrorResult(ErrorResult("Attempted to perform type checking on expression without an expression node"), node.location)
        return when(node){
            is ToylangHIRElement.StatementNode.ExpressionNode.BinaryExpressionNode -> return this.parseBinaryOperation(node, context)
            is ToylangHIRElement.StatementNode.ExpressionNode.FunctionCallNode -> return this.parseFunctionCall(node, context)
            is ToylangHIRElement.StatementNode.ExpressionNode.ValueReferenceNode -> return this.parseValueReference(node, context)
            else -> this.parseElement(node, context)
        }
    }

    override fun parseValueReference(node: ToylangHIRElement, context: Context): Result {
        if(node !is ToylangHIRElement.StatementNode.ExpressionNode.ValueReferenceNode) return ParserErrorResult(ErrorResult("Attempted to perform type checking on value reference without a value reference node"), node.location)
        return when(val result = context.findIdentifier(node.identifier)){
            is WrappedResult<*> -> {
                when(result.t){
                    is ToylangHIRElement.StatementNode.VariableNode -> WrappedResult(Type(result.t.type?.typeName ?: return ParserErrorResult(ErrorResult("Type name of variable came back null"), node.location)))
                    is ToylangHIRElement.FunctionParamNode -> WrappedResult(Type(result.t.type.typeName))
                    is ToylangHIRElement.StatementNode.FunctionDeclNode -> WrappedResult(Type(result.t.type.typeName))
                    else -> ParserErrorResult(ErrorResult("Could not resolve type of symbol: ${result.t}"), node.location)
                }
            }
            is ErrorResult -> ParserErrorResult(result,node.location)
            is ParserErrorResult<*> -> ErrorResult("An error occurred while looking for symbol in current context: $result")
            else -> ErrorResult("Unrecognized result: $result")
        }
    }

    override fun parseFunctionDeclaration(node: ToylangHIRElement, context: Context): Result {
        if(node !is ToylangHIRElement.StatementNode.FunctionDeclNode) return ParserErrorResult(ErrorResult("Attempted to parse function declaration without a function declaration node"), node.location)
        node.codeblock.statements.forEach {
            when(it){
                is ToylangHIRElement.StatementNode.ReturnStatementNode -> {
                    return when(val result = this.parseReturnStatement(it, node.context)){
                        is WrappedResult<*> -> {
                            when(result.t){
                                is Type -> {
                                    if(result.t.identifier != node.type.typeName) ParserErrorResult(ErrorResult("Type of return statement expression does not match function return type"), it.location)
                                    OKResult
                                }
                                else -> ErrorResult("Could not recognize result value type of return statement type check")
                            }
                        }
                        is ErrorResult -> ParserErrorResult(result, it.location)
                        is ParserErrorResult<*> -> ErrorResult("An error occurred during type checking of return statement: $result")
                        else -> ErrorResult("Unrecognized result: $result")
                    }
                }
                else -> this.parseStatement(it, node.context)
            }
        }
        return OKResult
    }

    override fun parseOperation(node: ToylangHIRElement, context: Context): Result {
        return when(node){
            is ToylangMainAST.StatementNode.ExpressionNode.BinaryOperation -> this.parseBinaryOperation(node, context)
            else -> ParserErrorResult(ErrorResult("Unrecognized operation: $node"), node.location)
        }
    }

    override fun parseBinaryOperation(node: ToylangHIRElement, context: Context): Result {
        if(node !is ToylangHIRElement.StatementNode.ExpressionNode.BinaryExpressionNode) return ParserErrorResult(ErrorResult("Attempted to parse binary operation without a binary operation node"), node.location)
        val left = when(val result = this.parseExpression(node.left, context)){
            is WrappedResult<*> -> {
                when(result.t){
                    is Type -> result.t
                    else -> return ParserErrorResult(ErrorResult("Unrecognized type of return result value: ${result.t!!::class.qualifiedName}"), node.left.location)
                }
            }
            is ErrorResult -> return ParserErrorResult(result, node.left.location)
            is ParserErrorResult<*> -> return ErrorResult("An error occurred while type checking the left operand of a binary operation: $result")
            else -> return ErrorResult("Unrecognized result: $result")
        }
        val right = when(val result = this.parseExpression(node.right, context)){
            is WrappedResult<*> -> {
                when(result.t){
                    is Type -> result.t
                    else -> return ParserErrorResult(ErrorResult("Unrecognized type of return result value: ${result.t!!::class.qualifiedName}"), node.left.location)
                }
            }
            is ErrorResult -> return ParserErrorResult(result, node.left.location)
            is ParserErrorResult<*> -> return ErrorResult("An error occurred while type checking the right operand of a binary operation: $result")
            else -> return ErrorResult("Unrecognized result: $result")
        }
        if(left != right) return ParserErrorResult(ErrorResult("The type of left hand: [$left] is not the same as right hand: [$right]"), node.location)
        return WrappedResult(left)
    }
}

fun ToylangHIRElement.StatementNode.ExpressionNode.BinaryExpressionNode.findType(): Result{
    val leftCheck = when(val result = this.left.findType()){
        is WrappedResult<*> -> {
            result.t
        }
        is ErrorResult -> return ParserErrorResult(result, this.location)
        is ParserErrorResult<*> -> return ParserErrorResult(result, this.location)
        else -> ErrorResult("Unrecognized result: $result")
    }
    val rightCheck = when(val result = this.left.findType()){
        is WrappedResult<*> -> {
            result.t
        }
        is ErrorResult -> return ParserErrorResult(result, this.location)
        is ParserErrorResult<*> -> return ParserErrorResult(result, this.location)
        else -> ErrorResult("Unrecognized result: $result")
    }
    if(leftCheck != rightCheck){
        return ErrorResult("Left and right expressions are not the same type. Left: $leftCheck; Right: $rightCheck")
    }
    return WrappedResult(leftCheck)
}

fun ToylangHIRElement.StatementNode.ExpressionNode.ValueReferenceNode.findType(): Result{
    this.walkAncestors().forEach {
        when(it){
            is ProvidesContext -> {
                when(val findResult = it.context.findIdentifier(this.identifier)){
                    is WrappedResult<*> -> {
                        when(findResult.t){
                            is ToylangHIRElement.StatementNode.FunctionDeclNode -> {
                                return WrappedResult(Type(findResult.t.type.typeName))
                            }
                            is ToylangHIRElement.StatementNode.VariableNode ->{
                                return if(findResult.t.type == null){
                                    when(val typeResult = findResult.t.findType()){
                                        is WrappedResult<*> -> {
                                            return when(typeResult.t){
                                                is Type -> WrappedResult(typeResult.t)
                                                else -> ParserErrorResult(ErrorResult("Could not find type for variable: $it"), this.location)
                                            }
                                        }
                                        else -> ErrorResult("No type info found for variable")
                                    }
                                }else{
                                    WrappedResult(Type(findResult.t.identifier))
                                }
                            }
                            is ToylangHIRElement.FunctionParamNode -> {
                                return WrappedResult(Type(findResult.t.type.typeName))
                            }
                            else -> ErrorResult("Unrecognized symbol: ${findResult.t}")
                        }
                    }
                    is ErrorResult -> ErrorResult("Could not find symbol for value reference identifier: ${this.identifier}")
                    else -> ErrorResult("Unrecognized result: $findResult")
                }
            }
        }
    }
    return ErrorResult("Could not find context")
}

fun ToylangHIRElement.StatementNode.ExpressionNode.FunctionCallNode.findType(): Result{
    this.walkAncestors().forEach {
        when(it){
            is ProvidesContext -> {
                return when(val findResult = it.context.findIdentifier(this.identifier)){
                    is WrappedResult<*> -> {
                        when(findResult.t){
                            is ToylangHIRElement.StatementNode.FunctionDeclNode -> {
                                WrappedResult(Type(findResult.t.type.typeName))
                            }
                            is ToylangHIRElement.StatementNode.VariableNode ->{
                                WrappedResult(Type(findResult.t.type!!.typeName))
                            }
                            else -> ErrorResult("Unrecognized symbol: ${findResult.t}")
                        }
                    }
                    is ErrorResult -> ErrorResult("Could not find symbol for function call identifier: ${this.identifier}")
                    else -> ErrorResult("Unrecognized result: $findResult")
                }
            }
        }
    }
    return ErrorResult("Could not find type of function call node")
}

fun ToylangHIRElement.StatementNode.ExpressionNode.findType(): Result{
    return when(this){
        is ToylangHIRElement.StatementNode.ExpressionNode.BinaryExpressionNode -> {
            when(val result = this.findType()){
                is WrappedResult<*> -> result
                is ErrorResult -> ErrorResult("Type checking for binary expression came back not okay", result)
                else -> ErrorResult("Unrecognized result: $result")
            }
        }
        is ToylangHIRElement.StatementNode.ExpressionNode.ValueReferenceNode -> {
            when(val result = this.findType()){
                is WrappedResult<*> -> result
                is ErrorResult -> ErrorResult("Type checking for value reference came back not okay", result)
                else -> ErrorResult("Unrecognized result: $result")
            }
        }
        is ToylangHIRElement.StatementNode.ExpressionNode.StringLiteralExpression -> {
            WrappedResult(Type.STRING)
        }
        is ToylangHIRElement.StatementNode.ExpressionNode.IntegerLiteralExpression -> {
            WrappedResult(Type.INTEGER)
        }
        is ToylangHIRElement.StatementNode.ExpressionNode.DecimalLiteralExpression -> {
            WrappedResult(Type.DECIMAL)
        }
        is ToylangHIRElement.StatementNode.ExpressionNode.FunctionCallNode -> {
            when(val result = this.findType()){
                is WrappedResult<*> -> result
                is ErrorResult -> ErrorResult("Type checking for value reference came back not okay", result)
                else -> ErrorResult("Unrecognized result: $result")
            }
        }
        else -> ErrorResult("Unrecognized type of expression $this")
    }
}

fun ToylangHIRElement.StatementNode.findType(): Result{
    return when(this){
        is ToylangHIRElement.StatementNode.VariableNode -> {
            when(val exprTypeResult = this.assignment.expression.findType()){
                is WrappedResult<*> -> {
                    when(exprTypeResult.t){
                        is Type -> {
                            if(this.type == null){
                                this.type = ToylangHIRElement.TypeAnnotation(this.location, exprTypeResult.t.identifier)
                            }
                            WrappedResult(exprTypeResult.t)
                        }
                        else -> ErrorResult("No type annotation found in expression")
                    }
                }
                is ErrorResult -> ParserErrorResult(exprTypeResult, this.location)
                else -> ErrorResult("Unrecognized result: $exprTypeResult")
            }
        }
        is ToylangHIRElement.StatementNode.FunctionDeclNode -> {
            this.codeblock.statements.map {
                it.findType()
            }.apply {
                this.forEach {
                    when(it){
                        is ErrorResult -> return ErrorResult("Could not verify type of codeblock statement", it)
                    }
                }
            }
            WrappedResult(Type(this.type.typeName))
        }
        is ToylangHIRElement.StatementNode.ReturnStatementNode -> {
           when(val findResult = this.expression.findType()){
               is WrappedResult<*> -> findResult
               is ErrorResult -> ParserErrorResult(findResult, this.location)
               is ParserErrorResult<*> -> ParserErrorResult(findResult, this.location)
               else -> ErrorResult("Unrecognized result: $findResult")
           }
        }
        is ToylangHIRElement.StatementNode.ExpressionNode -> this.findType()
        else -> ErrorResult("Could not recognized statement: $this")
    }
}*/
