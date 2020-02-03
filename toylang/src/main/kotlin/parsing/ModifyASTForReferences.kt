package com.couch.kotlinx.parsing

import com.couch.kotlinx.Scope
import com.couch.kotlinx.ast.ExpressionNode
import com.couch.kotlinx.ast.LetNode
import com.couch.kotlinx.ast.RootNode
import com.strumenta.kolasu.model.walk

class ModifyASTForReferences(val ast: RootNode){
    fun start(){
        this.ast.walk().forEach {
            when(it){
                is LetNode -> {
                    checkForReferences(it.assignment.expression)
                }
            }
        }
    }

    private fun checkForReferences(expression: ExpressionNode){

    }
}