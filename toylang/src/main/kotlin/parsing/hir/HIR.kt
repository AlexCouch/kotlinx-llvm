package parsing.hir

import bytecode.*
import parsing.ToylangMainAST

fun hirBytecode(){
    val engine = BytecodeEngine.init {
        createChunk("global_var"){
            createOpcode(0x30){
                this.name = "GLOBAL_VAR"
                this.description = "This is used to denote the beginning of a global variable"
                this.stringify {
                    "global.var"
                }
            }
            include(StringWriter)
        }
    }
    BytecodeInputApplication.init<ToylangMainAST>(engine){
        this.use(IteratorApp){

        }
    }
}