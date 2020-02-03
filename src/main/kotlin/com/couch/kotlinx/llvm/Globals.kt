package com.couch.kotlinx.llvm

import org.bytedeco.llvm.global.LLVM

fun Module.createGlobalVariable(name: String, type: Type, block: Variable.NamedVariable.GlobalVariable.()->Unit): Variable.NamedVariable.GlobalVariable {
    val globalPointer = LLVM.LLVMAddGlobal(this.module, type.llvmType, name)
    val globalVar = Variable.NamedVariable.GlobalVariable(name, type, Pointer(type, globalPointer))
    globalVar.block()
    this.globalVariables.add(globalVar)
    return globalVar
}

fun Variable.NamedVariable.GlobalVariable.setGlobalInitializer(block: Variable.NamedVariable.GlobalVariable.()->Value){
    val value = this.block()
    this.value = value
    LLVM.LLVMSetInitializer(pointer.alloc, value.value)
}