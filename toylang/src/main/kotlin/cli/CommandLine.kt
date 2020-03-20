package cli

data class CLIArgs(val paths: List<String>, val immediateMode: Boolean, val outPath: String, val runAfter: Boolean)

fun Array<String>.parseArgs(): CLIArgs{
    this.joinToString(separator = "")
    var inPath = ""
    var immediateMode = false
    var outputPath = ""
    var runAfter = false
    val inPathPattern = Regex("--path=([\\w\\d\\W]+)")
    val immediateModePattern = Regex("--run=vm")
    val outPathPattern = Regex("--output=([\\w\\d\\W]+)")
    val runAfterPattern = Regex("--after")
    this@parseArgs.forEach {
        when{
            it.matches(inPathPattern) -> inPath = System.getProperty("user.dir") + "\\" + (inPathPattern.find(it)?.groups?.get(1)?.value ?: "")
            it.matches(immediateModePattern) -> immediateMode = immediateModePattern.find(it) != null
            it.matches(outPathPattern) -> outputPath = System.getProperty("user.dir") + "\\" +  (outPathPattern.find(it)?.groups?.get(1)?.value ?: "")
            it.matches(runAfterPattern) -> runAfter = runAfterPattern.find(it) != null
        }
    }
    return CLIArgs(inPath.split(' '), immediateMode, outputPath, runAfter)
}