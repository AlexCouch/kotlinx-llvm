package com.couch.kotlinx.parsing

import com.couch.kotlinx.ast.ScopeProvider
import com.couch.kotlinx.ast.ToylangASTNode

sealed class ToylangMainAST: ToylangASTNode(){
    data class RootNode(override var scope: Scope, val statements: ArrayList<StatementNode> = arrayListOf()): ToylangMainAST(), ScopeProvider
    data class TypeAnnotationNode(val identifier: IdentifierNode): ToylangMainAST()
    data class IdentifierNode(val identifier: String) : ToylangMainAST()
    sealed class StatementNode: ToylangMainAST() {
        data class LetNode(val identifier: IdentifierNode, val mutable: Boolean, val type: TypeAnnotationNode, val assignment: AssignmentNode) : StatementNode()
        data class AssignmentNode(val expression: ExpressionNode) : StatementNode()
        sealed class ExpressionNode : StatementNode() {
            data class IntegerLiteralNode(val integer: Int) : ExpressionNode()
            data class DecimalLiteralNode(val float: Float) : ExpressionNode()
            sealed class StringLiteralContentNode : ExpressionNode() {
                data class RawStringLiteralContentNode(val string: String) : StringLiteralContentNode()
                data class StringInterpolationNode(val interpolatedExpr: ExpressionNode) : StringLiteralContentNode()
            }

            data class StringLiteralNode(val content: List<StringLiteralContentNode>) : ExpressionNode()
            data class ValueReferenceNode(val ident: IdentifierNode) : ExpressionNode()
            sealed class BinaryOperation(open val left: ExpressionNode, open val right: ExpressionNode) : ExpressionNode() {
                data class BinaryPlusOperation(
                        override val left: ExpressionNode,
                        override val right: ExpressionNode
                ) : BinaryOperation(left, right)

                data class BinaryMinusOperation(
                        override val left: ExpressionNode,
                        override val right: ExpressionNode
                ) : BinaryOperation(left, right)

                data class BinaryMultOperation(
                        override val left: ExpressionNode,
                        override val right: ExpressionNode
                ) : BinaryOperation(left, right)

                data class BinaryDivOperation(
                        override val left: ExpressionNode,
                        override val right: ExpressionNode
                ) : BinaryOperation(left, right)
            }
            class NoneExpression: ExpressionNode()
            class ReferenceNode: ExpressionNode()
            class FunctionCallNode(val name: String, val args: List<ExpressionNode>): ExpressionNode()
        }
        data class FunctionDeclNode(
                override var scope: Scope,
                val identifier: IdentifierNode,
                val params: List<FunctionParamNode>,
                val codeBlock: CodeblockNode,
                val returnType: TypeAnnotationNode?
        ): StatementNode(), ScopeProvider
        data class ReturnStatementNode(val expression: ExpressionNode): StatementNode()
    }
    data class FunctionParamNode(val identifier: IdentifierNode, val type: TypeAnnotationNode): ToylangASTNode()
    data class CodeblockNode(val statements: List<StatementNode>, val returnStatement: StatementNode.ReturnStatementNode): ToylangASTNode()
}