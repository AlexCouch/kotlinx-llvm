package com.couch.kotlinx.llvm

import org.bytedeco.javacpp.PointerPointer
import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM

sealed class Variable(open val name: String, open val type: Type){
    open val value: Value? = null

    sealed class NamedVariable(override val name: String, override val type: Type, override val value: Value): Variable(name, type){
        data class GlobalVariable(override val name: String, override val type: Type, override val value: Value): NamedVariable(name, type, value)
        data class LocalVariable(override val name: String, override val type: Type, override val value: Value): NamedVariable(name, type, value)
    }
    data class FunctionParam(val paramIndex: Int, override val name: String, override val type: Type): Variable(name, type)

}

data class AllocatedVariable(val variable: Variable)
data class Pointer(val type: Type, val alloc: LLVMValueRef)

fun Builder.createLocalVariable(name: String, block: ()->Value): Variable.NamedVariable.LocalVariable{
    val value = block()
    val alloc = LLVM.LLVMBuildAlloca(builder, value.type.llvmType, name)
    val variable = Variable.NamedVariable.LocalVariable(name, value.type, object : Value{
        override val type: Type = value.type
        override val value: LLVMValueRef
            get() = alloc

    })
    LLVM.LLVMBuildStore(builder, value.value, alloc)
    return variable
}