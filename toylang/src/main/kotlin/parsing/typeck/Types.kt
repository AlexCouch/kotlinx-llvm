package parsing.typeck

data class Type(val identifier: String){
    companion object {
        val NONE = Type("None")
        val STRING = Type("String")
        val INTEGER = Type("Int")
        val DECIMAL = Type("Float")
    }
}
