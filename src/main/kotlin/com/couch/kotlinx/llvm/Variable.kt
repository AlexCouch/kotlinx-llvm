package com.couch.kotlinx.llvm

import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM
import org.omg.CORBA.NameValuePair

sealed class Variable(open val name: String, open val type: Type){
    var value: Value? = null

    sealed class NamedVariable(override val name: String, override val type: Type, open val pointer: Pointer): Variable(name, type){
        data class GlobalVariable(override val name: String, override val type: Type, override val pointer: Pointer): NamedVariable(name, type, pointer)
        data class LocalVariable(override val name: String, override val type: Type, override val pointer: Pointer): NamedVariable(name, type, pointer)
    }
    data class FunctionParam(val paramIndex: Int, override val name: String, override val type: Type): Variable(name, type)

}

data class AllocatedVariable(val variable: Variable)
data class Pointer(val type: Type, val alloc: LLVMValueRef)

fun BasicBlock.createLocalVariable(name: String, type: Type, block: Variable.NamedVariable.LocalVariable.()->Unit): Variable.NamedVariable.LocalVariable{
    val builder = LLVM.LLVMCreateBuilder()
    LLVM.LLVMPositionBuilderAtEnd(builder, this.ref)
    val alloc = LLVM.LLVMBuildAlloca(builder, type.llvmType, name)
    val variable = Variable.NamedVariable.LocalVariable(name, type, Pointer(type, alloc))
    variable.block()
    LLVM.LLVMBuildStore(builder, variable.value?.value, alloc)
    return variable
}