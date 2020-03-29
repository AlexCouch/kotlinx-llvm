package parsing.mir

import kotlinx.io.core.ByteReadPacket
import parsing.hir.ToylangMainASTBytecode
import Result
import WrappedResult
import ErrorResult
import OKResult
import kotlinx.io.core.BytePacketBuilder
import kotlinx.io.core.readBytes
import kotlinx.serialization.*
import kotlinx.serialization.internal.SerialClassDescImpl
import kotlinx.serialization.modules.EmptyModule
import kotlinx.serialization.modules.SerialModule
import org.antlr.v4.kotlinruntime.ast.Point
import parsing.ParserErrorResult
import parsing.ast.Location
import parsing.hir.readStringUntilDelimiter

sealed class Symbol(open val name: String, open val typename: String){
    sealed class VariableSymbol(override val name: String, override val typename: String): Symbol(name, typename){
        data class GlobalVariableSymbol(override val name: String, override val typename: String): VariableSymbol(name, typename)
        data class LocalVariableSymbol(override val name: String, override val typename: String): VariableSymbol(name, typename)
    }
    data class FunctionSymbol(override val name: String, override val typename: String): Symbol(name, typename)
}
sealed class ScopeContext(open val parent: ScopeContext?){
    data class GlobalScopeContext(val globalVariables: ArrayList<Symbol.VariableSymbol.GlobalVariableSymbol> = arrayListOf(), val functions: ArrayList<Symbol.FunctionSymbol> = arrayListOf()): ScopeContext(null){
        override fun findSymbol(name: String, specifier: String): Result {
            if(specifier.matches(Regex("function"))){
                return WrappedResult(this.functions.find { it.name == name } ?: "None")
            }
            if(specifier.matches(Regex("globalvar"))){
                return WrappedResult(this.globalVariables.find { it.name == name } ?: "None")
            }
            val found = this.globalVariables.find {
                it.name == name
            } ?: this.functions.find {
                it.name == name
            }
            return WrappedResult(found ?: "None")
        }
    }
    data class LocalScopeContext(override val parent: ScopeContext?, val localVariables: ArrayList<Symbol.VariableSymbol.LocalVariableSymbol>): ScopeContext(parent) {
        override fun findSymbol(name: String, specifier: String): Result {
            if(specifier.matches(Regex("localvar"))){
                return WrappedResult(this.localVariables.find { it.name == name } ?: "None")
            }
            if(specifier.matches(Regex("function"))){
                return WrappedResult(this.parent?.findSymbol(name, specifier) ?: "None")
            }
            if(specifier.matches(Regex("globalvar"))){
                return WrappedResult(this.parent?.findSymbol(name, specifier) ?: "None")
            }
            val found = this.localVariables.find {
                it.name == name
            } ?: this.parent?.findSymbol(name)
            return WrappedResult(found ?: "None")
        }
    }

    abstract fun findSymbol(name: String, specifier: String = ""): Result
}

@Serializer(ByteReadPacket::class)
object MIRBytecodeSerialization : KSerializer<ByteReadPacket>{
    override val descriptor: SerialDescriptor
        get() = SerialClassDescImpl("MIR")

    private var currentContext = ScopeContext.GlobalScopeContext()

    override fun serialize(encoder: Encoder, obj: ByteReadPacket) {
        if(encoder !is MIRBytecode.MIRBytecodeWriter) return
        encoder.beginFile()
        while(obj.readByte() != (ToylangMainASTBytecode.END_FILE and 0xff).toByte()){
            when(val result = this.decodeMetadata(encoder, obj)){
                is ErrorResult -> {
                    println("An error occurred while converting file metadata from HIR to MIR")
                    println(result)
                    return
                }
            }
            when(val result = this.decodeFileData(encoder, obj)){
                is ErrorResult -> {
                    println("An error occurred while converting file data from HIR to MIR")
                    println(result)
                    return
                }
            }
        }
        encoder.endFile()
    }

    private fun decodeFileData(encoder: MIRBytecode.MIRBytecodeWriter, packet: ByteReadPacket): Result{
        var byte = packet.readByte()
        while(byte != (ToylangMainASTBytecode.END_DATA and 0xff).toByte()){
            when(byte){
                (ToylangMainASTBytecode.BEGIN_STATEMENT and 0xff).toByte() -> when(val result = this.decodeStatement(encoder, packet)){
                    is ErrorResult -> return ErrorResult("An error occurred while converting statement from HIR to MIR", result)
                }
            }
            byte = packet.readByte()
        }
        return OKResult
    }

    private fun decodeStatement(encoder: MIRBytecode.MIRBytecodeWriter, packet: ByteReadPacket): Result{
        var byte = packet.readByte()
        encoder.beginStatement()
        while(byte != (ToylangMainASTBytecode.END_STATEMENT and 0xff).toByte()){
            when(byte){
                (ToylangMainASTBytecode.BEGIN_GLOBAL_VAR and 0xff).toByte() -> when(val result = this.decodeGlobalVar(encoder, packet)){
                    is ErrorResult -> return ErrorResult("An error occurred while converting global variable from HIR to MIR", result)
                }
                (ToylangMainASTBytecode.BEGIN_FUNCTION and 0xff).toByte() -> when(val result = this.decodeFunction(encoder, packet)){
                    is ErrorResult -> return ErrorResult("An error occurred while converting function from HIR to MIR", result)
                }
                (ToylangMainASTBytecode.BEGIN_LOCAL_VAR and 0xff).toByte() -> when(val result = this.decodeLocalVar(encoder, packet)){
                    is ErrorResult -> return ErrorResult("An error occurred while converting local variable from HIR to MIR", result)
                }
                (ToylangMainASTBytecode.BEGIN_TERMINAL_RETURN and 0xff).toByte() -> when(val result = this.decodeTerminalReturn(encoder, packet)){
                    is ErrorResult -> return ErrorResult("An error occurred while converting terminal return from HIR to MIR", result)
                }
                (ToylangMainASTBytecode.BEGIN_ASSIGNMENT and 0xff).toByte() -> when(val result = this.decodeAssignment(encoder, packet)){
                    is ErrorResult -> return ErrorResult("An error occurred while converting assignment from HIR to MIR", result)
                }
                (ToylangMainASTBytecode.BEGIN_EXPRESSION and 0xff).toByte() -> when(val result = this.decodeExpression(encoder, packet)){
                    is ErrorResult -> return ErrorResult("An error occurred while converting expression from HIR to MIR", result)
                }
            }
            byte = packet.readByte()
        }
        return OKResult
    }

    private fun decodeTerminalReturn(encoder: MIRBytecode.MIRBytecodeWriter, packet: ByteReadPacket): Result{
        var byte = packet.readByte()
        if(byte != (ToylangMainASTBytecode.BEGIN_EXPRESSION and 0xff).toByte()) return ErrorResult("Expected a BEGIN_EXPRESSION opcode but instead got $byte")
        encoder.beginTerminalReturn()
        val result = this.decodeExpression(encoder, packet)
        byte = packet.readByte()
        if(byte != (ToylangMainASTBytecode.BEGIN_LOCATION and 0xff).toByte()) return ErrorResult("Expected a BEGIN_LOCATION opcode but instead got $byte")
        this.decodeLocation(packet)
        return result
    }

    private fun decodeFunction(encoder: MIRBytecode.MIRBytecodeWriter, packet: ByteReadPacket): Result{
        var byte = packet.readByte()
        if(byte != (ToylangMainASTBytecode.IDENTIFIER and 0xff).toByte()) return ErrorResult("Malformed bytecode: Expected an IDENTIFIER opcode but instead got $byte")
        val size = packet.readInt()
        val name = packet.readTextExact(size)

        byte = packet.readByte()
        val params = arrayListOf<MIRBytecode.FunctionParam>()
        var returnType = ""
        while(byte != (ToylangMainASTBytecode.BEGIN_FUNCTION_BODY and 0xff).toByte()){
            when(byte){
                (ToylangMainASTBytecode.BEGIN_FUNCTION_PARAM and 0xff).toByte() -> {
                    if(packet.readByte() != (ToylangMainASTBytecode.IDENTIFIER and 0xff).toByte()) return ErrorResult("Malformed bytecode: Expected an IDENTIFIER opcode but instead got $byte")
                    val pnameSize = packet.readInt()
                    val pname = packet.readTextExact(pnameSize)
                    if(packet.readByte() != (ToylangMainASTBytecode.IDENTIFIER and 0xff).toByte()) return ErrorResult("Malformed bytecode: Expected an IDENTIFIER opcode but instead got $byte")
                    val ptypenameSize = packet.readInt()
                    val ptypename = packet.readTextExact(ptypenameSize)
                    params.add(MIRBytecode.FunctionParam(pname, ptypename))
                }
                (ToylangMainASTBytecode.BEGIN_FUNCTION_RETURN_TYPE and 0xff).toByte() -> {
                    val pnameSize = packet.readInt()
                    returnType = packet.readTextExact(pnameSize)
                }
            }
            byte = packet.readByte()
        }
        when(val result = this.currentContext.findSymbol(name, "function")){
            is WrappedResult<*> -> {
                when(result.t){
                    "None" -> {
                        this.currentContext.functions.add(Symbol.FunctionSymbol(name, returnType))
                    }
                    else -> return ErrorResult("A function with identifier $name already exists")
                }
            }
        }
        encoder.beginFunction(name, returnType, params.toTypedArray())
        byte = packet.readByte()
        if(byte != (ToylangMainASTBytecode.BEGIN_BLOCK and 0xff).toByte()) return ErrorResult("Malformed bytecode: Expected a BEGIN_BLOCK but instead got $byte")
        byte = packet.readByte()
        while(byte != (ToylangMainASTBytecode.END_FUNCTION_BODY and 0xff).toByte()){
            when(byte){
                (ToylangMainASTBytecode.BEGIN_STATEMENT and 0xff).toByte() -> when(val result = this.decodeStatement(encoder, packet)){
                    is ErrorResult -> return ErrorResult("An error occurred while converting statement from HIR to MIR", result)
                }
            }
            byte = packet.readByte()
        }
        byte = packet.readByte()
        if(byte != (ToylangMainASTBytecode.BEGIN_LOCATION and 0xff).toByte()) return ErrorResult("Malformed bytecode: Expected a BEGIN_LOCATION but instead got $byte")
        this.decodeLocation(packet)
        byte = packet.readByte()
        if(byte != (ToylangMainASTBytecode.END_FUNCTION and 0xff).toByte()) return ErrorResult("Malformed bytecode: Expected a BEGIN_FUNCTION_BODY but instead got $byte")
        encoder.endFunction()
        return OKResult
    }

    private fun decodeGlobalVar(encoder: MIRBytecode.MIRBytecodeWriter, packet: ByteReadPacket): Result{
        val (identifier, typename) = when(val result = this.decodeVariableHeader(packet)){
            is WrappedResult<*> -> {
                when(result.t){
                    is Pair<*, *> -> result.t
                    else -> return ErrorResult("did not get identifier or typename from variable header")
                }
            }
            is ErrorResult -> return ErrorResult("An error occurred while decoding variable header", result)
            else -> return ErrorResult("Unrecognized result: $result")
        }
        if(identifier !is String || typename !is String) return ErrorResult("Header result did not come back with strings: ${identifier!!::class.java}, ${typename!!::class.java}")
        when(val result = this.currentContext.findSymbol(identifier, "globalvar")){
            is WrappedResult<*> -> {
                when(result.t){
                    "None" -> this.currentContext.globalVariables.add(Symbol.VariableSymbol.GlobalVariableSymbol(identifier, typename))
                    else -> return ErrorResult("A symbol in current context already exists with identifier: $identifier and it is ${result.t}")
                }
            }
        }
        encoder.beginGlobalVariable(identifier, typename)
        when(val result = this.decodeAssignment(encoder, packet)){
            is ErrorResult -> return ErrorResult("An error occurred while decoding variable assignment", result)
        }

        val byte = packet.readByte()
        if(byte != (ToylangMainASTBytecode.BEGIN_LOCATION and 0xff).toByte()) return ErrorResult("Malformed bytecode: Expected a BEGIN_LOCATION but instead got $byte")
        this.decodeLocation(packet)
        return OKResult
    }

    private fun decodeVariableHeader(packet: ByteReadPacket): Result{
        var identifier = ""
        var typename = ""
        var byte = packet.readByte()
        if(byte == (ToylangMainASTBytecode.IDENTIFIER and 0xff).toByte()) {
            val identsize = packet.readInt()
            identifier = packet.readTextExact(identsize)
            byte = packet.readByte()
            if(byte != (ToylangMainASTBytecode.TYPENAME and 0xff).toByte()) return ErrorResult("Malformed bytecode: Expected an TYPENAME opcode but instead got $byte")
            val typenameSize = packet.readInt()
            typename = packet.readTextExact(typenameSize)
            return WrappedResult(identifier to typename)
        }
        return ErrorResult("Could not decode identifier; TYPENAME byte expected but instead got $byte")
    }

    private fun decodeLocalVar(encoder: MIRBytecode.MIRBytecodeWriter, packet: ByteReadPacket): Result{
        val (identifier, typename) = when(val result = this.decodeVariableHeader(packet)){
            is WrappedResult<*> -> {
                when(result.t){
                    is Pair<*, *> -> result.t
                    else -> return ErrorResult("did not get identifier or typename from variable header")
                }
            }
            is ErrorResult -> return ErrorResult("An error occurred while decoding variable header", result)
            else -> return ErrorResult("Unrecognized result: $result")
        }
        if(identifier !is String || typename !is String) return ErrorResult("Header result did not come back with strings: ${identifier!!::class.java}, ${typename!!::class.java}")
        when(val result = this.currentContext.findSymbol(identifier, "localvar")){
            is WrappedResult<*> -> {
                when(result.t){
                    "None" -> this.currentContext.globalVariables.add(Symbol.VariableSymbol.GlobalVariableSymbol(identifier, typename))
                    else -> return ErrorResult("A symbol in current context already exists with identifier: $identifier and it is ${result.t}")
                }
            }
        }
        encoder.beginLocalVariable(identifier, typename)
        when(val result = this.decodeAssignment(encoder, packet)){
            is ErrorResult -> return ErrorResult("An error occurred while decoding variable assignment", result)
        }

        val byte = packet.readByte()
        if(byte != (ToylangMainASTBytecode.BEGIN_LOCATION and 0xff).toByte()) return ErrorResult("Malformed bytecode: Expected a BEGIN_LOCATION but instead got $byte")
        this.decodeLocation(packet)
        return OKResult
    }

    private fun decodeAssignment(encoder: MIRBytecode.MIRBytecodeWriter, packet: ByteReadPacket): Result{
        var byte = packet.readByte()
        while(byte != (ToylangMainASTBytecode.END_ASSIGNMENT and 0xff).toByte()){
            when(byte){
                (ToylangMainASTBytecode.BEGIN_EXPRESSION and 0xff).toByte() -> this.decodeExpression(encoder, packet)
                (ToylangMainASTBytecode.BEGIN_LOCATION and 0xff).toByte() -> this.decodeLocation(packet)
            }
            byte = packet.readByte()
        }
        return OKResult
    }

    private fun decodeExpression(encoder: MIRBytecode.MIRBytecodeWriter, packet: ByteReadPacket): Result{
        var byte = packet.readByte()
        encoder.beginExpression()
        while(byte != (ToylangMainASTBytecode.END_EXPRESSION and 0xff).toByte()){
            when(byte){
                (ToylangMainASTBytecode.INTEGER_LITERAL and 0xff).toByte() -> {
                    val int = packet.readInt()
                    encoder.encodeInt(int)
                }
                (ToylangMainASTBytecode.DECIMAL_LITERAL and 0xff).toByte() -> {
                    val float = packet.readFloat()
                    encoder.encodeFloat(float)
                }
                (ToylangMainASTBytecode.BEGIN_STRING_LITERAL and 0xff).toByte() -> {
                    this.decodeStringLiteral(encoder, packet)
                }
                (ToylangMainASTBytecode.BEGIN_FUNCTION_CALL and 0xff).toByte() -> {
                    when(val result = this.decodeFunctionCall(encoder, packet)){
                        is ErrorResult -> return ErrorResult("An error occurred while converting function call from HIR to MIR")
                    }
                }
                (ToylangMainASTBytecode.BEGIN_VALUE_REFERENCE and 0xff).toByte() -> {
                    this.decodeValueReference(encoder, packet)
                }
                (ToylangMainASTBytecode.BEGIN_BINARY and 0xff).toByte() -> {
                    this.decodeBinaryOperation(encoder, packet)
                }
            }
            byte = packet.readByte()
        }
        return OKResult
    }

    private fun decodeBinaryOperation(encoder: MIRBytecode.MIRBytecodeWriter, packet: ByteReadPacket): Result{
        var byte = packet.readByte()
        if(byte != (ToylangMainASTBytecode.BINARY_LEFT and 0xff).toByte()) return ErrorResult("Malformed bytecode: Excepted BINARY_LEFT but instead got $byte")
        when(val result = this.decodeExpression(encoder, packet)){
            is ErrorResult -> return ErrorResult("An error occurred while decoding left hand of binary operation", result)
        }
        byte = packet.readByte()
        if(byte != (ToylangMainASTBytecode.BINARY_RIGHT and 0xff).toByte()) return ErrorResult("Malformed bytecode: Excepted BINARY_RIGHT but instead got $byte")
        when(val result = this.decodeExpression(encoder, packet)){
            is ErrorResult -> return ErrorResult("An error occurred while decoding right hand of binary operation", result)
        }
        byte = packet.readByte()
        encoder.binaryOperation(byte)
        byte=packet.readByte()
        if(byte != (ToylangMainASTBytecode.BEGIN_LOCATION and 0xff).toByte()) return ErrorResult("Malformed bytecode: Excepted BEGIN_LOCATION but instead got $byte")
        this.decodeLocation(packet)
        byte=packet.readByte()
        if(byte != (ToylangMainASTBytecode.END_BINARY and 0xff).toByte()) return ErrorResult("Malformed bytecode: Excepted END_BINARY but instead got $byte")
        return OKResult
    }

    private fun decodeLocation(packet: ByteReadPacket): Result{
        var byte = packet.readByte()
        if(byte != (ToylangMainASTBytecode.BEGIN_LOCATION_START and 0xff).toByte()) return ErrorResult("Malformed bytecode: Expected a BEGIN_LOCATION_START opcode but instead got $byte")
        val startline = packet.readInt()
        val startcol = packet.readInt()
        byte = packet.readByte()
        if(byte != (ToylangMainASTBytecode.END_LOCATION_START and 0xff).toByte()) return ErrorResult("Malformed bytecode: Expected a END_LOCATION_START opcode but instead got $byte")
        byte = packet.readByte()
        if(byte != (ToylangMainASTBytecode.BEGIN_LOCATION_STOP and 0xff).toByte()) return ErrorResult("Malformed bytecode: Expected a BEGIN_LOCATION_STOP opcode but instead got $byte")
        val stopline = packet.readInt()
        val stopcol = packet.readInt()
        byte = packet.readByte()
        if(byte != (ToylangMainASTBytecode.END_LOCATION_STOP and 0xff).toByte()) return ErrorResult("Malformed bytecode: Expected a END_LOCATION_STOP opcode but instead got $byte")
        byte = packet.readByte()
        if(byte != (ToylangMainASTBytecode.END_LOCATION and 0xff).toByte()) return ErrorResult("Malformed bytecode: Could not get location from bytecode")
        return WrappedResult(Location(Point(startline, startcol), Point(stopline, stopcol)))

    }

    private fun decodeValueReference(encoder: MIRBytecode.MIRBytecodeWriter, packet: ByteReadPacket): Result{
        var byte = packet.readByte()
        if(byte != (ToylangMainASTBytecode.IDENTIFIER and 0xff).toByte()) return ErrorResult("Malformed bytecode: Expected an IDENTIFIER opcode but instead got $byte")
        val length = packet.readInt()
        val name = packet.readTextExact(length)
        byte = packet.readByte()
        if(byte != (ToylangMainASTBytecode.END_VALUE_REFERENCE and 0xff).toByte()) return ErrorResult("Malformed bytecode: Expected an END_VALUE_REFERENCE opcode but instead got $byte")
        byte = packet.readByte()
        if(byte != (ToylangMainASTBytecode.BEGIN_LOCATION and 0xff).toByte()) return ErrorResult("Malformed bytecode: Expected an BEGIN_LOCATION opcode but instead got $byte")
        val location = when(val result = this.decodeLocation(packet)){
            is WrappedResult<*> -> {
                when(result.t){
                    is Location -> result.t
                    else -> return ErrorResult("Did not get location for location decoder")
                }
            }
            is ErrorResult -> return ParserErrorResult(result, Location(Point(1, 1), Point(1, 1)))
            else -> return ErrorResult("Unrecognized result: $result")
        }
        when(val result = this.currentContext.findSymbol(name)){
            is WrappedResult<*> -> {
                when(result.t){
                    "None" -> {
                        encoder.encodeValueReference(name)
                    }
                    else -> return ParserErrorResult(ErrorResult("No symbol with identifier $name was found in current context"), location)
                }
            }
        }

        return OKResult
    }

    private fun decodeFunctionCall(encoder: MIRBytecode.MIRBytecodeWriter, packet: ByteReadPacket): Result{
        var byte = packet.readByte()
        if(byte != (ToylangMainASTBytecode.IDENTIFIER and 0xff).toByte()) return ErrorResult("Malformed bytecode: Expected an IDENTIFIER opcode but instead got $byte")
        val length = packet.readInt()
        val name = packet.readTextExact(length)
        when(val result = this.currentContext.findSymbol(name)){
            is WrappedResult<*> -> {
                when(result.t){
                    "None" -> return ErrorResult("No function with identifier $name was found in current context.")
                }
            }
        }
        encoder.beginFunctionCall(name)
        while(byte != (ToylangMainASTBytecode.END_FUNCTION_CALL_ARGS and 0xff).toByte()){
            when(byte){
                (ToylangMainASTBytecode.BEGIN_EXPRESSION and 0xff).toByte() -> this.decodeExpression(encoder, packet)
                (ToylangMainASTBytecode.BEGIN_LOCATION and 0xff).toByte() -> {
                    when(val result = this.decodeLocation(packet)){
                        is WrappedResult<*> -> {
                            when(result.t){
                                is Location -> result.t
                            }
                        }
                    }
                }
            }

            byte = packet.readByte()
        }
        encoder.endFunctionCall()
        return OKResult
    }

    private fun decodeStringLiteral(encoder: MIRBytecode.MIRBytecodeWriter, packet: ByteReadPacket): Result{
        var byte = packet.readByte()
        encoder.beginStringLiteral()
        while(byte != (ToylangMainASTBytecode.END_STRING_LITERAL and 0xff).toByte()){
            when(byte){
                (ToylangMainASTBytecode.BEGIN_RAW_STRING_LITERAL and 0xff).toByte() -> {
                    val length = packet.readInt()
                    encoder.encodeRawStringLiteral(packet.readTextExact(length))
                }
                (ToylangMainASTBytecode.BEGIN_INTERP_STRING_LITERAL and 0xff).toByte() -> {
                    encoder.beginStringInterp()
                    this.decodeExpression(encoder, packet)
                    encoder.endStringInterp()
                }
                (ToylangMainASTBytecode.BEGIN_LOCATION and 0xff).toByte() -> {
                    this.decodeLocation(packet)
                }
            }
            byte = packet.readByte()
        }
        encoder.endStringLiteral()
        return OKResult
    }

    private fun decodeMetadata(encoder: MIRBytecode.MIRBytecodeWriter, packet: ByteReadPacket): Result{
        var byte = packet.readByte()
        if(byte != (ToylangMainASTBytecode.BEGIN_FILE_DESC and 0xff).toByte()) return ErrorResult("Malformed bytecode: Expected BEGIN_FILE_DESC but instead got $byte")

        byte = packet.readByte()
        if(byte != (ToylangMainASTBytecode.BEGIN_TARGET_TYPE and 0xff).toByte()) return ErrorResult("Malformed bytecode: Expected BEGIN_TARGET_TYPE but instead got $byte")
        byte = packet.readByte()
        if(byte != (ToylangMainASTBytecode.TARGET_TYPE_VM and 0xff).toByte() &&
                byte != (ToylangMainASTBytecode.TARGET_TYPE_LLVM and 0xff).toByte()) return ErrorResult("Malformed bytecode: Expected TARGET_TYPE_LLVM or TARGET_TYPE_VM but instead got $byte")

        val targetType = byte
        byte = packet.readByte()
        if(byte != (ToylangMainASTBytecode.END_TARGET_TYPE and 0xff).toByte()) return ErrorResult("Malformed bytecode: Expected END_TARGET_TYPE but instead got $byte")

        byte = packet.readByte()
        if(byte != (ToylangMainASTBytecode.BEGIN_FILE_LOCATION and 0xff).toByte()) return ErrorResult("Malformed bytecode: Expected BEGIN_FILE_LOCATION but instead got $byte")
        val size = packet.readInt()
        val fileLocation = packet.readTextExact(size)
        byte = packet.readByte()
        if(byte != (ToylangMainASTBytecode.END_FILE_LOCATION and 0xff).toByte()) return ErrorResult("Malformed bytecode: Expected END_FILE_LOCATION but instead got $byte")
        encoder.encodeMetadata(targetType, fileLocation)
        byte = packet.readByte()
        if(byte != (ToylangMainASTBytecode.END_FILE_DESC and 0xff).toByte()) return ErrorResult("Malformed bytecode: Expected END_FILE but instead got $byte")
        return OKResult
    }

    override fun deserialize(decoder: Decoder): ByteReadPacket {
        TODO("Not yet implemented")
    }
}

class MIRBytecode internal constructor(override val context: SerialModule = EmptyModule): AbstractSerialFormat(context), BinaryFormat{

    internal class MIRBytecodeWriter(val encoder: MIRBytecodeEncoder): ElementValueEncoder(){
        override val context: SerialModule
            get() = this@MIRBytecodeWriter.context

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

        fun encodeMetadata(target: Byte, location: String){
            this.encoder.setTargetType(target)
            this.encoder.setFileLocation(location)
        }

        fun beginStatement() = this.encoder.beginStatement()

        /*
            BEGIN_STRING_LITERAL BEGIN_RAW_STRING [raw_string_literal] END_RAW_STRING BEGIN_STRING_INTERP BEGIN_BLOCK [expression] END_BLOCK END_STRING_INTERP END_STRING_LITERAL
         */
        fun beginExpression() = this.encoder.beginExpression()
        fun beginStringLiteral() = this.encoder.startStringLiteral()
        fun encodeRawStringLiteral(string: String) = this.encoder.rawStringLiteral(string)
        fun beginStringInterp(){
            this.encoder.beginStringInterp()
        }
        fun endStringInterp(){
            this.encoder.endStringInterp()
        }
        fun endStringLiteral() = this.encoder.endStringLiteral()

        fun binaryOperation(opcode: Byte){
            when(opcode){
                (ToylangMainASTBytecode.BINARY_PLUS and 0xff).toByte() -> this.encoder.binaryPlus()
                (ToylangMainASTBytecode.BINARY_MINUS and 0xff).toByte() -> this.encoder.binaryMinus()
                (ToylangMainASTBytecode.BINARY_MULT and 0xff).toByte() -> this.encoder.binaryMult()
                (ToylangMainASTBytecode.BINARY_DIV and 0xff).toByte() -> this.encoder.binaryDiv()
            }
        }

        fun beginFunctionCall(name: String){
            this.encoder.functionCall(name)
        }

        fun endFunctionCall(){
            this.encoder.endFunctionCall()
        }

        fun encodeValueReference(name: String){
            this.encoder.reference(name)
        }

        fun encodeAssignment(){
            this.encoder.assignment()
        }

        fun beginGlobalVariable(name: String, typename: String){
            this.encoder.beginGlobalVar(name, typename)
            this.encodeAssignment()
        }

        fun beginLocalVariable(name: String, typename: String){
            this.encoder.beginLocalVar(name, typename)
            this.encodeAssignment()
        }

        fun beginFunction(name: String, typename: String, params: Array<FunctionParam>){
            this.encoder.function(name, typename, params)
            this.encoder.beginBlock()
        }

        fun endFunction(){
            this.encoder.endBlock()
        }

        fun beginTerminalReturn(){
            this.encoder.terminalReturn()
        }

        fun endFile() {
            this.encoder.endFile()
        }
    }

    data class FunctionParam(val name: String, val typename: String)

    inner class MIRBytecodeEncoder(val packet: BytePacketBuilder){
        //Metadata
        fun startFile() = packet.writeByte((BEGIN_FILE and 0xff).toByte())
        fun endFile() = packet.writeByte((END_FILE and 0xff).toByte())
        fun writeString(string: String, size: Int){
            packet.writeInt(size)
            packet.writeStringUtf8(string)
        }
        fun setFileLocation(location: String) {
            packet.writeByte((FILE_NAME and 0xff).toByte())
            writeString(location, location.length)
        }
        fun setTargetType(targetType: Byte){
            packet.writeByte((TARGET_TYPE and 0xff).toByte())
            packet.writeByte(targetType)
        }

        //Statements
        fun beginStatement() = packet.writeByte((STATEMENT and 0xff).toByte())
        fun beginGlobalVar(name: String, typename: String){
            packet.writeByte((GLOBAL_VAR and 0xff).toByte())
            writeString(name, name.length)
            writeString(typename, typename.length)
        }
        fun beginLocalVar(name: String, typename: String){
            packet.writeByte((LOCAL_VAR and 0xff).toByte())
            writeString(name, name.length)
            writeString(typename, typename.length)
        }
        fun function(name: String, typename: String, params: Array<FunctionParam>){
            packet.writeByte((FUNCTION and 0xff).toByte())
            writeString(name, name.length)
            writeString(typename, typename.length)
            packet.writeByte((BEGIN_FUNCTION_PARAMS and 0xff).toByte())
            params.forEach {
                writeString(it.name, it.name.length)
                writeString(it.typename, it.typename.length)
            }
            packet.writeByte((END_FUNCTION_PARAMS and 0xff).toByte())
        }

        //Blocks
        fun beginBlock() = packet.writeByte((BEGIN_BLOCK and 0xff).toByte())
        fun endBlock() = packet.writeByte((END_BLOCK and 0xff).toByte())

        fun beginExpression() = packet.writeByte((EXPRESSION and 0xff).toByte())
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
            writeString(string, string.length - 1)
            packet.writeByte((END_RAW_STRING_LITERAL and 0xff).toByte())
        }
        //String interpolation
        fun beginStringInterp() = packet.writeByte((BEGIN_INTERP_STRING_LITERAL and 0xff).toByte())
        fun endStringInterp() = packet.writeByte((END_INTERP_STRING_LITERAL and 0xff).toByte())
        fun endStringLiteral() = packet.writeByte((END_STRING_LITERAL and 0xff).toByte())

        fun reference(name: String) {
            packet.writeByte((REFERENCE and 0xff).toByte())
            writeString(name, name.length)
        }

        //Function call
        fun functionCall(name: String){
            packet.writeByte((FUNCTION_CALL and 0xff).toByte())
            writeString(name, name.length)
        }
        fun endFunctionCall() = packet.writeByte((END_FUNCTION_CALL and 0xff).toByte())

        fun binaryPlus() = packet.writeByte((BINARY_PLUS and 0xff).toByte())
        fun binaryMinus() = packet.writeByte((BINARY_MINUS and 0xff).toByte())
        fun binaryMult() = packet.writeByte((BINARY_MULT and 0xff).toByte())
        fun binaryDiv() = packet.writeByte((BINARY_DIV and 0xff).toByte())

        //Terminals
        //Return
        fun terminalReturn() = packet.writeByte((TERMINAL_RETURN and 0xff).toByte())

        //Misc
        //Assignment
        fun assignment() = packet.writeByte((ASSIGNMENT and 0xff).toByte())
    }

    companion object: BinaryFormat{
        //Metadata stuff
        const val BEGIN_FILE = 0xfe
        const val FILE_NAME = 0x11
        const val TARGET_TYPE = 0x12
        const val END_FILE = 0xff
        const val STATEMENT = 0x20

        //Statements
        //Global variable
        /*
            GLOBAL_VAR [identifier] [typename] [expression]
         */
        const val GLOBAL_VAR = 0x21

        /*
            Function
            FUNCTION [identifier] [params_list_start] [param_identifier] [param_typename] ... [params_list_end] [return_typename] BEGIN_BLOCK [statements] ... END_BLOCK
         */
        const val FUNCTION = 0x22
        const val BEGIN_FUNCTION_PARAMS = 0x23
        const val END_FUNCTION_PARAMS = 0x24

        const val ASSIGNMENT = 0x25
        const val TERMINAL_RETURN = 0x26

        //Local stuff
        const val LOCAL_VAR = 0x27

        //Blocks
        const val BEGIN_BLOCK = 0x28
        const val END_BLOCK = 0x29

        //Expressions
        const val EXPRESSION = 0x30
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
        /*
            [left_expression] [right_expression] [binary_op]
         */
        const val BINARY_PLUS = 0x35
        const val BINARY_MINUS = 0x36
        const val BINARY_MULT = 0x37
        const val BINARY_DIV = 0x38

        //Function call stuff
        const val FUNCTION_CALL = 0x39
        const val END_FUNCTION_CALL = 0x40

        //Rest of expressions
        const val REFERENCE = 0x42

        private val plain = MIRBytecode()
        override val context: SerialModule
            get() = plain.context

        override fun <T> dump(serializer: SerializationStrategy<T>, obj: T): ByteArray = plain.dump(serializer, obj)

        override fun <T> load(deserializer: DeserializationStrategy<T>, bytes: ByteArray): T = plain.load(deserializer, bytes)
    }

    override fun <T> dump(serializer: SerializationStrategy<T>, obj: T): ByteArray {
        val bytepacket = BytePacketBuilder()
        val dumper = MIRBytecodeWriter(MIRBytecodeEncoder(bytepacket))
        dumper.encode(serializer, obj)
        return bytepacket.build().readBytes()
    }

    override fun <T> load(deserializer: DeserializationStrategy<T>, bytes: ByteArray): T {
        TODO("Not yet implemented")
    }
}