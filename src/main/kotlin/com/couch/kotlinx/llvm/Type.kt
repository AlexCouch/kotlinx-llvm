package com.couch.kotlinx.llvm

import org.bytedeco.javacpp.PointerPointer
import org.bytedeco.llvm.LLVM.LLVMTypeRef
import org.bytedeco.llvm.global.LLVM

/**
 * This sealed class allows you to capture information about a given type.
 * This lets you create either primitive types, or collection types that contain information about what you're collecting
 */
sealed class Type(val llvmType: LLVMTypeRef){
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
    open class ArrayType(arrayType: Type, arrayCount: Int): Type(LLVM.LLVMArrayType(arrayType.llvmType, arrayCount))
    class VectorType(vectorType: Type, arrayCount: Int): Type(LLVM.LLVMVectorType(vectorType.llvmType, arrayCount))
    class StructType(val fields: Array<Type>, val packed: Boolean = false, typeRef: LLVMTypeRef): Type(typeRef)
    open class PointerType(val type: Type): Type(LLVM.LLVMPointerType(type.llvmType, 0))
    class ReferenceType(variable: Variable): Type(variable.type.llvmType)
    class StringType: PointerType(Int8Type())
    class CustomType(llvmType: LLVMTypeRef): Type(llvmType)
}