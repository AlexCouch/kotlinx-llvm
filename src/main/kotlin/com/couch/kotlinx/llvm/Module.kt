package com.couch.kotlinx.llvm

import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM

class Module(name: String){
    val module = LLVM.LLVMModuleCreateWithName(name)
}

fun buildModule(name: String, block: Module.()->Unit): Module{
    println("Building new module")
    val module = Module(name)
    module.block()
    return module
}

