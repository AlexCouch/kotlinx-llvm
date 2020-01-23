package com.couch.kotlinx.llvm

import org.bytedeco.llvm.global.LLVM

class GlobalVariable(val name: String, val module: Module, val type: Type, val pointer: Pointer){
    var value: Value? = null
}

fun createGlobalVariable(name: String, type: Type, module: Module, block: GlobalVariable.()->Unit): GlobalVariable{
    println("Creating new global variable")
    val globalPointer = LLVM.LLVMAddGlobal(module.module, type.llvmType, name)
    val globalVar = GlobalVariable(name, module, type, Pointer(type, globalPointer))
    globalVar.block()
    return globalVar
}