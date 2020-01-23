package com.couch.kotlinx

class PrettyPrintTree{
    private var indentationLevel = 0
    private val prettyPrint = StringBuilder()

    fun append(string: String){
        if(this.indentationLevel > 0) {
            for (ind in 1..indentationLevel) {
                this.prettyPrint.append("\t")
            }
        }
        this.prettyPrint.append("$string\n")
    }

    fun append(string: String, block: PrettyPrintTree.()->Unit){
        this.append(string)
        this.block()
    }

    fun indent(){
        this.indentationLevel++
    }

    fun indent(block: PrettyPrintTree.()->Unit){
        this.indent()
        this.block()
    }

    fun unindent(){
        this.indentationLevel--

    }

    fun unindent(block: PrettyPrintTree.()->Unit){
        this.unindent()
        this.block()
    }

    fun build(): String = this.prettyPrint.toString()
}