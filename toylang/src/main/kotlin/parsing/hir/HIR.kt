package parsing.hir

import kotlinx.io.core.ByteReadPacket
import kotlinx.io.core.buildPacket
import kotlinx.io.core.readUTF8Line
import kotlinx.io.core.readUTF8UntilDelimiter
import parsing.ast.Location
import parsing.typeck.Type

sealed class HIR(open val location: Location, open val opcode: Byte){
    class FileStart(val identifier: String, override val location: Location, val content: List<Statement>): HIR(location, 0x10)
    class FileEnd(val identifier: String, override val location: Location): HIR(location, 0x1f)
    data class Type(override val location: Location, val typename: String): HIR(location, 0x12)
    sealed class Statement(override val location: Location, override val opcode: Byte): HIR(location, opcode){
        class GlobalVarStart(val identifier: String, override val location: Location, val type: Type): Statement(location, 0x20)

        class GlobalVarEnd(val identifier: String, override val location: Location): HIR(location, 0x2f)

        class LocalVarStart(val identifier: String, override val location: Location, val type: Type): Statement(location, 0x30)
        class LocalVarEnd(val identifier: String, override val location: Location): HIR(location, 0x3f)

        class VarAssign(override val location: Location, val expression: Expression): HIR(location, 0x40)

        class FunctionStart(val identifier: String, override val location: Location, val returnType: Type): Statement(location, 0x41)
        class FunctionParamList(override val location: Location, val params: List<FunctionParam>): HIR(location, 0x42)
        data class FunctionParam(override val location: Location, val name: String, val type: Type): HIR(location, 0x43)
        data class FunctionBody(val identifier: String, override val location: Location, val statements: List<Statement>): HIR(location, 0x44)
        class FunctionEnd(val identifier: String, override val location: Location): HIR(location, 0x4f)

        sealed class Expression(override val location: Location, open val type: Type, override val opcode: Byte): Statement(location, opcode){
            data class Integer(override val location: Location, val integer: Int): Expression(location, Type(location, "Int"), 0x51)
            data class Decimal(override val location: Location, val decimal: Float, override val type: Type): Expression(location, Type(location, "Decimal"), 0x52)
            data class StringLiteral(override val location: Location, val content: List<StringContent>, override val opcode: Byte): Expression(location, Type(location, "String"), opcode)
            sealed class StringContent(override val location: Location, override val opcode: Byte): Expression(location, Type(location, "String"), opcode){
                data class StringRawContent(override val location: Location, val string: String): StringContent(location, 0x53)
                data class StringInterpolation(override val location: Location, val expression: Expression): StringContent(location, 0x54)
            }
            sealed class BinaryOperation(override val location: Location, override val opcode: Byte, override val type: Type, open val left: Expression, open val right: Expression): Expression(location, type, opcode){
                data class BinaryPlus(override val location: Location, override val type: Type, override val left: Expression, override val right: Expression): BinaryOperation(location, 0x54, type, left, right)
                data class BinaryMinus(override val location: Location, override val type: Type, override val left: Expression, override val right: Expression): BinaryOperation(location, 0x55, type, left, right)
                data class BinaryMult(override val location: Location, override val type: Type, override val left: Expression, override val right: Expression): BinaryOperation(location, 0x56, type, left, right)
                data class BinaryDiv(override val location: Location, override val type: Type, override val left: Expression, override val right: Expression): BinaryOperation(location, 0x57, type, left, right)
            }
            data class Reference(override val location: Location, override val type: Type, val identifier: String): Expression(location, type, 0x58)
            data class FunctionCall(override val location: Location, val identifier: String, override val type: Type, val args: List<Expression>): Expression(location, type, 0x59)
        }
        sealed class Terminal(override val location: Location, override val opcode: Byte): Statement(location, opcode){
            data class FunctionReturn(override val location: Location, val expression: Expression): Terminal(location, 0x60)
        }
    }
}