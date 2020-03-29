package bytecode

import kotlinx.serialization.internal.HexConverter
import Result
import WrappedResult
import ErrorResult
import OKResult
import buildPrettyString
import kotlinx.io.core.ByteReadPacket
import kotlinx.io.core.buildPacket

data class Opcode(val code: Byte, val name: String, val description: String, val textualRepresentation: String = ""){
    override fun toString(): String = buildString{
        append("$name[${HexConverter.printHexBinary(byteArrayOf(code))}];$description")
        append(textualRepresentation)
    }
}

object OpcodeBuilder{
    /**
     *
     */
    var name: String = ""
    var description: String = ""
    private var stringify = ""

    fun stringify(block: OpcodeBuilder.() -> String){
        stringify = this.block()
    }

    fun build(code: Byte): Opcode{
        val opcode = Opcode(code, this.name, this.description, this.stringify)
        this.name = ""
        this.description = ""
        this.stringify = ""
        return opcode
    }
}

class BytecodeChunk{
    internal val instructions = ArrayList<Opcode>()

    fun createOpcode(code: Int, block: OpcodeBuilder.() -> Unit){
        OpcodeBuilder.block()
        this.instructions += OpcodeBuilder.build((code and 0xff).toByte())
    }
}

internal interface ChunkContainer{
    val chunks: HashMap<String, BytecodeChunk>
}
internal interface ChunkProducer{
    val chunk: BytecodeChunk
}

data class FallCatcher(val catchCallback: suspend BytecodeApp<*>.(Throwable)->Unit)
internal interface Fallable{
    var catchCallback: FallCatcher
}

object ErrorRecovery{
    suspend fun createMessage(throwable: Throwable): String = buildPrettyString{
        buildPrettyString {
            appendWithNewLine("Error message: ${throwable.message}")
            if(throwable.cause != null) {
                appendWithNewLine("Cause: ${createMessage(throwable.cause as Throwable)}")
            }
        }
    }

    suspend fun report(throwable: Throwable){
        println(createMessage(throwable))
    }
}

abstract class BytecodeApp<T>: Fallable{
    override var catchCallback = FallCatcher {
        ErrorRecovery.report(it)
    }

    fun catch(block: suspend BytecodeApp<*>.(Throwable)->Unit){
        this.catchCallback = FallCatcher(block)
    }

    abstract fun configure(block: T.()->Unit)
}

object StringWriter: BytecodeApp<StringWriter>(){
    fun writeString(string: String): ByteReadPacket = buildPacket{
        writeStringUtf8("$string\\0")
    }

    override fun configure(block: StringWriter.() -> Unit) {
        this.block()
    }
}

object IteratorApp: BytecodeApp<IteratorApp>(){
    override fun configure(block: IteratorApp.() -> Unit) {
        TODO("Not yet implemented")
    }

}

/*
//Initialize the core bytecode engine. The result is the built engine object that can then be used by the IO Apps
val HIREngine = BytecodeEngine.init{
    configureBytecode{
        //Configure a chunk of bytecode called "identifier" which will instruct the engine how to construct this chunk.
        //The order to which you call these factory methods dictates the exact order and mechanism the engine will construct the chunk
        //This chunk can then be referenced by the IO Apps, for constructing a certain chunk from a certain input at a certain time
        //Or for deserializing, converting, or "lowering" by the output apps.
        createChunk("identifier"){
            createOpcode(0x11){
                this.name = "IDENTIFIER"
                this.description = "This denotes an identifier and the engine should read in a null-terminated string after reading this byte"
            }
            //Include the StringWriter plugin which will tell the engine to read in a string following the read of the previous opcode 0x11
            include{
                //Configure the StringReader plugin
                plugin(StringWriter){
                    onCatch{ it ->
                        //`it` is the exception that was caught during string writing
                        ErrorManager.createErrorMessage(it)
                    }
                }
                plugin(Stringify)
            }
        }
        //Create a chunk called "global_variable". It is the opcode 0x35, followed by an "identifier" chunk, followed by an "expression" chunk.
        createChunk("global_variable"){
            createOpcode(0x35){
                this.name = "GLOBAL_VAR"
                this.description = "This denotes the start of a global variable and should be treated as such"
                stringify("global.var")
            }
            include{
                chunk("identifier")
            }
            include{
                chunk("expression")
            }
        }
    }
}
 */

class BytecodeEngine {
    /**
     * A map of chunk names to chunks
     */
    internal val bytecodeChunks = HashMap<String, BytecodeChunk>()
    internal val inclusions = ArrayList<BytecodeApp<*>>()

    fun <T> include(module: T, block: T.() -> Unit) where T : BytecodeApp<T>{
        module.block()
        this.inclusions += module
    }

    fun <T> include(module: T) where T : BytecodeApp<T>{
        this.inclusions += module
    }

    fun createChunk(name: String, block: BytecodeChunk.() -> Unit){
        val chunk = BytecodeChunk()
        chunk.block()
        bytecodeChunks[name] = chunk
    }


    companion object{
        fun init(block: BytecodeEngine.() -> Unit): BytecodeEngine{
            val engine = BytecodeEngine()
            engine.block()
            return engine
        }

    }

}

class BytecodeInputApplication<T>(private val engine: BytecodeEngine){
    private val modulesUsed = ArrayList<BytecodeApp<*>>()

    fun invoke(data: T){

    }

    fun <T: BytecodeApp<*>> use(module: T, block: BytecodeInputApplication<*>.() -> Unit){
        this.modulesUsed += module
        this.block()
    }

    fun configure(block: BytecodeInputApplication<*>.()->Unit){
        this.block()
    }

    companion object{
        fun <T> init(engine: BytecodeEngine, block: BytecodeInputApplication<*>.() -> Unit): BytecodeInputApplication<*>{
            val app = BytecodeInputApplication<T>(engine)
            app.block()
            return app
        }
    }
}

class BytecodeOutputEngine{

}
