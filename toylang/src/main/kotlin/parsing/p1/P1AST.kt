package com.couch.kotlinx.parsing.p1

import com.couch.kotlinx.ast.ToylangASTNode

sealed class ToylangP1ASTNode: ToylangASTNode(){
    data class RootNode(val statements: List<StatementNode>, override val context: Context): ProvidesContext
    data class TypeAnnotation(val typeName: String): ToylangP1ASTNode()
    sealed class StatementNode: ToylangP1ASTNode(){
        data class VariableNode(val identifier: String, val type: TypeAnnotation, val assignment: AssignmentNode): ToylangASTNode()
        data class AssignmentNode(val expression: ExpressionNode): ToylangASTNode()
        sealed class ExpressionNode: ToylangASTNode(){
            data class StringLiteralExpression(val content: List<StringLiteralContentNode>): ExpressionNode()
            sealed class StringLiteralContentNode: ExpressionNode(){
                data class StringLiteralInterpolationNode(val expression: ExpressionNode): StringLiteralContentNode()
                data class StringLiteralRawNode(val content: String): StringLiteralContentNode()
            }
            sealed class BinaryExpressionNode(open val left: ExpressionNode, open val right: ExpressionNode): ExpressionNode(){
                data class PlusNode(override val left: ExpressionNode, override val right: ExpressionNode): BinaryExpressionNode(left, right)
                data class MinusNode(override val left: ExpressionNode, override val right: ExpressionNode): BinaryExpressionNode(left, right)
                data class DivNode(override val left: ExpressionNode, override val right: ExpressionNode): BinaryExpressionNode(left, right)
                data class MultiplyNode(override val left: ExpressionNode, override val right: ExpressionNode): BinaryExpressionNode(left, right)
            }
            sealed class GroupedExpression(val innerExpression: ExpressionNode): ExpressionNode()
            data class ValueReferenceNode(val identifier: String) : ExpressionNode()
            data class FunctionCallNode(val identifier: String, val functionDeclNode: FunctionDeclNode): ExpressionNode()
        }
        data class FunctionDeclNode(val identifier: String, override val context: Context): ToylangP1ASTNode(), ProvidesContext
    }
    data class FunctionParamNode(val identifier: String, val type: TypeAnnotation, val returnType: TypeAnnotation, val codeblock: CodeBlockNode): ToylangP1ASTNode()
    data class CodeBlockNode(val statements: List<StatementNode>): ToylangP1ASTNode()
}