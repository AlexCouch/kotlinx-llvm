package com.couch.kotlinx.llvm

import org.bytedeco.javacpp.PointerPointer
import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM
import org.bytedeco.llvm.global.LLVM.LLVMConstInt
import org.bytedeco.llvm.global.LLVM.LLVMInt32Type

class BinaryArithInstruction{
    var left: Value = NoneValue
    var right: Value = NoneValue
}

fun Builder.buildAdditionInstruction(name: String, block: BinaryArithInstruction.()->Unit): Value{
    val additionInstruction = BinaryArithInstruction()
    additionInstruction.block()
    val add = LLVM.LLVMBuildAdd(this.builder, additionInstruction.left.value, additionInstruction.right.value, name)
    return object : Value{
        override val type: Type
            get() = additionInstruction.left.type
        override val value: LLVMValueRef
            get() = add

    }
}

fun Builder.buildFAdditionInstruction(name: String, block: BinaryArithInstruction.()->Unit): Value{
    val additionInstruction = BinaryArithInstruction()
    additionInstruction.block()
    val add = LLVM.LLVMBuildFAdd(this.builder, additionInstruction.left.value, additionInstruction.right.value, name)
    return object : Value{
        override val type: Type
            get() = additionInstruction.left.type
        override val value: LLVMValueRef
            get() = add

    }
}

fun Builder.buildMinusInstruction(name: String, block: BinaryArithInstruction.()->Unit): Value{
    val additionInstruction = BinaryArithInstruction()
    additionInstruction.block()
    val add = LLVM.LLVMBuildSub(this.builder, additionInstruction.left.value, additionInstruction.right.value, name)
    return object : Value{
        override val type: Type
            get() = additionInstruction.left.type
        override val value: LLVMValueRef
            get() = add

    }
}

fun Builder.buildFMinusInstruction(name: String, block: BinaryArithInstruction.()->Unit): Value{
    val additionInstruction = BinaryArithInstruction()
    additionInstruction.block()
    val add = LLVM.LLVMBuildFSub(this.builder, additionInstruction.left.value, additionInstruction.right.value, name)
    return object : Value{
        override val type: Type
            get() = additionInstruction.left.type
        override val value: LLVMValueRef
            get() = add

    }
}

fun Builder.buildMultiplyInstruction(name: String, block: BinaryArithInstruction.()->Unit): Value{
    val additionInstruction = BinaryArithInstruction()
    additionInstruction.block()
    val add = LLVM.LLVMBuildMul(this.builder, additionInstruction.left.value, additionInstruction.right.value, name)
    return object : Value{
        override val type: Type
            get() = additionInstruction.left.type
        override val value: LLVMValueRef
            get() = add

    }
}

fun Builder.buildFMultiply(name: String, block: BinaryArithInstruction.()->Unit): Value{
    val additionInstruction = BinaryArithInstruction()
    additionInstruction.block()
    val add = LLVM.LLVMBuildFMul(this.builder, additionInstruction.left.value, additionInstruction.right.value, name)
    return object : Value{
        override val type: Type
            get() = additionInstruction.left.type
        override val value: LLVMValueRef
            get() = add

    }
}

fun Builder.buildSDivide(name: String, block: BinaryArithInstruction.()->Unit): Value{
    val additionInstruction = BinaryArithInstruction()
    additionInstruction.block()
    val add = LLVM.LLVMBuildSDiv(this.builder, additionInstruction.left.value, additionInstruction.right.value, name)
    return object : Value{
        override val type: Type
            get() = additionInstruction.left.type
        override val value: LLVMValueRef
            get() = add

    }
}

fun Builder.buildUDivide(name: String, block: BinaryArithInstruction.()->Unit): Value{
    val additionInstruction = BinaryArithInstruction()
    additionInstruction.block()
    val add = LLVM.LLVMBuildUDiv(this.builder, additionInstruction.left.value, additionInstruction.right.value, name)
    return object : Value{
        override val type: Type
            get() = additionInstruction.left.type
        override val value: LLVMValueRef
            get() = add

    }
}

fun Builder.buildFDivide(name: String, block: BinaryArithInstruction.()->Unit): Value{
    val additionInstruction = BinaryArithInstruction()
    additionInstruction.block()
    val add = LLVM.LLVMBuildFDiv(this.builder, additionInstruction.left.value, additionInstruction.right.value, name)
    return object : Value{
        override val type: Type
            get() = additionInstruction.left.type
        override val value: LLVMValueRef
            get() = add

    }
}

/*
    AGGREGATE INSTRUCTIONS
 */

//inline fun Builder.buildExtractValue(tempVarName: String, block: ()->Value): Value{
//    val value = block()
//    require(value.type is Type.VectorType){
//        "Variable is not a vector type"
//    }
//    val instrs = arrayListOf(LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 0, 0))
//    val extract = LLVM.LLVMBuildExtractElement()
//}

class GEPInstructionBuilder(private val builder: Builder, private val tempVarName: String){
    private val indices = arrayListOf<Value>()
    fun index(index: Value){
        indices += index
    }

    fun build(value: Value): Value{
        val indices = indices.map{ it.value }
        val elementPtr = LLVM.LLVMBuildGEP(builder.builder, value.value, PointerPointer(*indices.toTypedArray()), 1, "${tempVarName}_ref")
        return object : Value{
            override val type: Type = value.type
            override val value: LLVMValueRef = elementPtr
        }
    }
}

fun Builder.buildGetElementPointer(tempVarName: String, value: Value, block: GEPInstructionBuilder.()->Unit): Value{
    val gepBuilder = GEPInstructionBuilder(this, tempVarName)
    gepBuilder.block()
    val thing = gepBuilder.build(value)
    return createReferenceValue(thing)
}

inline fun Builder.buildFunctionCall(name: String, function: Function, block: Builder.()->Array<Value>): Value{
    val args = block()
    val call = LLVM.LLVMBuildCall(this.builder, function.functionRef, PointerPointer(*args.map { it.value }.toTypedArray()), args.size, name)
    return object : Value{
        override val type: Type
            get() = Type.CustomType(LLVM.LLVMTypeOf(call))
        override val value: LLVMValueRef
            get() = call
    }
}

fun Builder.buildBitcast(source: Value, dest: Type, name: String): Value{
    val tempCast = LLVM.LLVMBuildBitCast(builder, source.value, dest.llvmType, name)
    return object : Value{
        override val type: Type = dest
        override val value: LLVMValueRef = tempCast
    }
}

fun Builder.buildPtrToInt(sourcePtr: Value, dest: Type, name: String): Value{
    val ptrToInt = LLVM.LLVMBuildPtrToInt(this.builder, sourcePtr.value, dest.llvmType, name)
    return object : Value{
        override val type: Type
            get() = dest
        override val value: LLVMValueRef
            get() = ptrToInt
    }
}

fun Builder.buildLoad(ptr: Value, name: String): Value{
    val loaded = LLVM.LLVMBuildLoad(this.builder, ptr.value, name)
    return object : Value{
        override val type: Type
            get() = Type.CustomType(LLVM.LLVMTypeOf(loaded))
        override val value: LLVMValueRef
            get() = loaded
    }
}

inline fun Builder.addReturnStatement(block: Builder.()->Value?) {
    val ret = block()
    when{
        ret == null -> LLVM.LLVMBuildRetVoid(this.builder)
        ret.type is Type.VoidType -> LLVM.LLVMBuildRetVoid(this.builder)
        else -> LLVM.LLVMBuildRet(this.builder, ret.value)

    }
}
