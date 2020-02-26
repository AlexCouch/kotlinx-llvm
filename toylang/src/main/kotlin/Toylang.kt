package com.couch.kotlinx

import com.couch.kotlinx.ast.*
import com.couch.kotlinx.parsing.llvm.ASTToLLVM
import com.couch.kotlinx.parsing.GeneralParsingStage
import com.couch.toylang.ToylangLexer
import com.couch.toylang.ToylangParser
import com.strumenta.kolasu.model.*
import org.antlr.v4.kotlinruntime.*
import org.bytedeco.javacpp.BytePointer
import org.bytedeco.llvm.global.LLVM
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream

const val DEBUG = true

fun main(){
//    val file = BufferedInputStream(FileInputStream(File("test.txt")))
//    val src = String(file.readBytes(), charset=Charsets.UTF_8)
    val testFile = File("test.toy")
    val `in` = BufferedInputStream(FileInputStream(testFile))
    val src = String(`in`.readBytes(), charset=Charsets.UTF_8)
    val lexer = ToylangLexer(CharStreams.fromString(src))
    val parser = ToylangParser(CommonTokenStream(lexer))
    val tree = parser.toylangFile()
//    val listener = ToylangListener()
//    val parseTreeWalker = ParseTreeWalker()
//    parseTreeWalker.walk(listener, tree)
    val visitor = ToylangVisitor()
    println("About to visit parsing.ast")
    val rootNode = visitor.visit(tree)!!
    rootNode.assignParents()
    rootNode.walkChildren().forEach {
        println(it.debugPrint())
    }
    val generalParsingStage = GeneralParsingStage()
    generalParsingStage.startParsing(rootNode as RootNode)
    val astToLLVM = ASTToLLVM()
    val module = astToLLVM.startGeneratingLLVM(testFile.name, rootNode)
    println(LLVM.LLVMPrintModuleToString(module.module).string)
    val buffer = BytePointer()
    LLVM.LLVMVerifyModule(module.module, LLVM.LLVMAbortProcessAction, buffer)
    LLVM.LLVMDisposeMessage(buffer)
    val result = LLVM.LLVMWriteBitcodeToFile(module.module, "test.bc")
    println(result)
}