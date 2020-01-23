package com.couch.kotlinx.llvm

import org.bytedeco.javacpp.BytePointer
import org.bytedeco.javacpp.PointerPointer
import org.bytedeco.llvm.LLVM.*
import org.bytedeco.llvm.global.LLVM

class BasicBlock(val name: String, private val function: LLVMValueRef){
    val builder = LLVM.LLVMCreateBuilder()

    init{
        println("Adding basic block $name")
        val blockRef = LLVM.LLVMAppendBasicBlock(this.function, this.name)
        LLVM.LLVMPositionBuilderAtEnd(this.builder, blockRef)
        println("Block address: ${blockRef.address()}")
        val terminator = LLVM.LLVMGetBasicBlockTerminator(blockRef)
        println(terminator)
    }
    fun addAddition(name: String, block: AdditionInstruction.()->Unit): LLVMValueRef{
        val additionInstruction = AdditionInstruction(name)
        additionInstruction.block()
        return additionInstruction.build(this.builder)
    }

    fun addLocalVariable(name: String, varType: LLVMTypeRef, variable: LLVMValueRef): LLVMValueRef{
        println("Adding local variable $name")
        val alloc = LLVM.LLVMBuildAlloca(this.builder, varType, name)
        println("Allocated local variable on the stack")
        println("Local variable address: ${alloc.address()}")
        return LLVM.LLVMBuildStore(this.builder, variable, alloc)
    }

    fun addRet(retValue: LLVMValueRef? = null) = LLVM.LLVMBuildRet(this.builder, retValue)
}

class AdditionInstruction(private val name: String){
    var left: LLVMValueRef? = null
    var right: LLVMValueRef? = null
    fun build(builder: LLVMBuilderRef): LLVMValueRef = LLVM.LLVMBuildAdd(builder, this.left, this.right, name)
}



fun main(){
    val module = buildModule("test"){
        createGlobalVariable("testVar", Type.Int32Type(), this){
            setGlobalInitializer(this, 5)
        }
        /*
            TODO: Functions
        this.addFunction("testFunc"){
            this.paramsArray.add(LLVM.LLVMInt32Type())
            this.addBlock("testFunc_local"){
                val firstParam = LLVM.LLVMGetFirstParam(this@addFunction.functionRef)
                this.addLocalVariable("five", LLVM.LLVMInt32Type(), firstParam)
                val globalVariableRef = LLVM.LLVMGetNamedGlobal(this@addFunction.module, "testVar")
                val globalVariableType = LLVM.LLVMGlobalGetValueType(globalVariableRef)
                this.addLocalVariable("global_reference",  globalVariableType, globalVariableRef)
                this.addRet()
            }

            LLVM.LLVMVerifyFunction(this.functionRef, LLVM.LLVMAbortProcessAction)
        }
         */
    }
    val error = BytePointer()
    val status = LLVM.LLVMVerifyModule(module.module, LLVM.LLVMAbortProcessAction, error)
    println("Verified module")
    println(status)
    LLVM.LLVMDisposeMessage(error)
    val bitcodeFile = LLVM.LLVMWriteBitcodeToFile(module.module, "test.bc")
    if (bitcodeFile != 0) {
        println("error writing bitcode to file...")
    }
}