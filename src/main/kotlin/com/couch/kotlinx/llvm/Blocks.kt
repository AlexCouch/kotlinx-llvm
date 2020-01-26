package com.couch.kotlinx.llvm

import org.bytedeco.llvm.LLVM.LLVMTypeRef
import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM

class BasicBlock(val name: String, val function: Function){
    val ref = LLVM.LLVMAppendBasicBlock(this.function.functionRef, name)
}