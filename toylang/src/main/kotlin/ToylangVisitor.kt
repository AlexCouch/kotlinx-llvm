package com.couch.kotlinx

import com.couch.kotlinx.ast.*
import com.couch.kotlinx.parsing.Scope
import com.couch.kotlinx.parsing.Symbol
import com.couch.kotlinx.parsing.ToylangMainAST
import com.couch.kotlinx.parsing.p1.P1Scope
import com.couch.kotlinx.parsing.p1.P1Symbol
import com.couch.kotlinx.parsing.p1.ToylangP1ASTNode
import com.couch.toylang.ToylangParser
import com.couch.toylang.ToylangParserBaseVisitor
import jdk.internal.org.objectweb.asm.tree.TypeAnnotationNode

class ToylangVisitor: ToylangParserBaseVisitor<ToylangASTNode>(){
    override fun visitFunctionCall(ctx: ToylangParser.FunctionCallContext): ToylangMainAST.StatementNode.ExpressionNode.FunctionCallNode {
        val fnName = ctx.IDENT()!!.symbol!!.text!!
        val fnArgs = ctx.findFnArgs()!!
        val exprs = fnArgs.findExpression().map {
            this.determineExpression(it)
        }

        return ToylangMainAST.StatementNode.ExpressionNode.FunctionCallNode(fnName, exprs)
    }

    override fun visitToylangFile(ctx: ToylangParser.ToylangFileContext): ToylangMainAST.RootNode {
        val scope = P1Scope.GlobalScope()
        val root = ToylangMainAST.RootNode(scope)
        ctx.findLine().forEach {
            val statement = this.visitStatement(it.findStatement()!!).also{
                if(it is ToylangMainAST.StatementNode.LetNode){
                    scope.symbols.add(P1Symbol.VarSymbol(it.identifier.identifier, it))
                }
            }
            root.statements.add(statement)
        }
        return root
    }

    override fun visitStatement(ctx: ToylangParser.StatementContext): ToylangMainAST.StatementNode = when{
            ctx.findLetDeclaration() != null -> {
                this.visitLetDeclaration(ctx.findLetDeclaration()!!)
            }
            ctx.findFnDeclaration() != null -> {
                this.visitFnDeclaration(ctx.findFnDeclaration()!!)
            }
            ctx.findExpression() != null -> {
                this.determineExpression(ctx.findExpression()!!)
            }
            else -> throw IllegalArgumentException("Could not parse statement")
    }

    override fun visitLetDeclaration(ctx: ToylangParser.LetDeclarationContext): ToylangMainAST.StatementNode.LetNode {
        val assignmentNode = this.visitAssignment(ctx.findAssignment()!!)
        val identifierNode = ToylangMainAST.IdentifierNode(ctx.IDENT()?.symbol?.text!!)
        val mutable = ctx.MUT() != null ?: false
        return ToylangMainAST.StatementNode.LetNode(identifierNode, mutable, ToylangMainAST.TypeAnnotationNode(ToylangMainAST.IdentifierNode("Unit")), assignmentNode)
    }

    override fun visitAssignment(ctx: ToylangParser.AssignmentContext): ToylangMainAST.StatementNode.AssignmentNode {
        val expr = ctx.findExpression()
        val expressionNode: ToylangMainAST.StatementNode.ExpressionNode = if(expr != null){
            this.determineExpression(expr)
        }else{
            throw IllegalArgumentException("Could not parse assignment: ${ctx.findExpression()?.text}")
        }
        return ToylangMainAST.StatementNode.AssignmentNode(expressionNode)
    }

    fun determineExpression(ctx: ToylangParser.ExpressionContext): ToylangMainAST.StatementNode.ExpressionNode {
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
            else -> throw IllegalArgumentException("Unknown Expression!")
        }
    }

    override fun visitStringLiteral(ctx: ToylangParser.StringLiteralContext): ToylangMainAST.StatementNode.ExpressionNode.StringLiteralNode {
        val content = ctx.parts.map {
            this.visitStringLiteralContent(it)
        }
        return ToylangMainAST.StatementNode.ExpressionNode.StringLiteralNode(content)
    }

    override fun visitStringLiteralContent(ctx: ToylangParser.StringLiteralContentContext): ToylangMainAST.StatementNode.ExpressionNode.StringLiteralContentNode = when{
        ctx.STRING_CONTENT() != null -> {
            ToylangMainAST.StatementNode.ExpressionNode.StringLiteralContentNode.RawStringLiteralContentNode(ctx.STRING_CONTENT()?.symbol?.text!!)
        }
        ctx.INTERPOLATION_OPEN() != null && ctx.INTERPOLATION_CLOSE() != null -> {
            ToylangMainAST.StatementNode.ExpressionNode.StringLiteralContentNode.StringInterpolationNode(this.determineExpression(ctx.findExpression()!!))
        }
        else -> throw IllegalArgumentException("Tried to parse string contents but couldn't")
    }

    override fun visitDecimalLiteral(ctx: ToylangParser.DecimalLiteralContext): ToylangMainAST.StatementNode.ExpressionNode.DecimalLiteralNode {
        return ToylangMainAST.StatementNode.ExpressionNode.DecimalLiteralNode(ctx.DECIMALLITERAL()?.symbol?.text!!.toFloat())
    }

    override fun visitParenExpression(ctx: ToylangParser.ParenExpressionContext): ToylangMainAST.StatementNode.ExpressionNode {
        return this.determineExpression(ctx.findExpression()!!)
    }

    override fun visitValueReference(ctx: ToylangParser.ValueReferenceContext): ToylangMainAST.StatementNode.ExpressionNode.ValueReferenceNode =
            ToylangMainAST.StatementNode.ExpressionNode.ValueReferenceNode(ToylangMainAST.IdentifierNode(ctx.IDENT()?.symbol?.text
                    ?: "Unknown"))

    override fun visitIntLiteral(ctx: ToylangParser.IntLiteralContext): ToylangMainAST.StatementNode.ExpressionNode.IntegerLiteralNode =
            ToylangMainAST.StatementNode.ExpressionNode.IntegerLiteralNode(ctx.INTLITERAL()?.symbol?.text?.toInt() ?: 0)

    override fun visitBinaryOperation(ctx: ToylangParser.BinaryOperationContext): ToylangMainAST.StatementNode.ExpressionNode.BinaryOperation {
        val leftNode = this.determineExpression(ctx.left!!)
        val rightNode = this.determineExpression(ctx.right!!)
        return when {
            ctx.PLUS() != null -> {
                ToylangMainAST.StatementNode.ExpressionNode.BinaryOperation.BinaryPlusOperation(leftNode, rightNode)
            }
            ctx.MINUS() != null -> {
                ToylangMainAST.StatementNode.ExpressionNode.BinaryOperation.BinaryMinusOperation(leftNode, rightNode)
            }
            ctx.ASTERISK() != null -> {
                ToylangMainAST.StatementNode.ExpressionNode.BinaryOperation.BinaryMultOperation(leftNode, rightNode)
            }
            ctx.FORWORD_SLASH() != null -> {
                ToylangMainAST.StatementNode.ExpressionNode.BinaryOperation.BinaryDivOperation(leftNode, rightNode)
            }
            else -> throw IllegalArgumentException("Unknown binary operation: ${ctx.operator}")
        }
    }

    override fun visitFnParam(ctx: ToylangParser.FnParamContext): ToylangMainAST.FunctionParamNode {
        return ToylangMainAST.FunctionParamNode(ToylangMainAST.IdentifierNode(ctx.IDENT()?.symbol?.text!!), this.visitType(ctx.findType()!!))
    }

    override fun visitCodeBlock(ctx: ToylangParser.CodeBlockContext): ToylangMainAST.CodeblockNode {
        val statements = ctx.findCodeBlockStatements()?.findStatement()!!.map{
            this.visitStatement(it)
        }
        val returnStatement = if(ctx.findReturnStatement() != null) this.visitReturnStatement(ctx.findReturnStatement()!!) else ToylangMainAST.StatementNode.ReturnStatementNode(ToylangMainAST.StatementNode.ExpressionNode.NoneExpression())
        return ToylangMainAST.CodeblockNode(statements, returnStatement)
    }

    override fun visitType(ctx: ToylangParser.TypeContext): ToylangMainAST.TypeAnnotationNode {
        return ToylangMainAST.TypeAnnotationNode(ToylangMainAST.IdentifierNode(ctx.IDENT()!!.symbol!!.text!!))
    }

    override fun visitFnDeclaration(ctx: ToylangParser.FnDeclarationContext): ToylangMainAST.StatementNode.FunctionDeclNode {
        val scope = P1Scope.FunctionScope()
        val params = ctx.findFnParams()?.findFnParam()?.map{
            this.visitFnParam(it)
        }?.also {
            it.withIndex().forEach { (idx, it) ->
                scope.symbols.add(P1Symbol.FunctionParamSymbol(it.identifier.identifier, idx, it))
            }
        }
        val identifier = ToylangMainAST.IdentifierNode(ctx.IDENT()?.symbol?.text!!)
        val codeBlock = this.visitCodeBlock(ctx.findCodeBlock()!!)
        codeBlock.statements.forEach {
            if(it is ToylangMainAST.StatementNode.LetNode){
                scope.symbols.add(P1Symbol.VarSymbol(it.identifier.identifier, it))
            }
        }
        val returnType = if(ctx.findFnType() != null) this.visitFnType(ctx.findFnType()!!) else ToylangMainAST.TypeAnnotationNode(ToylangMainAST.IdentifierNode("Unit"))
        return ToylangMainAST.StatementNode.FunctionDeclNode(scope, identifier, params!!, codeBlock, returnType)
    }

    override fun visitReturnStatement(ctx: ToylangParser.ReturnStatementContext): ToylangMainAST.StatementNode.ReturnStatementNode {
        return ToylangMainAST.StatementNode.ReturnStatementNode(this.determineExpression(ctx.findExpression()!!))
    }

    override fun visitFnType(ctx: ToylangParser.FnTypeContext): ToylangMainAST.TypeAnnotationNode {
        return ToylangMainAST.TypeAnnotationNode(ToylangMainAST.IdentifierNode(ctx.findType()?.IDENT()?.symbol?.text!!))
    }
}
