package com.couch.kotlinx.parsing

import com.couch.kotlinx.Scope
import com.couch.kotlinx.Symbol
import com.couch.kotlinx.ast.*
import com.couch.kotlinx.llvm.*
import com.strumenta.kolasu.model.walkChildren

class ASTToLLVM{
    fun startGeneratingLLVM(moduleName: String, rootNode: RootNode) = buildModule(moduleName){
            rootNode.walkChildren().forEach {
                when(it){
                    is LetNode -> this.parseLetNode(it)
                    is FunctionDeclNode -> this.parseFunctionDeclNode(it)
                }
            }
        }

    fun Module.parseLetNode(letNode: LetNode){
        when(letNode.assignment.expression){
            is IntegerLiteralNode -> this.createGlobalVariable(letNode.identifier.identifier, Type.Int32Type()){
                this.setGlobalInitializer {
                    Value.Int32ConstValue(letNode.assignment.expression.integer)
                }
            }
            is DecimalLiteralNode -> this.createGlobalVariable(letNode.identifier.identifier, Type.FloatType()){
                this.setGlobalInitializer {
                    Value.FloatConstValue(letNode.assignment.expression.float)
                }
            }
            is StringLiteralNode -> {
                val stringContent = this@ASTToLLVM.parseStringLiteralNode(letNode.assignment.expression)
                this.createGlobalVariable(letNode.identifier.identifier, Type.ArrayType(Type.Int8Type(), stringContent.length + 1)){
                    this.setGlobalInitializer {
                        Value.StringConstValue(stringContent)
                    }
                }
            }
        }
    }

    fun parseStringLiteralNode(stringNode: StringLiteralNode): String{
        return stringNode.content.map {
            when(it){
                is RawStringLiteralContentNode -> {
                    it.string
                }
                is StringInterpolationNode -> {
                    when(it.interpolatedExpr){
                        is IntegerLiteralNode -> {
                            it.interpolatedExpr.integer.toString()
                        }
                        is DecimalLiteralNode -> {
                            it.interpolatedExpr.float.toString()
                        }
                        is StringLiteralNode -> {
                            this.parseStringLiteralNode(it.interpolatedExpr)
                        }
                        else -> throw IllegalArgumentException("Could not parse expression in string interpolation")
                    }
                }
            }
        }.joinToString()
    }

    fun BasicBlock.parseLocalVariableNode(letNode: LetNode){
        when(letNode.assignment.expression){
            is IntegerLiteralNode -> this.createVariable(letNode.identifier.identifier, Type.Int32Type()){
                this.value = Value.Int32ConstValue(letNode.assignment.expression.integer)
            }
            is DecimalLiteralNode -> this.createVariable(letNode.identifier.identifier, Type.FloatType()){
                this.value = Value.FloatConstValue(letNode.assignment.expression.float)
            }
            is StringLiteralNode -> this.createVariable(letNode.identifier.identifier, Type.Int32Type()){
                val stringContent = this@ASTToLLVM.parseStringLiteralNode(letNode.assignment.expression)
                this.value = Value.StringConstValue(stringContent)
            }
        }
    }

    fun convertTypeIdentifier(identifierNode: IdentifierNode): Type = when(identifierNode.identifier){
            "Int" -> Type.Int32Type()
            "Float" -> Type.FloatType()
            "String" -> Type.PointerType(Type.Int8Type())
            else -> Type.VoidType()
    }



    fun Module.parseFunctionDeclNode(functionDeclNode: FunctionDeclNode){
        this.createFunction(functionDeclNode.identifier.identifier){
            this.returnType = this@ASTToLLVM.convertTypeIdentifier(functionDeclNode.returnType.type.typeIdentifier)
            functionDeclNode.params.forEach {
                this.createFunctionParam {
                    this@ASTToLLVM.convertTypeIdentifier(it.type.typeIdentifier)
                }
            }
            this.addBlock("local_${functionDeclNode.identifier.identifier}_block"){
                functionDeclNode.codeBlock.statements.forEach {
                    when(it){
                        is LetNode -> {
                            this.parseLocalVariableNode(it)
                        }
                    }
                }
                this.addReturnStatement {
                    val returnStatement = functionDeclNode.codeBlock.returnStatement
                    when(returnStatement.expression){
                        is IntegerLiteralNode -> {
                            Value.Int32ConstValue(returnStatement.expression.integer)
                        }
                        is DecimalLiteralNode -> {
                            Value.FloatConstValue(returnStatement.expression.float)
                        }
                        is StringLiteralNode -> {
                            val stringContent = this@ASTToLLVM.parseStringLiteralNode(returnStatement.expression)
                            Value.StringConstValue(stringContent)
                        }
                        is ValueReferenceNode -> {
                            if(!functionDeclNode.scope!!.doesSymbolExist(returnStatement.expression.ident.identifier)){
                                throw IllegalArgumentException("Symbol ${returnStatement.expression.ident.identifier} does not exists in current scope!")
                            }
                            val symbolNodeReference = functionDeclNode.scope!!.getSymbol(returnStatement.expression.ident.identifier)!!
                            when(symbolNodeReference){
                                is Symbol.FunctionParamSymbol -> {
                                    this.function.getParam(symbolNodeReference.paramIndex)
                                }
                                else -> throw IllegalArgumentException("Unrecognized symbol reference")
                            }
                        }
                        else -> throw IllegalArgumentException("Unrecognized return value")
                    }
                }
            }
        }
    }
}