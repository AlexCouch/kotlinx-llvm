package com.couch.kotlinx.ast

import com.strumenta.kolasu.model.Node

typealias RootNode = ToylangASTNode.ToylangASTRootNode
typealias StatementNode = ToylangASTNode.ToylangASTStatementNode
typealias IdentifierNode = ToylangASTNode.ToylangASTStatementNode.ToylangASTIdentifierNode
typealias LetNode = ToylangASTNode.ToylangASTStatementNode.ToylangASTLetNode
typealias AssignmentNode = ToylangASTNode.ToylangASTStatementNode.ToylangASTAssignmentNode
typealias ExpressionNode = ToylangASTNode.ToylangASTStatementNode.ToylangASTExpressionNode
typealias IntegerLiteralNode = ToylangASTNode.ToylangASTStatementNode.ToylangASTExpressionNode.ToylangASTIntegerLiteralNode
typealias StringLiteralNode = ToylangASTNode.ToylangASTStatementNode.ToylangASTExpressionNode.ToylangASTStringLiteralNode
typealias StringLiteralContentNode = ToylangASTNode.ToylangASTStatementNode.ToylangASTExpressionNode.ToylangASTStringLiteralContentNode
typealias RawStringLiteralContentNode = ToylangASTNode.ToylangASTStatementNode.ToylangASTExpressionNode.ToylangASTStringLiteralContentNode.ToylangASTStringLiteralRawContentNode
typealias StringInterpolationNode = ToylangASTNode.ToylangASTStatementNode.ToylangASTExpressionNode.ToylangASTStringLiteralContentNode.ToylangASTStringLiteralInterpContentNode
typealias DecimalLiteralNode = ToylangASTNode.ToylangASTStatementNode.ToylangASTExpressionNode.ToylangASTDecimalLiteralNode
typealias ValueReferenceNode = ToylangASTNode.ToylangASTStatementNode.ToylangASTExpressionNode.ToylangASTValueReferenceNode
typealias BinaryOperation = ToylangASTNode.ToylangASTStatementNode.ToylangASTExpressionNode.ToylangASTBinaryOperationExpression
typealias BinaryPlusOperation = ToylangASTNode.ToylangASTStatementNode.ToylangASTExpressionNode.ToylangASTBinaryOperationExpression.ToylangASTBinaryPlusOperationExpression
typealias BinaryMinusOperation = ToylangASTNode.ToylangASTStatementNode.ToylangASTExpressionNode.ToylangASTBinaryOperationExpression.ToylangASTBinaryMinusOperationExpression
typealias BinaryMultOperation = ToylangASTNode.ToylangASTStatementNode.ToylangASTExpressionNode.ToylangASTBinaryOperationExpression.ToylangASTBinaryMultOperationExpression
typealias BinaryDivOperation = ToylangASTNode.ToylangASTStatementNode.ToylangASTExpressionNode.ToylangASTBinaryOperationExpression.ToylangASTBinaryDivOperationExpression
typealias FunctionDeclNode = ToylangASTNode.ToylangASTStatementNode.ToylangASTFunctionDeclarationNode
typealias ParamNode = ToylangASTNode.ToylangASTFunctionParamNode
typealias TypeNode = ToylangASTNode.ToylangASTTypeNode
typealias CodeblockNode = ToylangASTNode.ToylangASTFunctionCodeblockNode
typealias FunctionTypeNode = ToylangASTNode.ToylangASTFunctionTypeNode
typealias ReturnStatementNode = ToylangASTNode.ToylangASTStatementNode.ToylangASTReturnStatement
typealias NoneExpressionNode = ToylangASTNode.ToylangASTStatementNode.ToylangASTExpressionNode.ToylangASTNoneExpression

sealed class ToylangASTNode: Node(){
    data class ToylangASTRootNode(val statements: ArrayList<ToylangASTStatementNode> = arrayListOf()): ToylangASTNode()
    sealed class ToylangASTStatementNode: ToylangASTNode() {
        data class ToylangASTIdentifierNode(val identifier: String) : StatementNode()
        data class ToylangASTLetNode(val identifier: ToylangASTIdentifierNode, val assignment: AssignmentNode) : StatementNode()
        data class ToylangASTAssignmentNode(val expression: ToylangASTExpressionNode) : StatementNode()
        sealed class ToylangASTExpressionNode : StatementNode() {
            data class ToylangASTIntegerLiteralNode(val integer: Int) : ExpressionNode()
            data class ToylangASTDecimalLiteralNode(val float: Float) : ExpressionNode()
            sealed class ToylangASTStringLiteralContentNode : ExpressionNode() {
                data class ToylangASTStringLiteralRawContentNode(val string: String) : StringLiteralContentNode()
                data class ToylangASTStringLiteralInterpContentNode(val interpolatedExpr: ExpressionNode) : StringLiteralContentNode()
            }

            data class ToylangASTStringLiteralNode(val content: List<ToylangASTStringLiteralContentNode>) : ExpressionNode()
            data class ToylangASTValueReferenceNode(val ident: ToylangASTIdentifierNode) : ExpressionNode()
            sealed class ToylangASTBinaryOperationExpression(open val left: ToylangASTExpressionNode, open val right: ToylangASTExpressionNode) : ExpressionNode() {
                data class ToylangASTBinaryPlusOperationExpression(
                        override val left: ToylangASTExpressionNode,
                        override val right: ToylangASTExpressionNode
                ) : BinaryOperation(left, right)

                data class ToylangASTBinaryMinusOperationExpression(
                        override val left: ToylangASTExpressionNode,
                        override val right: ToylangASTExpressionNode
                ) : BinaryOperation(left, right)

                data class ToylangASTBinaryMultOperationExpression(
                        override val left: ToylangASTExpressionNode,
                        override val right: ToylangASTExpressionNode
                ) : BinaryOperation(left, right)

                data class ToylangASTBinaryDivOperationExpression(
                        override val left: ToylangASTExpressionNode,
                        override val right: ToylangASTExpressionNode
                ) : BinaryOperation(left, right)
            }
            class ToylangASTNoneExpression: ExpressionNode()
        }
        data class ToylangASTFunctionDeclarationNode(
                val identifier: IdentifierNode,
                val params: List<ParamNode>,
                val codeBlock: CodeblockNode,
                val returnType: FunctionTypeNode
        ): ToylangASTStatementNode()
        data class ToylangASTReturnStatement(val expression: ExpressionNode): StatementNode()
    }
    data class ToylangASTFunctionParamNode(val identifier: IdentifierNode, val type: TypeNode): ToylangASTNode()
    data class ToylangASTFunctionCodeblockNode(val statements: List<StatementNode>, val returnStatement: ReturnStatementNode): ToylangASTNode()
    data class ToylangASTTypeNode(val typeIdentifier: IdentifierNode): ToylangASTNode()
    data class ToylangASTFunctionTypeNode(val type: TypeNode): ToylangASTNode()
}