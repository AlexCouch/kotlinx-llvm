package com.couch.kotlinx.llvm

import com.sun.org.apache.bcel.internal.generic.ReturnInstruction
import org.bytedeco.llvm.global.LLVM

class AdditionInstruction{
    var left: Value = NoneValue
    var right: Value = NoneValue
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
    when(value){
        is NoneValue -> LLVM.LLVMBuildRetVoid(builder)
        else -> LLVM.LLVMBuildRet(builder, value.value)
    }
}