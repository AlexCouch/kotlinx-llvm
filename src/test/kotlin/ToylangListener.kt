package com.couch.kotlinx

import org.antlr.v4.kotlinruntime.ParserRuleContext
import org.antlr.v4.kotlinruntime.tree.ErrorNode
import org.antlr.v4.kotlinruntime.tree.TerminalNode

class ToylangListener: ToylangParserListener{
    private val prettyPrinter = PrettyPrintTree()
    private val bytecodeSB = StringBuilder()
    override fun enterToylangFile(ctx: ToylangParser.ToylangFileContext) {
        prettyPrinter.append("ROOT{"){
            this.indent()
        }
    }

    override fun exitToylangFile(ctx: ToylangParser.ToylangFileContext) {
        prettyPrinter.unindent()
        if(DEBUG) {
            println(this.prettyPrinter.build())
        }
    }

    override fun enterLine(ctx: ToylangParser.LineContext) {
        prettyPrinter.append("LINE{"){
            this.indent()
        }
    }

    override fun exitLine(ctx: ToylangParser.LineContext) {
        this.prettyPrinter.unindent{
            this.append("}")
        }
    }

    override fun enterStatement(ctx: ToylangParser.StatementContext) {
        this.prettyPrinter.append("STATEMENT{"){
            this.indent()
        }
    }

    override fun exitStatement(ctx: ToylangParser.StatementContext) {
        this.prettyPrinter.unindent{
            this.append("}")
        }
    }

    override fun enterLetDeclaration(ctx: ToylangParser.LetDeclarationContext) {
        this.prettyPrinter.append("LET{"){
            this.indent{
                this.append("Mutable: ${ctx.MUT() ?: "false"}")
            }
        }
        this.bytecodeSB.append("")
    }

    override fun exitLetDeclaration(ctx: ToylangParser.LetDeclarationContext) {
        this.prettyPrinter.unindent{
            this.append("}")
        }
    }

    override fun enterFnDeclaration(ctx: ToylangParser.FnDeclarationContext) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun exitFnDeclaration(ctx: ToylangParser.FnDeclarationContext) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun enterFnParam(ctx: ToylangParser.FnParamContext) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun exitFnParam(ctx: ToylangParser.FnParamContext) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun enterFnParams(ctx: ToylangParser.FnParamsContext) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun exitFnParams(ctx: ToylangParser.FnParamsContext) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun enterCodeBlockStatements(ctx: ToylangParser.CodeBlockStatementsContext) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun exitCodeBlockStatements(ctx: ToylangParser.CodeBlockStatementsContext) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun enterCodeBlock(ctx: ToylangParser.CodeBlockContext) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun exitCodeBlock(ctx: ToylangParser.CodeBlockContext) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun enterReturnStatement(ctx: ToylangParser.ReturnStatementContext) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun exitReturnStatement(ctx: ToylangParser.ReturnStatementContext) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun enterAssignment(ctx: ToylangParser.AssignmentContext) {
        this.prettyPrinter.append("ASSIGNMENT{"){
            this.indent{
                this.append("Identifier: ${ctx.IDENT()?.text}")
            }
        }
    }

    override fun exitAssignment(ctx: ToylangParser.AssignmentContext) {
        this.prettyPrinter.unindent{
            this.append("}")
        }
    }

    override fun enterDecimalLiteral(ctx: ToylangParser.DecimalLiteralContext) {
        this.prettyPrinter.append("DECIMAL_LITERAL")
    }

    override fun exitDecimalLiteral(ctx: ToylangParser.DecimalLiteralContext) {
    }

    override fun enterMinusExpression(ctx: ToylangParser.MinusExpressionContext) {
        this.prettyPrinter.append("MINUS{")
    }

    override fun exitMinusExpression(ctx: ToylangParser.MinusExpressionContext) {
        this.prettyPrinter.unindent{
            this.append("}")
        }
    }

    override fun enterValueReference(ctx: ToylangParser.ValueReferenceContext) {
        this.prettyPrinter.append("VALUE_REFERENCE{"){
            this.indent{
                this.append(ctx.IDENT()!!.text)
            }
        }
    }

    override fun exitValueReference(ctx: ToylangParser.ValueReferenceContext) {
        this.prettyPrinter.unindent{
            this.append("}")
        }
    }

    override fun enterStringLiteral(ctx: ToylangParser.StringLiteralContext) {
        this.prettyPrinter.append("STRING_LITERAL")
    }

    override fun exitStringLiteral(ctx: ToylangParser.StringLiteralContext) {
    }

    override fun enterIntLiteral(ctx: ToylangParser.IntLiteralContext) {
        this.prettyPrinter.append("INTEGER_LITERAL")
    }

    override fun exitIntLiteral(ctx: ToylangParser.IntLiteralContext) {
    }

    override fun enterParenExpression(ctx: ToylangParser.ParenExpressionContext) {
        this.prettyPrinter.append("GROUP_EXPRESSION{"){
            this.indent()
        }
    }

    override fun exitParenExpression(ctx: ToylangParser.ParenExpressionContext) {
        this.prettyPrinter.unindent {
            this.append("}")
        }
    }

    override fun enterBinaryOperation(ctx: ToylangParser.BinaryOperationContext) {
        this.prettyPrinter.append("BINARY_OP{"){
            this.indent{
                this.append("Operator: ${ctx.operator?.text}")
            }
        }
    }

    override fun exitBinaryOperation(ctx: ToylangParser.BinaryOperationContext) {
        this.prettyPrinter.unindent {
            this.append("}")
        }
    }

    override fun enterStringLiteralContent(ctx: ToylangParser.StringLiteralContentContext) {
        this.prettyPrinter.append("STRING_LITERAL_CONTENT{"){
            this.indent()
        }
    }

    override fun exitStringLiteralContent(ctx: ToylangParser.StringLiteralContentContext) {
        this.prettyPrinter.unindent {
            this.append("}")
        }
    }

    override fun enterType(ctx: ToylangParser.TypeContext) {
        this.prettyPrinter.append("TYPE{"){
            this.indent()
        }
    }

    override fun exitType(ctx: ToylangParser.TypeContext) {
        this.prettyPrinter.unindent { this.append("}") }
    }

    override fun enterEveryRule(ctx: ParserRuleContext) {

    }

    override fun exitEveryRule(ctx: ParserRuleContext) {
    }

    override fun visitErrorNode(node: ErrorNode) {
    }

    override fun visitTerminal(node: TerminalNode) {
    }

}