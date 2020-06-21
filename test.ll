; ModuleID = 'test'
source_filename = "test"
target triple = "x86_64-pc-windows-msvc"

%testStruct = type { i32, i8 }

@testVar = global i32 10
@dPrint = global [4 x i8] c"%d\0A\00"

declare i32 @printf(i8*, ...)

define i32 @add(i32, i32 %y) {
entry:
  %add_res = add i32 %0, %y
  ret i32 %add_res
}

define i32 @calcAverage(i32, i32, i32 %z) {
entry:
  %rightAddVar = add i32 %1, %z
  %addVar = add i32 %0, %rightAddVar
  %addResult = alloca i32
  store i32 %addVar, i32* %addResult
  %addLoad = load i32, i32* %addResult
  %divRes = sdiv i32 %addLoad, 3
  ret i32 %divRes
}

define i32 @main() {
test_block_1:
  %addFuncCall = call i32 @add(i32 10, i32 10)
  %addCallRes = alloca i32
  store i32 %addFuncCall, i32* %addCallRes
  %calcAvFuncCall = call i32 @calcAverage(i32 10, i32 8, i32 10)
  %calcAvCallRes = alloca i32
  store i32 %calcAvFuncCall, i32* %calcAvCallRes
  %0 = load i32, i32* %addCallRes
  %call = call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([4 x i8], [4 x i8]* @dPrint, i32 0, i32 0), i32 %0)
  %1 = load i32, i32* %calcAvCallRes
  %call1 = call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([4 x i8], [4 x i8]* @dPrint, i32 0, i32 0), i32 %1)
  %numArray = alloca [4 x i32]
  store [4 x i32] [i32 5, i32 10, i32 15, i32 20], [4 x i32]* %numArray
  %numArray_ptr_ref = getelementptr [4 x i32], [4 x i32]* %numArray, i32 0, i32 2
  %index1 = load i32, i32* %numArray_ptr_ref
  %numPrint1 = call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([4 x i8], [4 x i8]* @dPrint, i32 0, i32 0), i32 %index1)
  %testStructLocal = alloca %testStruct
  store %testStruct { i32 15, i8 10 }, %testStruct* %testStructLocal
  %testStructIndex1 = getelementptr inbounds %testStruct, %testStruct* %testStructLocal, i32 0, i32 1
  %structIndex1 = load i8, i8* %testStructIndex1
  %structFieldPrint1 = call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([4 x i8], [4 x i8]* @dPrint, i32 0, i32 0), i8 %structIndex1)
  ret i32 0
}
