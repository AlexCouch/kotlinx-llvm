package com.couch.kotlinx.llvm

import org.bytedeco.javacpp.PointerPointer
import org.bytedeco.llvm.LLVM.LLVMTypeRef
import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM

/*
sealed class Value(open val type: Type, open val value: LLVMValueRef){
    class NoneValue: Value(Type.VoidType(), LLVM.LLVMConstNull(Type.Int8Type().llvmType))

    data class Int8ConstValue(val byte: Byte): Value(Type.Int8Type(), LLVM.LLVMConstInt(Type.Int8Type().llvmType, byte.toLong(), 0))
    data class Int16ConstValue(val short: Short): Value(Type.Int16Type(), LLVM.LLVMConstInt(Type.Int16Type().llvmType, short.toLong(), 0))
    data class Int32ConstValue(val int: Int): Value(Type.Int32Type(), LLVM.LLVMConstInt(Type.Int32Type().llvmType, int.toLong(), 0))
    data class Int64ConstValue(val long: Long): Value(Type.Int64Type(), LLVM.LLVMConstInt(Type.Int64Type().llvmType, long, 0))

    data class FloatConstValue(val float: Float): Value(Type.FloatType(), LLVM.LLVMConstReal(Type.FloatType().llvmType, float.toDouble()))
    data class DoubleConstValue(val double: Double): Value(Type.DoubleType(), LLVM.LLVMConstReal(Type.DoubleType().llvmType, double))
    data class StringConstValue(val string: String): Value(Type.ArrayType(Type.Int8Type(), string.length + 1), LLVM.LLVMArray(string, string.length, 0))
    data class VariableReferenceValue(val variable: Variable): Value(variable.type, variable.value?.value!!)
    data class FunctionParamReferenceValue(override val type: Type, override val value: LLVMValueRef): Value(type, value)
}*/

interface Value{
    val type: Type
    val value: LLVMValueRef
}

fun createInt8Value(byte: Byte, signExtend: Boolean = false) = object : Value{
    override val type: Type = Type.Int8Type()
    override val value: LLVMValueRef = LLVM.LLVMConstInt(type.llvmType, byte.toLong(), if(signExtend) 1 else 0)
}

fun createInt16Value(short: Short, signExtend: Boolean = false) = object : Value{
    override val type: Type = Type.Int16Type()
    override val value: LLVMValueRef = LLVM.LLVMConstInt(type.llvmType, short.toLong(), if(signExtend) 1 else 0)
}

fun createInt32Value(int: Int, signExtend: Boolean = false) = object : Value{
    override val type: Type = Type.Int32Type()
    override val value: LLVMValueRef = LLVM.LLVMConstInt(type.llvmType, int.toLong(), if(signExtend) 1 else 0)
}

fun createInt64Value(long: Long, signExtend: Boolean = false) = object : Value{
    override val type: Type = Type.Int64Type()
    override val value: LLVMValueRef = LLVM.LLVMConstInt(type.llvmType, long, if(signExtend) 1 else 0)
}

fun createFloatValue(float: Float) = object : Value{
    override val type: Type = Type.FloatType()
    override val value: LLVMValueRef = LLVM.LLVMConstReal(type.llvmType, float.toDouble())
}

fun createDoubleValue(double: Double) = object : Value{
    override val type: Type = Type.DoubleType()
    override val value: LLVMValueRef = LLVM.LLVMConstReal(type.llvmType, double)
}

fun createStringValue(string: String) = object : Value {
        override val type: Type = Type.ArrayType(Type.Int8Type(), string.length)
        override val value: LLVMValueRef = LLVM.LLVMConstString(string, string.length, 0)
    }

fun createReferenceValue(valueToReference: Value) = object : Value{
    override val type: Type = Type.PointerType(valueToReference.type)
    override val value: LLVMValueRef = valueToReference.value
}

object NoneValue: Value{
    override val type: Type = Type.Int8Type()
    override val value: LLVMValueRef = LLVM.LLVMConstNull(type.llvmType)

}