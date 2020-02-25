package com.couch.kotlinx.parsing

import com.couch.kotlinx.Scope
import com.couch.kotlinx.Symbol
import com.couch.kotlinx.ast.*
import com.couch.kotlinx.llvm.Type
import com.couch.kotlinx.llvm.Value
import com.couch.kotlinx.llvm.createGlobalVariable
import com.strumenta.kolasu.model.*

class GeneralParsingStage{
    private val startScope = Scope.GlobalScope()
    private var currentScope: Scope = startScope
    fun startParsing(rootNode: RootNode){
        rootNode.walkChildren().forEach {
            when (it) {
                is LetNode -> {
                    this.currentScope.symbols.add(this.createLetSymbol(it))
                }
                is FunctionDeclNode -> {
                    this.parseFunctionDeclNode(it)
                }
            }
        }

        rootNode.scope = this.startScope
        rootNode.scope!!.assignParents()
    }

    fun createLetSymbol(letNode: LetNode): Symbol = Symbol.VarSymbol(letNode.identifier.identifier)

    fun parseFunctionDeclNode(functionDeclNode: FunctionDeclNode){
        this.currentScope.symbols.add(Symbol.FunctionDeclSymbol(functionDeclNode.identifier.identifier))
        val newScope = Scope.FunctionScope()
        this.currentScope.addChildScope(newScope)
        this.currentScope = newScope
        functionDeclNode.params.withIndex().forEach {(idx, it) ->
            this.currentScope.symbols.add(Symbol.FunctionParamSymbol(it.identifier.identifier, idx))
        }
        functionDeclNode.codeBlock.statements.forEach{
            when(it){
                is LetNode -> this.currentScope.symbols.add(this.createLetSymbol(it))
                //Add more as time goes on
            }
        }
        functionDeclNode.scope = this.currentScope
        this.currentScope = this.currentScope.parent as? Scope ?: this.startScope
    }
}