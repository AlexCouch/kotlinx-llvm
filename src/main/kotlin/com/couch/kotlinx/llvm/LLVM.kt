package com.couch.kotlinx.llvm

import org.bytedeco.javacpp.BytePointer
import org.bytedeco.javacpp.PointerPointer
import org.bytedeco.llvm.LLVM.*
import org.bytedeco.llvm.global.LLVM

class Module(val name: String){
    val module = LLVM.LLVMModuleCreateWithName(name)

    init{
        println("Created an empty module")
    }

    fun addGlobal(name: String, block: GlobalVariable.()->Unit): LLVMValueRef{
        val globalVariable = GlobalVariable(name, this.module)
        globalVariable.block()
        val global = globalVariable.build()
        println("Created global variable in empty module")
        return global
    }

    fun addFunction(name: String, block: Function.()->Unit): LLVMValueRef{
        val function = Function(name, this.module)
        function.block()
        return function.functionRef
    }
}

class GlobalVariable(val name: String, val module: LLVMModuleRef){
    var type: LLVMTypeRef? = null
    var value: LLVMValueRef? = null
    fun build(): LLVMValueRef {
        val global = LLVM.LLVMAddGlobal(this.module, this.type ?: LLVM.LLVMVoidType(), this.name)
        LLVM.LLVMSetInitializer(global, LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 5, 0))
        return global
    }
}

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

class AdditionInstruction(private val name: String){
    var left: LLVMValueRef? = null
    var right: LLVMValueRef? = null
    fun build(builder: LLVMBuilderRef): LLVMValueRef = LLVM.LLVMBuildAdd(builder, this.left, this.right, name)
}

fun buildModule(name: String, block: Module.()->Unit): Module{
    val module = Module(name)
    module.block()
    return module
}

fun main(){
    val module = buildModule("test"){
        this.addGlobal("testVar"){
            this.type = LLVM.LLVMInt32Type()
        }
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

            LLVM.LLVMVerifyFunction(this.functionRef, LLVM.LLVMPrintMessageAction)
        }
    }
    val error = BytePointer()
    val status = LLVM.LLVMVerifyModule(module.module, LLVM.LLVMReturnStatusAction, error)
    println("Verified module")
    println(status)
    LLVM.LLVMDisposeMessage(error)
    val bitcodeFile = LLVM.LLVMWriteBitcodeToFile(module.module, "test.bc")
    if (bitcodeFile != 0) {
        println("error writing bitcode to file...")
    }
}