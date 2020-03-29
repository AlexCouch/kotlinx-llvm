package parsing.mir

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import nl.komponents.kovenant.Kovenant
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.deferred
import nl.komponents.kovenant.task
import parsing.ToylangMainAST
import java.util.*

interface BytecodeTask{

}

interface BytecodeComponent{
    val name: String
    val description: String
}

interface BytecodeComponentFactory<T> where T : BytecodeComponent{
    val nameChannel: Channel<String>
    var descriptionChannel: Channel<String>

    val tasks: ArrayDeque<Promise<T, Exception>>

    infix fun name(name: String)
    infix fun describe(desc: String)

    suspend fun build()
    fun run()
}

interface Opcode: BytecodeComponent{
    val opcode: Byte
}

@ExperimentalCoroutinesApi
class ChunkFactory: BytecodeComponentFactory<Opcode>{
    override val nameChannel: Channel<String> = Channel()
    override var descriptionChannel: Channel<String> = Channel()
    val opcodeChannel = Channel<Byte>()
    override val tasks: ArrayDeque<Promise<Opcode, Exception>> = ArrayDeque()


    override infix fun name(name: String){
        GlobalScope.launch {
            nameChannel.send(name)
        }
    }

    override infix fun describe(desc: String){
        GlobalScope.launch {
            descriptionChannel.send(desc)
        }
    }

    infix fun with(opcode: Byte){
        GlobalScope.launch {
            opcodeChannel.send(opcode)
        }
    }

    override fun run(){

    }

    override suspend fun build(){
        GlobalScope.launch {
            val name = nameChannel.receive()
            val description = descriptionChannel.receive()
            val opcode = opcodeChannel.receive()
            val promise = task {
                object : Opcode{
                    override val opcode: Byte = opcode
                    override val name: String = name
                    override val description: String = description
                }
            }.fail {
                println("An error occurred while building opcode")
                println(it.message)
            }
            tasks.push(promise)
        }
    }

}



/*object ChunkFactory: BytecodeComponentFactory<Chunk>{
    override var name = ""
    override var description = ""

    private var chunkData = ArrayDeque<BytecodeComponent>()

    override infix fun name(name: String) {
        this.name = name
    }

    override infix fun describe(desc: String) {
        this.description = desc
    }

    fun createOpcode(block: OpcodeFactory.()->Unit){
        task{
            OpcodeFactory.block()
            this.chunkData.push(OpcodeFactory.build())
        }
    }

    override fun build(): Chunk = Chunk(this.chunkData, this.name, this.description)
}*/

/*fun createChunk(block: ChunkFactory.()->Unit): Chunk{
    ChunkFactory.block()
    return ChunkFactory.build()
}

fun createBytecode(block: ChunkFactory.()->Unit): ByteArray{
    ChunkFactory.block()
    return ChunkFactory.build()
}*/

/*fun test(){
    val engine = BytecodeFactory.setupBytecode {
        createChunk {
            this name "expression"
            this describe "An expression of some type"

            select{
                expect{ input (Any?) ->
                    when(input){
                        is Int -> createOpcode{
                            this with 0x41
                            this name "Integer literal"
                            this describe "An integer literal"
                        }.prefix()
                        is Float -> createOpcode{
                            this with 0x42
                            this name "Decimal literal"
                            this describe "A decimal literal"
                        }.prefix()
                        is String -> createOpcode{
                            this with 0x41
                            this name "Integer literal"
                            this describe "An integer literal"
                        }.prefix()
                    }
                }
            }
        }
        createChunk {
            this name "globalvar"
            this describe "This chunk represents a global variable"
            createOpcode{
                this with 0x21
                this name "GLOBAL_VAR"
                this describe "A global variable and its assigned expression"
            }
            createOpcode{
                this with 0x25
                this name "ASSIGNMENT"
                this describe "This opcode is a signaling opcode for the start of an assignment expression"
            }
            addOpcode("EXPRESSION")
        }
    }
    engine.construct{
        getChunk("globalvar"){
            inputOf{
                if(someList.contain(...)){
                    "someIdentifier"
                }else{
                    error(InvalidStateException("..."))
                }
            }

        }
    }
}*/

data class Chunk(val opcodes: Deque<BytecodeComponent>, override val name: String, override val description: String): BytecodeComponent

class BytecodeFactory{
    fun init(){

    }
}