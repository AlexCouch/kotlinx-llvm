package com.couch.kotlinx.llvm

import org.bytedeco.javacpp.PointerPointer
import org.bytedeco.llvm.LLVM.LLVMModuleRef
import org.bytedeco.llvm.LLVM.LLVMTypeRef
import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM

class Function(val name: String, val module: Module){
    var returnType: Type = Type.VoidType()
    val params = arrayListOf<Type>()
    val functionType get() = LLVM.LLVMFunctionType(this.returnType.llvmType, PointerPointer(*params.map{ it.llvmType }.toTypedArray()), params.size, 0)
    val functionRef get() = LLVM.LLVMAddFunction(this.module.module, this.name, functionType)
    fun createFunctionParam(block: Function.()->Type){
        val type = this.block()
        this.params.add(type)
    }

    fun addBlock(name: String, block: BasicBlock.()->Unit){
        val basicblock = BasicBlock(name, this)
        basicblock.block()
        val builder = LLVM.LLVMCreateBuilder()
        LLVM.LLVMPositionBuilderAtEnd(builder, basicblock.ref)
    }
}

fun Module.createFunction(name: String, block: Function.()->Unit){
    val function = Function(name, this)
    function.block()
}