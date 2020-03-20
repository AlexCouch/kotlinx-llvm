package parsing

import parsing.ast.ToylangASTNode
import Result

abstract class Parse<in IN: ToylangASTNode, out OUT: Result>{
    open fun parseElement(node: IN, context: Context): OUT{
        return this.parseElement(node, context)
    }

    open fun parseFile(node: IN, context: Context): OUT{
        return this.parseStatement(node, context)
    }

    open fun parseStatement(node: IN, context: Context): OUT{
        return this.parseElement(node, context)
    }

    open fun parseVariableDeclaration(node: IN, context: Context): OUT{
        return this.parseAssignment(node, context)
    }
    open fun parseAssignment(node: IN, context: Context): OUT{
        return this.parseExpression(node, context)
    }
    open fun parseExpression(node: IN, context: Context): OUT{
        return this.parseElement(node, context)
    }

    open fun parseFunctionDeclaration(node: IN, context: Context): OUT{
        return this.parseFunctionParams(node, context)
    }
    open fun parseFunctionParams(node: IN, context: Context): OUT{
        return this.parseElement(node, context)
    }
    open fun parseFunctionBody(node: IN, context: Context): OUT{
        return this.parseStatement(node, context)
    }

    open fun parseOperation(node: IN, context: Context): OUT{
        return this.parseExpression(node, context)
    }

    open fun parseBinaryOperation(node: IN, context: Context): OUT{
        return this.parseExpression(node, context)
    }

    open fun parseUnaryOperation(node: IN, context: Context): OUT{
        return this.parseExpression(node, context)
    }

    open fun parseReturnStatement(node: IN, context: Context): OUT{
        return this.parseStatement(node, context)
    }

    open fun parseFunctionCall(node: IN, context: Context): OUT{
        return this.parseElement(node, context)
    }

    open fun parseValueReference(node: IN, context: Context): OUT{
        return this.parseElement(node, context)
    }

    open fun parseStringLiteral(node: IN, context: Context): OUT{
        return this.parseElement(node, context)
    }
}