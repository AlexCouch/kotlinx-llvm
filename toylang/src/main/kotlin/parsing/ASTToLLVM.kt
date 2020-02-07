package com.couch.kotlinx.parsing

import com.couch.kotlinx.Symbol
import com.couch.kotlinx.ast.*
import com.couch.kotlinx.llvm.*
import com.strumenta.kolasu.model.walkChildren
import org.bytedeco.llvm.global.LLVM
import kotlin.IllegalStateException

class ASTToLLVM{
    fun startGeneratingLLVM(moduleName: String, rootNode: RootNode) = buildModule(moduleName){
            rootNode.walkChildren().forEach {
                when(it){
                    is LetNode -> this.globalVariables.add(this.parseLetNode(it))
                    is FunctionDeclNode -> this.parseFunctionDeclNode(it)
                }
            }
        }

    fun Module.parseLetNode(letNode: LetNode): Variable.NamedVariable.GlobalVariable{
        return when(letNode.assignment.expression){
            is IntegerLiteralNode -> {
                this.createGlobalVariable(letNode.identifier.identifier, Type.Int32Type()){
                    createInt32Value(letNode.assignment.expression.integer)
                }
            }
            is DecimalLiteralNode -> this.createGlobalVariable(letNode.identifier.identifier, Type.FloatType()){
                createFloatValue(letNode.assignment.expression.float)
            }
            is StringLiteralNode -> {
                val stringContent = this@ASTToLLVM.parseStringLiteralNode(letNode.assignment.expression)
                this.createGlobalVariable(letNode.identifier.identifier, Type.ArrayType(Type.Int8Type(), stringContent.length + 1)){
                    createStringValue(stringContent)
                }
            }
            else -> throw IllegalStateException("Could not create global variable: ${letNode.identifier.identifier}")
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

    fun BasicBlock.parseLocalVariableNode(letNode: LetNode): Variable{
        return when(letNode.assignment.expression){
            is IntegerLiteralNode -> this.createLocalVariable(letNode.identifier.identifier, Type.Int32Type()){
                createInt32Value(letNode.assignment.expression.integer)
            }
            is DecimalLiteralNode -> this.createLocalVariable(letNode.identifier.identifier, Type.FloatType()){
                createFloatValue(letNode.assignment.expression.float)
            }
            is StringLiteralNode -> this.createLocalVariable(letNode.identifier.identifier, Type.Int32Type()){
                val stringContent = this@ASTToLLVM.parseStringLiteralNode(letNode.assignment.expression)
                createStringValue(stringContent)
            }
            else -> throw IllegalStateException("Could not parse local variable")
        }
    }

    fun convertTypeIdentifier(identifierNode: IdentifierNode): Type = when(identifierNode.identifier){
            "Int" -> Type.Int32Type()
            "Float" -> Type.FloatType()
            "String" -> Type.PointerType(Type.Int8Type().llvmType)
            else -> Type.VoidType()
    }



    fun Module.parseFunctionDeclNode(functionDeclNode: FunctionDeclNode){
        this.createFunction(functionDeclNode.identifier.identifier){
            this.returnType = this@ASTToLLVM.convertTypeIdentifier(functionDeclNode.returnType.type.typeIdentifier)
            this.localVariables.addAll(functionDeclNode.params.map {
                this.createFunctionParam(it.identifier.identifier) {
                    this@ASTToLLVM.convertTypeIdentifier(it.type.typeIdentifier)
                }
            })
            this.addBlock("local_${functionDeclNode.identifier.identifier}_block"){
                val localVars = functionDeclNode.codeBlock.statements.map {
                    when(it){
                        is LetNode -> {
                            this.parseLocalVariableNode(it)
                        }
                        else -> throw IllegalStateException("Could not parse local block statement for block ${this.name}")
                    }
                }
                this@createFunction.localVariables.addAll(localVars)
                this.startBuilder {
                    this.addReturnStatement {
                        val returnStatement = functionDeclNode.codeBlock.returnStatement
                        when(returnStatement.expression){
                            is IntegerLiteralNode -> {
                                createInt32Value(returnStatement.expression.integer)
                            }
                            is DecimalLiteralNode -> {
                                createFloatValue(returnStatement.expression.float)
                            }
                            is StringLiteralNode -> {
                                val stringContent = this@ASTToLLVM.parseStringLiteralNode(returnStatement.expression)
                                createStringValue(stringContent)
                            }
                            is ValueReferenceNode -> {
                                if(!functionDeclNode.scope!!.doesSymbolExist(returnStatement.expression.ident.identifier)){
                                    throw IllegalArgumentException("Symbol ${returnStatement.expression.ident.identifier} does not exists in current scope!")
                                }
                                val symbolNodeReference = functionDeclNode.scope!!.getSymbol(returnStatement.expression.ident.identifier)!!
                                when(symbolNodeReference){
                                    is Symbol.FunctionParamSymbol -> {
                                        this@addBlock.function.getParam(symbolNodeReference.paramIndex)
                                    }
                                    is Symbol.VarSymbol -> {
                                        val symbolName = symbolNodeReference.symbol.identifier
                                        val varRef = this@createFunction.localVariables.find{ it.name == symbolName } ?:
                                        this@createFunction.module.getGlobalReference(symbolNodeReference.symbol.identifier)!!
                                        var retStatement: Value = varRef.value!!
                                        if(varRef.type is Type.ArrayType){
                                            retStatement = this.buildGetElementPointer(varRef){
                                                retStatement
                                            }
                                        }
                                        retStatement
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
}