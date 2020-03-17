package com.couch.kotlinx.llvm

import org.bytedeco.javacpp.PointerPointer
import org.bytedeco.llvm.LLVM.LLVMBuilderRef
import org.bytedeco.llvm.LLVM.LLVMModuleRef
import org.bytedeco.llvm.LLVM.LLVMTypeRef
import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM

class Function(val name: String, val module: Module){
    var vararg = false
        set(new){
            this.functionType = LLVM.LLVMFunctionType(this.returnType.llvmType, PointerPointer(*params.map{ it.value.llvmType }.toTypedArray()), params.size, if(new) 1 else 0)
            field = new
        }
    var returnType: Type = Type.VoidType()
        set(new){
            field = new
            this.functionType = LLVM.LLVMFunctionType(field.llvmType, PointerPointer(*params.map{ it.value.llvmType }.toTypedArray()), params.size, if(this.vararg) 1 else 0)
        }
    val params = hashMapOf<String, Type>()
    val localVariables = arrayListOf<Variable>()
    var functionType = LLVM.LLVMFunctionType(this.returnType.llvmType, PointerPointer(*params.map{ it.value.llvmType }.toTypedArray()), params.size, if(this.vararg) 1 else 0)
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
        this.params[name] =  type
        this.functionType = LLVM.LLVMFunctionType(this.returnType.llvmType, PointerPointer(*params.map{ it.value.llvmType }.toTypedArray()), params.size, if(this.vararg) 1 else 0)
        if(name.isNotEmpty()){
            LLVM.LLVMSetValueName(LLVM.LLVMGetParam(this.functionRef, this.params.keys.indexOf(name)), name)
        }
        return Variable.FunctionParam(params.keys.indexOf(name), name, type)
    }

    fun getParam(index: Int): Value {
        val paramRef = LLVM.LLVMGetParam(this.functionRef, index)
        return object : Value{
            override val type: Type = this@Function.params.values.withIndex().find { (idx, _) -> idx == index }?.value!!
            override val value: LLVMValueRef = paramRef
        }
    }

    fun getParamByName(name: String): Value{
        val index = this.params.keys.indexOf(name)
        val paramRef = LLVM.LLVMGetParam(this.functionRef, index)
        return object : Value{
            override val type: Type = this@Function.params.values.withIndex().find { (idx, _) -> idx == index }?.value!!
            override val value: LLVMValueRef = paramRef
        }
    }

    inline fun addBlock(name: String, block: BasicBlock.()->Unit){
        val basicblock = BasicBlock(name, this)
        basicblock.block()
    }
}

inline fun Module.createFunction(name: String, block: Function.()->Unit): Function{
    val function = Function(name, this)
    function.block()
    this.functions.add(function)
    return function
}