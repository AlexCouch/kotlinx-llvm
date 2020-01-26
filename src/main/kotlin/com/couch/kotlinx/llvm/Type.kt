package com.couch.kotlinx.llvm

import org.bytedeco.llvm.LLVM.LLVMTypeRef
import org.bytedeco.llvm.global.LLVM

/**
 * This sealed class allows you to capture information about a given type.
 * This lets you create either primitive types, or collection types that contain information about what you're collecting
 */
sealed class Type(val llvmType: LLVMTypeRef? = null){
    /*
        Integer Types
     */
    class Int8Type(): Type(LLVM.LLVMInt8Type())
    class Int16Type(): Type(LLVM.LLVMInt16Type())
    class Int32Type(): Type(LLVM.LLVMInt32Type())
    class Int64Type(): Type(LLVM.LLVMInt64Type())

    class FloatType(): Type(LLVM.LLVMFloatType())
    class DoubleType(): Type(LLVM.LLVMDoubleType())
    /*
        Other Type
     */
    class VoidType(): Type(LLVM.LLVMVoidType())
    class ArrayType(val arrayType: Type, val arrayCount: Int): Type(LLVM.LLVMArrayType(arrayType.llvmType, arrayCount))
    class VectorType(val vectorType: Type, val arrayCount: Int): Type(LLVM.LLVMVectorType(vectorType.llvmType, arrayCount))
    class PointerType(val type: Type): Type(LLVM.LLVMPointerType(type.llvmType, LLVM.LLVMGetPointerAddressSpace(type.llvmType)))

}