package parsing.vm

import kotlinx.io.core.ByteReadPacket
import kotlinx.io.core.buildPacket
import kotlinx.io.core.readBytes
import parsing.hir.readStringUntilDelimiter
import parsing.mir.MIRBytecode
import java.util.Random
import Result
import WrappedResult
import ErrorResult
import OKResult
import parsing.hir.ToylangMainASTBytecode

interface VMValue<T>{
    val value: T
}

sealed class BasicValue<T>(override val value: T): VMValue<T>{
    data class VMNamedValue<T>(val name: String, override val value: T): VMValue<T>
    sealed class VMPrimitiveValue<T>(override val value: T): BasicValue<T>(value){
        data class VMIntValue(override val value: Int): VMPrimitiveValue<Int>(value)
        data class VMDecimalValue(override val value: Float): VMPrimitiveValue<Float>(value)
        data class VMStringValue(override val value: String): VMPrimitiveValue<String>(value)
        object VMNoneValue: VMPrimitiveValue<Nothing?>(null)
    }
    data class FunctionDecl(val name: String, val returnType: String, val params: ArrayList<FunctionParam>, val localBlock: LocalBlock): BasicValue<Nothing?>(null)
}

/**
 * @param name The name or label of the block
 * @param bytes They array of bytes to be executed when the vm goes to this block
 */
data class LocalBlock(val name: String, val bytes: ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LocalBlock

        if (name != other.name) return false
        if (!bytes.contentEquals(other.bytes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + bytes.contentHashCode()
        return result
    }
}

data class FunctionParam(val name: String, val typename: String)
data class StackFrame(val name: String, val typename: String, val params: ArrayList<FunctionParam>, val block: LocalBlock){
    val stackIdices = ArrayList<Int>()
}

@ExperimentalStdlibApi
class VirtualMachine {
    private var moduleName = ""
    private val stack = ArrayDeque<VMValue<*>>()

    private val frames = ArrayDeque<StackFrame>()

    fun start(): Result{
        val mainFunction = this.stack.filterIsInstance<BasicValue.FunctionDecl>().find { it.name == "main" } ?: return ErrorResult("No main function found in $moduleName")
        val block = mainFunction.localBlock.bytes
        when(val result = evaluateBlock(ByteReadPacket(block))){
            is ErrorResult -> return ErrorResult("An error occurred while evaluating main function local block", result)
        }
        return OKResult
    }

    private fun evaluateBlock(packet: ByteReadPacket): Result{
        var byte = packet.readByte()
        while(packet.canRead()){
            when(byte){
                (MIRBytecode.STATEMENT and 0xff).toByte() -> this.evaluateStatement(packet)
                (MIRBytecode.EXPRESSION and 0xff).toByte() -> {
                    val exprValue = when(val result = this.evaluateExpression(packet)){
                        is WrappedResult<*> -> {
                            when(result){
                                is VMValue<*> -> {
                                    result.t
                                }
                                else -> return ErrorResult("Failed to evaluate expression: $result")
                            }
                        }
                        is ErrorResult -> return ErrorResult("An error occurred while evaluating expression", result)
                        else -> return ErrorResult("Unrecognized result: $result")
                    }
                    this.stack.add(exprValue as VMValue<*>)
                }
                else -> return ErrorResult("Expected with a STATEMENT or an EXPRESSION but instead got $byte")
            }
            byte = packet.readByte()
        }
        return OKResult
    }

    private fun evaluateStatement(packet: ByteReadPacket): Result{
        when(packet.readByte()){
            (MIRBytecode.LOCAL_VAR and 0xff).toByte() -> when(val result = this.evaluateLocalVariable(packet)){
                is ErrorResult -> return ErrorResult("An error occurred while evaluating local variable", result)
            }
            (MIRBytecode.GLOBAL_VAR and 0xff).toByte() -> when(val result = this.evaluateGlobalVar(packet)){
                is ErrorResult -> return ErrorResult("An error occurred while evaluating global variable", result)
            }
            (MIRBytecode.EXPRESSION and 0xff).toByte() -> when(val result = this.evaluateExpression(packet)){
                is ErrorResult -> return ErrorResult("An error occurred while evaluating expression", result)
            }
            (MIRBytecode.FUNCTION and 0xff).toByte() -> when(val result = this.initFunction(packet)){
                is ErrorResult -> return ErrorResult("An error occurred while evaluating expression", result)
            }
            (MIRBytecode.ASSIGNMENT and 0xff).toByte() -> when(val result = this.evaluateAssignment(packet)){
                is ErrorResult -> return ErrorResult("An error occurred while evaluating expression", result)
            }
        }
        return OKResult
    }

    private fun evaluateAssignment(packet: ByteReadPacket): Result{
        return when(val result = this.evaluateExpression(packet)){
            is WrappedResult<*> -> result
            is ErrorResult -> return ErrorResult("An error occurred while evaluating assignment expression", result)
            else -> return ErrorResult("Unrecognized result: $result")
        }

    }

    private fun evaluateString(stream: ByteReadPacket): Result{
        var byte = stream.readByte()
        return WrappedResult(buildString {
            do{
                when(byte){
                    (MIRBytecode.BEGIN_RAW_STRING_LITERAL and 0xff).toByte() -> this.append(stream.readStringUntilDelimiter((MIRBytecode.END_RAW_STRING_LITERAL and 0xff).toByte()))
                    (MIRBytecode.BEGIN_INTERP_STRING_LITERAL and 0xff).toByte() -> {
                        this.append(evaluateExpression(stream))
                    }
                }
                byte = stream.readByte()
            }while(byte != (MIRBytecode.END_STRING_LITERAL and 0xff).toByte())
        })
    }

    private fun evaluateBinary(byte: Byte): Result{
        val right = this.stack.removeLast()
        val left = this.stack.removeLast()
        if(right::class == left::class){
            when(byte){
                (MIRBytecode.BINARY_PLUS and 0xff).toByte() -> when(left){
                    is BasicValue.VMPrimitiveValue.VMIntValue -> this.stack.add(BasicValue.VMPrimitiveValue.VMIntValue(left.value + (right as BasicValue.VMPrimitiveValue.VMIntValue).value))
                    is BasicValue.VMPrimitiveValue.VMDecimalValue -> this.stack.add(BasicValue.VMPrimitiveValue.VMDecimalValue(left.value + (right as BasicValue.VMPrimitiveValue.VMDecimalValue).value))
                }
                (MIRBytecode.BINARY_MINUS and 0xff).toByte() -> when(left){
                    is BasicValue.VMPrimitiveValue.VMIntValue -> this.stack.add(BasicValue.VMPrimitiveValue.VMIntValue(left.value - (right as BasicValue.VMPrimitiveValue.VMIntValue).value))
                    is BasicValue.VMPrimitiveValue.VMDecimalValue -> this.stack.add(BasicValue.VMPrimitiveValue.VMDecimalValue(left.value - (right as BasicValue.VMPrimitiveValue.VMDecimalValue).value))
                }
                (MIRBytecode.BINARY_MULT and 0xff).toByte() -> when(left){
                    is BasicValue.VMPrimitiveValue.VMIntValue -> this.stack.add(BasicValue.VMPrimitiveValue.VMIntValue(left.value * (right as BasicValue.VMPrimitiveValue.VMIntValue).value))
                    is BasicValue.VMPrimitiveValue.VMDecimalValue -> this.stack.add(BasicValue.VMPrimitiveValue.VMDecimalValue(left.value * (right as BasicValue.VMPrimitiveValue.VMDecimalValue).value))
                }
                (MIRBytecode.BINARY_DIV and 0xff).toByte() -> when(left){
                    is BasicValue.VMPrimitiveValue.VMIntValue -> this.stack.add(BasicValue.VMPrimitiveValue.VMIntValue(left.value / (right as BasicValue.VMPrimitiveValue.VMIntValue).value))
                    is BasicValue.VMPrimitiveValue.VMDecimalValue -> this.stack.add(BasicValue.VMPrimitiveValue.VMDecimalValue(left.value / (right as BasicValue.VMPrimitiveValue.VMDecimalValue).value))
                }
            }
        }
        return OKResult
    }

    private fun evaluateFunction(frame: StackFrame): Result{
        frame.params.withIndex().forEach { (idx, it) ->
            val stackIdx = (this.stack.size - (frame.params.size - 1) + idx)
            when(this.stack[stackIdx]){
                is BasicValue.VMPrimitiveValue.VMIntValue -> {
                    if(it.typename != "Int") return ErrorResult("Function call argument does not match function param type: Int and ${it.typename}")
                }
                is BasicValue.VMPrimitiveValue.VMDecimalValue -> {
                    if(it.typename != "Float") return ErrorResult("Function call argument does not match function param type: Float and ${it.typename}")
                }
                is BasicValue.VMPrimitiveValue.VMStringValue -> {
                    if(it.typename != "String") return ErrorResult("Function call argument does not match function param type: String and ${it.typename}")
                }
                else -> return ErrorResult("Unrecognized type of function argument expression: ${it.typename}")
            }
        }
        val bytes = ByteReadPacket(frame.block.bytes)
        when(val result = this.evaluateBlock(bytes)){
            is ErrorResult -> return ErrorResult("An error occurred while evaluating local block", result)
        }
        return OKResult
    }

    private fun evaluateLocalVariable(packet: ByteReadPacket): Result{
        val nameLength = packet.readInt()
        val name = packet.readTextExact(nameLength)
        val typenameLength = packet.readInt()
        val typename = packet.readTextExact(typenameLength)
        var byte = packet.readByte()
        while(packet.canRead()){
            when(byte){
                (MIRBytecode.ASSIGNMENT and 0xff).toByte() -> {
                    val vmValue = when(val result = this.evaluateAssignment(packet)){
                        is WrappedResult<*> -> {
                            when(result.t){
                                is Int -> {
                                    if (typename != "Int") return ErrorResult("Local variable of type $typename does not match assignment expression type Int")
                                    BasicValue.VMNamedValue(name, result.t)
                                }
                                is Float -> {
                                    if (typename != "Float") return ErrorResult("Local variable of type $typename does not match assignment expression type Float")
                                    BasicValue.VMNamedValue(name, result.t)
                                }
                                is String -> {
                                    if (typename != "String") return ErrorResult("Local variable of type $typename does not match assignment expression type String")
                                    BasicValue.VMNamedValue(name, result.t)
                                }
                                else -> return ErrorResult("Unrecognized type of expression: ${result.t}")
                            }
                        }
                        is ErrorResult -> return ErrorResult("An error occurred while evaluating assignment of local variable")
                        else -> return ErrorResult("Unrecognized result: $result")
                    }
                    this.stack.add(vmValue)
                    this.frames.last().stackIdices.add(this.stack.size - 1)
                }
            }
            byte = packet.readByte()
        }
        return OKResult
    }

    private fun evaluateFunctionCall(stream: ByteReadPacket): Result{
        val nameLength = stream.readInt()
        val callName = stream.readTextExact(nameLength)
        val function = this.stack.filterIsInstance<BasicValue.FunctionDecl>().find{ it.name == callName } ?: return ErrorResult("There is no function by the name of $callName")
        var byte = stream.readByte()
        while(byte != (MIRBytecode.END_FUNCTION_CALL and 0xff).toByte()){
            this.evaluateExpression(stream)
            byte = stream.readByte()
        }
        val frame = StackFrame(function.name, function.returnType, function.params, function.localBlock)
        this.frames.add(frame)
        return this.evaluateFunction(frame)
    }

    private fun evaluateExpression(stream: ByteReadPacket): Result{
        var byte = stream.readByte()
        do{
            when(byte){
                (MIRBytecode.INTEGER_LITERAL and 0xff).toByte() -> return WrappedResult(stream.readInt())
                (MIRBytecode.DECIMAL_LITERAL and 0xff).toByte() -> return WrappedResult(stream.readFloat())
                (MIRBytecode.BEGIN_STRING_LITERAL and 0xff).toByte() -> return WrappedResult(this.evaluateString(stream))
                (MIRBytecode.FUNCTION_CALL and 0xff).toByte() -> return WrappedResult(when(val result = this.evaluateFunctionCall(stream)){
                    is WrappedResult<*> -> result.t
                    is ErrorResult -> return ErrorResult("An error occurred while evaluating function call", result)
                    else -> return ErrorResult("Unrecognized result: $result")
                })
                (MIRBytecode.REFERENCE and 0xff).toByte() -> return WrappedResult(when(val result = this.evaluateReference(stream)){
                    is WrappedResult<*> -> result.t
                    is ErrorResult -> return ErrorResult("An error occurred while evaluating reference", result)
                    else -> return ErrorResult("Unrecognized result: $result")
                })
                (MIRBytecode.BINARY_PLUS and 0xff).toByte() -> /**/return WrappedResult(when(val result = this.evaluateBinary(byte)){
                    is WrappedResult<*> -> result.t
                    is ErrorResult -> return ErrorResult("An error occurred while evaluating binary add", result)
                    else -> return ErrorResult("Unrecognized result: $result")
                })
                (MIRBytecode.BINARY_MINUS and 0xff).toByte() -> /**/return WrappedResult(when(val result = this.evaluateBinary(byte)){
                    is WrappedResult<*> -> result.t
                    is ErrorResult -> return ErrorResult("An error occurred while evaluating binary minus", result)
                    else -> return ErrorResult("Unrecognized result: $result")
                })

                (MIRBytecode.BINARY_MULT and 0xff).toByte() -> return WrappedResult(when(val result = this.evaluateBinary(byte)){
                    is WrappedResult<*> -> result.t
                    is ErrorResult -> return ErrorResult("An error occurred while evaluating binary mult", result)
                    else -> return ErrorResult("Unrecognized result: $result")
                })
                (MIRBytecode.BINARY_DIV and 0xff).toByte() -> return WrappedResult(when(val result = this.evaluateBinary(byte)){
                    is WrappedResult<*> -> result.t
                    is ErrorResult -> return ErrorResult("An error occurred while evaluating binary div", result)
                    else -> return ErrorResult("Unrecognized result: $result")
                })
            }
            byte = stream.readByte()
        }while(stream.canRead())
        return WrappedResult(null)
    }

    private fun evaluateReference(packet: ByteReadPacket): Result{
        val length = packet.readInt()
        val name = packet.readTextExact(length)
        val variable = this.stack.filterIsInstance<BasicValue.VMNamedValue<*>>().find { it.name == name } ?: return ErrorResult("Could not find anything that matches the name $name")
        return WrappedResult(variable.value)
    }

    private fun evaluateGlobalVar(stream: ByteReadPacket): Result{
        var length = stream.readInt()
        val name = stream.readTextExact(length)
        length = stream.readInt()
        val typename = stream.readTextExact(length)
        var byte = stream.readByte()
        do{
            when(byte){
                (MIRBytecode.ASSIGNMENT and 0xff).toByte() -> {
                    val vmValue = when(val result = this.evaluateAssignment(stream)){
                        is WrappedResult<*> -> {
                            when(result.t){
                                is Int -> {
                                    if (typename != "Int") return ErrorResult("Local variable of type $typename does not match assignment expression type Int")
                                    BasicValue.VMNamedValue(name, result.t)
                                }
                                is Float -> {
                                    if (typename != "Float") return ErrorResult("Local variable of type $typename does not match assignment expression type Float")
                                    BasicValue.VMNamedValue(name, result.t)
                                }
                                is String -> {
                                    if (typename != "String") return ErrorResult("Local variable of type $typename does not match assignment expression type String")
                                    BasicValue.VMNamedValue(name, result.t)
                                }
                                else -> return ErrorResult("Unrecognized type of expression: ${result.t}")
                            }
                        }
                        is ErrorResult -> return ErrorResult("An error occurred while evaluating assignment of local variable")
                        else -> return ErrorResult("Unrecognized result: $result")
                    }
                    this.stack.add(vmValue)
                    this.frames.last().stackIdices.add(this.stack.size - 1)
                }
            }
            byte = stream.readByte()
        }while(stream.canRead())
        return OKResult
    }

    private fun initBlock(stream: ByteReadPacket): Result{
        var byte = stream.readByte()
        val bytes = buildPacket {
            while(byte != (MIRBytecode.END_BLOCK and 0xff).toByte()){
                if(byte == (MIRBytecode.END_FILE and 0xff).toByte()) return ErrorResult("Incomplete local block: End of File Reached")
                this.writeByte(byte)
                byte = stream.readByte()
            }
        }.readBytes()
        return WrappedResult(LocalBlock("block-${Random(System.currentTimeMillis()).nextInt()}", bytes))
    }

    private fun initFunction(stream: ByteReadPacket): Result{
        var length = stream.readInt()
        val name = stream.readTextExact(length)
        length = stream.readInt()
        val returnTypeName = stream.readTextExact(length)
        var byte = stream.readByte()
        val params = arrayListOf<FunctionParam>()
        do{
            when(byte){
                (MIRBytecode.BEGIN_FUNCTION_PARAMS and 0xff).toByte() -> {
                    while(stream.canRead()){
                        length = stream.readInt()
                        val pname = stream.readTextExact(length)
                        length = stream.readInt()
                        val ptypename = stream.readTextExact(length)
                        params.add(FunctionParam(pname, ptypename))
                        if(stream.tryPeek() == MIRBytecode.END_FUNCTION_PARAMS) break
                    }
                }
                (MIRBytecode.BEGIN_BLOCK and 0xff).toByte() -> {
                    val block = when(val result = this.initBlock(stream)){
                        is WrappedResult<*> -> {
                            when(result.t){
                                is LocalBlock -> result.t
                                else -> return ErrorResult("Did not get local block from local block initializer, instead got: ${result.t}")
                            }
                        }
                        is ErrorResult -> return ErrorResult("An error occurred while initializing local block", result)
                        else -> return ErrorResult("Unrecognized result: $result")
                    }
                    this.stack.add(BasicValue.FunctionDecl(name, returnTypeName, params, block))
                    return OKResult
                }
            }
            byte = stream.readByte()
        }while(byte != (MIRBytecode.END_FILE and 0xff).toByte())
        return OKResult
    }

    private fun initModule(stream: ByteReadPacket): Result{
        var byte = stream.readByte()
        do{
            when(byte){
                (MIRBytecode.TARGET_TYPE and 0xff).toByte() -> {
                    val targetType = stream.readByte()
                    if(targetType != (ToylangMainASTBytecode.TARGET_TYPE_VM and 0xff).toByte()) return ErrorResult("Compiled bytecode is not intended for the VM. Please run compiler with --immediate")
                }
                (MIRBytecode.FILE_NAME and 0xff).toByte() -> {
                    val size = stream.readInt()
                    this.moduleName = stream.readTextExact(size)
                }
                (MIRBytecode.GLOBAL_VAR and 0xff).toByte() -> {
                    when(val result = this.evaluateGlobalVar(stream)){
                        is ErrorResult -> return ErrorResult("An error occurred while evaluating global variable", result)
                    }
                }
                (MIRBytecode.FUNCTION and 0xff).toByte() -> {
                    when(val result = this.initFunction(stream)){
                        is ErrorResult -> return ErrorResult("An error occurred while initializing function declaration", result)
                    }
                }
            }
            byte = stream.readByte()
        }while(byte != (MIRBytecode.END_FILE and 0xff).toByte())
        return OKResult
    }

    fun init(stream: ByteReadPacket): Result{
        var byte = stream.readByte()
        do{
            when(byte){
                (MIRBytecode.BEGIN_FILE and 0xff).toByte() -> {
                    when(val result = this.initModule(stream)){
                        is OKResult -> return result
                        is ErrorResult -> return ErrorResult("An error occurred while initializing file", result)
                    }
                }
            }
            byte = stream.readByte()
        }while(stream.canRead())
        return OKResult
    }
}

/*

import ErrorResult
import OKResult
import parsing.hir.ToylangHIRElement
import Result
import WrappedResult
import com.couch.toylang.ToylangParser
import parsing.*
import parsing.ast.ToylangASTNode
import parsing.typeck.Type

data class VMValue(val type: Type, val value: Any?)

class VMParser: Parse<ToylangHIRElement, Result>(){
    val variables: HashMap<ToylangHIRElement.StatementNode.VariableNode, VMValue> = hashMapOf()
    override fun parseFile(node: ToylangHIRElement, context: Context): Result {
        if(node !is ToylangHIRElement.RootNode) return ParserErrorResult(ErrorResult("Attempted to run file without root node"), node.location)
        node.statements.forEach {
            this.parseStatement(it, context)
        }
        return OKResult
    }

    override fun parseVariableDeclaration(node: ToylangHIRElement, context: Context): Result {
        if(node !is ToylangHIRElement.StatementNode.VariableNode) return ParserErrorResult(ErrorResult("Attempted to evaluate variable declaration "), node.location)
        val assignment = when(val assignmentResult = this.parseAssignment(node.assignment, context)){
            is WrappedResult<*> -> {
                when(assignmentResult.t){
                    is VMValue -> {
                        if(assignmentResult.t.type.identifier == node.type?.typeName ?:
                                return ErrorResult("Variable ${node.identifier} does not have a type but tried to assign it to a value of type ${assignmentResult.t.type}")){
                            assignmentResult.t
                        }else{
                            return ErrorResult("Variable ${node.identifier} is of type ${node.type?.typeName} but its assignment was of type ${assignmentResult.t.type}")
                        }
                    }
                    else -> return ErrorResult("Unrecognized return value: ${assignmentResult.t}")
                }
            }
            is ErrorResult -> return ParserErrorResult(assignmentResult, node.location)
            is ParserErrorResult<*> -> return ErrorResult("An error occurred while parsing assignment for variable ${node.identifier}")
            else -> return ErrorResult("Unrecognized result: $assignmentResult")
        }
        this.variables[node] = assignment
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

    override fun parseFunctionDeclaration(node: ToylangHIRElement, context: Context): Result {
        if(node !is ToylangHIRElement.StatementNode.FunctionDeclNode) return ParserErrorResult(ErrorResult("Attempted to evaluate function without function decl node"), node.location)
        node.codeblock.statements.forEach {
            when(it){
                is ToylangHIRElement.StatementNode.ReturnStatementNode -> {
                    return when(val returnResult = this.parseReturnStatement(it, node.context)){
                        is WrappedResult<*> -> {
                            when(returnResult.t){
                                is VMValue -> {
                                    if(returnResult.t.type.identifier == node.type.typeName){
                                        ErrorResult("Return statement value does not match return type of ${node.identifier} which is ${node.type.typeName}")
                                    }else{
                                        WrappedResult(returnResult.t.value)
                                    }
                                }
                                else -> ErrorResult("Unrecognized return result value: ${returnResult.t}")
                            }
                        }
                        is ErrorResult -> ParserErrorResult(returnResult, it.location)
                        is ParserErrorResult<*> -> ErrorResult("An error occurred while parsing return statement: $returnResult")
                        else -> ErrorResult("Unrecognized result: $returnResult")
                    }
                }
                else -> {
                    this.parseStatement(it, node.context)
                }
            }
        }
        return super.parseFunctionDeclaration(node, context)
    }

    override fun parseValueReference(node: ToylangHIRElement, context: Context): Result {
        if(node !is ToylangHIRElement.StatementNode.ExpressionNode.ValueReferenceNode) return ParserErrorResult(ErrorResult("Attempted to evaluate function call without function call node"), node.location)
        return when(val result = context.findIdentifier(node.identifier)){
            is WrappedResult<*> -> {
                when(result.t){
                    is ToylangHIRElement.StatementNode.VariableNode -> {
                        if(this.variables.containsKey(result.t)){
                            WrappedResult(variables[result.t])
                        }else{
                            ErrorResult("Value reference identifier ${node.identifier} does not reference a variable that has been initialized")
                        }
                    }
                    else -> {
                        ErrorResult("Value reference identifier ${node.identifier} does not reference a variable: ${result.t}")
                    }
                }
            }
            is ErrorResult -> ParserErrorResult(result, node.location)
            is ParserErrorResult<*> -> ErrorResult("An error occurred while parsing finding symbol for identifier ${node.identifier}: $result")
            else -> ErrorResult("Unrecognized result: $result")
        }
    }

    override fun parseFunctionCall(node: ToylangHIRElement, context: Context): Result {
        if(node !is ToylangHIRElement.StatementNode.ExpressionNode.FunctionCallNode) return ParserErrorResult(ErrorResult("Attempted to evaluate function call without function call node"), node.location)
        when(val findResult = context.findIdentifier(node.identifier)){
            is WrappedResult<*> -> {
                when(findResult.t){
                    is ToylangHIRElement.StatementNode.FunctionDeclNode -> {
                        return when(val callResult = this.parseFunctionDeclaration(findResult.t, findResult.t.context.parentContext)){
                            is WrappedResult<*> -> {
                                callResult
                            }
                            is ErrorResult -> ParserErrorResult(callResult, node.location)
                            is ParserErrorResult<*> -> ErrorResult("An error occurred while parsing function call: $callResult")
                            is OKResult -> WrappedResult(Type.NONE)
                            else -> ErrorResult("Unrecognized result: $callResult")
                        }
                    }
                    is ToylangHIRElement.StatementNode.VariableNode -> return ParserErrorResult(ErrorResult("Variable ${findResult.t.identifier} is not callable"), node.location)
                    is ToylangHIRElement.FunctionParamNode -> return ParserErrorResult(ErrorResult("Variable ${findResult.t.identifier} is not callable"), node.location)
                    else -> return ErrorResult("Unrecognized callable element: ${findResult.t}")
                }
            }
            is ErrorResult -> return ParserErrorResult(findResult, node.location)
            else -> return ErrorResult("Unrecognized result: $findResult")
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
        return WrappedResult(VMValue(Type.STRING, str))
    }

    override fun parseBinaryOperation(node: ToylangHIRElement, context: Context): Result {
        if(node !is ToylangHIRElement.StatementNode.ExpressionNode.BinaryExpressionNode) return ParserErrorResult(ErrorResult("Attempted to evaluate left hand of binary operation without binary node"), node.location)
        val left = when(val result = this.parseExpression(node.left, context)){
            is WrappedResult<*> -> {
                when(result.t){
                    is VMValue -> {
                        result.t.value
                                ?: return ParserErrorResult(ErrorResult("Value of left hand of binary expression cannot be null"), node.left.location)
                    }
                    else -> return ErrorResult("Unrecognized return result value: ${result.t}")
                }
            }
            is ErrorResult -> ParserErrorResult(result, node.left.location)
            is ParserErrorResult<*> -> ErrorResult("An error occurred while parsing left hand of binary operation: $result")
            else -> ErrorResult("Unrecognized result: $result")
        }
        return super.parseBinaryOperation(node, context)
    }

    override fun parseExpression(node: ToylangHIRElement, context: Context): Result {
        return when(node){
            is ToylangHIRElement.StatementNode.ExpressionNode.BinaryExpressionNode -> this.parseBinaryOperation(node, context)
            is ToylangHIRElement.StatementNode.ExpressionNode.IntegerLiteralExpression -> WrappedResult(VMValue(Type.INTEGER, node.integer))
            is ToylangHIRElement.StatementNode.ExpressionNode.DecimalLiteralExpression -> WrappedResult(VMValue(Type.DECIMAL, node.decimal))
            is ToylangHIRElement.StatementNode.ExpressionNode.StringLiteralExpression -> this.parseStringLiteral(node, context)
            else -> ParserErrorResult(ErrorResult("Unrecognized expression: $node"), node.location)
        }
    }
}*/
