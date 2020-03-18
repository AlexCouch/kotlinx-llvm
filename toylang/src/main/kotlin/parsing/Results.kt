package parsing

import Result
import ErrorResult
import buildPrettyString
import com.couch.kotlinx.ast.Location

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