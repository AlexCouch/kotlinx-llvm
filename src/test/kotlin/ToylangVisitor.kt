package com.couch.kotlinx

import com.couch.kotlinx.ast.*
import com.couch.kotlinx.llvm.Module
import com.couch.kotlinx.llvm.buildModule
import com.couch.toylang.ToylangLexer
import com.couch.toylang.ToylangParser
import com.couch.toylang.ToylangParserBaseVisitor
import org.antlr.v4.kotlinruntime.tree.ParseTree

class ToylangVisitor: ToylangParserBaseVisitor<ToylangASTNode>(){
    /*override fun visit(tree: ParseTree): ToylangASTNode? {
        println("Visiting ast")
        return super.visit(tree)
    }*/
    override fun visitToylangFile(ctx: ToylangParser.ToylangFileContext): ToylangASTNode.ToylangASTRootNode {
        val root = ToylangASTNode.ToylangASTRootNode()
        ctx.findLine().forEach {
            root.statements.add(this.visitStatement(it.findStatement()!!))
        }
        return root
    }

    override fun visitStatement(ctx: ToylangParser.StatementContext): StatementNode = when{
            ctx.findLetDeclaration() != null -> {
                this.visitLetDeclaration(ctx.findLetDeclaration()!!)
            }
            ctx.findFnDeclaration() != null -> {
                this.visitFnDeclaration(ctx.findFnDeclaration()!!)
            }
            else -> throw IllegalArgumentException("Could not parse statement")
    }

    override fun visitLetDeclaration(ctx: ToylangParser.LetDeclarationContext): LetNode {
        val assignmentNode = this.visitAssignment(ctx.findAssignment()!!)
        val identifierNode = IdentifierNode(ctx.IDENT()?.symbol?.text!!)
        return LetNode(identifierNode, assignmentNode)
    }

    override fun visitAssignment(ctx: ToylangParser.AssignmentContext): AssignmentNode {
        val expr = ctx.findExpression()
        val expressionNode: ExpressionNode = if(expr != null){
            this.determineExpression(expr)
        }else{
            throw IllegalArgumentException("Could not parse assignment: ${ctx.findExpression()?.text}")
        }
        return AssignmentNode(expressionNode)
    }

    fun determineExpression(ctx: ToylangParser.ExpressionContext): ExpressionNode{
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
            else -> throw IllegalArgumentException("Unknown Expression!")
        }
    }

    override fun visitStringLiteral(ctx: ToylangParser.StringLiteralContext): StringLiteralNode {
        val content = ctx.parts.map {
            this.visitStringLiteralContent(it)
        }
        return StringLiteralNode(content)
    }

    override fun visitStringLiteralContent(ctx: ToylangParser.StringLiteralContentContext): StringLiteralContentNode = when{
        ctx.STRING_CONTENT() != null -> {
            RawStringLiteralContentNode(ctx.STRING_CONTENT()?.symbol?.text!!)
        }
        ctx.INTERPOLATION_OPEN() != null && ctx.INTERPOLATION_CLOSE() != null -> {
            StringInterpolationNode(this.determineExpression(ctx.findExpression()!!))
        }
        else -> throw IllegalArgumentException("Tried to parse string contents but couldn't")
    }

    override fun visitDecimalLiteral(ctx: ToylangParser.DecimalLiteralContext): DecimalLiteralNode {
        return DecimalLiteralNode(ctx.DECIMALLITERAL()?.symbol?.text!!.toFloat())
    }

    override fun visitParenExpression(ctx: ToylangParser.ParenExpressionContext): ExpressionNode {
        return this.determineExpression(ctx.findExpression()!!)
    }

    override fun visitValueReference(ctx: ToylangParser.ValueReferenceContext): ValueReferenceNode =
            ValueReferenceNode(IdentifierNode(ctx.IDENT()?.symbol?.text ?: "Unknown"))

    override fun visitIntLiteral(ctx: ToylangParser.IntLiteralContext): IntegerLiteralNode =
            IntegerLiteralNode(ctx.INTLITERAL()?.symbol?.text?.toInt() ?: 0)

    override fun visitBinaryOperation(ctx: ToylangParser.BinaryOperationContext): BinaryOperation {
        val leftNode = this.determineExpression(ctx.left!!)
        val rightNode = this.determineExpression(ctx.right!!)
        return when {
            ctx.PLUS() != null -> {
                BinaryPlusOperation(leftNode, rightNode)
            }
            ctx.MINUS() != null -> {
                BinaryMinusOperation(leftNode, rightNode)
            }
            ctx.ASTERISK() != null -> {
                BinaryMultOperation(leftNode, rightNode)
            }
            ctx.DIVISION() != null -> {
                BinaryDivOperation(leftNode, rightNode)
            }
            else -> throw IllegalArgumentException("Unknown binary operation: ${ctx.operator}")
        }
    }

    override fun visitFnParam(ctx: ToylangParser.FnParamContext): ParamNode {
        return ParamNode(IdentifierNode(ctx.IDENT()?.symbol?.text!!), this.visitType(ctx.findType()!!))
    }

    override fun visitCodeBlock(ctx: ToylangParser.CodeBlockContext): CodeblockNode {
        val statements = ctx.findCodeBlockStatements()?.findStatement()!!.map{
            this.visitStatement(it)
        }
        return CodeblockNode(statements)
    }

    override fun visitType(ctx: ToylangParser.TypeContext): TypeNode {
        return TypeNode(IdentifierNode(ctx.IDENT()?.symbol?.text!!))
    }

    override fun visitFnDeclaration(ctx: ToylangParser.FnDeclarationContext): FunctionDeclNode {
        val params = ctx.findFnParams()?.findFnParam()?.map{
            this.visitFnParam(it)
        }
        val identifier = IdentifierNode(ctx.IDENT()?.symbol?.text!!)
        val codeBlock = this.visitCodeBlock(ctx.findCodeBlock()!!)
        return FunctionDeclNode(identifier, params!!, codeBlock)
    }
}
