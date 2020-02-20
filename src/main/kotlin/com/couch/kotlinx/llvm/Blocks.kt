package com.couch.kotlinx.llvm

import org.bytedeco.llvm.LLVM.LLVMTypeRef
import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM

class BasicBlock(val name: String, val function: Function){
    val ref = LLVM.LLVMAppendBasicBlock(this.function.functionRef, name)
}

/**
 * Represents an LLVM IR builder. It allows you to position the builder at the end of the basic block, otherwise continue building IR in the current position.
 *
 * An example of a non-end positioned IR is [buildGetElementPointer](Get element pointer instruction) and bitcast instruction.
 * When these are used together in the same builder, they are inlined together.
 *
 *
 *  @testVar = global [13 x i8] c"Hello world!\00"
 *
 *  define i8* @testFunc() {
 *  test_block_1:
 *      ret i8* getelementptr inbounds ([13 x i8], [13 x i8]* @testVar, i32 1, i32 0)
 *  }
 *
 */
class Builder{
    internal val builder = LLVM.LLVMCreateBuilder()
}

/**
 * Starting building intermediate representation inside a basic block.
 *
 * You can use the same builder for multiple instructions
 */
fun BasicBlock.startBuilder(block: Builder.()->Unit){
    val builder = Builder()
    LLVM.LLVMPositionBuilderAtEnd(builder.builder, this.ref)
    builder.block()
}