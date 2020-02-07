package com.couch.kotlinx.llvm

import org.bytedeco.javacpp.BytePointer
import org.bytedeco.javacpp.PointerPointer
import org.bytedeco.llvm.LLVM.*
import org.bytedeco.llvm.global.LLVM

fun main(){
    val module = buildModule("test"){
        val globalVarValue = "Hello world!"
        val globalVar = createGlobalVariable("testVar", Type.ArrayType(Type.Int8Type(), globalVarValue.length + 1)){
            createStringValue(globalVarValue)
        }

        createFunction("testFunc"){
            this.returnType = Type.PointerType(Type.Int8Type())
            this.addBlock("test_block_1"){
                this.startBuilder {
                    this.addReturnStatement {
                        val gep = this.buildGetElementPointer("testVarPointer"){
                            this@buildModule.getGlobalReference("testVar")!!.value!!
                        }
                        this.buildBitcast(gep, Type.PointerType(Type.Int8Type()), "testVarCast")
                    }
                }
            }
            println(LLVM.LLVMPrintModuleToString(this.module.module).string)

        }
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