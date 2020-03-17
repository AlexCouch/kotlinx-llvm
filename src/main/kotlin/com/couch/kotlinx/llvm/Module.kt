package com.couch.kotlinx.llvm

import org.bytedeco.javacpp.BytePointer
import org.bytedeco.javacpp.PointerPointer
import org.bytedeco.llvm.LLVM.LLVMBuilderRef
import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM

class Module(name: String){
    val module = LLVM.LLVMModuleCreateWithName(name)
    val globalVariables = arrayListOf<Variable.NamedVariable.GlobalVariable>()
    val functions = arrayListOf<Function>()

    fun getGlobalReference(name: String): Variable?{
        if(this.globalVariables.find{ it.name == name } == null) return null
        val global = this.globalVariables.first { it.name == name }
        val namedGlobal = LLVM.LLVMGetNamedGlobal(this.module, name)
        val value = object : Value{
            override val type: Type
                get() = global.type
            override val value: LLVMValueRef get() = namedGlobal

        }
        return Variable.NamedVariable.GlobalVariable(name, global.type, value)
    }
}

inline fun buildModule(name: String, block: Module.()->Unit): Module{
    println("Building new module")
    val module = Module(name)
    module.block()
    return module
}

