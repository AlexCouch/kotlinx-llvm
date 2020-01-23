package com.couch.kotlinx.llvm

import org.bytedeco.llvm.global.LLVM

class Builder{
    val builder = LLVM.LLVMCreateBuilder()

    fun storeVariable(variable: Variable, value: Value){
        LLVM.LLVMBuildStore(this.builder, value.value, variable.pointer.alloc)
    }
}

inline fun Module.startBuilding(block: Builder.()->Unit){
    val builder = Builder()
    builder.block()
}