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

        val testStructType = createStruct("testStruct", arrayOf(Type.Int32Type(), Type.Int8Type()))

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
                    val addResult = createLocalVariable("addResult"){
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
                    val dmessage = getGlobalReference(decimalPrintStr.name)?.value ?: decimalPrintStr.value
                    val gep = buildGetElementPointer("messageGEP", dmessage){
                        index(createInt32Value(0))
                    }
                    val addVar = createLocalVariable("addCallRes"){
                        buildFunctionCall("addFuncCall", add){
                            arrayOf(createInt32Value(10), globalVar.value)
                        }
                    }
                    val calcAvVar = createLocalVariable("calcAvCallRes"){
                        buildFunctionCall("calcAvFuncCall", calcAverage){
                            arrayOf(createInt32Value(10), createInt32Value(8), globalVar.value)
                        }
                    }
                    val gepCast = buildBitcast(gep, Type.PointerType(Type.Int8Type()), "gepCast")
                    buildFunctionCall("call", printf){
                        val addValue = buildLoad(addVar.value, "")
                        arrayOf(gepCast, addValue)
                    }

                    val gepDPrint = buildGetElementPointer("decimalPrint_gep", dmessage){
                        index(createInt32Value(0))
                    }
                    val gepDPrintCast = buildBitcast(gepDPrint, Type.PointerType(Type.Int8Type()), "decimalPrint_cast")
                    buildFunctionCall("call", printf){
                        val addValue = buildLoad(calcAvVar.value, "")
                        arrayOf(gepCast, addValue)
                    }
                    val numArray = createLocalVariable("numArray"){
                        createArray(arrayOf(
                                createInt32Value(5),
                                createInt32Value(10),
                                createInt32Value(15),
                                createInt32Value(20)
                        ), Type.Int32Type())
                    }
                    val gepIndex1 = buildGetElementPointer("numArray_ptr", numArray.value, Type.Int32Type()){
                        index(createInt32Value(0))
                        index(createInt32Value(2))
                    }
                    buildFunctionCall("numPrint1", printf){
                        arrayOf(gepDPrintCast, buildLoad(gepIndex1, "index1"))
                    }
                    val testStruct = createLocalVariable("testStructLocal"){
                        initStruct(testStructType){
                            field(createInt32Value(15))
                            field(createInt8Value(10))
                        }
                    }
                    val gepStructIndex1 = buildStructFieldAccessor("testStructIndex1", testStruct.value, 1)
                    val structIndex1 = buildLoad(gepStructIndex1, "structIndex1")
                    buildFunctionCall("structFieldPrint1", printf){
                        arrayOf(gepDPrintCast, structIndex1)
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
    LLVM.LLVMPrintModuleToFile(module.module, "test.ll", error)
    val status = LLVM.LLVMVerifyModule(module.module, LLVM.LLVMPrintMessageAction, error)
    println("Verified module")
    println(status)
    LLVM.LLVMDisposeMessage(error)
    val bitcodeFile = LLVM.LLVMWriteBitcodeToFile(module.module, "test.bc")
    if (bitcodeFile != 0) {
        println("error writing bitcode to file...")
    }
}
