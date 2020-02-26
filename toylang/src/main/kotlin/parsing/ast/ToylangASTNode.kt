package com.couch.kotlinx.ast

import com.couch.kotlinx.parsing.Scope
import com.strumenta.kolasu.model.Node



interface ScopeProvider{
    var scope: Scope
}

abstract class ToylangASTNode: Node()