package com.couch.kotlinx

import com.couch.kotlinx.llvm.Module
import com.couch.kotlinx.llvm.buildModule
import com.couch.toylang.ToylangParser
import com.couch.toylang.ToylangParserBaseVisitor

/*
class ToylangVisitor: ToylangParserBaseVisitor<Module>(){
    override fun visitToylangFile(ctx: ToylangParser.ToylangFileContext): Module {
        return Module(ctx.)
    }
    override fun visitLetDeclaration(ctx: ToylangParser.LetDeclarationContext): Module {
        val name = ctx.IDENT()?.symbol?.text
        return Module(name ?: "")
    }

    override fun visitAssignment(ctx: ToylangParser.AssignmentContext): Module {

    }

    override fun visitBinaryOperation(ctx: ToylangParser.BinaryOperationContext) {

    }
}*/
