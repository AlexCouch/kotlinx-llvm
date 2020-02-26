package com.couch.kotlinx.parsing.llvm

import com.couch.kotlinx.parsing.Scope
import com.couch.kotlinx.parsing.Symbol
import com.couch.kotlinx.ast.*
import com.couch.kotlinx.llvm.*
import com.couch.kotlinx.llvm.Function
import com.strumenta.kolasu.model.walkChildren
import kotlin.IllegalStateException
import kotlin.random.Random

class ASTToLLVM{
    fun startGeneratingLLVM(moduleName: String, rootNode: RootNode) = buildModule(moduleName){
        rootNode.scope!!.symbols.add(Symbol.FunctionDeclSymbol("println", this.createFunction("printf"){
            this.returnType = Type.Int32Type()
            this.vararg = true
            this.createFunctionParam("argc"){
                Type.PointerType(Type.Int8Type())
            }
        }))
            rootNode.walkChildren().forEach {
                when(it){
                    is LetNode -> {
                        val variable = this.parseLetNode(it)
                        this.globalVariables.add(variable)
                        if(rootNode.scope!!.doesSymbolExist(variable.name)){
                            val symbol = rootNode.scope!!.getSymbol(variable.name)!!
                            if(symbol !is Symbol.VarSymbol){
                                throw IllegalStateException("Symbol ${variable.name} is not a variable symbol")
                            }
                            symbol.variable = variable
                        }
                    }
                    is FunctionDeclNode -> this.parseFunctionDeclNode(it, rootNode.scope!!)
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

    fun Builder.parseLocalVariableNode(letNode: LetNode): Variable{
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
            "String" -> Type.PointerType(Type.Int8Type())
            else -> Type.VoidType()
    }

    fun Builder.parseFunctionCall(functionCallNode: FunctionCallNode, function: Function, scope: Scope): Value{
        val fnName = functionCallNode.name
        if(scope.doesSymbolExist(fnName)){
            val fnBeingCalled = scope.getSymbol(fnName)!!
            if(fnBeingCalled !is Symbol.FunctionDeclSymbol){
                throw IllegalStateException("Symbol $fnName is not a function name")
            }
            return this.buildFunctionCall("${fnName}_call", fnBeingCalled.node){
                functionCallNode.args.map{
                    when(it){
                        is ValueReferenceNode -> {
                            createReferenceValue(this.parseExpressionNode(it, function, scope))
                        }
                        is IntegerLiteralNode -> {
                            createInt32Value(it.integer)
                        }
                        is DecimalLiteralNode -> {
                            createFloatValue(it.float)
                        }
                        is StringLiteralNode -> {
                            this.parseStringLiteralValue(it)
                        }
                        is FunctionCallNode -> {
                            this.parseFunctionCall(it, function, scope)
                        }
                        else -> throw IllegalArgumentException("Could not parse function call")
                    }
                }.toTypedArray()
            }
        }
        throw IllegalStateException("Symbol $fnName does not exist in current scope")
    }

    fun Builder.parseReturnStatement(returnStatement: ReturnStatementNode, function: Function, scope: Scope){
        this.addReturnStatement {
            this.parseExpressionNode(returnStatement.expression, function, scope)
        }
    }

    private fun Builder.parsePlusOperationExpression(plusOpNode: BinaryPlusOperation, function: Function, scope: Scope): Value{
        val left = this.parseExpressionNode(plusOpNode.left, function, scope)
        val right = this.parseExpressionNode(plusOpNode.right, function, scope)
        return this.addAdditionInstruction(""){
            this.left = left
            this.right = right
        }
    }

    private fun Builder.parseStringLiteralValue(expressionNode: StringLiteralNode): Value{
        val rand = Random.nextInt()
        val strContent = parseStringLiteralNode(expressionNode)
        val temp = this.createLocalVariable("strLiteral_tmp$rand", Type.ArrayType(Type.Int8Type(), strContent.length + 1)){
            createStringValue(strContent)
        }
        val gep = this.buildGetElementPointer("strLiteral_tmp_gep$rand"){
            temp.value
        }
        return this.buildBitcast(gep, Type.PointerType(Type.Int8Type()), "strLiteral_tmp_bitcast$rand")
    }

    private fun Builder.parseExpressionNode(expressionNode: ExpressionNode, function: Function, scope: Scope): Value{
        return when(expressionNode){
            is FunctionCallNode -> {
                this.parseFunctionCall(expressionNode, function, scope)
            }
            is BinaryPlusOperation -> {
                this.parsePlusOperationExpression(expressionNode, function, scope)
            }
            is StringLiteralNode -> {
                this.parseStringLiteralValue(expressionNode)
            }
            is ValueReferenceNode -> {
                if(!scope.doesSymbolExist(expressionNode.ident.identifier)){
                    throw IllegalArgumentException("Symbol ${expressionNode.ident.identifier} does not exists in current scope!")
                }
                val symbolNodeReference = scope.getSymbol(expressionNode.ident.identifier)!!
                when(symbolNodeReference){
                    is Symbol.FunctionParamSymbol -> {
                        function.getParam(symbolNodeReference.paramIndex)
                    }
                    is Symbol.VarSymbol -> {
                        val symbolName = symbolNodeReference.symbol
                        val varRef = function.localVariables.find{
                            it.name == symbolName
                        } ?: function.module.getGlobalReference(symbolName)!!
                        var refStatement: Value = varRef.value!!
                        if(varRef.type is Type.ArrayType){
                            val gep = this.buildGetElementPointer("${varRef.name}ptr_tmp"){
                                function.module.getGlobalReference(symbolName)!!.value!!
                            }
                            refStatement = this.buildBitcast(gep, ((gep.type as Type.PointerType).type as Type.ArrayType).elementPtrType, "${symbolName}_bitcast")
                        }
                        refStatement
                    }
                    else -> throw IllegalArgumentException("Unrecognized symbol reference")
                }
            }
            else -> {
                throw IllegalStateException("Could not parse expression $expressionNode")
            }
        }
    }

    fun Module.parseFunctionDeclNode(functionDeclNode: FunctionDeclNode, scope: Scope){
        val func = this.createFunction(functionDeclNode.identifier.identifier){
            this.returnType = this@ASTToLLVM.convertTypeIdentifier(functionDeclNode.returnType.type.typeIdentifier)
            this.localVariables.addAll(functionDeclNode.params.map {
                this.createFunctionParam(it.identifier.identifier) {
                    this@ASTToLLVM.convertTypeIdentifier(it.type.typeIdentifier)
                }
            })
            this.addBlock("local_${functionDeclNode.identifier.identifier}_block"){
                this.startBuilder {
                    functionDeclNode.codeBlock.statements.forEach {
                        when(it){
                            is LetNode -> {
                                this@createFunction.localVariables.add(this.parseLocalVariableNode(it))
                            }
                            is ExpressionNode -> {
                                this.parseExpressionNode(it, this@createFunction, functionDeclNode.scope!!)
                            }
                        }
                    }
                    val returnStatement = functionDeclNode.codeBlock.returnStatement
                    if(returnStatement.expression !is NoneExpressionNode){
                        this.parseReturnStatement(returnStatement, this@createFunction, functionDeclNode.scope!!)
                    }else{
                        this.addReturnStatement {
                            null
                        }
                    }
                }
            }
        }
        scope.symbols.add(Symbol.FunctionDeclSymbol(func.name, func))
    }
}