package com.couch.kotlinx.llvm

import org.bytedeco.javacpp.BytePointer
import org.bytedeco.javacpp.PointerPointer
import org.bytedeco.llvm.LLVM.*
import org.bytedeco.llvm.global.LLVM

fun main(){
    val module = buildModule("test"){
        createGlobalVariable("testVar", Type.Int32Type()){
            setGlobalInitializer{
                Value.FloatConstValue(5.0f)
            }
        }

        createFunction("testFunc"){
            this.returnType = Type.Int8Type()
            this.createFunctionParam{
                Type.Int32Type()
            }
            this.createFunctionParam{
                Type.FloatType()
            }
            this.addBlock("test_block_1"){
                this.addReturnStatement {
                    Value.Int32ConstValue(10)
                }
            }
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