package com.couch.kotlinx.llvm

import org.bytedeco.javacpp.BytePointer
import org.bytedeco.javacpp.PointerPointer
import org.bytedeco.llvm.LLVM.*
import org.bytedeco.llvm.global.LLVM

fun main(){
    val module = buildModule("test"){
        val globalVarValue = "Hello world!"
        val globalVar = createGlobalVariable("testVar", Type.ArrayType(Type.Int8Type(), globalVarValue.length + 1)){
            setGlobalInitializer{
                createStringValue(globalVarValue)
            }
        }

        createFunction("testFunc"){
            this.returnType = Type.PointerType(Type.Int8Type().llvmType)
            /*this.createFunctionParam("f"){
                Type.Int8Type()
            }*/
            this.addBlock("test_block_1"){ builder ->
                this.addReturnStatement {
                    val namedGlobal = LLVM.LLVMGetNamedGlobal(this@buildModule.module, "testVar")
                    val pointerpointer = PointerPointer<LLVMValueRef>(*arrayListOf(LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 1, 0)).toTypedArray())
                    val gep = LLVM.LLVMBuildGEP(builder, namedGlobal, pointerpointer, 1, "testVar_tmp")
                    val temp = createLocalVariable("testVar_temp", Type.PointerType(Type.ArrayType(Type.Int8Type(), globalVarValue.length + 1).llvmType)){
                        this.value = object : Value{
                            override val type: Type = Type.CustomType(LLVM.LLVMTypeOf(gep))
                            override val value: LLVMValueRef = gep
                        }
                    }
                    val tempCast = LLVM.LLVMBuildBitCast(builder, temp.pointer.alloc, Type.PointerType(Type.Int8Type().llvmType).llvmType, "temp")
                    object : Value{
                        override val type: Type = Type.CustomType(LLVM.LLVMPointerType(LLVM.LLVMTypeOf(tempCast), 0))
                        override val value: LLVMValueRef = tempCast
                    }
                    /*object : Value{
                        override val type: Type = globalVar.type
                        override val value: LLVMValueRef = globalVar.pointer.alloc

                    }*/
                }
            }
            println(LLVM.LLVMPrintModuleToString(this.module.module).string)

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