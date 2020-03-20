package parsing

import Result
import ErrorResult
import OKResult
import WrappedResult
import parsing.hir.ToylangHIRElement

interface Context{
    val parentContext: Context?
    fun findIdentifier(identifier: String): Result
}

interface ProvidesContext{
    val context: Context
}

data class GlobalContext(override val parentContext: Context? = null, val globalVariables: ArrayList<ToylangHIRElement.StatementNode.VariableNode.GlobalVariableNode> = arrayListOf(), val functions: ArrayList<ToylangHIRElement.StatementNode.FunctionDeclNode> = arrayListOf()): Context {
    override fun findIdentifier(identifier: String): Result {
        val found = this.globalVariables.find { it.identifier == identifier } ?: this.functions.find { it.identifier == identifier }
        return if(found != null) WrappedResult(found) else when(val parentFound = this.parentContext?.findIdentifier(identifier) ?: return ErrorResult("No parent context, and no symbol found in current context: $identifier")){
            is WrappedResult<*> -> parentFound
            is ErrorResult -> ErrorResult("Could not find symbol with identifier: $identifier", parentFound)
            else -> OKResult
        }
    }
}

data class FunctionContext(override val parentContext: Context, val params: ArrayList<ToylangHIRElement.FunctionParamNode> = arrayListOf(), val localVariables: ArrayList<ToylangHIRElement.StatementNode.VariableNode.LocalVariableNode> = arrayListOf()): Context{
    override fun findIdentifier(identifier: String): Result {
        val found = this.params.find { it.identifier == identifier } ?: this.localVariables.find { it.identifier == identifier }
        return if(found != null) WrappedResult(found) else when(val parentFound = this.parentContext.findIdentifier(identifier)){
            is WrappedResult<*> -> parentFound
            is ErrorResult -> ErrorResult("Could not find symbol with identifier: $identifier", parentFound)
            else -> ErrorResult("Unrecognized result: $parentFound")
        }
    }
}