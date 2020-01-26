package com.couch.kotlinx.llvm

import org.bytedeco.llvm.LLVM.LLVMBuilderRef
import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM

class AdditionInstruction{
    var left: Value = Value.NoneValue()
    var right: Value = Value.NoneValue()
}

fun BasicBlock.addAdditionInstruction(name: String, block: AdditionInstruction.()->Unit){
    val additionInstruction = AdditionInstruction()
    additionInstruction.block()
    val builder = LLVM.LLVMCreateBuilder()
    LLVM.LLVMPositionBuilderAtEnd(builder, this.ref)
    LLVM.LLVMBuildAdd(builder, additionInstruction.left.value, additionInstruction.right.value, name)

}

fun BasicBlock.addReturnStatement(block: BasicBlock.()->Value) {
    val value = this.block()
    val builder = LLVM.LLVMCreateBuilder()
    LLVM.LLVMPositionBuilderAtEnd(builder, this.ref)
    if(value is Value.NoneValue){
        LLVM.LLVMBuildRetVoid(builder)
    }else{
        LLVM.LLVMBuildRet(builder, value.value)
    }
}