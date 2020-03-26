package parsing.hir

import kotlinx.io.core.BytePacketBuilder
import kotlinx.io.core.buildPacket
import kotlinx.io.core.readBytes
import kotlinx.serialization.*
import kotlinx.serialization.internal.SerialClassDescImpl
import kotlinx.serialization.modules.EmptyModule
import kotlinx.serialization.modules.SerialModule
import org.antlr.v4.kotlinruntime.ast.Point
import parsing.ToylangMainAST
import parsing.ast.Location

enum class ToylangTarget{
    VM,
    LLVM
}

@Serializer(HIR::class)
object ToylangMainASTBytecodeSerialization : KSerializer<ToylangMainAST.RootNode>{
    override val descriptor: SerialDescriptor
        get() = SerialClassDescImpl("HIR")

    override fun serialize(encoder: Encoder, obj: ToylangMainAST.RootNode) {
        if(encoder !is ToylangMainASTBytecode.ToylangMainASTBytecodeBytecodeWriter) return
        encoder.beginFile()
        encoder.encodeMetadata(obj.metadata?.target ?: ToylangTarget.VM, obj.metadata?.fileLocation ?: "")
        encoder.beginData()
        obj.statements.forEach {
            encoder.encodeStatement(it)
        }
        encoder.endData()
        encoder.endFile()

    }

    override fun deserialize(decoder: Decoder): ToylangMainAST.RootNode {
        TODO("Not yet implemented")
    }
}

class ToylangMainASTBytecode internal constructor(override val context: SerialModule = EmptyModule): AbstractSerialFormat(context), BinaryFormat{

    internal class ToylangMainASTBytecodeBytecodeWriter(val encoder: ToylangMainASTBytecodeBytecodeEncoder): ElementValueEncoder(){
        override val context: SerialModule
            get() = this@ToylangMainASTBytecodeBytecodeWriter.context

        fun beginFile(){
            this.encoder.startFile()
        }

        override fun encodeInt(value: Int) {
            this.encoder.integerLiteral(value)
        }

        override fun encodeFloat(value: Float) {
            this.encoder.decimalLiteral(value)
        }

        override fun encodeString(value: String) {
            this.encoder.rawStringLiteral(value)
        }

        fun encodeMetadata(target: ToylangTarget, location: String){
            this.encoder.startFileDesc()
            this.encoder.startTargetType()
            when(target){
                ToylangTarget.VM -> this.encoder.setTargetTypeVM()
                ToylangTarget.LLVM -> this.encoder.setTargetTypeLLVM()
            }
            this.encoder.endTargetType()
            this.encoder.startFileLocation()
            this.encoder.setFileLocation(location)
            this.encoder.endFileLocation()
            this.encoder.endFileDesc()
        }

        fun beginData(){
            this.encoder.startData()
        }

        fun endData(){
            this.encoder.endData()
        }

        fun encodeStatement(statement: ToylangMainAST.StatementNode){
            this.encoder.beginStatement()
            when(statement){
                is ToylangMainAST.StatementNode.VariableNode.GlobalVariableNode -> this.encodeGlobalVariable(statement)
                is ToylangMainAST.StatementNode.VariableNode.LocalVariableNode -> this.encodeLocalVariable(statement)
                is ToylangMainAST.StatementNode.FunctionDeclNode -> this.encodeFunction(statement)
                is ToylangMainAST.StatementNode.AssignmentNode -> this.encodeAssignment(statement)
                is ToylangMainAST.StatementNode.ExpressionNode -> this.encodeExpression(statement)
                is ToylangMainAST.StatementNode.ReturnStatementNode -> this.encodeTerminalReturn(statement)
            }
            this.encoder.endStatement()
        }

        fun encodeStringLiteral(stringContent: ToylangMainAST.StatementNode.ExpressionNode.StringLiteralNode){
            this.encoder.startStringLiteral()
            stringContent.content.forEach {
                when(it){
                    is ToylangMainAST.StatementNode.ExpressionNode.StringLiteralContentNode.RawStringLiteralContentNode -> this.encodeString(it.string)
                    is ToylangMainAST.StatementNode.ExpressionNode.StringLiteralContentNode.StringInterpolationNode -> {
                        this.encoder.beginStringInterp()
                        this.encodeExpression(it.interpolatedExpr)
                        this.encoder.endStringInterp()
                    }
                }
            }
            this.encoder.encodeLocation(stringContent.location)
            this.encoder.endStringLiteral()
        }

        fun encodeExpression(expression: ToylangMainAST.StatementNode.ExpressionNode){
            this.encoder.beginExpression()
            when(expression){
                is ToylangMainAST.StatementNode.ExpressionNode.IntegerLiteralNode -> {
                    this.encodeInt(expression.integer)
                    this.encoder.encodeLocation(expression.location)
                }
                is ToylangMainAST.StatementNode.ExpressionNode.DecimalLiteralNode -> {
                    this.encodeFloat(expression.float)
                    this.encoder.encodeLocation(expression.location)
                }
                is ToylangMainAST.StatementNode.ExpressionNode.StringLiteralNode -> {
                    this.encodeStringLiteral(expression)
                    this.encoder.encodeLocation(expression.location)
                }
                is ToylangMainAST.StatementNode.ExpressionNode.ValueReferenceNode -> {
                    this.encodeValueReference(expression)
                }
                is ToylangMainAST.StatementNode.ExpressionNode.FunctionCallNode -> {
                    this.encodeFunctionCall(expression)
                }
                is ToylangMainAST.StatementNode.ExpressionNode.BinaryOperation -> {
                    this.encodeBinaryOperation(expression)
                }
            }
            this.encoder.endExpression()
        }

        fun encodeBinaryOperation(binaryOp: ToylangMainAST.StatementNode.ExpressionNode.BinaryOperation){
            this.encoder.beginBinary()
            this.encoder.binaryLeft()
            this.encodeExpression(binaryOp.left)
            this.encoder.binaryRight()
            this.encodeExpression(binaryOp.right)
            when(binaryOp){
                is ToylangMainAST.StatementNode.ExpressionNode.BinaryOperation.BinaryPlusOperation -> this.encoder.binaryPlus()
                is ToylangMainAST.StatementNode.ExpressionNode.BinaryOperation.BinaryMinusOperation -> this.encoder.binaryMinus()
                is ToylangMainAST.StatementNode.ExpressionNode.BinaryOperation.BinaryMultOperation -> this.encoder.binaryMult()
                is ToylangMainAST.StatementNode.ExpressionNode.BinaryOperation.BinaryDivOperation -> this.encoder.binaryDiv()
            }
            this.encoder.encodeLocation(binaryOp.location)
            this.encoder.endBinary()
        }

        fun encodeFunctionCall(functionCallNode: ToylangMainAST.StatementNode.ExpressionNode.FunctionCallNode){
            this.encoder.beginFunctionCall()
            this.encoder.identifier(functionCallNode.name)
            this.encoder.beginFunctionCallArgs()
            functionCallNode.args.forEach {
                this.encodeExpression(it)
            }
            this.encoder.endFunctionCallArgs()
            this.encoder.encodeLocation(functionCallNode.location)
            this.encoder.endFunctionCall()
        }

        fun encodeValueReference(valueReferenceNode: ToylangMainAST.StatementNode.ExpressionNode.ValueReferenceNode){
            this.encoder.beginReference()
            this.encoder.encodeReference(valueReferenceNode.ident.identifier)
            this.encoder.endReference()
            this.encoder.encodeLocation(valueReferenceNode.location)
        }

        fun encodeAssignment(assignment: ToylangMainAST.StatementNode.AssignmentNode){
            this.encoder.beginAssignment()
            this.encodeExpression(assignment.expression)
            this.encoder.encodeLocation(assignment.location)
            this.encoder.endAssignment()
        }

        fun encodeGlobalVariable(globalVar: ToylangMainAST.StatementNode.VariableNode.GlobalVariableNode){
            this.encoder.beginGlobalVar()
            this.encoder.identifier(globalVar.identifier.identifier)
            this.encodeAssignment(globalVar.assignment)
            this.encoder.encodeLocation(globalVar.location)
            this.encoder.endGlobalVar()
        }

        fun encodeLocalVariable(localVar: ToylangMainAST.StatementNode.VariableNode.LocalVariableNode){
            this.encoder.beginLocalVar()
            this.encoder.identifier(localVar.identifier.identifier)
            this.encodeAssignment(localVar.assignment)
            this.encoder.encodeLocation(localVar.location)
            this.encoder.endLocalVar()
        }

        fun encodeFunction(function: ToylangMainAST.StatementNode.FunctionDeclNode){
            this.encoder.beginFunction()
            this.encoder.identifier(function.identifier.identifier)
            function.params.forEach {
                this.encoder.beginFunctionParam()
                this.encoder.setFunctionParam(it.identifier.identifier, it.type.identifier.identifier)
                this.encoder.endFunctionParam()
            }
            this.encoder.beginFunctionReturnType()
            this.encoder.setFunctionReturnType(function.returnType?.identifier?.identifier ?: "Unit")
            this.encoder.endFunctionReturnType()
            this.encoder.beginFunctionBody()
            this.encoder.beginBlock()
            function.codeBlock.statements.forEach {
                this.encodeStatement(it)
            }
            this.encoder.endBlock()
            this.encoder.endFunctionBody()
            this.encoder.encodeLocation(function.location)
            this.encoder.endFunction()
        }

        fun encodeTerminalReturn(returnStatement: ToylangMainAST.StatementNode.ReturnStatementNode){
            this.encoder.beginTerminalReturn()
            this.encodeExpression(returnStatement.expression)
            this.encoder.encodeLocation(returnStatement.location)
            this.encoder.endTerminalReturn()
        }

        fun endFile() {
            this.encoder.endFile()
        }
    }

    inner class ToylangMainASTBytecodeBytecodeEncoder(val packet: BytePacketBuilder){
        //Metadata
        fun startFile() = packet.writeByte((BEGIN_FILE and 0xff).toByte())
        fun endFile() = packet.writeByte((END_FILE and 0xff).toByte())
        fun startFileDesc() = packet.writeByte((BEGIN_FILE_DESC and 0xff).toByte())
        fun startFileLocation() = packet.writeByte((BEGIN_FILE_LOCATION and 0xff).toByte())
        fun setFileLocation(location: String) = packet.writeStringUtf8(location)
        fun endFileLocation() = packet.writeByte((END_FILE_LOCATION and 0xff).toByte())
        fun startTargetType() = packet.writeByte((BEGIN_TARGET_TYPE and 0xff).toByte())
        fun setTargetTypeVM() = packet.writeByte((TARGET_TYPE_VM and 0xff).toByte())
        fun setTargetTypeLLVM() = packet.writeByte((TARGET_TYPE_LLVM and 0xff).toByte())
        fun endTargetType() = packet.writeByte((END_TARGET_TYPE and 0xff).toByte())
        fun endFileDesc() = packet.writeByte((END_FILE_DESC and 0xff).toByte())

        //Data
        fun startData() = packet.writeByte((BEGIN_DATA and 0xff).toByte())
        fun endData() = packet.writeByte((END_DATA and 0xff).toByte())
        fun identifier(identifier: String){
            packet.writeByte((IDENTIFIER and 0xff).toByte())
            packet.writeStringUtf8(identifier)
        }
        fun encodeLocation(location: Location){
            LocationEncoder(this.packet){
                this.encodeStartLocation(location.start)
                this.encodeStopLocation(location.stop)
            }
        }

        //Statements
        fun beginStatement() = packet.writeByte((BEGIN_STATEMENT and 0xff).toByte())
        fun endStatement() = packet.writeByte((END_STATEMENT and 0xff).toByte())

        //Variables
        //Global var
        fun beginGlobalVar() = packet.writeByte((BEGIN_GLOBAL_VAR and 0xff).toByte())
        fun endGlobalVar() = packet.writeByte((END_GLOBAL_VAR and 0xff).toByte())
        //Local var
        fun beginLocalVar() = packet.writeByte((BEGIN_LOCAL_VAR and 0xff).toByte())
        fun endLocalVar() = packet.writeByte((END_LOCAL_VAR and 0xff).toByte())

        //Blocks
        fun beginBlock() = packet.writeByte((BEGIN_BLOCK and 0xff).toByte())
        fun endBlock() = packet.writeByte((END_BLOCK and 0xff).toByte())

        //Function
        fun beginFunction() = packet.writeByte((BEGIN_FUNCTION and 0xff).toByte())
        fun endFunction() = packet.writeByte((END_FUNCTION and 0xff).toByte())

        //Function param
        fun beginFunctionParam() = packet.writeByte((BEGIN_FUNCTION_PARAM and 0xff).toByte())
        fun setFunctionParam(name: String, typename: String){
            this.identifier(name)
            this.identifier(typename)
        }
        fun endFunctionParam() = packet.writeByte((END_FUNCTION_PARAM and 0xff).toByte())

        //Function return type
        fun beginFunctionReturnType() = packet.writeByte((BEGIN_FUNCTION_RETURN_TYPE and 0xff).toByte())
        fun setFunctionReturnType(typename: String) = packet.writeStringUtf8(typename)
        fun endFunctionReturnType() = packet.writeByte((END_FUNCTION_RETURN_TYPE and 0xff).toByte())

        //Function body
        fun beginFunctionBody() = packet.writeByte((BEGIN_FUNCTION_BODY and 0xff).toByte())
        fun endFunctionBody() = packet.writeByte((END_FUNCTION_BODY and 0xff).toByte())

        //Expressions
        fun beginExpression() = packet.writeByte((BEGIN_EXPRESSION and 0xff).toByte())
        fun endExpression() = packet.writeByte((END_EXPRESSION and 0xff).toByte())

        //Literals
        //Integer
        fun integerLiteral(integer: Int) {
            packet.writeByte((INTEGER_LITERAL and 0xff).toByte())
            packet.writeInt(integer)
        }
        //Decimal
        fun decimalLiteral(decimal: Float) {
            packet.writeByte((DECIMAL_LITERAL and 0xff).toByte())
            packet.writeFloat(decimal)
        }

        //Strings
        fun startStringLiteral() = packet.writeByte((BEGIN_STRING_LITERAL and 0xff).toByte())
        //Raw string
        fun rawStringLiteral(string: String) {
            packet.writeByte((BEGIN_RAW_STRING_LITERAL and 0xff).toByte())
            packet.writeStringUtf8(string)
            packet.writeByte((END_RAW_STRING_LITERAL and 0xff).toByte())
        }
        //String interpolation
        fun beginStringInterp() = packet.writeByte((BEGIN_INTERP_STRING_LITERAL and 0xff).toByte())
        fun endStringInterp() = packet.writeByte((END_INTERP_STRING_LITERAL and 0xff).toByte())
        fun endStringLiteral() = packet.writeByte((END_STRING_LITERAL and 0xff).toByte())

        fun beginReference() = packet.writeByte((BEGIN_VALUE_REFERENCE and 0xff).toByte())
        fun endReference() = packet.writeByte((END_VALUE_REFERENCE and 0xff).toByte())

        //Reference
        fun encodeReference(identifier: String) {
            this.identifier(identifier)
        }

        //Function call
        fun beginFunctionCall() = packet.writeByte((BEGIN_FUNCTION_CALL and 0xff).toByte())
        fun beginFunctionCallArgs() = packet.writeByte((BEGIN_FUNCTION_CALL_ARGS and 0xff).toByte())
        fun endFunctionCallArgs() = packet.writeByte((END_FUNCTION_CALL_ARGS and 0xff).toByte())

        fun endFunctionCall() = packet.writeByte((END_FUNCTION_CALL and 0xff).toByte())

        fun beginBinary() = packet.writeByte((BEGIN_BINARY and 0xff).toByte())
        fun binaryLeft() = packet.writeByte((BINARY_LEFT and 0xff).toByte())
        fun binaryRight() = packet.writeByte((BINARY_RIGHT and 0xff).toByte())
        fun binaryPlus() = packet.writeByte((BINARY_PLUS and 0xff).toByte())
        fun binaryMinus() = packet.writeByte((BINARY_MINUS and 0xff).toByte())
        fun binaryMult() = packet.writeByte((BINARY_MULT and 0xff).toByte())
        fun binaryDiv() = packet.writeByte((BINARY_DIV and 0xff).toByte())
        fun endBinary() = packet.writeByte((END_BINARY and 0xff).toByte())

        //Terminals
        //Return
        fun beginTerminalReturn() = packet.writeByte((BEGIN_TERMINAL_RETURN and 0xff).toByte())
        fun endTerminalReturn() = packet.writeByte((END_TERMINAL_RETURN and 0xff).toByte())

        //Misc
        //Assignment
        fun beginAssignment() = packet.writeByte((BEGIN_ASSIGNMENT and 0xff).toByte())
        fun endAssignment() = packet.writeByte((END_ASSIGNMENT and 0xff).toByte())
    }
    inner class LocationEncoder internal constructor(val output: BytePacketBuilder, block: LocationEncoder.()->Unit){
        init{
            this.startLocation(block)
            this.endLocation()
        }
        private fun startLocation(block: LocationEncoder.()->Unit){
            output.writeByte((BEGIN_LOCATION and 0xff).toByte())
            this.block()
        }
        private fun endLocation(){
            output.writeByte((END_LOCATION and 0xff).toByte())
        }
        fun encodeStartLocation(start: Point) {
            output.writeByte((BEGIN_LOCATION_START and 0xff).toByte())
            output.writeInt(start.line)
            output.writeInt(start.column)
            output.writeByte((END_LOCATION_START and 0xff).toByte())
        }
        fun encodeStopLocation(stop: Point){
            output.writeByte((BEGIN_LOCATION_STOP and 0xff).toByte())
            output.writeInt(stop.line)
            output.writeInt(stop.column)
            output.writeByte((END_LOCATION_STOP and 0xff).toByte())
        }

    }
    companion object: BinaryFormat{
        //Metadata stuff
         const val BEGIN_FILE = 0xfe
         const val BEGIN_FILE_DESC = 0x11
         const val BEGIN_FILE_LOCATION = 0x61
         const val END_FILE_LOCATION = 0x62
         const val BEGIN_TARGET_TYPE = 0x63
         const val TARGET_TYPE_VM = 0x71
         const val TARGET_TYPE_LLVM = 0x72
         const val END_TARGET_TYPE = 0x64
         const val END_FILE_DESC = 0x12
         const val END_FILE = 0xff
         const val BEGIN_DATA = 0x13
         const val END_DATA = 0x14
        //Location
         const val BEGIN_LOCATION = 0x15
         const val BEGIN_LOCATION_START = 0x65
         const val END_LOCATION_START = 0x66
         const val BEGIN_LOCATION_STOP = 0x67
         const val END_LOCATION_STOP = 0x68
         const val END_LOCATION = 0x16
        //Misc
         const val IDENTIFIER = 0x17

        //Statements
         const val BEGIN_STATEMENT = 0x20
         const val END_STATEMENT = 0x2f
         const val BEGIN_GLOBAL_VAR = 0x21
         const val END_GLOBAL_VAR = 0x22
         const val BEGIN_FUNCTION = 0x23
         const val BEGIN_FUNCTION_PARAM = 0x51
         const val END_FUNCTION_PARAM = 0x52
         const val BEGIN_FUNCTION_RETURN_TYPE = 0x53
         const val END_FUNCTION_RETURN_TYPE = 0x54
         const val BEGIN_FUNCTION_BODY = 0x55
         const val END_FUNCTION_BODY = 0x56
         const val END_FUNCTION = 0x24
         const val BEGIN_ASSIGNMENT = 0x25
         const val END_ASSIGNMENT = 0x26
         const val BEGIN_TERMINAL_RETURN = 0x27
         const val END_TERMINAL_RETURN = 0x28

        //Local stuff
        const val BEGIN_LOCAL_VAR = 0x29
        const val END_LOCAL_VAR = 0x2a

        //Blocks
        const val BEGIN_BLOCK = 0x18
        const val END_BLOCK = 0x19

        //Expressions
        const val BEGIN_EXPRESSION = 0x30
        const val INTEGER_LITERAL = 0x31
        const val DECIMAL_LITERAL = 0x32
        //String stuff
        const val BEGIN_STRING_LITERAL = 0x33
        const val BEGIN_RAW_STRING_LITERAL = 0x71
        const val BEGIN_INTERP_STRING_LITERAL = 0x72
        const val END_INTERP_STRING_LITERAL = 0x73
        const val END_RAW_STRING_LITERAL = 0x74
        const val END_STRING_LITERAL = 0x34

        //Binary expressions
        const val BEGIN_BINARY = 0x35
        const val BINARY_LEFT = 0x41
        const val BINARY_RIGHT = 0x42
        const val BINARY_PLUS = 0x43
        const val BINARY_MINUS = 0x44
        const val BINARY_MULT = 0x45
        const val BINARY_DIV = 0x46
        const val END_BINARY = 0x36

        //Function call stuff
        const val BEGIN_FUNCTION_CALL = 0x37
        const val BEGIN_FUNCTION_CALL_ARGS = 0x48
        const val END_FUNCTION_CALL_ARGS = 0x49
        const val END_FUNCTION_CALL = 0x3a

        //Rest of expressions
        const val BEGIN_VALUE_REFERENCE = 0x3b
        const val END_VALUE_REFERENCE = 0x3c
        const val END_EXPRESSION = 0x3f

        private val plain = ToylangMainASTBytecode()
        override val context: SerialModule
            get() = plain.context

        override fun <T> dump(serializer: SerializationStrategy<T>, obj: T): ByteArray = plain.dump(serializer, obj)

        override fun <T> load(deserializer: DeserializationStrategy<T>, bytes: ByteArray): T = plain.load(deserializer, bytes)
    }

    override fun <T> dump(serializer: SerializationStrategy<T>, obj: T): ByteArray {
        val bytepacket = BytePacketBuilder()
        val dumper = ToylangMainASTBytecodeBytecodeWriter(ToylangMainASTBytecodeBytecodeEncoder(bytepacket))
        dumper.encode(serializer, obj)
        return bytepacket.build().readBytes()
    }

    override fun <T> load(deserializer: DeserializationStrategy<T>, bytes: ByteArray): T {
        TODO("Not yet implemented")
    }
}