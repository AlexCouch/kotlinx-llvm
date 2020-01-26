package com.couch.kotlinx.llvm

import org.bytedeco.javacpp.PointerPointer
import org.bytedeco.llvm.LLVM.LLVMTypeRef
import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM

sealed class Value(val type: Type, val value: LLVMValueRef){
    class NoneValue: Value(Type.VoidType(), LLVM.LLVMConstNull(Type.Int8Type().llvmType))

    data class Int8ConstValue(val byte: Byte): Value(Type.Int8Type(), LLVM.LLVMConstInt(Type.Int8Type().llvmType, byte.toLong(), 0))
    data class Int16ConstValue(val short: Short): Value(Type.Int16Type(), LLVM.LLVMConstInt(Type.Int8Type().llvmType, short.toLong(), 0))
    data class Int32ConstValue(val int: Int): Value(Type.Int32Type(), LLVM.LLVMConstInt(Type.Int8Type().llvmType, int.toLong(), 0))
    data class Int64ConstValue(val long: Long): Value(Type.Int64Type(), LLVM.LLVMConstInt(Type.Int8Type().llvmType, long, 0))

    data class FloatConstValue(val float: Float): Value(Type.FloatType(), LLVM.LLVMConstReal(Type.FloatType().llvmType, float.toDouble()))
    data class DoubleConstValue(val double: Double): Value(Type.DoubleType(), LLVM.LLVMConstReal(Type.DoubleType().llvmType, double))
    data class StringConstValue(val string: String): Value(Type.ArrayType(Type.Int8Type(), string.length), LLVM.LLVMConstString(string, string.length, 0))
}