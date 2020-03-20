package parsing.vm

import ErrorResult
import OKResult
import parsing.Parse
import parsing.hir.ToylangHIRElement
import Result
import WrappedResult
import com.couch.toylang.ToylangParser
import parsing.Context
import parsing.ParserErrorResult
import parsing.ToylangMainAST

class VMParser: Parse<ToylangHIRElement, Result>(){
    override fun parseFile(node: ToylangHIRElement, context: Context): Result {
        if(node !is ToylangHIRElement.RootNode) return ParserErrorResult(ErrorResult("Attempted to run file without root node"), node.location)
        node.statements.forEach {
            this.parseStatement(it, context)
        }
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
        return WrappedResult(str)
    }

    override fun parseExpression(node: ToylangHIRElement, context: Context): Result {
        return when(node){
            is ToylangHIRElement.StatementNode.ExpressionNode.BinaryExpressionNode -> this.parseBinaryOperation(node, context)
            is ToylangHIRElement.StatementNode.ExpressionNode.IntegerLiteralExpression -> WrappedResult(node.integer)
            is ToylangHIRElement.StatementNode.ExpressionNode.DecimalLiteralExpression -> WrappedResult(node.decimal)
            is ToylangHIRElement.StatementNode.ExpressionNode.StringLiteralExpression -> this.parseStringLiteral(node, context)
            else -> ParserErrorResult(ErrorResult("Unrecognized expression: $node"), node.location)
        }
    }
}