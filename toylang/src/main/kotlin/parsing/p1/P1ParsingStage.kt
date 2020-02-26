package com.couch.kotlinx.parsing.p1

import com.couch.kotlinx.parsing.*
import com.strumenta.kolasu.model.assignParents
import com.strumenta.kolasu.model.walkChildren

interface Context

interface ProvidesContext{
    val context: Context
}

class GeneralParsingStage {
    fun parseRootNode(rootNode: ToylangMainAST.RootNode): ToylangP1ASTNode.RootNode{
        val statements = rootNode.walkChildren().map<ToylangMainAST.StatementNode, ToylangP1ASTNode.StatementNode> {
            when (it) {
                is ToylangMainAST.StatementNode.LetNode -> {
                    this.parseLetNode(it)
                }
                is ToylangMainAST.StatementNode.FunctionDeclNode -> {
                    this.parseFunctionDeclNode(it)
                }
                else -> println("Skipping node...")
            }
        }.toList()
        return ToylangP1ASTNode.RootNode(statements)
    }

    fun parseLetNode(letNode: ToylangMainAST.StatementNode.LetNode): ToylangP1ASTNode.StatementNode.VariableNode{
        val variableNode = ToylangP1ASTNode.StatementNode.VariableNode()
    }

    fun parseFunctionDeclNode(functionDeclNode: ToylangMainAST.StatementNode.FunctionDeclNode){
        functionDeclNode.scope.symbols.add(P1Symbol.FunctionDeclSymbol(functionDeclNode.identifier.identifier))
        functionDeclNode.params.withIndex().forEach {(idx, it) ->
            functionDeclNode.scope.symbols.add(P1Symbol.FunctionParamSymbol(it.identifier.identifier, idx))
        }
        functionDeclNode.codeBlock.statements.forEach{
            when(it){
                is ToylangMainAST.StatementNode.LetNode -> functionDeclNode.scope.symbols.add(this.parseLetNode(it))
                //Add more as time goes on
            }
        }
    }
}