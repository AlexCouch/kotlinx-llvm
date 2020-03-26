package cli

import ErrorResult
import OKResult
import ToylangVisitor
import WrappedResult
import com.couch.kotlinx.llvm.Module
import com.couch.toylang.ToylangLexer
import com.couch.toylang.ToylangParser
import com.strumenta.kolasu.model.assignParents
import org.antlr.v4.kotlinruntime.CharStreams
import org.antlr.v4.kotlinruntime.CommonTokenStream
import org.bytedeco.javacpp.BytePointer
import org.bytedeco.llvm.global.LLVM
import parsing.GlobalContext
import parsing.ParserErrorResult
import parsing.ToylangMainAST
import parsing.llvm.ASTToLLVM
import parsing.typeck.TypecheckingParser
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import Result
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.internal.HexConverter
import parsing.ast.ToylangASTNode
import parsing.hir.*
import java.util.concurrent.TimeUnit

fun runParser(file: File): Result{
    val `in` = BufferedInputStream(FileInputStream(file))
    val src = String(`in`.readBytes(), charset=Charsets.UTF_8)
    val lexer = ToylangLexer(CharStreams.fromString(src))
    val parser = ToylangParser(CommonTokenStream(lexer))
    val tree = parser.toylangFile()
    val visitor = ToylangVisitor()
    val rootNode = when(val visitorResult = visitor.visitToylangFile(tree)){
        is WrappedResult<*> -> {
            when(visitorResult.t){
                is ToylangMainAST.RootNode -> visitorResult.t
                else -> return ErrorResult("Grammar visitor result value came back abnormal: $visitorResult")
            }
        }
        is ErrorResult -> {
            return ErrorResult("An error occurred while visiting grammar tree: $visitorResult")
        }
        else -> {
            return ErrorResult("Unrecognized result while parsing grammar to general AST: $visitorResult")
        }
    }

    rootNode.assignParents()
    return WrappedResult(rootNode)
}

/*fun genHIR(rootnode: ToylangMainAST.RootNode): Result{
    val generalParsingStage = HIRGenerator()
    val phase1AST = when(val result = generalParsingStage.parseFile(rootnode, GlobalContext())){
        is WrappedResult<*> -> {
            when(result.t){
                is ToylangHIRElement.RootNode -> result.t
                else -> {
                    return ErrorResult("Phase 1 AST result came back abnormal: $result")
                }
            }
        }
        is ErrorResult -> {
            return ErrorResult("An error occurred while parsing general AST into phase 1 AST: $result")
        }
        else -> {
            return ErrorResult("Unrecognized result while parsing general AST to phase 1 AST: $result")
        }
    }
    phase1AST.assignParents()
    return WrappedResult(phase1AST)
}*/

fun compile(file: File, objectFilePaths: ArrayList<String>): Result{
    val ast = when (val result = runParser(file)) {
        is WrappedResult<*> -> when (result.t) {
            is ToylangMainAST.RootNode -> {
                result.t
            }
            else -> return ErrorResult("Could not get ast from parser")
        }
        is ErrorResult -> return result
        else -> return ErrorResult("Unrecogtnized result: $result")
    }
    ast.metadata = ToylangMainAST.MetadataNode(file.absolutePath, ToylangTarget.LLVM)
    val stream = ToylangMainASTBytecode().dump(ToylangMainASTBytecodeSerialization, ast)
    println(when(val result = stringifyBytecode(stream)){
        is WrappedResult<*> -> result.t
        is ErrorResult -> {
            result.toString()
        }
        else -> "Could not get stringified bytecode from stringify function: Unrecognized result: $result"
    })
    /*val hir = when (val result = genHIR(ast)) {
        is WrappedResult<*> -> when (result.t) {
            is ToylangHIRElement.RootNode -> {
                result.t
            }
            else -> {
                return ErrorResult("Did not get hir from generator")
            }
        }
        is ErrorResult -> {
            return result
        }
        else -> return ErrorResult("Unrecogtnized result: $result")
    }
    val typechecking = TypecheckingParser()
    when (val typeCheckResult = typechecking.parseFile(hir, hir.context)) {
        is ErrorResult -> {
            return ErrorResult("An error occurred during type checking: $typeCheckResult")
        }
        is ParserErrorResult<*> -> {
            return ErrorResult("A parser error occurred during type checking: $typeCheckResult")
        }

    }
    val astToLLVM = ASTToLLVM()
    val module = when (val result = astToLLVM.startGeneratingLLVM(file.name, hir)) {
        is WrappedResult<*> -> {
            when (result.t) {
                is Module -> result.t
                else -> {
                    return ErrorResult("LLVM generation result came back abnormal: $result")
                }
            }
        }
        is ErrorResult -> {
            return ErrorResult("An error occurred while parsing phase 1 AST to LLVM", result)
        }
        else -> {
            return ErrorResult("Unrecognized result while parsing phase 1 AST to LLVM: $result")
        }
    }
    val buffer = BytePointer()
    LLVM.LLVMVerifyModule(module.module, LLVM.LLVMAbortProcessAction, buffer)
    LLVM.LLVMDisposeMessage(buffer)
    val outfile = File("${System.getProperty("user.dir")}/out/${file.nameWithoutExtension}.bc")
    if (!outfile.exists() || !outfile.parentFile.exists()) {
        outfile.parentFile.mkdirs()
    }
    val result = LLVM.LLVMWriteBitcodeToFile(module.module, outfile.path)
    if (result != 0) {
        println("Write bitcode to file operation returned with non-zero exit status: $result")
    }
    val llcP = ProcessBuilder().command("llc", outfile.path).inheritIO()
    val llc = llcP.start()
    val exitCode = llc.waitFor()
    if(exitCode != 0){
        return ErrorResult("LLC returned non-zero exit code: $exitCode")
    }
    objectFilePaths.add("${outfile.parent}/${outfile.nameWithoutExtension}.s")*/
    return OKResult
}

@ExperimentalCoroutinesApi
fun runCompiler(paths: List<String>, outputPath: String): Result {
    val objectFilePaths = arrayListOf<String>()
    paths.forEach {
        val file = File(it)
        if(file.isDirectory) {
            file.listFiles()?.forEach { f ->
                when(val result = compile(f, objectFilePaths)){
                    is ErrorResult -> return result
                    is ParserErrorResult<*> -> return ErrorResult("An error occurred during parsing: $result")
                }
            }
        }else{
            when(val result = compile(file, objectFilePaths)){
                is ErrorResult -> return result
                is ParserErrorResult<*> -> return ErrorResult("An error occurred during parsing: $result")
            }
        }

    }
    /*val output = "$outputPath.exe"
    val clangProcess = ProcessBuilder().command("clang", *objectFilePaths.toTypedArray(), "-o", output).inheritIO()
    val clang = clangProcess.start()
    val exitCode = clang.waitFor()
    if(exitCode != 0){
        return ErrorResult("Clang returned with non-zero exit code: $exitCode")
    }
    return WrappedResult(output)*/
    return OKResult
}

fun runVM(paths: List<String>): Result{


    return OKResult
}