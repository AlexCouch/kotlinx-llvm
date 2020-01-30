package com.couch.kotlinx

import com.couch.kotlinx.ast.*
import com.couch.kotlinx.llvm.*
import com.couch.kotlinx.parsing.ASTToLLVM
import com.couch.kotlinx.parsing.GeneralParsingStage
import com.couch.toylang.ToylangLexer
import com.couch.toylang.ToylangParser
import com.strumenta.kolasu.model.*
import org.antlr.v4.kotlinruntime.*
import org.bytedeco.javacpp.BytePointer
import org.bytedeco.llvm.global.LLVM

const val DEBUG = true

fun main(){
    val testFile = CharStreams.fromFileName("test.toy")
    val lexer = ToylangLexer(testFile)
    val parser = ToylangParser(CommonTokenStream(lexer))
    val tree = parser.toylangFile()
//    val listener = ToylangListener()
//    val parseTreeWalker = ParseTreeWalker()
//    parseTreeWalker.walk(listener, tree)
    val visitor = ToylangVisitor()
    println("About to visit ast")
    val rootNode = visitor.visit(tree)!!
    rootNode.assignParents()
    val generalParsingStage = GeneralParsingStage()
    generalParsingStage.startParsing(rootNode as RootNode)
    val astToLLVM = ASTToLLVM()
    val module = astToLLVM.startGeneratingLLVM(testFile.sourceName!!, rootNode)
    val buffer = BytePointer()
    LLVM.LLVMVerifyModule(module.module, LLVM.LLVMAbortProcessAction, buffer)
    LLVM.LLVMDisposeMessage(buffer)
    val result = LLVM.LLVMWriteBitcodeToFile(module.module, "test.bc")
    println(result)
}