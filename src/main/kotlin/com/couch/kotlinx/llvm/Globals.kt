package com.couch.kotlinx.llvm

import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM

fun Module.createGlobalVariable(name: String, type: Type, block: ()->Value): Variable.NamedVariable.GlobalVariable {
    val globalPointer = LLVM.LLVMAddGlobal(this.module, type.llvmType, name)
    val value = block()
    val globalVar = Variable.NamedVariable.GlobalVariable(name, type, value)
    LLVM.LLVMSetInitializer(globalPointer, value.value)
    this.globalVariables.add(globalVar)
    return globalVar
}