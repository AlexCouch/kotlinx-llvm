package parsing.ast

import com.strumenta.kolasu.model.Node
import kotlinx.io.core.ByteReadPacket
import kotlinx.io.core.buildPacket
import kotlinx.serialization.Serializable
import org.antlr.v4.kotlinruntime.ast.Point
import parsing.hir.ToylangMainASTBytecode

data class Location(val start: Point, val stop: Point)
abstract class ToylangASTNode(open val location: Location): Node()