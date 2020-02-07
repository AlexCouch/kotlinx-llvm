package com.couch.kotlinx.llvm

import org.bytedeco.javacpp.PointerPointer
import org.bytedeco.llvm.LLVM.LLVMBuilderRef
import org.bytedeco.llvm.LLVM.LLVMModuleRef
import org.bytedeco.llvm.LLVM.LLVMTypeRef
import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM

class Function(val name: String, val module: Module){
    var returnType: Type = Type.VoidType()
        set(new){
            field = new
            this.functionType = LLVM.LLVMFunctionType(field.llvmType, PointerPointer(*params.map{ it.llvmType }.toTypedArray()), params.size, 0)
        }
    val params = arrayListOf<Type>()
    val localVariables = arrayListOf<Variable>()
    var functionType = LLVM.LLVMFunctionType(this.returnType.llvmType, PointerPointer(*params.map{ it.llvmType }.toTypedArray()), params.size, 0)
    private set(new){
        LLVM.LLVMDeleteFunction(this.functionRef)
        this.functionRef = LLVM.LLVMAddFunction(this.module.module, this.name, new)
        field = new
    }
    var functionRef: LLVMValueRef? = null
        get(){
            if(field != null){
                return field
            }
            val funcRef = LLVM.LLVMAddFunction(this.module.module, this.name, functionType)
            return funcRef
        }
        private set
    fun createFunctionParam(name: String, block: Function.()->Type): Variable{
        val type = this.block()
        this.params.add(type)
        this.functionType = LLVM.LLVMFunctionType(this.returnType.llvmType, PointerPointer(*params.map{ it.llvmType }.toTypedArray()), params.size, 0)
        return Variable.FunctionParam(params.indexOf(type), name, type)
    }

    fun getParam(index: Int): Value {
        val paramRef = LLVM.LLVMGetParam(this.functionRef, index)
        return object : Value{
            override val type: Type = this@Function.params[index]
            override val value: LLVMValueRef = paramRef
        }
    }

    fun addBlock(name: String, block: BasicBlock.()->Unit){
        val basicblock = BasicBlock(name, this)
        basicblock.block()
    }
}

fun Module.createFunction(name: String, block: Function.()->Unit){
    val function = Function(name, this)
    function.block()
}