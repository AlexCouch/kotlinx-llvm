package com.couch.kotlinx.llvm

import com.sun.org.apache.bcel.internal.generic.ReturnInstruction
import org.bytedeco.javacpp.PointerPointer
import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM

class AdditionInstruction{
    var left: Value = NoneValue
    var right: Value = NoneValue
}

fun Builder.addAdditionInstruction(name: String, block: AdditionInstruction.()->Unit): Value{
    val additionInstruction = AdditionInstruction()
    additionInstruction.block()
    val add = LLVM.LLVMBuildAdd(this.builder, additionInstruction.left.value, additionInstruction.right.value, name)
    return object : Value{
        override val type: Type
            get() = additionInstruction.left.type
        override val value: LLVMValueRef
            get() = add

    }
}

fun Builder.buildGetElementPointer(tempVarName: String, block: ()->Value): Value{
    val value = block()
    require(value.type is Type.ArrayType){
        "Variable is not an array type"
    }
    val instrs = arrayListOf(LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 0, 0))
    val elementPtr = LLVM.LLVMBuildGEP(builder, block().value, PointerPointer(*instrs.toTypedArray()), 1, "${tempVarName}_ref")
    return createReferenceValue(object : Value{
        override val type: Type = value.type
        override val value: LLVMValueRef = elementPtr
    })
}

fun Builder.buildBitcast(source: Value, dest: Type, name: String): Value{
    val tempCast = LLVM.LLVMBuildBitCast(builder, source.value, dest.llvmType, name)
    return object : Value{
        override val type: Type = dest
        override val value: LLVMValueRef = tempCast
    }
}

fun Builder.addReturnStatement(block: Builder.()->Value?) {
    val ret = block()
    when{
        ret == null -> LLVM.LLVMBuildRetVoid(this.builder)
        ret.type is Type.VoidType -> LLVM.LLVMBuildRetVoid(this.builder)
        else -> LLVM.LLVMBuildRet(this.builder, ret.value)

    }
}