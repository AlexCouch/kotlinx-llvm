package parsing.hir

import PrettyPrinter
import buildPrettyString
import kotlinx.io.core.ByteReadPacket
import kotlinx.io.core.Input
import Result
import WrappedResult
import ErrorResult
import OKResult
import kotlinx.serialization.internal.HexConverter
import parsing.ParserErrorResult

fun PrettyPrinter.printFileDescription(bytes: ByteReadPacket): Result{
    var byte = bytes.readByte()
    loop@while(byte != (ToylangMainASTBytecode.END_FILE_DESC and 0xff).toByte()){
        when(byte){
            (ToylangMainASTBytecode.BEGIN_TARGET_TYPE and 0xff).toByte() -> {
                var targetTypeByte = bytes.readByte()
                while(targetTypeByte != (ToylangMainASTBytecode.END_TARGET_TYPE and 0xff).toByte()) {
                    when (targetTypeByte) {
                        (ToylangMainASTBytecode.TARGET_TYPE_VM and 0xff).toByte() -> {
                            this.append("vm@")
                        }
                        (ToylangMainASTBytecode.TARGET_TYPE_LLVM and 0xff).toByte() -> {
                            this.append("llvm@")
                        }
                    }
                    targetTypeByte = bytes.readByte()
                }
            }
            (ToylangMainASTBytecode.BEGIN_FILE_LOCATION and 0xff).toByte() -> {
                val filename = bytes.readStringUntilDelimiter((ToylangMainASTBytecode.END_FILE_LOCATION and 0xff).toByte())
                this.append("$filename'")
            }
        }
        byte = bytes.readByte()
    }
    return OKResult
}

fun PrettyPrinter.printFunctionCall(packet: ByteReadPacket): Result{
    if(packet.readByte() == (ToylangMainASTBytecode.IDENTIFIER and 0xff).toByte()){
        val ident = packet.readStringUntilDelimiter((ToylangMainASTBytecode.BEGIN_FUNCTION_CALL_ARGS and 0xff).toByte())
        this.append("call.$ident(")
        var byte = packet.readByte()
        var firstArgParsed = false
        loop@while(byte != (ToylangMainASTBytecode.END_FUNCTION_CALL_ARGS and 0xff).toByte()) {
            when(byte){
                (ToylangMainASTBytecode.BEGIN_FUNCTION_CALL_ARGS and 0xff).toByte() -> {}
                (ToylangMainASTBytecode.BEGIN_LOCATION and 0xff).toByte() -> {
                    this.printLocation(packet)
                }
                (ToylangMainASTBytecode.END_EXPRESSION and 0xff).toByte() -> {
                    if(!firstArgParsed) firstArgParsed = true
                }
                else ->{
                    if(firstArgParsed) this.append(", ")
                    this.printExpression(packet)
                }
            }
            byte = packet.readByte()
        }
        this.append(")")
        if(packet.readByte() == (ToylangMainASTBytecode.BEGIN_LOCATION and 0xff).toByte()){
            this.printLocation(packet)
        }
    }
    return OKResult
}

fun PrettyPrinter.printBinaryOperation(packet: ByteReadPacket): Result{
    var byte = packet.readByte()
    if(byte != (ToylangMainASTBytecode.BINARY_LEFT and 0xff).toByte()) {
        return ErrorResult("Expected a binary left opcode but instead got $byte")
    }
    byte = packet.readByte()
    if(byte != (ToylangMainASTBytecode.BEGIN_EXPRESSION and 0xff).toByte()) {
        return ErrorResult("Expected a begin expression opcode but instead got $byte")
    }
    val leftPrint = buildPrettyString {
        when (val result = this.printExpression(packet)) {
            is ErrorResult -> return ErrorResult("An error occurred while stringifying left expression of binary operation", result)
        }
    }
    byte = packet.readByte()
    if(byte != (ToylangMainASTBytecode.END_EXPRESSION and 0xff).toByte()) {
        return ErrorResult("Expected an end expression opcode but instead got $byte")
    }
    byte = packet.readByte()
    if(byte != (ToylangMainASTBytecode.BINARY_RIGHT and 0xff).toByte()) {
        return ErrorResult("Expected a binary right opcode but instead got $byte")
    }
    byte = packet.readByte()
    if(byte != (ToylangMainASTBytecode.BEGIN_EXPRESSION and 0xff).toByte()) {
        return ErrorResult("Expected a begin expression opcode but instead got $byte")
    }
    val rightPrint = buildPrettyString {
        when(val result = this.printExpression(packet)){
            is ErrorResult -> return ErrorResult("An error occurred while stringifying right expression of binary operation", result)
        }
    }
    byte = packet.readByte()
    if(byte != (ToylangMainASTBytecode.END_EXPRESSION and 0xff).toByte()) {
        return ErrorResult("Expected an end expression opcode but instead got $byte")
    }
    when(packet.readByte()){
        (ToylangMainASTBytecode.BINARY_PLUS and 0xff).toByte() -> {
            this.append("op.add $leftPrint, $rightPrint")
        }
        (ToylangMainASTBytecode.BINARY_MINUS and 0xff).toByte() -> {
            this.append("op.sub $leftPrint, $rightPrint")
        }
        (ToylangMainASTBytecode.BINARY_MULT and 0xff).toByte() -> {
            this.append("op.mul $leftPrint, $rightPrint")
        }
        (ToylangMainASTBytecode.BINARY_DIV and 0xff).toByte() -> {
            this.append("op.div $leftPrint, $rightPrint")
        }
    }
    byte = packet.readByte()
    if(byte == (ToylangMainASTBytecode.BEGIN_LOCATION and 0xff).toByte()){
        this.printLocation(packet)
    }
    byte = packet.readByte()
    if(byte != (ToylangMainASTBytecode.END_BINARY and 0xff).toByte()){
        return ErrorResult("Maformed byte packet: Expected an end binary opcode but instead got ${HexConverter.printHexBinary(byteArrayOf(byte))}")
    }
    return OKResult
}

fun PrettyPrinter.printExpression(packet: ByteReadPacket): Result{
    when(packet.readByte()){
        (ToylangMainASTBytecode.INTEGER_LITERAL and 0xff).toByte() -> {
            val intValue = packet.readInt()
            this.append("int $intValue")
            val next = packet.readByte()
            if(next == (ToylangMainASTBytecode.BEGIN_LOCATION and 0xff).toByte()){
                this.printLocation(packet)
            }
        }
        (ToylangMainASTBytecode.DECIMAL_LITERAL and 0xff).toByte() -> {
            val decimalValue = packet.readFloat()
            this.append("decimal $decimalValue")
            val next = packet.readByte()
            if(next == (ToylangMainASTBytecode.BEGIN_LOCATION and 0xff).toByte()){
                this.printLocation(packet)
            }
        }
        (ToylangMainASTBytecode.BEGIN_STRING_LITERAL and 0xff).toByte() -> {
            when(val result = this.printString(packet)){
                is ErrorResult -> return ErrorResult("An error occurred while stringifying string literal", result)
            }
        }
        (ToylangMainASTBytecode.BEGIN_FUNCTION_CALL and 0xff).toByte() -> {
            when(val result = this.printFunctionCall(packet)){
                is ErrorResult -> return ErrorResult("An error occurred while stringifying function call", result)
            }
            if(packet.readByte() == (ToylangMainASTBytecode.END_FUNCTION_CALL and 0xff).toByte()){
                return OKResult
            }
        }
        (ToylangMainASTBytecode.BEGIN_BINARY and 0xff).toByte() -> {
            when(val result = this.printBinaryOperation(packet)){
                is ErrorResult -> return ErrorResult("An error occurred while stringifying binary opcode", result)
            }
        }
        (ToylangMainASTBytecode.BEGIN_VALUE_REFERENCE and 0xff).toByte() -> {
            if(packet.readByte() != (ToylangMainASTBytecode.IDENTIFIER and 0xff).toByte()){
                return ErrorResult("Attempted to stringify value reference, but no identifier was provided. Compiler issue.")
            }
            val ident = packet.readStringUntilDelimiter((ToylangMainASTBytecode.END_VALUE_REFERENCE and 0xff).toByte())
            this.append("ref $ident")
            val next = packet.readByte()
            if(next == (ToylangMainASTBytecode.END_VALUE_REFERENCE and 0xff).toByte()){
                if(packet.readByte() == (ToylangMainASTBytecode.BEGIN_LOCATION and 0xff).toByte()){
                    this.printLocation(packet)
                }
            }
        }
    }
    return OKResult
}

fun PrettyPrinter.printString(packet: ByteReadPacket): Result{
    var byte = packet.readByte()
    while(byte != (ToylangMainASTBytecode.END_STRING_LITERAL and 0xff).toByte()) {
        when (byte) {
            (ToylangMainASTBytecode.BEGIN_RAW_STRING_LITERAL and 0xff).toByte() -> {
                val strValue = packet.readStringUntilDelimiter((ToylangMainASTBytecode.END_RAW_STRING_LITERAL and 0xff).toByte())
                this.append("string \"$strValue\" ")
            }
            (ToylangMainASTBytecode.BEGIN_INTERP_STRING_LITERAL and 0xff).toByte() -> {
                val exprValue = this.printExpression(packet)
                this.append("weave $exprValue ")
            }
            (ToylangMainASTBytecode.BEGIN_LOCATION and 0xff).toByte() -> {
                this.printLocation(packet)
            }
        }
        byte = packet.readByte()
    }
    return OKResult
}

fun PrettyPrinter.printAssignment(packet: ByteReadPacket): Result{
    var byte = packet.readByte()
    while(byte != (ToylangMainASTBytecode.END_ASSIGNMENT and 0xff).toByte()){
        when(byte){
            (ToylangMainASTBytecode.BEGIN_ASSIGNMENT and 0xff).toByte() -> {
                this.append(" = ")
            }
            (ToylangMainASTBytecode.BEGIN_EXPRESSION and 0xff).toByte() -> {
                when(val result = this.printExpression(packet)){
                    is ErrorResult -> return ErrorResult("An error occurred while stringifying expression", result)
                }
            }
            (ToylangMainASTBytecode.BEGIN_LOCATION and 0xff).toByte() -> {
                when(val result = this.printLocation(packet)){
                    is ErrorResult -> return ErrorResult("An error occurred while stringifying location", result)
                }
            }
        }
        byte = packet.readByte()
    }
    return OKResult
}

fun PrettyPrinter.printGlobalVariable(packet: ByteReadPacket): Result{
    this.append("global.var ")
    var byte = packet.readByte()
    while(byte != (ToylangMainASTBytecode.END_GLOBAL_VAR and 0xff).toByte()){
        when(byte){
            (ToylangMainASTBytecode.IDENTIFIER and 0xff).toByte() -> {
                val ident = packet.readStringUntilDelimiter((ToylangMainASTBytecode.BEGIN_ASSIGNMENT and 0xff).toByte())
                this.append(ident)
                when(val result = this.printAssignment(packet)){
                    is ErrorResult -> return ErrorResult("An error occurred while stringifying assignment for global variable", result)
                }
            }
            (ToylangMainASTBytecode.BEGIN_LOCATION and 0xff).toByte() -> {
                this.printLocation(packet)
            }
        }
        byte = packet.readByte()
    }
    return OKResult
}

fun PrettyPrinter.printLocalVariable(packet: ByteReadPacket): Result{
    this.append("local.var ")
    var byte = packet.readByte()
    while(byte != (ToylangMainASTBytecode.END_LOCAL_VAR and 0xff).toByte()){
        when(byte){
            (ToylangMainASTBytecode.IDENTIFIER and 0xff).toByte() -> {
                val ident = packet.readStringUntilDelimiter((ToylangMainASTBytecode.BEGIN_ASSIGNMENT and 0xff).toByte())
                this.append(ident)
                when(val result = this.printAssignment(packet)){
                    is ErrorResult -> return ErrorResult("An error occurred while stringifying assignment of local variable", result)
                }
            }
            (ToylangMainASTBytecode.BEGIN_LOCATION and 0xff).toByte() -> {
                this.printLocation(packet)
                return OKResult
            }
        }
        byte = packet.readByte()
    }
    return OKResult

}

inline fun PrettyPrinter.printBlock(packet: ByteReadPacket, crossinline block: PrettyPrinter.()->Result): Result{
    var result: Result? = null
    this.indent {
        result = block()
    }
    val next = packet.readByte()
    if(next == (ToylangMainASTBytecode.END_BLOCK and 0xff).toByte()){
        return result ?: OKResult
    }
    return ErrorResult("Malformed bytecode: Expected END_BLOCK bytecode, instead got $next")
}

fun PrettyPrinter.printFunction(packet: ByteReadPacket): Result{
    this.append("global.fun ")
    var byte = packet.readByte()
    var firstParamDone = false
    while(byte != (ToylangMainASTBytecode.END_FUNCTION and 0xff).toByte()){
        when(byte){
            (ToylangMainASTBytecode.IDENTIFIER and 0xff).toByte() -> {
                val ident = packet.readStringUntilDelimiter((ToylangMainASTBytecode.BEGIN_FUNCTION_PARAM and 0xff).toByte())
                this.append("$ident(")
            }
            (ToylangMainASTBytecode.BEGIN_FUNCTION_PARAM and 0xff).toByte() -> {
                if(firstParamDone) this.append(", ")
                if(packet.readByte() != (ToylangMainASTBytecode.IDENTIFIER and 0xff).toByte()) return ErrorResult("Malformed bytecode: expected an identifier opcode for param name but found $byte")
                val ident = packet.readStringUntilDelimiter((ToylangMainASTBytecode.IDENTIFIER and 0xff).toByte())
                if(packet.readByte() != (ToylangMainASTBytecode.IDENTIFIER and 0xff).toByte()) return ErrorResult("Malformed bytecode: expected an identifier opcode for param type name but found $byte")
                val typeident = packet.readStringUntilDelimiter((ToylangMainASTBytecode.IDENTIFIER and 0xff).toByte(), (ToylangMainASTBytecode.END_FUNCTION_PARAM and 0xff).toByte())
                this.append("param.$ident type.$typeident")
                if(!firstParamDone) firstParamDone = true
            }
            (ToylangMainASTBytecode.BEGIN_FUNCTION_RETURN_TYPE and 0xff).toByte() -> {
                val ident = packet.readStringUntilDelimiter((ToylangMainASTBytecode.END_FUNCTION_RETURN_TYPE and 0xff).toByte())
                this.appendWithNewLine(") type.$ident{")
            }
            (ToylangMainASTBytecode.BEGIN_FUNCTION_BODY and 0xff).toByte() -> {
                var next = packet.tryPeek()
                if(next == (ToylangMainASTBytecode.BEGIN_BLOCK and 0xff)){
                    val result = this.printBlock(packet){
                        while(next != (ToylangMainASTBytecode.END_BLOCK and 0xff)){
                            when(val result = this.printStatement(packet)){
                                is ErrorResult -> return@printBlock ErrorResult("An error occurred while stringifying statement in function body", result)
                            }
                            this.appendWithNewLine("")
                            next = packet.tryPeek()
                        }
                        OKResult
                    }
                    when(result){
                        is ErrorResult -> return ErrorResult("An error occurred while stringifying local block of function", result)
                    }
                }
                this.appendWithNewLine("}")
            }
            (ToylangMainASTBytecode.BEGIN_LOCATION and 0xff).toByte() -> {
                this.printLocation(packet)
            }
        }
        byte = packet.readByte()
    }
    return OKResult
}

fun PrettyPrinter.printReturnStatement(packet: ByteReadPacket): Result{
    this.append("term.return ")
    var byte = packet.readByte()
    while(byte != (ToylangMainASTBytecode.END_TERMINAL_RETURN and 0xff).toByte()) {
        when (byte) {
            (ToylangMainASTBytecode.BEGIN_EXPRESSION and 0xff).toByte() -> {
                when(val result = this.printExpression(packet)){
                    is ErrorResult -> return ErrorResult("An error occurred while stringifying expression", result)
                }
            }
            (ToylangMainASTBytecode.BEGIN_LOCATION and 0xff).toByte() -> {
                when(val result = this.printLocation(packet)){
                    is ErrorResult -> return ErrorResult("An error occurred while stringifying location", result)
                }
            }
        }
        byte = packet.readByte()
    }
    return OKResult
}

fun PrettyPrinter.printStatement(packet: ByteReadPacket): Result{
    val statementStr = buildPrettyString {
        var byte = packet.readByte()
        while(byte != (ToylangMainASTBytecode.END_STATEMENT and 0xff).toByte()){
            when(byte){
                (ToylangMainASTBytecode.BEGIN_GLOBAL_VAR and 0xff).toByte() -> {
                    when(val result = this.printGlobalVariable(packet)){
                        is ErrorResult -> return ErrorResult("An error occurred while stringifying global variable", result)
                    }
                }
                (ToylangMainASTBytecode.BEGIN_FUNCTION and 0xff).toByte() -> {
                    when(val result = this.printFunction(packet)){
                        is ErrorResult -> return ErrorResult("An error occurred while stringifying function", result)
                    }
                }
                (ToylangMainASTBytecode.BEGIN_LOCAL_VAR and 0xff).toByte() -> {
                    when(val result = this.printLocalVariable(packet)){
                        is ErrorResult -> return ErrorResult("An error occurred while stringifying local variable", result)
                    }
                }
                (ToylangMainASTBytecode.BEGIN_EXPRESSION and 0xff).toByte() -> {
                    when(val result = this.printExpression(packet)){
                        is ErrorResult -> return ErrorResult("An error occurred while stringifying expression", result)
                    }
                }
                (ToylangMainASTBytecode.BEGIN_TERMINAL_RETURN and 0xff).toByte() -> {
                    when(val result = this.printReturnStatement(packet)){
                        is ErrorResult -> return ErrorResult("An error occurred while stringifying terminal return", result)
                    }
                }
            }
            byte = packet.readByte()
        }
    }
    this.append(statementStr)
    return OKResult
}

fun PrettyPrinter.printFileData(packet: ByteReadPacket): Result{
    var byte = packet.readByte()
    while(byte != (ToylangMainASTBytecode.END_DATA and 0xff).toByte()){
        when(byte) {
            (ToylangMainASTBytecode.BEGIN_STATEMENT and 0xff).toByte() -> {
                when(val result = this.printStatement(packet)){
                    is ErrorResult -> return ErrorResult("An error occurred while stringifying statement", result)
                }
                this.appendWithNewLine("")
            }
        }
        byte = packet.readByte()
    }
    return OKResult
}

fun PrettyPrinter.printLocation(packet: ByteReadPacket): Result{
//    this.append("@(")
    var byte = packet.readByte()
    if(byte != (ToylangMainASTBytecode.BEGIN_LOCATION_START and 0xff).toByte()) return ErrorResult("Malformed bytecode: Expected begin start location point opcode, but instead got $byte")
    packet.readInt()
    packet.readInt()
    byte = packet.readByte()
    if(byte != (ToylangMainASTBytecode.END_LOCATION_START and 0xff).toByte()) return ErrorResult("Malformed bytecode: Expected end start location point opcode, but instead got $byte")
    byte = packet.readByte()
    if(byte != (ToylangMainASTBytecode.BEGIN_LOCATION_STOP and 0xff).toByte()) return ErrorResult("Malformed bytecode: Expected begin stop location point opcode, but instead got $byte")
    packet.readInt()
    packet.readInt()
    byte = packet.readByte()
    if(byte != (ToylangMainASTBytecode.END_LOCATION_STOP and 0xff).toByte()) return ErrorResult("Malformed bytecode: Expected end stop location point opcode, but instead got $byte")
    packet.readByte()
    return OKResult
}

fun stringifyBytecode(byteArray: ByteArray): Result{
    return WrappedResult(buildPrettyString{
        val packet = ByteReadPacket(byteArray)
        var byte = packet.readByte()
        while(byte != (ToylangMainASTBytecode.END_FILE and 0xff).toByte()){
            when(byte){
                (ToylangMainASTBytecode.BEGIN_FILE and 0xff).toByte() -> {
                    this.append("file '")
                    when(val result = this.printFileDescription(packet)){
                        is ErrorResult -> return ErrorResult("An error occurred while stringifying metadata", result)
                    }
                }
                (ToylangMainASTBytecode.BEGIN_DATA and 0xff).toByte() -> {
                    when(val result = this.printFileData(packet)){
                        is ErrorResult -> return ErrorResult("An error occurred while stringifying file data", result)
                    }
                }
            }
            this.appendWithNewLine("")
            byte = packet.readByte()
        }
    })
}

fun Input.readStringUntilDelimiter(vararg delimiters: Byte): String = buildString{
    var byte: Byte
    while(true){
        byte = this@readStringUntilDelimiter.tryPeek().toByte()
        if(byte in delimiters){
            break
        }
        byte = this@readStringUntilDelimiter.readByte()
        this.append(byte.toChar())
    }
}