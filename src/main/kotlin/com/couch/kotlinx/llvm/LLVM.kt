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
            createInt32Value(10)
        }

        val printf = this.createFunction("printf"){
            this.returnType = Type.Int32Type()
            this.vararg = true
            this.createFunctionParam("argc"){
                Type.PointerType(Type.Int8Type())
            }
        }

        val add = this.createFunction("add"){
            this.returnType = Type.Int32Type()
            this.createFunctionParam("x"){
                Type.Int32Type()
            }
            this.createFunctionParam("y"){
                Type.Int32Type()
            }
            this.addBlock("entry"){
                this.startBuilder {
                    this.addReturnStatement {
                        this.addAdditionInstruction("add_res"){
                            this.left = this@addBlock.function.getParamByName("x")
                            this.right = this@addBlock.function.getParamByName("y")
                        }
                    }
                }
            }
        }

        createFunction("main"){
            this.returnType = Type.Int32Type()
            this.addBlock("test_block_1"){
                this.startBuilder {
//                    val printfFuncType = LLVM.LLVMFunctionType(Type.Int32Type().llvmType, PointerPointer(*arrayOf(Type.PointerType(Type.Int8Type()).llvmType)), 1, 1)
//                    val printfFunc = LLVM.LLVMAddFunction(this@buildModule.module, "printf", printfFuncType)
                    val message = createLocalVariable("message", Type.ArrayType(Type.Int8Type(), "%d".length + 1)){
                        createStringValue("%d")
                    }
                    val gep = this.buildGetElementPointer("messageGEP"){
                        message.value
                    }
                    val addVar = createLocalVariable("addCallRes", Type.Int32Type()){
                        this.buildFunctionCall("addFuncCall", add){
                            arrayOf(createInt32Value(10), globalVar.value)
                        }
                    }
                    val gepCast = this.buildBitcast(gep, Type.PointerType(Type.Int8Type()), "gepCast")
//                    LLVM.LLVMBuildCall(this.builder, printfFunc, PointerPointer(*arrayOf(gepCast.value, add.value.value)), 2, "call")
                    this.buildFunctionCall("call", printf){
                        val addValue = this.buildLoad(addVar.value, "")
                        arrayOf(gepCast, addValue)
                    }
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