package com.couch.kotlinx

import com.couch.toylang.ToylangLexer
import com.couch.toylang.ToylangParser
import org.antlr.v4.kotlinruntime.*
import org.antlr.v4.kotlinruntime.tree.ParseTreeWalker

const val DEBUG = true

fun main(){
    val testFile = CharStreams.fromFileName("src/main/resources/test.toy")
    val lexer = ToylangLexer(testFile)
    val parser = ToylangParser(CommonTokenStream(lexer))
    val listener = ToylangListener()
    val walker = ParseTreeWalker()
    walker.walk(listener, parser.toylangFile())
}