package parsing

import com.couch.kotlinx.ast.Location
import com.couch.kotlinx.ast.ToylangASTNode
import com.strumenta.kolasu.model.Position

sealed class ToylangMainAST(override val location: Location): ToylangASTNode(location){
    data class RootNode(override val location: Location, val statements: List<StatementNode> = arrayListOf()): ToylangMainAST(location)
    data class TypeAnnotationNode(override val location: Location, val identifier: IdentifierNode): ToylangMainAST(location)
    data class IdentifierNode(override val location: Location, val identifier: String) : ToylangMainAST(location)
    sealed class StatementNode(override val location: Location): ToylangMainAST(location) {
        data class LetNode(
                override val location: Location,
                val identifier: IdentifierNode,
                val mutable: Boolean,
                val type: TypeAnnotationNode?,
                val assignment: AssignmentNode
        ) : StatementNode(location)
        data class AssignmentNode(override val location: Location, val expression: ExpressionNode) : StatementNode(location)
        sealed class ExpressionNode(override val location: Location) : StatementNode(location) {
            data class IntegerLiteralNode(override val location: Location, val integer: Int) : ExpressionNode(location)
            data class DecimalLiteralNode(override val location: Location, val float: Float) : ExpressionNode(location)
            sealed class StringLiteralContentNode(override val location: Location) : ExpressionNode(location) {
                data class RawStringLiteralContentNode(override val location: Location, val string: String) : StringLiteralContentNode(location)
                data class StringInterpolationNode(override val location: Location, val interpolatedExpr: ExpressionNode) : StringLiteralContentNode(location)
            }

            data class StringLiteralNode(override val location: Location, val content: List<StringLiteralContentNode>) : ExpressionNode(location)
            data class ValueReferenceNode(override val location: Location, val ident: IdentifierNode) : ExpressionNode(location)
            sealed class BinaryOperation(override val location: Location, open val left: ExpressionNode, open val right: ExpressionNode) : ExpressionNode(location) {
                data class BinaryPlusOperation(
                        override val location: Location,
                        override val left: ExpressionNode,
                        override val right: ExpressionNode
                ) : BinaryOperation(location, left, right)

                data class BinaryMinusOperation(
                        override val location: Location,
                        override val left: ExpressionNode,
                        override val right: ExpressionNode
                ) : BinaryOperation(location, left, right)

                data class BinaryMultOperation(
                        override val location: Location,
                        override val left: ExpressionNode,
                        override val right: ExpressionNode
                ) : BinaryOperation(location, left, right)

                data class BinaryDivOperation(
                        override val location: Location,
                        override val left: ExpressionNode,
                        override val right: ExpressionNode
                ) : BinaryOperation(location, left, right)
            }
            class NoneExpression(override val location: Location): ExpressionNode(location)
            class ReferenceNode(override val location: Location): ExpressionNode(location)
            class FunctionCallNode(override val location: Location, val name: String, val args: List<ExpressionNode>): ExpressionNode(location)
        }
        data class FunctionDeclNode(
                override val location: Location,
                val identifier: IdentifierNode,
                val params: List<FunctionParamNode>,
                val codeBlock: CodeblockNode,
                val returnType: TypeAnnotationNode?
        ): StatementNode(location)
        data class ReturnStatementNode(override val location: Location, val expression: ExpressionNode): StatementNode(location)
    }
    data class FunctionParamNode(override val location: Location, val identifier: IdentifierNode, val type: TypeAnnotationNode): ToylangASTNode(location)
    data class CodeblockNode(override val location: Location, val statements: List<StatementNode>): ToylangASTNode(location)
}