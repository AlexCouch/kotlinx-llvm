import parsing.ast.Location
import com.couch.toylang.ToylangParser
import com.couch.toylang.ToylangParserBaseVisitor
import org.antlr.v4.kotlinruntime.ast.Point
import parsing.*

class ToylangVisitor: ToylangParserBaseVisitor<Result>(){
    private var currentContext: Context = GlobalContext()

    override fun visitFunctionCall(ctx: ToylangParser.FunctionCallContext): Result {
        val fnName = ctx.IDENT()!!.symbol!!.text!!
        val fnArgs = ctx.findFnArgs()!!
        val exprs = fnArgs.findExpression().map {
            when(val expressionResult = this.determineExpression(it)){
                is WrappedResult<*> -> {
                    when(expressionResult.t){
                        is ToylangMainAST.StatementNode.ExpressionNode -> expressionResult.t
                        null -> return ErrorResult("Expression node came back null")
                        else -> return ErrorResult("Expression node came back wrong type: ${expressionResult.t::class.qualifiedName}")
                    }
                }
                is ErrorResult -> return ParserErrorResult(
                        ErrorResult("Could not get expression node", expressionResult),
                        Location(
                                ctx.start?.startPoint() ?: Point(0, 0),
                                ctx.stop?.startPoint() ?: Point(0, 0)
                        )
                )
                else -> return ErrorResult("Unrecognized result: $expressionResult")
            }
        }

        return WrappedResult(ToylangMainAST.StatementNode.ExpressionNode.FunctionCallNode(Location(ctx.position?.start ?: Point(0, 0), ctx.position?.end ?: Point(0, 0)), fnName, exprs))
    }

    override fun visitToylangFile(ctx: ToylangParser.ToylangFileContext): Result {
        val rootContext = GlobalContext()
        if(this.currentContext !is GlobalContext) this.currentContext = GlobalContext()
        val statements = ctx.findLine().map {
            when(val result = this.visitStatement(it.findStatement()!!)){
                is WrappedResult<*> -> {
                    when(result.t){
                        is ToylangMainAST.StatementNode -> {
                            val statement = result.t
                            when(statement){
                                is ToylangMainAST.StatementNode.VariableNode.GlobalVariableNode -> rootContext.globalVariables.add(statement)
                                is ToylangMainAST.StatementNode.FunctionDeclNode -> {
                                    rootContext.functions.add(statement)
                                    this.currentContext = rootContext
                                }
                            }
                            statement
                        }
                        else -> return ErrorResult("Did not get statement node from statement visitor in initial parser")
                    }
                }
                is ErrorResult -> {
                    return ParserErrorResult(
                            ErrorResult("Could not get statement node from statement visitor", result),
                            Location(
                                    ctx.start?.startPoint() ?: Point(0, 0),
                                    ctx.stop?.startPoint() ?: Point(0, 0)
                            )
                    )
                }
                is ParserErrorResult<*> -> return ParserErrorResult(result, Location(ctx.start?.startPoint()?:Point(0, 0), ctx.stop?.endPoint() ?: Point(0, 0)))
                else -> return ErrorResult("Unknown result: $result")
            }
        }
        val root = ToylangMainAST.RootNode(Location(ctx.position?.start ?: Point(0, 0), ctx.position?.end ?: Point(0, 0)), statements, this.currentContext)
        return WrappedResult(root)
    }

    override fun visitStatement(ctx: ToylangParser.StatementContext): Result {
        val result = when{
            ctx.findLetDeclaration() != null -> {
                this.visitLetDeclaration(ctx.findLetDeclaration()!!)
            }
            ctx.findFnDeclaration() != null -> {
                this.visitFnDeclaration(ctx.findFnDeclaration()!!)
            }
            ctx.findExpression() != null -> {
                this.determineExpression(ctx.findExpression()!!)
            }
            else -> ParserErrorResult(
                    ErrorResult("Could not recognize statement:\nStart: ${ctx.start?.startPoint()}\nEnd: ${ctx.stop?.startPoint()}"),
                    Location(
                            ctx.start?.startPoint() ?: Point(0, 0),
                            ctx.stop?.startPoint() ?: Point(0, 0)
                    )
            )
        }
        val semicolon = ctx.SEMICOLON()
        if(semicolon?.symbol?.text?.contains("missing") == true) return ParserErrorResult(
                ErrorResult("Statement must end with a semicolon at ${Location(semicolon.symbol?.startPoint() ?: Point(0, 0), semicolon.symbol?.endPoint() ?: Point(0, 0))}"),
                Location(
                        ctx.start?.startPoint() ?: Point(0, 0),
                        ctx.stop?.endPoint() ?: Point(0, 0)
                )
        )
        return result
    }

    private fun visitTypeAnnotation(ctx: ToylangParser.TypeAnnotationContext): Result{
        val location = Location(
                ctx.start?.startPoint() ?: Point(1, 1),
                ctx.stop?.endPoint() ?: Point(1, 1)
        )
        return WrappedResult(ToylangMainAST.TypeAnnotationNode(location, ToylangMainAST.IdentifierNode(location, ctx.text.trim { it == ':' })))
    }

    override fun visitLetDeclaration(ctx: ToylangParser.LetDeclarationContext): Result {
        val location = Location(
                ctx.start?.startPoint() ?: Point(1, 1),
                ctx.stop?.endPoint() ?: Point(1, 1)
        )
        val assignmentNode = when(val result = this.visitAssignment(ctx.findAssignment()!!)){
            is WrappedResult<*> -> {
                when(result.t){
                    is ToylangMainAST.StatementNode.AssignmentNode -> result.t
                    null -> return ErrorResult("Could not get assignment node from assignment visitor: node is null")
                    else -> return ErrorResult("Wrapped object type: ${result.t::class.qualifiedName}")
                }
            }
            is ErrorResult -> {
                return ParserErrorResult(ErrorResult("Could not get assignment node from assignment visitor:\n" +
                        "Line: ${ctx.position}", result),
                        location
                )
            }
            else -> return ErrorResult("Unknown result: $result")
        }
        val identifierNode = ToylangMainAST.IdentifierNode(
                location,
                ctx.IDENT()?.symbol?.text!!
        )
        val mutable = if(ctx.MUT() != null) ctx.MUT()?.symbol?.text?.toBoolean() ?: false else false
        val typeAnnotation = when(val result = if(ctx.findTypeAnnotation() != null) this.visitTypeAnnotation(ctx.findTypeAnnotation()!!) else null){
            null -> null
            is WrappedResult<*> -> when(result.t){
                is ToylangMainAST.TypeAnnotationNode -> result.t
                else -> return ParserErrorResult(result, location)
            }
            is ErrorResult -> return ParserErrorResult(result, location)
            else -> return ParserErrorResult(ErrorResult("Unrecognized result: $result"), location)
        }
        return if(this.currentContext is GlobalContext)
            WrappedResult(
                ToylangMainAST.StatementNode.VariableNode.GlobalVariableNode(
                        location,
                        identifierNode,
                        mutable,
                        typeAnnotation,
                        assignmentNode
                )
            )
        else
            WrappedResult(
                    ToylangMainAST.StatementNode.VariableNode.LocalVariableNode(
                            location,
                            identifierNode,
                            mutable,
                            typeAnnotation,
                            assignmentNode
                    )
            )
    }

    override fun visitAssignment(ctx: ToylangParser.AssignmentContext): Result {
        val expr = ctx.findExpression() ?: return ParserErrorResult(
                ErrorResult("No expression context found"),
                Location(
                        ctx.start?.startPoint() ?: Point(0, 0),
                        ctx.stop?.startPoint() ?: Point(0, 0)
                )
        )
        val expressionNode: ToylangMainAST.StatementNode.ExpressionNode =
            when(val expressionResult = this.determineExpression(expr)){
                is WrappedResult<*> -> {
                    when(expressionResult.t){
                        is ToylangMainAST.StatementNode.ExpressionNode -> {
                            expressionResult.t
                        }
                        null -> return ParserErrorResult(
                                ErrorResult("Expression node is null"),
                                Location(
                                        ctx.start?.startPoint() ?: Point(0, 0),
                                        ctx.stop?.startPoint() ?: Point(0, 0)
                                )
                        )
                        else -> return ParserErrorResult(
                                ErrorResult("Wrong type of node returned: ${expressionResult.t::class.qualifiedName}"),
                                Location(
                                        ctx.start?.startPoint() ?: Point(0, 0),
                                        ctx.stop?.startPoint() ?: Point(0, 0)
                                )
                        )
                    }
                }
                is ErrorResult -> {
                    return ParserErrorResult(
                            ErrorResult("Could not get expression node from expression visitor", expressionResult),
                            Location(
                                    ctx.start?.startPoint() ?: Point(0, 0),
                                    ctx.stop?.startPoint() ?: Point(0, 0)
                            )
                    )
                }
                else -> return ParserErrorResult(
                        ErrorResult("Unrecognized result: $expressionResult"),
                        Location(
                                ctx.start?.startPoint() ?: Point(0, 0),
                                ctx.stop?.startPoint() ?: Point(0, 0)
                        )
                )
            }
        return WrappedResult(
                ToylangMainAST.StatementNode.AssignmentNode(
                        Location(
                                ctx.start?.startPoint() ?: Point(0, 0),
                                ctx.stop?.startPoint() ?: Point(0, 0)
                        ),
                        expressionNode
                )
        )
    }

    fun determineExpression(ctx: ToylangParser.ExpressionContext): Result {
        return when(ctx){
            is ToylangParser.ValueReferenceContext -> {
                this.visitValueReference(ctx)
            }
            is ToylangParser.IntLiteralContext -> {
                this.visitIntLiteral(ctx)
            }
            is ToylangParser.BinaryOperationContext -> {
                this.visitBinaryOperation(ctx)
            }
            is ToylangParser.StringLiteralContext -> {
                this.visitStringLiteral(ctx)
            }
            is ToylangParser.DecimalLiteralContext -> {
                this.visitDecimalLiteral(ctx)
            }
            is ToylangParser.ParenExpressionContext -> {
                this.visitParenExpression(ctx)
            }
            is ToylangParser.FunctionCallContext -> {
                this.visitFunctionCall(ctx)
            }
            else -> return ParserErrorResult(
                    ErrorResult("Could not recognize kind of expression node"),
                    Location(
                            ctx.start?.startPoint() ?: Point(0, 0),
                            ctx.stop?.startPoint() ?: Point(0, 0)
                    )
            )
        }
    }

    override fun visitStringLiteral(ctx: ToylangParser.StringLiteralContext): Result {
        val content = ctx.parts.map {
            when(val contentResult = this.visitStringLiteralContent(it)){
                is WrappedResult<*> -> {
                    when(contentResult.t){
                        is ToylangMainAST.StatementNode.ExpressionNode.StringLiteralContentNode -> contentResult.t
                        null -> return ParserErrorResult(ErrorResult("String literal content node came back null"),
                                Location(
                                        ctx.start?.startPoint() ?: Point(0, 0),
                                        ctx.stop?.startPoint() ?: Point(0, 0)
                                )
                        )
                        else -> return ParserErrorResult(
                                ErrorResult("String literal content node came back wrong type: ${contentResult.t::class.qualifiedName}"),
                                Location(
                                        ctx.start?.startPoint() ?: Point(0, 0),
                                        ctx.stop?.startPoint() ?: Point(0, 0)
                                )
                        )
                    }
                }
                is ErrorResult -> {
                    return ParserErrorResult(ErrorResult("Could not get string literal content node", contentResult),
                            Location(
                                    ctx.start?.startPoint() ?: Point(0, 0),
                                    ctx.stop?.startPoint() ?: Point(0, 0)
                            )
                    )
                }
                else -> return ParserErrorResult(
                        ErrorResult("Unrecognized result: $contentResult"),
                        Location(
                                ctx.start?.startPoint() ?: Point(0, 0),
                                ctx.stop?.startPoint() ?: Point(0, 0)
                        )
                )
            }
        }
        return WrappedResult(
                ToylangMainAST.StatementNode.ExpressionNode.StringLiteralNode(
                        Location(
                                ctx.start?.startPoint() ?: Point(0, 0),
                                ctx.stop?.startPoint() ?: Point(0, 0)
                        ),
                        content
                )
        )
    }

    override fun visitStringLiteralContent(ctx: ToylangParser.StringLiteralContentContext): Result {
        when{
            ctx.STRING_CONTENT() != null -> {
                return WrappedResult(
                        ToylangMainAST.StatementNode.ExpressionNode.StringLiteralContentNode.RawStringLiteralContentNode(
                                Location(ctx.position?.start ?: Point(0, 0), ctx.position?.end ?: Point(0, 0)),
                                ctx.STRING_CONTENT()?.symbol?.text ?: return ErrorResult("Could not get string content from raw string literal content context")
                        )
                )
            }
            ctx.INTERPOLATION_OPEN() != null && ctx.INTERPOLATION_CLOSE() != null -> {
                val expression =
                    when(val expressionResult = this.determineExpression(
                        ctx.findExpression() ?: return ParserErrorResult(
                                ErrorResult("Could not get expression from string literal content interpolation context"),
                                Location(
                                        ctx.start?.startPoint() ?: Point(0, 0),
                                        ctx.stop?.startPoint() ?: Point(0, 0)
                                )
                            )
                        )
                    ){
                        is WrappedResult<*> -> {
                            when(expressionResult.t){
                                is ToylangMainAST.StatementNode.ExpressionNode -> expressionResult.t
                                null -> return ParserErrorResult(ErrorResult("Expression node came back null"),
                                        Location(
                                                ctx.start?.startPoint() ?: Point(0, 0),
                                                ctx.stop?.startPoint() ?: Point(0, 0)
                                        ))
                                else -> return ParserErrorResult(
                                        ErrorResult("Expression node came back with wrong type: ${expressionResult.t::class.qualifiedName}"),
                                        Location(
                                                ctx.start?.startPoint() ?: Point(0, 0),
                                                ctx.stop?.startPoint() ?: Point(0, 0)
                                        )
                                )
                            }
                        }
                        is ErrorResult -> return ErrorResult("Could not determine expression", expressionResult)
                        else -> return ParserErrorResult(
                                ErrorResult("Unrecognized result: $expressionResult"),
                                Location(
                                        ctx.start?.startPoint() ?: Point(0, 0),
                                        ctx.stop?.startPoint() ?: Point(0, 0)
                                )
                        )
                    }
                return WrappedResult(
                        ToylangMainAST.StatementNode.ExpressionNode.StringLiteralContentNode.StringInterpolationNode(
                                Location(
                                        ctx.start?.startPoint() ?: Point(0, 0),
                                        ctx.stop?.startPoint() ?: Point(0, 0)
                                ),
                                expression
                        )
                )
            }
            else -> return ErrorResult("Could not recognize string literal content from string literal content context")
        }
    }

    override fun visitDecimalLiteral(ctx: ToylangParser.DecimalLiteralContext): Result {
        return WrappedResult(
                ToylangMainAST.StatementNode.ExpressionNode.DecimalLiteralNode(
                        Location(ctx.position?.start ?: Point(0, 0), ctx.position?.end ?: Point(0, 0)),
                        ctx.DECIMALLITERAL()?.symbol?.text?.toFloat() ?: return ErrorResult("Could not get decimal literal from decimal literal context")
                )
        )
    }

    override fun visitParenExpression(ctx: ToylangParser.ParenExpressionContext): Result {
        return this.determineExpression(ctx.findExpression()!!)
    }

    override fun visitValueReference(ctx: ToylangParser.ValueReferenceContext): Result {
        val ident = ctx.IDENT()?.symbol?.text ?: return ErrorResult("No identifier found for value reference")
        return WrappedResult(ToylangMainAST.StatementNode.ExpressionNode.ValueReferenceNode(
                Location(
                        ctx.start?.startPoint() ?: Point(0, 0),
                        ctx.stop?.startPoint() ?: Point(0, 0)
                ),
                ToylangMainAST.IdentifierNode(
                        Location(
                                ctx.start?.startPoint() ?: Point(0, 0),
                                ctx.stop?.startPoint() ?: Point(0, 0)
                        ),
                        ident
                )
        ))
    }

    override fun visitIntLiteral(ctx: ToylangParser.IntLiteralContext): Result {
        val intLiteral = ctx.INTLITERAL()?.symbol?.text?.toInt() ?: return ErrorResult("No integer literal found for integer literal context")
        return WrappedResult(ToylangMainAST.StatementNode.ExpressionNode.IntegerLiteralNode(Location(ctx.position?.start ?: Point(0, 0), ctx.position?.end ?: Point(0, 0)), intLiteral))
    }

    override fun visitBinaryOperation(ctx: ToylangParser.BinaryOperationContext): Result {
        val leftNode = this.determineExpression(ctx.left ?: return ErrorResult("No left hand expression found in binary operation context"))
        val rightNode = this.determineExpression(ctx.right ?: return ErrorResult("No right hand expression found in binary operation context"))

        val left = when(leftNode){
            is WrappedResult<*> -> {
                when(leftNode.t){
                    is ToylangMainAST.StatementNode.ExpressionNode -> leftNode.t
                    null -> return ErrorResult("Left hand expression is null")
                    else -> return ErrorResult("Wrong type of node: ${leftNode.t::class.qualifiedName}")
                }
            }
            is ErrorResult -> {
                return ErrorResult("Could not get expression node for left hand of binary operation", leftNode)
            }
            else -> {
                return ErrorResult("Unrecognized result: $leftNode")
            }
        }
        val right = when(rightNode){
            is WrappedResult<*> -> {
                when(rightNode.t){
                    is ToylangMainAST.StatementNode.ExpressionNode -> rightNode.t
                    null -> return ErrorResult("Right hand expression is null")
                    else -> return ErrorResult("Wrong type of node: ${rightNode.t::class.qualifiedName}")
                }
            }
            is ErrorResult -> {
                return ErrorResult("Could not get expression node for right hand of binary operation", rightNode)
            }
            else -> {
                return ErrorResult("Unrecognized result: $rightNode")
            }
        }
        return when {
            ctx.PLUS() != null -> {
                WrappedResult(ToylangMainAST.StatementNode.ExpressionNode.BinaryOperation.BinaryPlusOperation(Location(ctx.position?.start ?: Point(0, 0), ctx.position?.end ?: Point(0, 0)), left, right))
            }
            ctx.MINUS() != null -> {
                WrappedResult(ToylangMainAST.StatementNode.ExpressionNode.BinaryOperation.BinaryMinusOperation(Location(ctx.position?.start ?: Point(0, 0), ctx.position?.end ?: Point(0, 0)), left, right))
            }
            ctx.ASTERISK() != null -> {
                WrappedResult(ToylangMainAST.StatementNode.ExpressionNode.BinaryOperation.BinaryMultOperation(Location(ctx.position?.start ?: Point(0, 0), ctx.position?.end ?: Point(0, 0)), left, right))
            }
            ctx.FORWORD_SLASH() != null -> {
                WrappedResult(ToylangMainAST.StatementNode.ExpressionNode.BinaryOperation.BinaryDivOperation(Location(ctx.position?.start ?: Point(0, 0), ctx.position?.end ?: Point(0, 0)), left, right))
            }
            else -> ErrorResult("No recognized operator in binary operation context")
        }
    }

    override fun visitFnParam(ctx: ToylangParser.FnParamContext): Result {
        val ident = ctx.IDENT()?.symbol?.text
            ?: return ParserErrorResult(
                ErrorResult(
                        "No identifier exists in function param context"
                ),
                    Location(
                            ctx.start?.startPoint() ?: Point(0, 0),
                            ctx.stop?.startPoint() ?: Point(0, 0)
                    )
                )
        val typeResult = this.visitType(
                ctx.findType()
                        ?: return ParserErrorResult(
                                ErrorResult(
                                        "No type exists in function param context"
                                ),
                                Location(
                                        ctx.start?.startPoint() ?: Point(0, 0),
                                        ctx.stop?.startPoint() ?: Point(0, 0)
                                )
                        )
            )
        val type = when(typeResult){
            is WrappedResult<*> -> {
                when(typeResult.t){
                    is ToylangMainAST.TypeAnnotationNode -> typeResult.t
                    null -> return ErrorResult("Type annotation node came back null")
                    else -> return ErrorResult("Type annotation node came back as wrong type: ${typeResult.t::class.qualifiedName}")
                }
            }
            is ErrorResult -> return ParserErrorResult(
                    ErrorResult("Could not get type annotation node from type annotation visitor", typeResult),
                    Location(
                            ctx.start?.startPoint() ?: Point(0, 0),
                            ctx.stop?.startPoint() ?: Point(0, 0)
                    ))
            else -> return ErrorResult("Unrecognized result: $typeResult")
        }
        return WrappedResult(
                ToylangMainAST.FunctionParamNode(
                        Location(
                                ctx.start?.startPoint() ?: Point(0, 0),
                                ctx.stop?.startPoint() ?: Point(0, 0)
                        ),
                        ToylangMainAST.IdentifierNode(Location(
                                ctx.start?.startPoint() ?: Point(0, 0),
                                ctx.stop?.startPoint() ?: Point(0, 0)
                        ),
                                ident
                        ),
                    type
                )
        )
    }

    override fun visitCodeblockStatement(ctx: ToylangParser.CodeblockStatementContext): Result {
        if(ctx.SEMICOLON() == null) return ErrorResult("Codeblock statement must end with semicolon")
        return when{
            ctx.findLetDeclaration() != null -> {
                this.visitLetDeclaration(ctx.findLetDeclaration()!!)
            }
            ctx.findReturnStatement() != null -> {
                this.visitReturnStatement(ctx.findReturnStatement()!!)
            }
            ctx.findExpression() != null -> {
                this.determineExpression(ctx.findExpression()!!)
            }
            else -> ParserErrorResult(
                    ErrorResult(
                            "Could not recognized codeblock statement"
                    ),
                    Location(
                            ctx.start?.startPoint() ?: Point(0, 0),
                            ctx.stop?.startPoint() ?: Point(0, 0)
                    )
            )
        }
    }

    override fun visitCodeBlock(ctx: ToylangParser.CodeBlockContext): Result {
        val statements = ctx.findCodeBlockStatements()?.findCodeblockStatement()?.map{
            when(val result = this.visitCodeblockStatement(it)){
                is WrappedResult<*> -> {
                    when(result.t){
                        is ToylangMainAST.StatementNode -> result.t
                        null -> return ErrorResult("Statement node came back null")
                        else -> return ErrorResult("Wrong type: ${result.t::class.qualifiedName}")
                    }
                }
                is ErrorResult -> {
                    return ErrorResult("Could not get statement node from codeblock statement visitor", result)
                }
                else -> return ErrorResult("Unrecognized result: $result")
            }
        } ?: return ErrorResult("No statements exists in codeblock context")
        return WrappedResult(ToylangMainAST.CodeblockNode(Location(
                ctx.start?.startPoint() ?: Point(0, 0),
                ctx.stop?.startPoint() ?: Point(0, 0)
        ), statements))
    }

    override fun visitType(ctx: ToylangParser.TypeContext): Result {
        val ident = ctx.IDENT()!!.symbol!!.text
                ?: return ParserErrorResult(
                        ErrorResult(
                                "No identifier exists in type annotation context"
                        ),
                        Location(
                                ctx.start?.startPoint() ?: Point(0, 0),
                                ctx.stop?.startPoint() ?: Point(0, 0)
                        )
                )
        return WrappedResult(
                ToylangMainAST.TypeAnnotationNode(
                        Location(
                                ctx.start?.startPoint() ?: Point(0, 0),
                                ctx.stop?.startPoint() ?: Point(0, 0)
                        ),
                        ToylangMainAST.IdentifierNode(
                                Location(
                                        ctx.start?.startPoint() ?: Point(0, 0),
                                        ctx.stop?.startPoint() ?: Point(0, 0)
                                ),
                                        ident
                        )
                )
        )
    }

    override fun visitFnDeclaration(ctx: ToylangParser.FnDeclarationContext): Result {
        this.currentContext = FunctionContext(this.currentContext)
        val params = ctx.findFnParams()?.findFnParam()?.map{
            when(val result = this.visitFnParam(it)){
                is WrappedResult<*> -> {
                    when(result.t) {
                        is ToylangMainAST.FunctionParamNode -> result.t
                        null -> return ErrorResult("Function param node came back null:\n" +
                                "Line: ${Location(
                                        ctx.start?.startPoint() ?: Point(0, 0),
                                        ctx.stop?.startPoint() ?: Point(0, 0)
                                )}")
                        else -> return ErrorResult("Wrong type: ${result.t::class.qualifiedName}:\n" +
                                "Line: ${Location(
                                        ctx.start?.startPoint() ?: Point(0, 0),
                                        ctx.stop?.startPoint() ?: Point(0, 0)
                                )}")
                    }
                }
                is ErrorResult -> {
                    return ErrorResult("Could not get function param node from fn param visitor:\n" +
                            "Line: ${Location(
                                    ctx.start?.startPoint() ?: Point(0, 0),
                                    ctx.stop?.startPoint() ?: Point(0, 0)
                            )}", result)
                }
                else -> return ErrorResult("Unrecognized result: $result")
            }
        }
        val identifier = ToylangMainAST.IdentifierNode(Location(
                ctx.start?.startPoint() ?: Point(0, 0),
                ctx.stop?.startPoint() ?: Point(0, 0)
        ), ctx.IDENT()?.symbol?.text ?: return ErrorResult("No identifier found in fn declaration context"))
        val codeblock = when(val result = this.visitCodeBlock(ctx.findCodeBlock()!!)){
            is WrappedResult<*> -> {
                when(result.t) {
                    is ToylangMainAST.CodeblockNode -> result.t
                    null -> return ErrorResult("Codeblock node came back null")
                    else -> return ErrorResult("Wrong type: ${result.t::class.qualifiedName}")
                }
            }
            is ErrorResult -> {
                return ErrorResult("Could not get function param node from fn param visitor:\n" +
                        "Line: ${ctx.position}", result)
            }
            else -> return ErrorResult("Unrecognized result: $result")
        }
//        val returnType = if(ctx.findFnType() != null) this.visitFnType(ctx.findFnType()!!) else ToylangMainAST.TypeAnnotationNode(ToylangMainAST.IdentifierNode("Unit"))
        val returnType = when (val returnTypeResult =
                if(ctx.findTypeAnnotation() != null)
                    this.visitTypeAnnotation(ctx.findTypeAnnotation()!!)
                else
                    WrappedResult(
                            ToylangMainAST.TypeAnnotationNode(
                                    Location(
                                            ctx.start?.startPoint() ?: Point(0, 0),
                                            ctx.stop?.startPoint() ?: Point(0, 0)
                                    ),
                                    ToylangMainAST.IdentifierNode(
                                            Location(
                                                    ctx.start?.startPoint() ?: Point(0, 0),
                                                    ctx.stop?.startPoint() ?: Point(0, 0)
                                            ),
                                            "Unit"
                                    ))
                            )
                    )
        {
            is WrappedResult<*> -> {
                when (returnTypeResult.t) {
                    is ToylangMainAST.TypeAnnotationNode -> returnTypeResult.t
                    null -> return ErrorResult("Function return type node came back null:\n" +
                            "Line: ${Location(
                                    ctx.start?.startPoint() ?: Point(0, 0),
                                    ctx.stop?.startPoint() ?: Point(0, 0)
                            )}")
                    else -> return ErrorResult("Function return type node came back as the wrong type: ${returnTypeResult.t::class.qualifiedName}:\n" +
                            "Line: ${Location(
                                    ctx.start?.startPoint() ?: Point(0, 0),
                                    ctx.stop?.startPoint() ?: Point(0, 0)
                            )}")
                }
            }
            is ErrorResult -> {
                return ErrorResult("Could not get return type node from return type visitor:\n" +
                        "Line: ${Location(
                                ctx.start?.startPoint() ?: Point(0, 0),
                                ctx.stop?.startPoint() ?: Point(0, 0)
                        )}", returnTypeResult)
            }
            else -> return ErrorResult("Unrecognized result: $returnTypeResult")
        }
                return WrappedResult(ToylangMainAST.StatementNode.FunctionDeclNode(Location(
                        ctx.start?.startPoint() ?: Point(0, 0),
                        ctx.stop?.startPoint() ?: Point(0, 0)
                ), identifier, params!!, codeblock, returnType, this.currentContext))
    }

    override fun visitReturnStatement(ctx: ToylangParser.ReturnStatementContext): Result {
        val expressionResult = this.determineExpression(ctx.findExpression() ?: return ErrorResult("Could not get expression from return statement context:\n" +
                "Line: ${ctx.position}"))
        val expression = when(expressionResult){
            is WrappedResult<*> -> {
                when(expressionResult.t){
                    is ToylangMainAST.StatementNode.ExpressionNode -> expressionResult.t
                    null -> return ErrorResult("Expression node came back null:\n" +
                            "Line: ${Location(
                                    ctx.start?.startPoint() ?: Point(0, 0),
                                    ctx.stop?.startPoint() ?: Point(0, 0)
                            )}")
                    else -> return ErrorResult("Wrong type: ${expressionResult.t::class.qualifiedName}:\n" +
                            "Line: ${Location(
                                    ctx.start?.startPoint() ?: Point(0, 0),
                                    ctx.stop?.startPoint() ?: Point(0, 0)
                            )}")
                }
            }
            is ErrorResult -> {
                return ErrorResult("Could not get expression node from expression visitor:\n" +
                        "Line: ${Location(
                                ctx.start?.startPoint() ?: Point(0, 0),
                                ctx.stop?.startPoint() ?: Point(0, 0)
                        )}", expressionResult)
            }
            else -> return ErrorResult("Unrecognized result: $expressionResult")
        }
        return WrappedResult(ToylangMainAST.StatementNode.ReturnStatementNode(Location(
                ctx.start?.startPoint() ?: Point(0, 0),
                ctx.stop?.startPoint() ?: Point(0, 0)
        ), expression))
    }

    override fun visitFnType(ctx: ToylangParser.FnTypeContext): Result {
        return WrappedResult(
                ToylangMainAST.TypeAnnotationNode(
                        Location(
                                ctx.start?.startPoint() ?: Point(0, 0),
                                ctx.stop?.startPoint() ?: Point(0, 0)
                        ),
                        ToylangMainAST.IdentifierNode(
                                Location(
                                        ctx.start?.startPoint() ?: Point(0, 0),
                                        ctx.stop?.startPoint() ?: Point(0, 0)
                                ),
                                ctx.findType()?.IDENT()?.symbol?.text
                                        ?:
                                        return ErrorResult("Could not get identifier in function type context:\nLine: ${ctx.position}"))))
    }
}
