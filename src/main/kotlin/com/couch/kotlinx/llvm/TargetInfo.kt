package com.couch.kotlinx.llvm

import org.bytedeco.llvm.LLVM.LLVMTargetDataRef
import org.bytedeco.llvm.LLVM.LLVMTargetMachineRef
import org.bytedeco.llvm.global.LLVM

class TargetTriple(val triple: String){
    companion object{
        val default = TargetTriple(LLVM.LLVMGetDefaultTargetTriple().string)
    }
}