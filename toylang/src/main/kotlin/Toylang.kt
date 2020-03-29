import cli.parseArgs
import cli.runCompiler
import cli.runVM
import kotlinx.coroutines.ExperimentalCoroutinesApi
import parsing.ParserErrorResult

@ExperimentalStdlibApi
@ExperimentalCoroutinesApi
fun main(args: Array<String>){
    val cliArgs = args.parseArgs()
    if(!cliArgs.immediateMode){
        val outputPath = when(val result = runCompiler(cliArgs.paths, cliArgs.outPath)){
            is ErrorResult -> {
                println("An error occurred while running the compiler")
                println(result)
                return
            }
            is ParserErrorResult<*> -> {
                println("An error occurred while running the compiler")
                println(result)
                return
            }
            is WrappedResult<*> -> result.t as String
            else -> {
                println("Did not get an output path from compiler")
                println(result)
                return
            }
        }
        val program = ProcessBuilder(outputPath).inheritIO().start()
        val exitCode = program.waitFor()
    }else{
        when(val result = runVM(cliArgs.paths)){
            is ErrorResult -> {
                println("An error occurred while running the virtual machine")
                println(result)
                return
            }
            is ParserErrorResult<*> -> {
                println("An error occurred while running the virtual machine")
                println(result)
                return
            }
            is WrappedResult<*> -> result.t as String
            else -> {
                println("Did not get an output path from virtual machine")
                println(result)
                return
            }
        }
    }
}