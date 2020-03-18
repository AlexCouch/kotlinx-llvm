package parsing.p1

import com.couch.kotlinx.ast.Location
import com.couch.kotlinx.ast.ToylangASTNode
import com.couch.kotlinx.parsing.p1.Context
import com.couch.kotlinx.parsing.p1.FunctionContext
import com.couch.kotlinx.parsing.p1.GlobalContext
import com.couch.kotlinx.parsing.p1.ProvidesContext
import org.antlr.v4.kotlinruntime.ast.Point
import parsing.typeck.Type

sealed class ToylangP1ASTNode(override val location: Location): ToylangASTNode(location){
    data class RootNode(override val location: Location, val statements: List<StatementNode>, override val context: GlobalContext): ToylangP1ASTNode(location), ProvidesContext
    data class TypeAnnotation(override val location: Location, val typeName: String): ToylangP1ASTNode(location){
        companion object {
            val NONE = Type("None")
            val STRING = Type("String")
            val INTEGER = Type("INTEGER")
            val DECIMAL = Type("DECIMAL")
        }
    }
    sealed class StatementNode(override val location: Location): ToylangP1ASTNode(location){
        sealed class VariableNode(
                override val location: Location,
                open val identifier: String,
                open val mutable: Boolean,
                open val type: TypeAnnotation,
                open val assignment: AssignmentNode
        ): StatementNode(location){
            data class GlobalVariableNode(
                    override val location: Location,
                    override val identifier: String,
                    override val mutable: Boolean,
                    override val type: TypeAnnotation,
                    override val assignment: AssignmentNode
            ): VariableNode(
                    location,
                    identifier,
                    mutable,
                    type,
                    assignment
            )
            data class LocalVariableNode(
                    override val location: Location,
                    override val identifier: String,
                    override val mutable: Boolean,
                    override val type: TypeAnnotation,
                    override val assignment: AssignmentNode
            ): VariableNode(
                    location,
                    identifier,
                    mutable,
                    type,
                    assignment
            )
        }
        data class AssignmentNode(override val location: Location, val expression: ExpressionNode): StatementNode(location)
        sealed class ExpressionNode(override val location: Location): StatementNode(location){
            data class IntegerLiteralExpression(override val location: Location,val integer: Int): ExpressionNode(location)
            data class DecimalLiteralExpression(override val location: Location, val decimal: Float): ExpressionNode(location)
            data class StringLiteralExpression(override val location: Location, val content: List<StringLiteralContentNode>): ExpressionNode(location)
            sealed class StringLiteralContentNode(override val location: Location): ExpressionNode(location){
                data class StringLiteralInterpolationNode(override val location: Location, val expression: ExpressionNode): StringLiteralContentNode(location)
                data class StringLiteralRawNode(override val location: Location, val content: String): StringLiteralContentNode(location)
            }
            sealed class BinaryExpressionNode(override val location: Location, open val left: ExpressionNode, open val right: ExpressionNode): ExpressionNode(location){
                data class PlusNode(override val location: Location,override val left: ExpressionNode, override val right: ExpressionNode): BinaryExpressionNode(location, left, right)
                data class MinusNode(override val location: Location,override val left: ExpressionNode, override val right: ExpressionNode): BinaryExpressionNode(location, left, right)
                data class DivNode(override val location: Location,override val left: ExpressionNode, override val right: ExpressionNode): BinaryExpressionNode(location, left, right)
                data class MultiplyNode(override val location: Location,override val left: ExpressionNode, override val right: ExpressionNode): BinaryExpressionNode(location, left, right)
            }
            sealed class GroupedExpression(override val location: Location, val innerExpression: ExpressionNode): ExpressionNode(location)
            data class ValueReferenceNode(override val location: Location,val identifier: String) : ExpressionNode(location)
            data class FunctionCallNode(override val location: Location,val identifier: String, val args: List<ExpressionNode>): ExpressionNode(location)
        }
        data class ReturnStatementNode(override val location: Location,val expression: ExpressionNode): StatementNode(location)
        data class FunctionDeclNode(
                override val location: Location,
                val identifier: String,
                val type: TypeAnnotation,
                val codeblock: CodeBlockNode,
                override val context: FunctionContext
        ): StatementNode(location), ProvidesContext
    }
    data class FunctionParamNode(override val location: Location, val identifier: String, val type: TypeAnnotation): ToylangP1ASTNode(location)
    data class CodeBlockNode(override val location: Location, val statements: List<StatementNode>): ToylangP1ASTNode(location)
}