package com.couch.kotlinx.llvm

import org.bytedeco.javacpp.BytePointer
import org.bytedeco.javacpp.PointerPointer
import org.bytedeco.llvm.LLVM.LLVMBuilderRef
import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM

class Module(name: String){
    val module = LLVM.LLVMModuleCreateWithName(name)
    val globalVariables = arrayListOf<Variable.NamedVariable.GlobalVariable>()

    fun getGlobalReference(builder: LLVMBuilderRef, name: String): Value?{
        if(this.globalVariables.find{ it.name == name } == null) return null
        val global = this.globalVariables.first { it.name == name }
        val namedGlobal = LLVM.LLVMGetNamedGlobal(this.module, name)
        println(LLVM.LLVMPrintTypeToString(LLVM.LLVMTypeOf(namedGlobal)).string)
        val newVar = Variable.NamedVariable.GlobalVariable(global.name, global.type, Pointer(global.type, namedGlobal))
        newVar.value = global.value
        println(LLVM.LLVMPrintValueToString(newVar.value!!.value).string)
        return when(newVar.value!!.type){
            is Type.ArrayType -> {
                val instrs = arrayListOf(LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 0, 0))
                val elementPtr = LLVM.LLVMBuildGEP(builder, namedGlobal, PointerPointer(*instrs.toTypedArray()), 1, "${global.name}_ref")
                println(LLVM.LLVMPrintValueToString(elementPtr).string)
                createReferenceValue(object : Value{
                    override val type: Type = Type.CustomType(LLVM.LLVMTypeOf(elementPtr))
                    override val value: LLVMValueRef = elementPtr
                })
//                NoneValue
            }
            else -> createReferenceValue(newVar.value!!)
        }
    }
}

fun buildModule(name: String, block: Module.()->Unit): Module{
    println("Building new module")
    val module = Module(name)
    module.block()
    return module
}

