package com.couch.kotlinx

import com.couch.kotlinx.ast.IdentifierNode
import com.strumenta.kolasu.model.Derived
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.walkAncestors

sealed class Symbol(open val symbol: IdentifierNode){
    data class FunctionParamSymbol(override val symbol: IdentifierNode, val paramIndex: Int): Symbol(symbol)
    data class FunctionDeclSymbol(override val symbol: IdentifierNode): Symbol(symbol)
    data class VarSymbol(override val symbol: IdentifierNode): Symbol(symbol)
}

sealed class Scope: Node(){
    @Derived
    val symbols = arrayListOf<Symbol>()
    val childScopes = arrayListOf<Scope>()

    class GlobalScope: Scope()
    class FunctionScope: Scope()

    fun doesSymbolExist(identifier: String): Boolean{
        var symbolFound = false
        this.symbols.forEach{
            if(it.symbol.identifier != identifier) {
                this.walkAncestors().forEach {
                    val parentScope = it as Scope
                    symbolFound = parentScope.doesSymbolExist(identifier)

                }
            }else{
                symbolFound = true
            }
        }
        return symbolFound
    }

    fun getSymbol(identifier: String): Symbol?{
        var symbolNodeRef: Symbol? = null
        this.symbols.withIndex().forEach{(idx, it) ->
            if(it.symbol.identifier == identifier) {
                symbolNodeRef = symbols[idx]
            }
            this.walkAncestors().forEach {
                val parentScope = it as Scope
                symbolNodeRef = parentScope.getSymbol(identifier)
            }
        }
        return symbolNodeRef
    }
}