package com.couch.kotlinx.llvm

import org.bytedeco.javacpp.PointerPointer
import org.bytedeco.llvm.LLVM.LLVMModuleRef
import org.bytedeco.llvm.LLVM.LLVMTypeRef
import org.bytedeco.llvm.global.LLVM

class Function(val name: String, val module: LLVMModuleRef){
    var returnType: LLVMTypeRef? = null
    val paramsArray = arrayListOf(LLVM.LLVMInt32Type(), LLVM.LLVMDoubleType())
    val params = PointerPointer<LLVMTypeRef>(*paramsArray.toTypedArray())
    val functionRef = LLVM.LLVMAddFunction(this.module, this.name, returnType ?: LLVM.LLVMFunctionType(LLVM.LLVMVoidType(), params, paramsArray.size, 0))
    init{
        println("Adding function $name")
        println("function address: ${functionRef.address()}")
    }
    fun addBlock(name: String, block: BasicBlock.()->Unit){
        val basicblock = BasicBlock(name, this.functionRef)
        basicblock.block()
    }
}