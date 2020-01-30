package com.couch.kotlinx.llvm

import org.bytedeco.llvm.global.LLVM

class GlobalVariable(val name: String, val module: Module, val type: Type, val pointer: Pointer){
    fun setGlobalInitializer(block: GlobalVariable.()->Value){
        val value = this.block()
        LLVM.LLVMSetInitializer(pointer.alloc, value.value)
    }
}

fun Module.createGlobalVariable(name: String, type: Type, block: GlobalVariable.()->Unit): GlobalVariable{
    val globalPointer = LLVM.LLVMAddGlobal(this.module, type.llvmType, name)
    val globalVar = GlobalVariable(name, this, type, Pointer(type, globalPointer))
    globalVar.block()
    return globalVar
}