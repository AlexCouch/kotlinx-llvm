package cli

import ErrorResult
import OKResult
import ToylangVisitor
import WrappedResult
import com.couch.toylang.ToylangLexer
import com.couch.toylang.ToylangParser
import com.strumenta.kolasu.model.assignParents
import org.antlr.v4.kotlinruntime.CharStreams
import org.antlr.v4.kotlinruntime.CommonTokenStream
import parsing.ParserErrorResult
import parsing.ToylangMainAST
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import Result
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.io.core.ByteReadPacket
import kotlinx.serialization.internal.HexConverter
import parsing.hir.*
import parsing.mir.MIRBytecode
import parsing.mir.MIRBytecodeSerialization
import parsing.vm.VirtualMachine

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

@ExperimentalStdlibApi
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
    println("HIR: ${HexConverter.printHexBinary(stream)}")
    val mir = MIRBytecode().dump(MIRBytecodeSerialization, ByteReadPacket(stream))
    println("MIR: ${HexConverter.printHexBinary(mir)}")
    val vm = VirtualMachine()
    when(val result = vm.init(ByteReadPacket(mir))){
        is ErrorResult -> return ErrorResult("The VM encountered an error", result)
    }


    return OKResult
}

@ExperimentalStdlibApi
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

@ExperimentalStdlibApi
fun run(file: File): Result{
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
    ast.metadata = ToylangMainAST.MetadataNode(file.absolutePath, ToylangTarget.VM)
    val stream = ToylangMainASTBytecode().dump(ToylangMainASTBytecodeSerialization, ast)
    println("HIR: ${HexConverter.printHexBinary(stream)}")
    val mir = MIRBytecode().dump(MIRBytecodeSerialization, ByteReadPacket(stream))
    println("MIR: ${HexConverter.printHexBinary(mir)}")
    val vm = VirtualMachine()
    when(val result = vm.init(ByteReadPacket(mir))){
        is ErrorResult -> return ErrorResult("The VM encountered an error", result)
    }
    when(val result = vm.start()){
        is ErrorResult -> return ErrorResult("The VM runtime encountered an error", result)
    }
    return OKResult
}

@ExperimentalStdlibApi
fun runVM(paths: List<String>): Result{
    paths.forEach {
        val file = File(it)
        if(file.isDirectory) {
            file.listFiles()?.forEach { f ->
                when(val result = run(f)){
                    is ErrorResult -> return result
                    is ParserErrorResult<*> -> return ErrorResult("An error occurred during parsing: $result")
                }
            }
        }else{
            when(val result = run(file)){
                is ErrorResult -> return result
                is ParserErrorResult<*> -> return ErrorResult("An error occurred during parsing: $result")
            }
        }

    }

    return OKResult
}