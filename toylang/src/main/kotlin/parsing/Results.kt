package parsing

import Result
import buildPrettyString
import parsing.ast.Location

data class ParserErrorResult<T: Result>(val cause: T, val location: Location): Result{
    override fun toString(): String {
        return buildPrettyString {
            this.appendWithNewLine("ParserErrorResult{")
            this.indent {
                this.appendWithNewLine("Location: $location")
                this.appendWithNewLine("Cause: $cause")
            }
            this.append("}")
        }
    }
}