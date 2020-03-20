package com.couch.kotlinx

import ErrorResult
import ToylangVisitor
import WrappedResult
import com.couch.kotlinx.llvm.Module
import parsing.llvm.ASTToLLVM
import parsing.ToylangMainAST
import parsing.hir.HIRGenerator
import com.couch.toylang.ToylangLexer
import com.couch.toylang.ToylangParser
import com.strumenta.kolasu.model.*
import org.antlr.v4.kotlinruntime.*
import org.bytedeco.javacpp.BytePointer
import org.bytedeco.llvm.global.LLVM
import parsing.GlobalContext
import parsing.ParserErrorResult
import parsing.hir.ToylangHIRElement
import parsing.typeck.TypecheckingParser
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.nio.file.Path
import java.nio.file.Paths

const val DEBUG = true

fun start(file: File): String{
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
                else -> {
                    println("Grammar visitor result value came back abnormal")
                    println(visitorResult)
                    return ""
                }
            }
        }
        is ErrorResult -> {
            println("An error occurred while visiting grammar tree")
            println(visitorResult)
            return ""
        }
        else -> {
            println("Unrecognized result while parsing grammar to general AST: $visitorResult")
            return ""
        }
    }
    rootNode.assignParents()
    /*rootNode.walkChildren().forEach {
        println(it.debugPrint())
    }*/
    val generalParsingStage = HIRGenerator()
    val phase1AST = when(val result = generalParsingStage.parseFile(rootNode, GlobalContext())){
        is WrappedResult<*> -> {
            when(result.t){
                is ToylangHIRElement.RootNode -> result.t
                else -> {
                    println("Phase 1 AST result came back abnormal")
                    println(result)
                    return ""
                }
            }
        }
        is ErrorResult -> {
            println("An error occurred while parsing general AST into phase 1 AST")
            println(result)
            return ""
        }
        else -> {
            println("Unrecognized result while parsing general AST to phase 1 AST: $result")
            return ""
        }
    }
    phase1AST.assignParents()
    val typechecking = TypecheckingParser()
    when(val typeCheckResult = typechecking.parseFile(phase1AST, phase1AST.context)){
        is ErrorResult -> {
            println("An error occurred during type checking")
            println(typeCheckResult)
            return ""
        }
        is ParserErrorResult<*> -> {
            println("A parser error occurred during type checking")
            println(typeCheckResult)
            return ""
        }
    }
    val astToLLVM = ASTToLLVM()
    val module = when(val result = astToLLVM.startGeneratingLLVM(file.name, phase1AST)){
        is WrappedResult<*> -> {
            when(result.t){
                is Module -> result.t
                else -> {
                    println("LLVM generation result came back abnormal")
                    println(result)
                    return ""
                }
            }
        }
        is ErrorResult -> {
            println("An error occurred while parsing phase 1 AST to LLVM")
            println(result)
            return ""
        }
        else -> {
            println("Unrecognized result while parsing phase 1 AST to LLVM: $result")
            return ""
        }
    }
//    println(LLVM.LLVMPrintModuleToString(module.module).string)
    val buffer = BytePointer()
    LLVM.LLVMVerifyModule(module.module, LLVM.LLVMAbortProcessAction, buffer)
    LLVM.LLVMDisposeMessage(buffer)
    val outfile = File("${System.getProperty("user.dir")}/out/${file.nameWithoutExtension}.bc")
    if(!outfile.exists() || !outfile.parentFile.exists()) {
        outfile.parentFile.mkdirs()
    }
    val result = LLVM.LLVMWriteBitcodeToFile(module.module, outfile.path)
    if(result != 0){
        println("Write bitcode to file operation returned with non-zero exit status: $result")
    }
    ProcessBuilder().command("llc", outfile.path).start()
    return "${outfile.parent}/${outfile.nameWithoutExtension}.s"
}

fun main(args: Array<String>){
    val pattern = Regex("--path=([\\w\\d\\W]+)")
    args.apply {
        this.isNotEmpty()
    }.let { paths ->
        val llPaths = arrayListOf<String>()
        paths.forEach { path ->
            val match = pattern.find(path)
            if(match != null){
                val file = File(match.groups[1]?.value ?: "")
                if(file.isDirectory){
                    file.listFiles()?.forEach {
                        llPaths.add(Paths.get(System.getProperty("user.dir")).relativize(Paths.get(start(it))).toString())
                    }?: llPaths.add("")
                }else{
                    llPaths.add(start(file))
                }
            }else{
                llPaths.add("")
            }
        }
        val clang = ProcessBuilder().command("clang", *llPaths.toTypedArray(), "-o", "out.exe")
        println(clang.command())
        clang.redirectError()
        clang.start()
    }


}