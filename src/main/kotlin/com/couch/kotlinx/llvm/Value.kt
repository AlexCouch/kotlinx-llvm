package com.couch.kotlinx.llvm

import org.bytedeco.javacpp.PointerPointer
import org.bytedeco.llvm.LLVM.LLVMTypeRef
import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM

data class Value(val type: Type, val value: LLVMValueRef)

fun <T> setGlobalInitializer(variable: GlobalVariable, value: T) {
    println("Creating global value")
    val type = variable.type
    val valueType = when (value) {
        is Int -> Value(type, LLVM.LLVMConstInt(type.llvmType, value.toLong(), 0))
        is Double -> Value(type, LLVM.LLVMConstReal(type.llvmType, value))
        is Float -> Value(type, LLVM.LLVMConstReal(type.llvmType, value as Double))
        is Array<*> -> {
            val elements = arrayListOf<LLVMTypeRef>()
            value.forEach { elements.add((it as Type).llvmType ?: LLVM.LLVMVoidType()) }
            Value(type, LLVM.LLVMConstArray((type as Type.ArrayType).arrayType.llvmType, PointerPointer<LLVMTypeRef>(*(elements.toTypedArray())), type.arrayCount))
        }
        else -> {
            Value(Type.VoidType(), LLVM.LLVMConstNull(LLVM.LLVMVoidType()))
        }
    }
    println("Setting global initializer")
    LLVM.LLVMSetInitializer(variable.pointer.alloc, valueType.value)
    variable.value = valueType
}