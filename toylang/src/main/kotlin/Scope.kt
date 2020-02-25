package com.couch.kotlinx

import com.couch.kotlinx.ast.IdentifierNode
import com.couch.kotlinx.llvm.Function
import com.couch.kotlinx.llvm.Variable
import com.strumenta.kolasu.model.*

sealed class Symbol(open val symbol: String){
    data class FunctionParamSymbol(override val symbol: String, val paramIndex: Int): Symbol(symbol)
    data class FunctionDeclSymbol(override val symbol: String, var function: Function? = null): Symbol(symbol)
    data class VarSymbol(override val symbol: String, var variable: Variable? = null): Symbol(symbol)
}

sealed class Scope: Node(){
    val symbols = arrayListOf<Symbol>()
    val childScopes = arrayListOf<Scope>()

    class GlobalScope: Scope()
    class FunctionScope: Scope()

    fun doesSymbolExist(identifier: String): Boolean{
        var symbolFound = false
        forEachSymbol@ for(symbol in symbols){
            if(symbol.symbol == identifier) {
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
                if(it.symbol == identifier) {
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