; ModuleID = 'test.bc'
source_filename = "test"

@testVar = global i32 5

define void @testFunc(i32, double) {
testFunc_local:
  %five = alloca i32
  store i32 %0, i32* %five
  ret void
}
