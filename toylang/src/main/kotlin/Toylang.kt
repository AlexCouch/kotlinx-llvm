package com.couch.kotlinx

import ErrorResult
import ToylangVisitor
import WrappedResult
import com.couch.kotlinx.llvm.Module
import parsing.llvm.ASTToLLVM
import parsing.ToylangMainAST
import com.couch.kotlinx.parsing.p1.GeneralParsingStage
import com.couch.toylang.ToylangLexer
import com.couch.toylang.ToylangParser
import com.strumenta.kolasu.model.*
import org.antlr.v4.kotlinruntime.*
import org.bytedeco.javacpp.BytePointer
import org.bytedeco.llvm.global.LLVM
import parsing.p1.ToylangP1ASTNode
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream

const val DEBUG = true

fun main(){
    val testFile = File("test.toy")
    val `in` = BufferedInputStream(FileInputStream(testFile))
    val src = String(`in`.readBytes(), charset=Charsets.UTF_8)
    val lexer = ToylangLexer(CharStreams.fromString(src))
    val parser = ToylangParser(CommonTokenStream(lexer))
    val tree = parser.toylangFile()
    val visitor = ToylangVisitor()
    println("About to visit parsing.ast")
    val rootNode = when(val visitorResult = visitor.visit(tree)!!){
        is WrappedResult<*> -> {
            when(visitorResult.t){
                is ToylangMainAST.RootNode -> visitorResult.t
                else -> {
                    println("Grammar visitor result value came back abnormal")
                    println(visitorResult)
                    return
                }
            }
        }
        is ErrorResult -> {
            println("An error occurred while visiting grammar tree")
            println(visitorResult)
            return
        }
        else -> {
            println("Unrecognized result while parsing grammar to general AST: $visitorResult")
            return
        }
    }
    rootNode.assignParents()
    rootNode.walkChildren().forEach {
        println(it.debugPrint())
    }
    val generalParsingStage = GeneralParsingStage()
    val phase1AST = when(val result = generalParsingStage.parseRootNode(rootNode)){
        is WrappedResult<*> -> {
            when(result.t){
                is ToylangP1ASTNode.RootNode -> result.t
                else -> {
                    println("Phase 1 AST result came back abnormal")
                    println(result)
                    return
                }
            }
        }
        is ErrorResult -> {
            println("An error occurred while parsing general AST into phase 1 AST")
            println(result)
            return
        }
        else -> {
            println("Unrecognized result while parsing general AST to phase 1 AST: $result")
            return
        }
    }
    val astToLLVM = ASTToLLVM()
    val module = when(val result = astToLLVM.startGeneratingLLVM(testFile.name, phase1AST)){
        is WrappedResult<*> -> {
            when(result.t){
                is Module -> result.t
                else -> {
                    println("LLVM generation result came back abnormal")
                    println(result)
                    return
                }
            }
        }
        is ErrorResult -> {
            println("An error occurred while parsing phase 1 AST to LLVM")
            println(result)
            return
        }
        else -> {
            println("Unrecognized result while parsing phase 1 AST to LLVM: $result")
            return
        }
    }
    println(LLVM.LLVMPrintModuleToString(module.module).string)
    val buffer = BytePointer()
    LLVM.LLVMVerifyModule(module.module, LLVM.LLVMAbortProcessAction, buffer)
    LLVM.LLVMDisposeMessage(buffer)
    val result = LLVM.LLVMWriteBitcodeToFile(module.module, "test.bc")
    println(result)
}