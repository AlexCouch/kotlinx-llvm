package com.couch.kotlinx.parsing.p1

import com.couch.kotlinx.ast.ToylangASTNode
import com.couch.kotlinx.parsing.Symbol

sealed class P1Symbol(override val symbolName: String, override val node: ToylangASTNode): Symbol{
    data class FunctionParamSymbol(override val symbolName: String, val paramIndex: Int, override val node: ToylangASTNode): P1Symbol(symbolName, node)
    data class FunctionDeclSymbol(override val symbolName: String, override val node: ToylangASTNode): P1Symbol(symbolName, node)
    data class VarSymbol(override val symbolName: String, override val node: ToylangASTNode): P1Symbol(symbolName, node)
}