package com.couch.kotlinx.parsing.p1

import com.couch.kotlinx.parsing.Scope

sealed class P1Scope: Scope(){
    class GlobalScope: P1Scope()
    class FunctionScope: P1Scope()
}