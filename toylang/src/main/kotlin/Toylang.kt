package com.couch.kotlinx

import com.couch.kotlinx.ast.*
import com.couch.kotlinx.llvm.*
import com.couch.toylang.ToylangLexer
import com.couch.toylang.ToylangParser
import com.strumenta.kolasu.model.*
import org.antlr.v4.kotlinruntime.*
import org.bytedeco.javacpp.BytePointer
import org.bytedeco.llvm.global.LLVM

const val DEBUG = true

fun convertStringLitNodeToString(stringNode: StringLiteralNode): String{
    return stringNode.content.map {
        val sb = StringBuilder()
        when(it){
            is RawStringLiteralContentNode -> sb.append(it.string)
            is StringInterpolationNode -> {
                when(it.interpolatedExpr){
                    is IntegerLiteralNode -> sb.append(it.interpolatedExpr.integer)
                    is DecimalLiteralNode -> sb.append(it.interpolatedExpr.float)
                    is StringLiteralNode -> sb.append(convertStringLitNodeToString(it.interpolatedExpr))
                }
            }
        }
        sb.toString()
    }.joinToString()
}

fun convertLetNode(node: LetNode, module: Module){
    when (node.assignment.expression) {
        is IntegerLiteralNode -> {
            module.createGlobalVariable(node.identifier.identifier, Type.Int32Type()) {
                this.setGlobalInitializer{
                    Value.Int32ConstValue(node.assignment.expression.integer)
                }
            }
        }
        is DecimalLiteralNode -> {
            module.createGlobalVariable(node.identifier.identifier, Type.FloatType()) {
                this.setGlobalInitializer{
                    Value.FloatConstValue(node.assignment.expression.float)
                }
            }
        }
        is StringLiteralNode -> {
            module.createGlobalVariable(node.identifier.identifier, Type.ArrayType(Type.Int8Type(), node.assignment.expression.content.size)) {
                val stringContent = convertStringLitNodeToString(node.assignment.expression)
                this.setGlobalInitializer{
                    Value.StringConstValue(stringContent)
                }
            }
        }
        else -> throw IllegalArgumentException("Could not convert let node properly")
    }
}

fun convertLocalLetNode(node: LetNode, block: BasicBlock){
    when (node.assignment.expression) {
        is IntegerLiteralNode -> {
            block.createVariable(node.identifier.identifier, Type.Int32Type()) {
                this.value = Value.Int32ConstValue(node.assignment.expression.integer)
            }
        }
        is DecimalLiteralNode -> {
            block.createVariable(node.identifier.identifier, Type.FloatType()) {
                this.value = Value.FloatConstValue(node.assignment.expression.float)
            }
        }
        is StringLiteralNode -> {
            block.createVariable(node.identifier.identifier, Type.ArrayType(Type.Int8Type(), node.assignment.expression.content.size)) {
                val stringContent = convertStringLitNodeToString(node.assignment.expression)
                this.value = Value.StringConstValue(stringContent)
            }
        }
        else -> throw IllegalArgumentException("Could not convert let node properly")
    }
}

fun convertTypeRefNode(node: TypeNode): Type = when(node.typeIdentifier.identifier){
    "Int" -> Type.Int32Type()
    "Float" -> Type.FloatType()
    "Int16" -> Type.Int16Type()
    "Int8" -> Type.Int8Type()
    "Int64" -> Type.Int64Type()
    else -> Type.VoidType()
}

fun convertReturnNode(node: ReturnStatementNode, block: BasicBlock) {
    block.addReturnStatement {
        when(node.expression){
            is IntegerLiteralNode -> {
                Value.Int32ConstValue(node.expression.integer)
            }
            is DecimalLiteralNode -> Value.FloatConstValue(node.expression.float)
            is StringLiteralNode -> Value.StringConstValue(convertStringLitNodeToString(node.expression))
            is NoneExpressionNode -> Value.NoneValue()
            else -> Value.NoneValue()
        }
    }
}

fun convertFunctionDeclNode(node: FunctionDeclNode, module: Module){
    module.createFunction(node.identifier.identifier){
        node.params.forEach {
            this.createFunctionParam {
                convertTypeRefNode(it.type)
            }
        }
        this.returnType = convertTypeRefNode(node.returnType.type)
        addBlock(node.identifier.identifier + "_loc1"){
            convertReturnNode(node.codeBlock.returnStatement, this)
            node.codeBlock.statements.forEach{
                when(it){
                    is LetNode -> convertLocalLetNode(it, this)
                    else -> throw IllegalArgumentException("Unrecognized or illegal statement")
                }
            }
        }
    }
}

fun main(){
    val testFile = CharStreams.fromFileName("test.toy")
    val lexer = ToylangLexer(testFile)
    val parser = ToylangParser(CommonTokenStream(lexer))
    val tree = parser.toylangFile()
//    val listener = ToylangListener()
//    val parseTreeWalker = ParseTreeWalker()
//    parseTreeWalker.walk(listener, tree)
    val visitor = ToylangVisitor()
    println("About to visit ast")
    val rootNode = visitor.visit(tree)!!
    rootNode.assignParents()
    val module: Module? = buildModule(lexer.sourceName) {
        rootNode.walkChildren().forEach {
            println(it)
            when (it) {
                is LetNode -> {
                    convertLetNode(it, this)
                }
                is FunctionDeclNode -> {
                    convertFunctionDeclNode(it, this)
                }
            }
        }
    }
    val buffer = BytePointer()
    LLVM.LLVMVerifyModule(module!!.module, LLVM.LLVMAbortProcessAction, buffer)
    LLVM.LLVMDisposeMessage(buffer)
    val result = LLVM.LLVMWriteBitcodeToFile(module.module, "test.bc")
    println(result)
}