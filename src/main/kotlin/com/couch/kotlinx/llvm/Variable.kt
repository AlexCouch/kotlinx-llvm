package com.couch.kotlinx.llvm

import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM

data class Variable(val name: String, val type: Type, var pointer: Pointer){
    var value: Value? = null
}

data class AllocatedVariable(val variable: Variable)
data class Pointer(val type: Type, val alloc: LLVMValueRef)

fun BasicBlock.createVariable(name: String, type: Type, block: Variable.()->Unit): Variable{
    val builder = LLVM.LLVMCreateBuilder()
    LLVM.LLVMPositionBuilderAtEnd(builder, this.ref)
    val alloc = LLVM.LLVMBuildAlloca(builder, type.llvmType, name)
    val variable = Variable(name, type, Pointer(type, alloc))
    variable.block()
    return variable
}