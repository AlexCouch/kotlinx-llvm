# Tokenized Bytecode
This document is the entire lexical token bytecode specification. This format applies to and only to lexical tokens. Other passes may share specifications with this bytecode but are in no way obligated to follow the same format.

## Table of Contents
* Available Tokens
    - Statements
        + Variable Declaration
            - Global
            - Local
        + Assignment
        + Function Declaration
            - Function Params
            - Function Return Type
        + Terminals
            - Return
        + Expressions
            - Literals
                - Integer
                - Decimal
                - Strings
                    - Raw String
                    - String Interpolation
            - Binary
                - Plus
                - Minus
                - Mult
                - Dev
            - Other
                - Value Reference
                - Function Call
    - Other Tokens
        + Identifier
        + Location
        + EOF
* Overall Bytecode Specifications
    - Head Bytecode
        + Start
    - Body Bytecode
        + Data
            - Data is not *a byte* but rather a sequence of bytes that sit between the *start* and *end* bytes. 
        + Location
            * Location Start
            * Location File Path
            * Location Line
                - Start
                - Stop
            * Token Location Column
                - Start
                - Stop
            * Location End
    - Tail Bytecode
        + Token End

#### Major Bytes
Major bytes are bytes that represent major constructs in the language, such variables, functions, statements, expressions, terminals, etc.

```
Let:
    Global start: 0x21
    Global end: 0x22
    Local start: 0x29
    Local end: 0x2a
fn:
    Param start: 0x51
    Param end: 0x52
    Return type start: 0x53
    Return type end: 0x54
    Body start: 0x55
    Body end: 0x56
```

#### Metadata
Metadata bytes are bytes that describe the file being parsed, such as where it starts, 

```
File Desc:
    Start: 0x11
    End: 0x12
File location:
    Start: 0x61
    End: 0x62
Target type:
    VM: 0x63
    LLVM: 0x64
Identifier: 0x15
File data:
    Start: 0x13
    Stop: 0x14
Location: 
    Start: 0x13
    End: 0x14
End of File: 0xff 
```

### Token Location Data
A token location is a data object that represents the location of a token. This includes the file path (in the future, this will be split into different kinds of input such as file input, console input, etc), the line number, and the column number (in the future there will be a column start and column end). This will be shown as bytes in the following format:

```
Location Start: 0x15
Token Location Line Number Start: 0x53
Token Location Line Number Data: ???
Token Location Line Number End: 0x54
Token Location Column Number Start: 0x55
Token Location Column Number Data: ???
Token Location Column Number End: 0x56
Token Location End: 0x16
```

### Overall Bytecode Specification
The overall bytecode specification is as follows:

```
|------------------------------------------------|
|               File Header (0xfe)               |
|------------------------------------------------|
|              File Desc Start (0x11)            |
|------------------------------------------------|
|            File Location Start(0x61)           |
|------------------------------------------------|
|                 Location Path                  |
|------------------------------------------------|
|            File Location End (0x62)            |
|------------------------------------------------|
|             Target Type Start (0x63)           |
|------------------------------------------------| 
|       VM (0x71)       |      LLVM (0x72)       |
|------------------------------------------------|
|             Target Type End (0x64)             |
|------------------------------------------------|
|              File Desc End (0x63)              |
|------------------------------------------------|
|                 *Statements*                   |
|------------------------------------------------|
|              Statement Start (0x20             |
|------------------------------------------------|
|           Global Variable Start (0x21)         |
|------------------------------------------------|
|             Assignment Start (0x25)            |
|------------------------------------------------|
|                  *Expression*                  |
|------------------------------------------------|
|             Expression Start (0x30)            |
|------------------------------------------------|
| Literals | Binary | Function Call | Reference  |
|------------------------------------------------|
|                  *Literals*                    |
|------------------------------------------------|
| Integer Literal (0x31) | Decimal Literal (0x32)|
|------------------------------------------------|
|           Stirng Literal Start (0x33)          |
|------------------------------------------------|
| Raw String Literal (0x3a) | *String Interp*    |
|------------------------------------------------|
|            String Interp Start (0x3b)          |
|------------------------------------------------|
|                  *Expression*                  |
|------------------------------------------------|
|             String Interp End (0x3c)           |
|------------------------------------------------|
|              Expression End (0x31)             |
|------------------------------------------------|
|              Assignment End (0x26)             |
|------------------------------------------------|
|           Global Variable End (0x21)           |
|------------------------------------------------|
```

### Examples
Def Token in file test.bg at line 1, column 1:
```
0xfe 
    0xa1 0x10 
    0xc0 
        0xc1 
            0x74 0x65 0x73 0x74 0x2e 0x62 0x67 
        0xc2 
        0xc3 
            0x00 0x00 0x00 0x01 
        0xc4 
        0xc5 
            0x00 0x00 0x00 0x01 
        0xc6 
    0xcf
0xff
```