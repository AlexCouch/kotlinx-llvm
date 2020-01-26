package com.couch.kotlinx

import com.couch.kotlinx.ast.*
import com.couch.kotlinx.llvm.*
import com.couch.toylang.ToylangLexer
import com.couch.toylang.ToylangParser
import com.strumenta.kolasu.model.*
import org.antlr.v4.kotlinruntime.*
import org.bytedeco.javacpp.BytePointer
import org.bytedeco.llvm.global.LLVM
import java.io.File
import java.io.FileInputStream

const val DEBUG = true

fun convertStringLitNodeToString(stringNode: StringLiteralNode){
    stringNode.content.map {
        val sb = StringBuilder()
        when(it){
            is RawStringLiteralContentNode -> sb.append(it.string)
            is StringInterpolationNode -> {
                when(it.interpolatedExpr){
                    is IntegerLiteralNode -> sb.append(it.interpolatedExpr.integer)
                    is DecimalLiteralNode -> sb.append(it.interpolatedExpr.float)
                    is StringLiteralNode -> sb.append(convertStringLitNodeToString(it.interpolatedExpr))
                }
            }
        }
        sb.toString()
    }
}

class Test

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
    val module: Module? = buildModule(lexer.sourceName) {
        rootNode.walkChildren().forEach {
            println(it)
            when (it) {
                is LetNode -> {
                    when (it.assignment.expression) {
                        is IntegerLiteralNode -> {
                            createGlobalVariable(it.identifier.identifier, Type.Int32Type(), this) {
                                setGlobalInitializer(this, it.assignment.expression.integer)
                            }
                        }
                        is DecimalLiteralNode -> {
                            createGlobalVariable(it.identifier.identifier, Type.FloatType(), this) {
                                setGlobalInitializer(this, it.assignment.expression.float)
                            }
                        }
                        is StringLiteralNode -> {
                            createGlobalVariable(it.identifier.identifier, Type.ArrayType(Type.Int8Type(), it.assignment.expression.content.size), this) {
                                val stringContent = convertStringLitNodeToString(it.assignment.expression)
                                setGlobalInitializer(this, stringContent)
                            }
                        }
                        else -> throw IllegalArgumentException("Could not convert let node properly")
                    }

                }
            }
        }
    }
    val buffer = BytePointer()
    LLVM.LLVMVerifyModule(module!!.module, LLVM.LLVMAbortProcessAction, buffer)
    LLVM.LLVMDisposeMessage(buffer)
    val result = LLVM.LLVMWriteBitcodeToFile(module.module, "test.bc")
    println(result)
}