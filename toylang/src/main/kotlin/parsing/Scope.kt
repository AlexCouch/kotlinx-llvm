package com.couch.kotlinx.parsing

import com.couch.kotlinx.ast.ToylangASTNode
import com.strumenta.kolasu.model.*

interface Symbol{
    val symbolName: String
    val node: ToylangASTNode
}

abstract class Scope: Node(){
    val symbols = arrayListOf<Symbol>()
    val childScopes = arrayListOf<Scope>()



    fun doesSymbolExist(identifier: String): Boolean{
        var symbolFound = false
        forEachSymbol@ for(symbol in symbols){
            if(symbol.symbolName == identifier) {
                symbolFound = true
                break
            }
            val parentScope = this.parent as? Scope
            for(parentSymbol in parentScope?.symbols ?: continue) {
                symbolFound = parentScope.doesSymbolExist(identifier)
                if(symbolFound) break@forEachSymbol
            }
        }
        if(!symbolFound){
            this.walkAncestors().forEach {
                val scope = it as Scope
                if(scope.doesSymbolExist(identifier)){
                    symbolFound = true
                }
            }
        }
        return symbolFound
    }

    fun addChildScope(scope: Scope){
        this.childScopes.add(scope)
        this.assignParents()
    }

    fun getSymbol(identifier: String): Symbol?{
        var symbolNodeRef: Symbol? = null
        this.walk().forEach {
            val scope = it as Scope
            scope.symbols.withIndex().forEach{(idx, it) ->
                if(it.symbolName == identifier) {
                    symbolNodeRef = symbols[idx]
                }
            }
            if(symbolNodeRef == null){
                scope.walkAncestors().forEach {
                    val parentScope = it as Scope
                    symbolNodeRef = parentScope.getSymbol(identifier)
                }
            }
        }
        return symbolNodeRef
    }
}