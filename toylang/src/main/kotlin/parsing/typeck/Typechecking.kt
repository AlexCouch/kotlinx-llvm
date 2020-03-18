package parsing.typeck

import ErrorResult
import OKResult
import com.couch.kotlinx.ast.ToylangASTNode
import Result
import WrappedResult
import com.couch.kotlinx.parsing.p1.ProvidesContext
import com.strumenta.kolasu.model.find
import com.strumenta.kolasu.model.walk
import com.strumenta.kolasu.model.walkAncestors
import com.strumenta.kolasu.model.walkChildren
import com.sun.org.apache.xpath.internal.ExpressionNode
import parsing.ParserErrorResult
import parsing.ToylangMainAST
import parsing.p1.ToylangP1ASTNode
import sun.reflect.annotation.TypeAnnotation

fun ToylangP1ASTNode.StatementNode.ExpressionNode.BinaryExpressionNode.findType(): Result{
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
    if(leftCheck !== rightCheck){
        return ErrorResult("Left and right expressions are not the same type. Left: $leftCheck; Right: $rightCheck")
    }
    return WrappedResult(leftCheck)
}

fun ToylangP1ASTNode.StatementNode.ExpressionNode.ValueReferenceNode.findType(): Result{
    this.walkAncestors().forEach {
        when(it){
            is ProvidesContext -> {
                when(val findResult = it.context.findIdentifier(this.identifier)){
                    is WrappedResult<*> -> {
                        when(findResult.t){
                            is ToylangP1ASTNode.StatementNode.FunctionDeclNode -> {
                                return WrappedResult(findResult.t.type)
                            }
                            is ToylangP1ASTNode.StatementNode.VariableNode ->{
                                return WrappedResult(findResult.t.type)
                            }
                            is ToylangP1ASTNode.FunctionParamNode -> {
                                return WrappedResult(findResult.t.type)
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

fun ToylangP1ASTNode.StatementNode.ExpressionNode.FunctionCallNode.findType(): Result{
    this.walkAncestors().forEach {
        when(it){
            is ProvidesContext -> {
                return when(val findResult = it.context.findIdentifier(this.identifier)){
                    is WrappedResult<*> -> {
                        when(findResult.t){
                            is ToylangP1ASTNode.StatementNode.FunctionDeclNode -> {
                                WrappedResult(findResult.t.type)
                            }
                            is ToylangP1ASTNode.StatementNode.VariableNode ->{
                                WrappedResult(findResult.t.type)
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

fun ToylangP1ASTNode.StatementNode.ExpressionNode.findType(): Result{
    return when(this){
        is ToylangP1ASTNode.StatementNode.ExpressionNode.BinaryExpressionNode -> {
            when(val result = this.findType()){
                is WrappedResult<*> -> result
                is ErrorResult -> ErrorResult("Type checking for binary expression came back not okay", result)
                else -> ErrorResult("Unrecognized result: $result")
            }
        }
        is ToylangP1ASTNode.StatementNode.ExpressionNode.ValueReferenceNode -> {
            when(val result = this.findType()){
                is WrappedResult<*> -> result
                is ErrorResult -> ErrorResult("Type checking for value reference came back not okay", result)
                else -> ErrorResult("Unrecognized result: $result")
            }
        }
        is ToylangP1ASTNode.StatementNode.ExpressionNode.StringLiteralExpression -> {
            WrappedResult(ToylangP1ASTNode.TypeAnnotation.STRING)
        }
        is ToylangP1ASTNode.StatementNode.ExpressionNode.IntegerLiteralExpression -> {
            WrappedResult(ToylangP1ASTNode.TypeAnnotation.INTEGER)
        }
        is ToylangP1ASTNode.StatementNode.ExpressionNode.DecimalLiteralExpression -> {
            WrappedResult(ToylangP1ASTNode.TypeAnnotation.DECIMAL)
        }
        is ToylangP1ASTNode.StatementNode.ExpressionNode.FunctionCallNode -> {
            when(val result = this.findType()){
                is WrappedResult<*> -> result
                is ErrorResult -> ErrorResult("Type checking for value reference came back not okay", result)
                else -> ErrorResult("Unrecognized result: $result")
            }
        }
        else -> ErrorResult("Unrecognized type of expression $this")
    }
}

fun ToylangP1ASTNode.StatementNode.findType(): Result{
    return when(this){
        is ToylangP1ASTNode.StatementNode.VariableNode -> {
            this.assignment.expression.findType()
        }
        is ToylangP1ASTNode.StatementNode.FunctionDeclNode -> {
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
        is ToylangP1ASTNode.StatementNode.ReturnStatementNode -> {
           when(val findResult = this.expression.findType()){
               is WrappedResult<*> -> findResult
               is ErrorResult -> ParserErrorResult(findResult, this.location)
               is ParserErrorResult<*> -> ParserErrorResult(findResult, this.location)
               else -> ErrorResult("Unrecognized result: $findResult")
           }
        }
        is ToylangP1ASTNode.StatementNode.ExpressionNode -> this.findType()
        else -> ErrorResult("Could not recognized statement: $this")
    }
}

fun ToylangP1ASTNode.typeChecking(): Result{
    this.walkChildren().map {
        when(it){
            is ToylangP1ASTNode.StatementNode -> {
                when(val result = it.findType()){
                    is WrappedResult<*> -> {
                        result
                    }
                    is ErrorResult -> ParserErrorResult(result, it.location)
                    else -> ErrorResult("Unrecognized result: $result")
                }
            }
            is ToylangP1ASTNode.StatementNode.ExpressionNode -> {
                when(val result = it.findType()){
                    is WrappedResult<*> -> {
                        result
                    }
                    is ErrorResult -> ParserErrorResult(result, it.location)
                    else -> ErrorResult("Unrecognized result: $result")
                }
            }
            else -> ErrorResult("Unrecognized node: $it")
        }
    }.apply {
        this.forEach {
            when(it){
                is ErrorResult -> return ErrorResult("an error occurred during type checking", it)
                is ParserErrorResult<*> -> return ErrorResult("A parser error occurred during type checking: $it")
            }
        }
    }
    return OKResult
}