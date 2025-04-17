# Kasmine Byte Code Writer Project
This project is focused on creating a byte code writer and related utilities to dynamically generate Java classes at runtime. The primary goal is to allow developers to write custom JVM bytecode instructions directly in Kotlin, enabling advanced scenarios such as runtime class generation and manipulation.
## Key Components
1. **ByteCodeWriterJvm.kt**:
    - This file contains the implementation of `ByteCodeWriter`, which provides methods for writing various types of byte code instructions.
    - The writer supports common operations like writing integers, shorts, strings, and method calls in bytecode format.
    - It also includes utility functions to convert hexadecimal strings into binary data and vice versa.

2. **DynamicClassLoader.kt**:
    - This file defines a dynamic class loader that can load classes from byte code arrays at runtime.
    - The `define` method takes a class name and its corresponding bytecode, and returns the loaded class object.

3. **Types.kt**:
    - This file contains definitions for various constant pool types used in Java bytecode, such as UTF8 strings, integers, class entries, and method references.
    - It also includes an enumeration of opcodes (instructions) that can be emitted in bytecode.

4. **ClassBuilder.kt**:
    - This file provides a DSL-like interface for constructing Java classes dynamically.
    - The `classDef` function allows you to specify the access modifiers, superclass, and methods of a class.
    - Each method can have its instructions defined using the `InstructionBlock` class.

## Usage
To use this project, follow these steps:
1. **Add Dependencies**:
    - Ensure your project includes the necessary Kotlin and Java SDK dependencies (Java 24, Kotlin 2.1).

2. **Create a Class Using ClassBuilder**:
``` kotlin
   val myClass = classBuilder {
       access = 33u // public final
       name = "MyDynamicClass"

       method {
           access = 9u // public static
           name = "main"
           signature = "()V"

           instructionBlock {
               +Opcode.IConst0
               +Opcode.Return
           }
       }
   }

   val bytecode = myClass.write()
```
1. **Load and Use the Generated Class**:
``` kotlin
   val classLoader = DynamicClassLoader(null)
   val clazz = classLoader.define("MyDynamicClass", bytecode)
   val mainMethod = clazz.getMethod("main")
   mainMethod.invoke(null)
```
This project is designed for advanced use cases where dynamic class generation and manipulation are required. It provides a flexible and powerful way to work with JVM bytecode directly from Kotlin code.
For more detailed information, please refer to the source code files provided in the project repository
