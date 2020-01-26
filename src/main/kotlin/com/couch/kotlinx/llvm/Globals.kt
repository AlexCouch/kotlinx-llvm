package com.couch.kotlinx.llvm

import org.bytedeco.llvm.global.LLVM

class GlobalVariable(val name: String, val module: Module, val type: Type, val pointer: Pointer){
    private var value: Value = Value.NoneValue()

    fun setGlobalInitializer(block: GlobalVariable.()->Value){
        this.value = this.block()
    }
}

fun Module.createGlobalVariable(name: String, type: Type, block: GlobalVariable.()->Unit): GlobalVariable{
    val globalPointer = LLVM.LLVMAddGlobal(this.module, type.llvmType, name)
    val globalVar = GlobalVariable(name, this, type, Pointer(type, globalPointer))
    globalVar.block()
    return globalVar
}