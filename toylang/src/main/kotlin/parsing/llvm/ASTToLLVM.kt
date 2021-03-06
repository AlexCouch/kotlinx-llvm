package parsing.llvm
/*
package parsing.llvm

import Result
import WrappedResult
import ErrorResult
import com.couch.kotlinx.llvm.*
import com.couch.kotlinx.llvm.Function
import com.strumenta.kolasu.model.walkChildren
import parsing.Context
import parsing.FunctionContext
import parsing.GlobalContext
import parsing.ParserErrorResult
import kotlin.random.Random

class ASTToLLVM{
    fun startGeneratingLLVM(moduleName: String, rootNode: ToylangHIRElement.RootNode): Result {
        return WrappedResult(buildModule(moduleName){
            rootNode.walkChildren().forEach {
                when(it){
                    is ToylangHIRElement.StatementNode.VariableNode.GlobalVariableNode -> {
                        when(val variable = this.parseLetNode(it, rootNode.context)){
                            is WrappedResult<*> -> {
                                when(variable.t){
                                    is Variable -> variable.t
                                    null -> return ErrorResult("Global variable parse result value came back null")
                                    else -> return ErrorResult("Unrecognized global variable parse result value type: ${variable.t::class.qualifiedName}")
                                }
                            }
                            is ErrorResult -> return ErrorResult("Failed to parse global variable to llvm", variable)
                            is ParserErrorResult<*> -> return ErrorResult("Parser error occurred while parsing global variable to LLVM bitcode: $variable")
                            else -> return ErrorResult("Unrecognized result: $variable")
                        }
                    }
                    is ToylangHIRElement.StatementNode.FunctionDeclNode -> when(val fn = this.parseFunctionDeclNode(it, rootNode.context)){
                        is WrappedResult<*> -> {
                            when(fn.t){
                                is Function -> {
                                    fn.t
                                }
                                null -> return ErrorResult("Function declaration parse result value came back null")
                                else -> return ErrorResult("Unrecognized function declaration parse result value type: ${fn.t::class.qualifiedName}")
                            }
                        }
                        is ErrorResult -> return ParserErrorResult(ErrorResult("Failed to parse function declaration to llvm", fn), rootNode.location)
                        is ParserErrorResult<*> -> return ErrorResult("Parser error occurred while parsing function declaration to LLVM bitcode: $fn")
                        else -> return ErrorResult("Unrecognized result: $fn")
                    }
                }
            }
        })
    }

    fun Module.parseLetNode(letNode: ToylangHIRElement.StatementNode.VariableNode.GlobalVariableNode, context: GlobalContext): Result{
        return when(val symbol = context.findIdentifier(letNode.identifier)){
            is WrappedResult<*> -> {
                when(symbol.t){
                    is ToylangHIRElement.StatementNode.VariableNode.GlobalVariableNode -> {
                        when(symbol.t.assignment.expression) {
                            is ToylangHIRElement.StatementNode.ExpressionNode.IntegerLiteralExpression -> {
                                WrappedResult(this.createGlobalVariable(letNode.identifier, Type.Int32Type()) {
                                    createInt32Value(symbol.t.assignment.expression.integer)
                                })
                            }
                            is ToylangHIRElement.StatementNode.ExpressionNode.DecimalLiteralExpression ->
                                WrappedResult(this.createGlobalVariable(letNode.identifier, Type.FloatType()) {
                                    createFloatValue(symbol.t.assignment.expression.decimal)
                                })
                            is ToylangHIRElement.StatementNode.ExpressionNode.StringLiteralExpression -> {
                                val stringContent = this@ASTToLLVM.parseStringLiteralNode(symbol.t.assignment.expression)
                                WrappedResult(this.createGlobalVariable(letNode.identifier, Type.ArrayType(Type.Int8Type(), stringContent.length + 1)) {
                                    createStringValue(stringContent)
                                })
                            }
                            else -> ErrorResult("Unrecognized symbol: $symbol")
                        }
                    }
                    else -> ErrorResult("Expected a global variable but got ${symbol.t!!::class.qualifiedName} instead")
                }
            }
            is ErrorResult -> ParserErrorResult(ErrorResult("Could not find symbol with identifier ${letNode.identifier} in global context"), letNode.location)
            is ParserErrorResult<*> -> return ErrorResult("Parser error occurred while parsing global variable to LLVM bitcode: $symbol")
            else -> ErrorResult("Unrecognized result: $symbol")
        }
    }

    fun parseStringLiteralNode(stringNode: ToylangHIRElement.StatementNode.ExpressionNode.StringLiteralExpression): String{
        return stringNode.content.map {
            when(it){
                is ToylangHIRElement.StatementNode.ExpressionNode.StringLiteralContentNode.StringLiteralRawNode -> {
                    it.content
                }
                is ToylangHIRElement.StatementNode.ExpressionNode.StringLiteralContentNode.StringLiteralInterpolationNode -> {
                    when(it.expression){
                        is ToylangHIRElement.StatementNode.ExpressionNode.IntegerLiteralExpression -> {
                            it.expression.integer.toString()
                        }
                        is ToylangHIRElement.StatementNode.ExpressionNode.DecimalLiteralExpression -> {
                            it.expression.decimal.toString()
                        }
                        is ToylangHIRElement.StatementNode.ExpressionNode.StringLiteralExpression -> {
                            this.parseStringLiteralNode(it.expression)
                        }
                        else -> ParserErrorResult(ErrorResult("Could not parse expression inside string interpolation"), stringNode.location)
                    }
                }
            }
        }.joinToString()
    }

    fun Builder.parseLocalVariableNode(letNode: ToylangHIRElement.StatementNode.VariableNode.LocalVariableNode, function: Function, context: FunctionContext): Result{
        if(letNode.type == null) return ParserErrorResult(ErrorResult("Variable type annotation cannot be null: $letNode"), letNode.location)
        return when(val result = context.findIdentifier(letNode.identifier)){
            is WrappedResult<*> -> {
                when(val parseResult = parseExpressionNode(letNode.assignment.expression, null, function, context)){
                    is WrappedResult<*> -> {
                        when(parseResult.t){
                            is Value -> {
                                WrappedResult(this.createLocalVariable(letNode.identifier, convertTypeIdentifier(letNode.type!!.typeName)){
                                    parseResult.t
                                })
                            }
                            is Variable.NamedVariable -> WrappedResult(this.buildLoad(parseResult.t.value, letNode.identifier))
                            else -> ErrorResult("Expression parse result came back abnormal: $result")
                        }
                    }
                    is ErrorResult -> ParserErrorResult(parseResult, letNode.location)
                    else -> ErrorResult("Unrecognized result: $parseResult")
                }
            }
            is ErrorResult -> ParserErrorResult(ErrorResult("Error while checking for name collision for local variable ${letNode.identifier}", result), letNode.location)
            else -> ErrorResult("No symbol found for ${letNode.identifier}")
        }
    }

    fun convertTypeIdentifier(identifier: String): Type = when(identifier){
            "Int" -> Type.Int32Type()
            "Float" -> Type.FloatType()
            "String" -> Type.PointerType(Type.Int8Type())
            else -> Type.VoidType()
    }

    fun Builder.parseFunctionCall(functionCallNode: ToylangHIRElement.StatementNode.ExpressionNode.FunctionCallNode, module: Module? = null, function: Function? = null, context: Context): Result{
        val fnName = functionCallNode.identifier
        return when(val symbol = context.findIdentifier(fnName)){
            is WrappedResult<*> -> {
                when(symbol.t){
                    is ToylangHIRElement.StatementNode.FunctionDeclNode -> {
                        val fn = module?.findFunction(fnName) ?: function?.module?.findFunction(fnName) ?: return ErrorResult("Function does not exist in current module context: $fnName")
                        WrappedResult(this.buildFunctionCall("${fnName}_call", fn) {
                            functionCallNode.args.map {
                                when(val exprResult = this.parseExpressionNode(it, module, function, context)) {
                                    is WrappedResult<*> -> {
                                        when(exprResult.t){
                                            is Value -> exprResult.t
                                            is Variable.NamedVariable -> this.buildLoad(exprResult.t.value, "${exprResult.t.name}_load")
                                            null -> return ErrorResult("Expression parse result value came back null")
                                            else -> return ErrorResult("Unrecognized parse result value: ${exprResult.t::class.qualifiedName}")
                                        }
                                    }
                                    is ErrorResult -> return ParserErrorResult(ErrorResult("Could not parse expression node to llvm", exprResult), functionCallNode.location)
                                    is ParserErrorResult<*> -> return ErrorResult("Parser error occurred while parsing function declaration to LLVM bitcode: $symbol")
                                    else -> return ErrorResult("Unrecognized result: $exprResult")
                                }
                            }.toTypedArray()
                        })
                    }
                    else -> ErrorResult("Unrecognized symbol for identifier: $fnName")
                }
            }
            is ErrorResult -> ParserErrorResult(ErrorResult("Could not get symbol for function identifier $fnName"), functionCallNode.location)
            else -> ErrorResult("Unrecognized result: $symbol")
        }
    }

    fun Builder.parseReturnStatement(returnStatement: ToylangHIRElement.StatementNode.ReturnStatementNode, function: Function, context: FunctionContext): Result{
        return WrappedResult(this.addReturnStatement {
            when(val result = this.parseExpressionNode(returnStatement.expression, function.module, function, context)){
                is WrappedResult<*> -> {
                    when(result.t){
                        is Value -> result.t
                        is Variable.NamedVariable -> this.buildLoad(result.t.value, result.t.name + "_load")
                        null -> return ErrorResult("Expression parser result value came back null")
                        else -> return ErrorResult("Unrecognzied parser result value: ${result.t::class.qualifiedName}")
                    }
                }
                is ErrorResult -> return ParserErrorResult(ErrorResult("Failed to parse expression node in return statement", result), returnStatement.location)
                is ParserErrorResult<*> -> return ErrorResult("Parser error occurred while parsing return statement to LLVM bitcode: $result")
                else -> return ErrorResult("Unrecognized result: $result")
            }
        })
    }

    private fun Builder.parseBinaryOperationExpression(binaryNode: ToylangHIRElement.StatementNode.ExpressionNode.BinaryExpressionNode, module: Module? = null, function: Function? = null, context: Context): Result {
        val left = when(val parseResult = this.parseExpressionNode(binaryNode.left, module, function, context)){
            is WrappedResult<*> -> {
                when(parseResult.t) {
                    is Value -> parseResult.t
                    is Variable.NamedVariable.LocalVariable -> this.buildLoad(parseResult.t.value, "${parseResult.t.name}_load")
                    null -> return ErrorResult("Expression parser result value came back null")
                    else -> return ErrorResult("Unrecognzied parser result value: ${parseResult.t::class.qualifiedName}")
                }
            }
            is ErrorResult -> return ParserErrorResult(ErrorResult("Failed to parse expression node in return statement", parseResult), binaryNode.location)
            is ParserErrorResult<*> -> return ErrorResult("Parser error occurred while parsing left hand of binary operation to LLVM bitcode: $parseResult")
            else -> return ErrorResult("Unrecognized result: $parseResult")
        }

        val right = when(val parseResult = this.parseExpressionNode(binaryNode.right, module, function, context)){
            is WrappedResult<*> -> {
                when(parseResult.t) {
                    is Value -> parseResult.t
                    is Variable.NamedVariable -> this.buildLoad(parseResult.t.value, parseResult.t.name + "_load")
                    null -> return ErrorResult("Expression parser result value came back null")
                    else -> return ErrorResult("Unrecognzied parser result value: ${parseResult.t::class.qualifiedName}")
                }
            }
            is ErrorResult -> return ParserErrorResult(ErrorResult("Failed to parse expression node in return statement", parseResult), binaryNode.location)
            is ParserErrorResult<*> -> return ErrorResult("Parser error occurred while parsing right hand of binary operation to LLVM bitcode: $parseResult")
            else -> return ErrorResult("Unrecognized result: $parseResult")
        }
        return when (binaryNode) {
            is ToylangHIRElement.StatementNode.ExpressionNode.BinaryExpressionNode.PlusNode -> {
                WrappedResult(this.buildAdditionInstruction("plusResult"){
                    this.left = left
                    this.right = right
                })
            }
            is ToylangHIRElement.StatementNode.ExpressionNode.BinaryExpressionNode.MinusNode -> {
                WrappedResult(this.buildMinusInstruction("subResult"){
                    this.left = left
                    this.right = right
                })
            }
            is ToylangHIRElement.StatementNode.ExpressionNode.BinaryExpressionNode.DivNode -> {
                WrappedResult(this.buildSDivide("divResult"){
                    this.left = left
                    this.right = right
                })
            }
            is ToylangHIRElement.StatementNode.ExpressionNode.BinaryExpressionNode.MultiplyNode -> {
                WrappedResult(this.buildMultiplyInstruction("multResult"){
                    this.left = left
                    this.right = right
                })
            }
        }
    }

    private fun Builder.parseStringLiteralValue(expressionNode: ToylangHIRElement.StatementNode.ExpressionNode.StringLiteralExpression, module: Module? = null, function: Function? = null): Result{
        val rand = Random.nextInt()
        val strContent = parseStringLiteralNode(expressionNode)
        val temp = this.createLocalVariable("strLiteral_tmp$rand", Type.ArrayType(Type.Int8Type(), strContent.length + 1)){
            createStringValue(strContent)
        }
        val gep = this.buildGetElementPointer("strLiteral_tmp_gep$rand"){
            temp.value
        }
        return WrappedResult(this.buildBitcast(gep, Type.PointerType(Type.Int8Type()), "strLiteral_tmp_bitcast$rand"))
    }

    private fun Builder.parseExpressionNode(expressionNode: ToylangHIRElement.StatementNode.ExpressionNode, module: Module? = null, function: Function? = null, context: Context): Result{
        return when(expressionNode){
            is ToylangHIRElement.StatementNode.ExpressionNode.FunctionCallNode -> {
                this.parseFunctionCall(expressionNode, module, function, context)
            }
            is ToylangHIRElement.StatementNode.ExpressionNode.BinaryExpressionNode -> {
                this.parseBinaryOperationExpression(expressionNode, module, function, context)
            }
            is ToylangHIRElement.StatementNode.ExpressionNode.StringLiteralExpression -> {
                this.parseStringLiteralValue(expressionNode)
            }
            is ToylangHIRElement.StatementNode.ExpressionNode.IntegerLiteralExpression -> {
                WrappedResult(createInt32Value(expressionNode.integer))
            }
            is ToylangHIRElement.StatementNode.ExpressionNode.DecimalLiteralExpression -> {
                WrappedResult(createFloatValue(expressionNode.decimal))
            }
            is ToylangHIRElement.StatementNode.ExpressionNode.ValueReferenceNode -> {
                when(val symbol = context.findIdentifier(expressionNode.identifier)){
                    is WrappedResult<*> -> {
                        when(symbol.t){
                            is ToylangHIRElement.StatementNode.VariableNode.LocalVariableNode -> {
                                val v = function?.localVariables?.find { it.name == symbol.t.identifier } ?: return ErrorResult("Local variable used before creation: ${symbol.t.identifier}")
                                when(v.type){
                                    is Type.ArrayType -> {
                                        WrappedResult(
                                                this.buildBitcast(
                                                        this.buildGetElementPointer("${v.name}_gep_load"){
                                                            v.value ?: return ErrorResult("Value reference has no value")
                                                        },
                                                        Type.PointerType(Type.Int8Type()),
                                                        "${v.name}_bitcast"
                                                )
                                        )
                                    }
                                        else -> WrappedResult(v)
                                    }
                                }
                            is ToylangHIRElement.FunctionParamNode -> {
                                WrappedResult(function?.getParamByName(symbol.t.identifier) ?: return ErrorResult("Tried to use function parameter before creation: ${symbol.t.identifier}"))
                            }
                            is ToylangHIRElement.StatementNode.VariableNode.GlobalVariableNode -> {
                                val v = module?.getGlobalReference(symbol.t.identifier) ?: return ErrorResult("Global variable does not exist: ${symbol.t.identifier}")
                                when(v.type){
                                    is Type.ArrayType -> {
                                        WrappedResult(
                                                this.buildBitcast(
                                                        this.buildGetElementPointer("${v.name}_gep_load"){
                                                            v.value ?: return ErrorResult("Value reference has no value")
                                                        },
                                                        Type.PointerType(Type.Int8Type()),
                                                        "${v.name}_bitcast"
                                                )
                                        )
                                    }
                                    else -> WrappedResult(v.value)
                                }
                            }
                            null -> ErrorResult("Could not get symbol from identifier ${expressionNode.identifier}")
                            else -> ErrorResult("Unrecognized node: ${symbol.t::class.qualifiedName}")
                        }
                    }
                    is ErrorResult -> ParserErrorResult(ErrorResult("Could not get symbol from identifier: ${expressionNode.identifier}", symbol), expressionNode.location)
                    is ParserErrorResult<*> -> return ErrorResult("Parser error occurred while parsing expression to LLVM bitcode: $symbol")
                    else -> ErrorResult("Unrecognized result: $symbol")
                }

            }
            else -> ErrorResult("Unrecognized expression: $expressionNode")
        }
    }

    fun Module.parseFunctionDeclNode(functionDeclNode: ToylangHIRElement.StatementNode.FunctionDeclNode, context: GlobalContext): Result{
        when(val symbolResult = context.findIdentifier(functionDeclNode.identifier)){
            is WrappedResult<*> -> {
                when(symbolResult.t){
                    is ToylangHIRElement.StatementNode.FunctionDeclNode -> return ErrorResult("Function with identifier ${symbolResult.t.identifier} already exists")
                }
            }
        }
        val func = this.createFunction(functionDeclNode.identifier){
            this.returnType = this@ASTToLLVM.convertTypeIdentifier(functionDeclNode.type.typeName)
            functionDeclNode.context.params.forEach {
                this.createFunctionParam(it.identifier.identifier) {
                    this@ASTToLLVM.convertTypeIdentifier(it.type.identifier.identifier)
                }
            }
            this.addBlock("local_${functionDeclNode.identifier}_block"){
                var hasTerminator = false
                this.startBuilder {
                    functionDeclNode.codeblock.statements.forEach {
                        when(it){
                            is ToylangHIRElement.StatementNode.VariableNode.LocalVariableNode -> {
                                this@createFunction.localVariables.add(when(val parseResult = this.parseLocalVariableNode(it, this@createFunction, functionDeclNode.context)){
                                    is WrappedResult<*> -> {
                                        when(parseResult.t){
                                            is Variable -> parseResult.t
                                            null -> return ErrorResult("Local variable parse result value came back null")
                                            else -> return ErrorResult("Unrecognized type of parse result value: ${parseResult.t::class.qualifiedName}")
                                        }
                                    }
                                    is ErrorResult -> return ParserErrorResult(ErrorResult("Could not parse local variable to llvm", parseResult), functionDeclNode.location)
                                    is ParserErrorResult<*> -> return ErrorResult("Parser error occurred while parsing local variable to LLVM bitcode: $parseResult")
                                    else -> return ErrorResult("Unrecognized result: $parseResult")
                                })
                            }
                            is ToylangHIRElement.StatementNode.ExpressionNode -> {
                                when(val parseResult = this.parseExpressionNode(it, this@createFunction.module, function, functionDeclNode.context)){
                                    is WrappedResult<*> -> {
                                        when(parseResult.t){
                                            is Value -> parseResult.t
                                            is Variable.NamedVariable -> this.buildLoad(parseResult.t.value, parseResult.t.name + "_load")
                                            else -> return ErrorResult("Unrecognized type: ${parseResult.t!!::class.qualifiedName}")
                                        }
                                    }
                                    is ErrorResult -> return ParserErrorResult(parseResult, functionDeclNode.location)
                                    is ParserErrorResult<*> -> return ErrorResult("Parser error occurred an error while paring function codeblock expression to llvm bitcode: $parseResult")
                                    else -> return ErrorResult("Unrecognized result: $parseResult")
                                }
                            }
                            is ToylangHIRElement.StatementNode.ReturnStatementNode -> {
                                when(val parseResult = parseReturnStatement(it, this@createFunction, functionDeclNode.context)){
                                    is WrappedResult<*> -> {
                                        when(parseResult.t){
                                            is Value -> parseResult.t
                                            is Variable.NamedVariable -> this.buildLoad(parseResult.t.value, "${parseResult.t.name}_load")
                                        }
                                    }
                                    is ErrorResult -> return ParserErrorResult(parseResult, functionDeclNode.location)
                                    is ParserErrorResult<*> -> return ErrorResult("Parser occurred an error while paring function codeblock expression to llvm bitcode: $parseResult")
                                }
                                hasTerminator = true
                            }
                        }
                    }
                    if(!hasTerminator){
                        this.addReturnStatement {
                            null
                        }
                    }
                }
            }
        }
        return WrappedResult(func)
    }
}*/
