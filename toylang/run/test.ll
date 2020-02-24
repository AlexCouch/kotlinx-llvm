; ModuleID = 'test.bc'
source_filename = "test.toy"

@test = global [6 x i8] c"Hello\00"

define i8* @testFunc(float) {
local_testFunc_block:
  ret i8* getelementptr inbounds ([6 x i8], [6 x i8]* @test, i32 0, i32 0)
}
