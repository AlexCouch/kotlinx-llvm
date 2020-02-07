package com.couch.kotlinx.llvm

import com.sun.org.apache.bcel.internal.generic.ReturnInstruction
import org.bytedeco.javacpp.PointerPointer
import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM

class AdditionInstruction{
    var left: Value = NoneValue
    var right: Value = NoneValue
}

fun BasicBlock.addAdditionInstruction(name: String, block: AdditionInstruction.()->Unit){
    val additionInstruction = AdditionInstruction()
    additionInstruction.block()
    val builder = LLVM.LLVMCreateBuilder()
    LLVM.LLVMBuildAdd(builder, additionInstruction.left.value, additionInstruction.right.value, name)

}

fun Builder.buildGetElementPointer(arrayVar: Variable, block: ()->Value): Value{
    require(arrayVar.type is Type.ArrayType){
        "Variable is not an array type"
    }
    val instrs = arrayListOf(LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 0, 0))
    val elementPtr = LLVM.LLVMBuildGEP(builder, block().value, PointerPointer(*instrs.toTypedArray()), 1, "${arrayVar.name}_ref")
    return createReferenceValue(object : Value{
        override val type: Type = Type.CustomType(LLVM.LLVMTypeOf(elementPtr))
        override val value: LLVMValueRef = elementPtr
    })
}

fun Builder.buildBitcast(source: Value, dest: Type, name: String): Value{
    val tempCast = LLVM.LLVMBuildBitCast(builder, source.value, Type.PointerType(Type.Int8Type()).llvmType, name)
    return object : Value{
        override val type: Type = dest
        override val value: LLVMValueRef = tempCast
    }
}

fun Builder.addReturnStatement(block: Builder.()->Value) {
    val ret = block()
    if(ret.type is Type.VoidType){
        LLVM.LLVMBuildRetVoid(this.builder)
    }else{
        LLVM.LLVMBuildRet(this.builder, ret.value)
    }
}