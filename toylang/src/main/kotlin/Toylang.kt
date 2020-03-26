import cli.parseArgs
import cli.runCompiler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import parsing.ParserErrorResult

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
    }
}