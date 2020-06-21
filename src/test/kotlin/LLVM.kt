import com.couch.kotlinx.llvm.*
import org.bytedeco.javacpp.BytePointer
import org.bytedeco.llvm.global.LLVM

fun main(){
    val module = buildModule("test"){
        val globalVar = createGlobalVariable("testVar", Type.Int32Type()){
            createInt32Value(10)
        }
        val decimalPrintStr = createGlobalVariable("dPrint", Type.ArrayType(Type.Int8Type(), "%d\\\n".length)){
            createStringValue("%d\n")
        }

        val printf = findFunction("printf") ?: throw IllegalStateException("Attempted to get printf function but couldn't find it.")

        val add = createFunction("add"){
            returnType = Type.Int32Type()
            createFunctionParam("x"){
                Type.Int32Type()
            }
            createFunctionParam("y"){
                Type.Int32Type()
            }
            addBlock("entry"){
                startBuilder {
                    addReturnStatement {
                        buildAdditionInstruction("add_res"){
                            left = function.getParamByName("x")
                            right = function.getParamByName("y")
                        }
                    }
                }
            }
        }

        val calcAverage = createFunction("calcAverage"){
            returnType = Type.Int32Type()
            createFunctionParam("x"){
                Type.Int32Type()
            }
            createFunctionParam("y"){
                Type.Int32Type()
            }
            createFunctionParam("z"){
                Type.Int32Type()
            }
            addBlock("entry"){
                startBuilder {
                    val addResult = createLocalVariable("addResult", Type.Int32Type()){
                        buildAdditionInstruction("addVar"){
                            left = function.getParamByName("x")
                            right = buildAdditionInstruction("rightAddVar"){
                                left = function.getParamByName("y")
                                right = function.getParamByName("z")
                            }
                        }
                    }
                    addReturnStatement {
                        buildSDivide("divRes"){
                            left = buildLoad(addResult.value, "addLoad")
                            right = createInt32Value(3)
                        }
                    }
                }
            }
        }

        createFunction("main"){
            returnType = Type.Int32Type()
            addBlock("test_block_1"){
                startBuilder {
                    val message = getGlobalReference(decimalPrintStr.name)?.value ?: decimalPrintStr.value
                    val gep = buildGetElementPointer("messageGEP", message){
                        index(createInt32Value(0))
                    }
                    val addVar = createLocalVariable("addCallRes", Type.Int32Type()){
                        buildFunctionCall("addFuncCall", add){
                            arrayOf(createInt32Value(10), globalVar.value)
                        }
                    }
                    val calcAvVar = createLocalVariable("calcAvCallRes", Type.Int32Type()){
                        buildFunctionCall("calcAvFuncCall", calcAverage){
                            arrayOf(createInt32Value(10), createInt32Value(8), globalVar.value)
                        }
                    }
                    val gepCast = buildBitcast(gep, Type.PointerType(Type.Int8Type()), "gepCast")
//                    LLVM.LLVMBuildCall(this.builder, printfFunc, PointerPointer(*arrayOf(gepCast.value, add.value.value)), 2, "call")
                    val call1 = buildFunctionCall("call", printf){
                        val addValue = buildLoad(addVar.value, "")
                        arrayOf(gepCast, addValue)
                    }

                    val gepDPrint = buildGetElementPointer("decimalPrint_gep", message){
                        index(createInt32Value(1))
                    }
                    val gepDPrintCast = buildBitcast(gepDPrint, Type.PointerType(Type.Int8Type()), "decimalPrint_cast")
                    buildFunctionCall("printf2", printf){
                        arrayOf(gepDPrintCast, call1)
                    }
                    val call2 = buildFunctionCall("call", printf){
                        val addValue = buildLoad(calcAvVar.value, "")
                        arrayOf(gepCast, addValue)
                    }
                    buildFunctionCall("printf3", printf){
                        arrayOf(gepDPrintCast, call2)
                    }
                    addReturnStatement {
                        createInt32Value(0)
                    }
                }
            }
            println(LLVM.LLVMPrintModuleToString(module.module).string)

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
