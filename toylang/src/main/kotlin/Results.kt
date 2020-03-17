typealias OKResult = StandardResult.OKResult
typealias WrappedResult<T> = StandardResult.WrappedResult<T>
typealias ErrorResult = StandardResult.ErrorResult

interface Result

sealed class StandardResult: Result {
    object OKResult: StandardResult()
    data class WrappedResult<T>(val t: T): StandardResult()
    data class ErrorResult(val message: String, val cause: ErrorResult? = null): StandardResult() {
        override fun toString(): String {
            return buildPrettyString {
                this.appendWithNewLine("ErrorResult{")
                this.indent {
                    this.appendWithNewLine("Message: $message")
                    if (cause != null) {
                        this.appendWithNewLine("Cause: $cause")
                    }
                }
                this.append("}")
            }
        }
    }
}