# kotlinx-llvm
A kotlin multiplatform library for llvm. This library provides dsl bindings around llvm.

## Status
Currently, this only works on JVM platform, but this will become multiplatform as time goes on.
test.ll will show you the result of the current llvm dsl test.

## Feature Checklist
- [x] Modules
- [x] Global Varaibles
- [x] Local Variables
- [x] Functions
- [x] Function Parameters
- [x] Basic Blocks
- [x] Add Instructions
- [ ] Get Element Pointer Instructions
   - [x] Arrays
   - [ ] Vectors
   - [ ] Structs
- [x] Bitcast Instruction
- [x] Function Call Instruction
- [ ] Break Instruction
- [ ] Switch Instruction
- [ ] Comparisons/Conditions (If-Else-If)
- [ ] Jump instructions
- [ ] Structs
- [ ] Attributes
- [ ] Metadata
- [ ] ...

## Examples
Toylang File:
```rust
let test = "Hello";

fn testFunc(a: Float): String{
    return test;
}
```
Compiles to:
```llvm
; ModuleID = 'test.bc'
source_filename = "test.toy"

@test = global [6 x i8] c"Hello\00"

define i8* @testFunc(float) {
local_testFunc_block:
  ret i8* getelementptr inbounds ([6 x i8], [6 x i8]* @test, i32 0, i32 0)
}
```

## Contributing
If you would like to contribute you must satisfy these requirements:

* You have llvm-dis on your machine.
    * I will write python and gradle scripts that will ensure you have llvm-dis installed in time
* You have python on your machine
    * I will update the shell scripts to be a little kinder with those with different python environment variables and location
* When this project goes multiplatform, you will need llvm sources on your machine.
    * This will be handled locally

That's it! 

I will also make a list of things to be done (preferably on trello) so that, before *anything* can be done, it must be assigned to someone. 

This will ensure that nobody does two things twice, and we are all aware of who is doing what to avoid conflicts in some way. 

This also promotes communication.

Also, when this project does move to KMP, every platform should have their own branch.

## Roadmap
I am hoping that at some point, the main gradle file will be changed to be multiplatform. It shouldn't be too hard to do.
I will also make a trello, as mentioned above.
