package com.couch.kotlinx

import com.couch.kotlinx.ast.RootNode
import com.couch.toylang.ToylangLexer
import com.couch.toylang.ToylangParser
import com.strumenta.kolasu.model.assignParents
import com.strumenta.kolasu.model.debugPrint
import com.strumenta.kolasu.model.mapTree
import com.strumenta.kolasu.model.walk
import org.antlr.v4.kotlinruntime.*
import org.antlr.v4.kotlinruntime.tree.ParseTreeWalker

const val DEBUG = true

fun main(){
    val testFile = CharStreams.fromFileName("src/main/resources/test.toy")
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
    println(rootNode.debugPrint())
}