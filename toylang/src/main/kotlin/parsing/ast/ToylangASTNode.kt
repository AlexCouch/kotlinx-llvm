package com.couch.kotlinx.ast

import com.couch.kotlinx.parsing.Scope
import com.strumenta.kolasu.model.Node
import org.antlr.v4.kotlinruntime.ast.Point

data class Location(val line: Point, val column: Point)
abstract class ToylangASTNode(open val location: Location): Node()