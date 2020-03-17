package parsing

import Result
import ErrorResult
import buildPrettyString
import com.couch.kotlinx.ast.Location

data class ParserErrorResult(val error: ErrorResult, val location: Location): Result{
    override fun toString(): String {
        return buildPrettyString {
            this.appendWithNewLine("ParserErrorResult{")
            this.indent {
                this.appendWithNewLine("Location: $location")
                this.appendWithNewLine("Cause: $error")
            }
            this.append("}")
        }
    }
}