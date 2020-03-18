# Toylang
Toylang is an educationally produced language designed and implemented for the sole purpose of educating the design and implementation of languages and compilers. I've found that there's very limited material on language development and compilers out there so I hope this language serves as a good example of how compilers can be made.

Toylang is also being made to test run kotlinx-llvm. It currently compiles and runs simple programs with multiple functions.

## Specification
##### _Functions_:
```
fn [fnName]( [arg: ArgType{, otherArg: ArgType}] )[: ReturnType]{

};
```
Example:
```
fn main(argc: Int){
    
};
```
------------------------------------------------------------------------------
##### _Global Variables_:
```
let [mut] [varName] = [expression];
```
Example:
```
let name = "Alex";
```

## Checklist:

* [x] Functions
* [x] Global Variables
* [x] Expression evaluation
* [x] String literal evaluation
* [x] Local variables
* [x] Basic arrays
* [x] Function calls
* [x] Addition
* [x] Other arithmetic
* [ ] Mutable variables
* [ ] Proper String interpolation (it technically does but not properly)
* [ ] Custom types (structs)
* [ ] Vectors
* [ ] Traits
* [ ] Advanced arrays
* [ ] Module imports

## Roadmap:
* [x] Refactor compiler to have a more robust implementation with better architecture
* [x] Merge scope information generation with parser visitor
* [ ] Create different AST nodes for different passes
    * [ ] Basic Type Construction and Generation
        * This is just for the initial type analysis. This will generate complex information about things like structs and traits
    * [ ] Partial evaluation
        * This will be good for compile time evaluation and metaprogramming like macros and generics
    * [ ] Type Metadata Analysis
        * This will be for things like concrete implementations of trait members, overrides, checking if an override is allowed, or if an abstract member is not implemented
    * [ ] Backend
        * This can be anything but I want to ensure that this architecture is of a pipeline rather than spaghetti which then allows any backend to use the same pipeline
        