package com.couch.kotlinx.llvm

import org.bytedeco.javacpp.PointerPointer
import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM
import org.omg.CORBA.NameValuePair

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

fun BasicBlock.createLocalVariable(name: String, type: Type, block: ()->Value): Variable.NamedVariable.LocalVariable{
    val builder = LLVM.LLVMCreateBuilder()
    LLVM.LLVMPositionBuilderAtEnd(builder, this.ref)
    val alloc = LLVM.LLVMBuildAlloca(builder, type.llvmType, name)
    val variable = Variable.NamedVariable.LocalVariable(name, type, object : Value{
        override val type: Type
            get() = type
        override val value: LLVMValueRef
            get() = alloc

    })
    val value = block()
    LLVM.LLVMBuildStore(builder, value.value, alloc)
    return variable
}