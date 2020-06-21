import com.couch.kotlinx.llvm.*
import org.bytedeco.javacpp.BytePointer
import org.bytedeco.llvm.global.LLVM
import java.lang.IllegalStateException
import java.nio.ByteBuffer

fun main(){
    val module = buildModule("structTest"){
        val testStructType = createStruct("testStruct", arrayOf(Type.Int8Type(), Type.Int32Type()))
        val printDGlobal = createGlobalVariable("dPrint", Type.ArrayType(Type.Int8Type(), "%d\\\n".length)){
            createStringValue("%d\n")
        }

        val printf = findFunction("printf") ?: throw IllegalStateException("Attempted to get printf function but couldn't find it.")

        createFunction("testFunc"){
            addBlock("entry"){
                startBuilder {
                    val message = getGlobalReference(printDGlobal.name)?.value ?: throw IllegalStateException("Could not find %d")
                    val gep = buildGetElementPointer("messageGEP", message){
                        index(createInt32Value(0))
                    }
                    val messageCast = buildBitcast(gep, Type.PointerType(Type.Int8Type()), "messageCast")
                    val localTest = createLocalVariable("testLocal"){
                        initStruct(testStructType){
                            field(createInt8Value(10))
                            field(createInt32Value(15))
                        }
                    }
                    val field1 = buildStructFieldAccessor("field1", localTest.value, 1)
                    buildFunctionCall("", printf){
                        val load = buildLoad(field1, "")
                        arrayOf(messageCast, load)
                    }
                    addReturnStatement {
                        null
                    }
                }
            }
        }
    }
    println(LLVM.LLVMPrintModuleToString(module.module).string)
    val error = BytePointer()
    val status = LLVM.LLVMVerifyModule(module.module, LLVM.LLVMPrintMessageAction, error)
    println("Verified module")
    println(status)
    LLVM.LLVMDisposeMessage(error)
    val bitcodeFile = LLVM.LLVMWriteBitcodeToFile(module.module, "structTest.bc")
    if (bitcodeFile != 0) {
        println("error writing bitcode to file...")
    }
}