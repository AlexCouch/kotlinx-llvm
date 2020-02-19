package com.couch.kotlinx.llvm

import org.bytedeco.javacpp.BytePointer
import org.bytedeco.javacpp.PointerPointer
import org.bytedeco.llvm.LLVM.*
import org.bytedeco.llvm.global.LLVM

fun main(){
    val module = buildModule("test"){
        LLVM.LLVMSetDataLayout(this.module, "e-m:w-i64:64-f80:128-n8:16:32:64-S128")
        LLVM.LLVMSetTarget(this.module, "x86_64-pc-windows-msvc19.23.28106")
        val globalVar = createGlobalVariable("testVar", Type.Int32Type()){
            createInt32Value(5)
        }

        createFunction("main"){
            this.returnType = Type.Int32Type()
            /*this.createFunctionParam("argc"){
                Type.Int32Type()
            }
            this.createFunctionParam("argv"){
                Type.PointerType(Type.PointerType(Type.Int8Type()))
            }*/
            this.addBlock("test_block_1"){
                this.startBuilder {
                    val printfFuncType = LLVM.LLVMFunctionType(Type.Int32Type().llvmType, PointerPointer(*arrayOf(Type.PointerType(Type.Int8Type()).llvmType)), 1, 1)
                    val printfFunc = LLVM.LLVMAddFunction(this@buildModule.module, "printf", printfFuncType)
                    val message = createLocalVariable("message", Type.ArrayType(Type.Int8Type(), "message".length + 1)){
                        createStringValue("message")
                    }
                    val gep = this.buildGetElementPointer("messageGEP"){
                        message.value
                    }
                    val gepCast = this.buildBitcast(gep, Type.PointerType(Type.Int8Type()), "gepCast")
                    LLVM.LLVMBuildCall(this.builder, printfFunc, gepCast.value, 1, BytePointer(""))
                    /*this.addReturnStatement {
                        this.addAdditionInstruction("addOp"){
                            this.left = this@startBuilder.buildBitcast(this@buildModule.getGlobalReference(globalVar.name)!!.value!!, Type.Int32Type(), "addBitcast")
                            this.right = createInt32Value(10)
                        }
                    }*/
                    this.addReturnStatement {
                        createInt32Value(0)
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