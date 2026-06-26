# Class Builder DSL

The `ClassBuilder` provides a Kotlin DSL for constructing JVM classes programmatically.

## Entry Point

```kotlin
val result = classBuilder {
    // DSL scope
}
```

Returns a `ClassBuilder` instance. Call `.write()` on it to produce the byte array.

## DSL Reference

### `classDef` block

```kotlin
classBuilder {
    access = 33u     // ACC_PUBLIC | ACC_FINAL (default: 33u)
    name = "MyClass" // internal class name (required)
    superClass = "java/lang/Object" // default: java/lang/Object
    version = 52     // class file version (default: 51, range: 45–65)

    field(access = 2u, name = "count", type = "I")          // private int count
    field(name = "name", type = "Ljava/lang/String;")       // default access (2u = private)

    method {
        // method definition (see below)
    }
}
```

### Version control

Set the `version` property to control which JVM class file format is emitted:

```kotlin
classBuilder {
    version = 49  // Java 5 — no StackMapTable
    version = 50  // Java 6 — stack map frames optional (StackMapTable still emitted)
    version = 51  // Java 7 — StackMapTable required for methods with branches (default)
    version = 52  // Java 8
    version = 65  // Java 21
}
```

- **version ≤ 49**: StackMapTable is never emitted; class files pass verification on older JVMs
- **version ≥ 50**: StackMapTable is emitted when any method contains branching instructions (ifeq, ifne, goto)
- maxStack and maxLocals are always computed from dataflow analysis regardless of version

### `method` block

```kotlin
method {
    access = 9u        // ACC_PUBLIC | ACC_STATIC (default: 9u)
    name = "main"      // method name (required)
    signature = "()V"  // JVM descriptor (required)

    parameter("args")   // optional — declares a local variable slot

    // bytecode instructions go here
}
```

### Grouping blocks

Use `block { }` to group instructions. It returns a `BlockRef` pointing to that block's start:

```kotlin
val init = block {
    loadConstant(0)
    istore("x")
}
```

`instructionBlock { }` is also available and works identically. Both are optional — you can write instructions directly in the method scope.

## Control Flow

### Labels / Jump Targets

Use `label()` to create a named target, then fill it with `labelName { }`:

```kotlin
val end = label()

loadConstant(0)
istore("x")
iload("x")
ifequal(end)
loadConstant(-1)
ireturn()

end {
    loadConstant(42)
    ireturn()
}
```

`block { }` groups instructions and returns a `BlockRef`, so you can assign a label in one step:

```kotlin
val loop = block {
    iload("x")
    // ...
    goto(loop)  // backward jump to self
}
```

Inside any block, `self` is a `BlockRef` pointing to the current block. Useful for loops:

```kotlin
val loop = block {
    iload("x")
    iconst1()
    iadd()
    istore("x")
    iload("x")
    ifnotequal { self }  // jump back to loop head
}
```

Branch instructions accept a `BlockRef` directly or via a lambda:

```kotlin
// Direct reference
ifequal(end)
goto(loop)

// Lambda (lazy — useful for forward references)
ifequal { end }
goto { loop }
```

| DSL Function | JVM Instruction | Accepts |
|---|---|---|
| `ifequal(target)` | `ifeq` | `InstructionBlock`, `BlockRef`, `() -> BlockRef` |
| `ifnotequal(target)` | `ifne` | `InstructionBlock`, `BlockRef`, `() -> BlockRef` |
| `goto(target)` | `goto` | `InstructionBlock`, `BlockRef`, `() -> BlockRef` |

Direct offset overloads are also available (`ifequal(offset: Short)`, etc.).

The old `createTarget()` / `insertInstructionBlock()` / `goto(InstructionBlock)` API still works and is fully compatible.

## Instruction Methods

### Stack operations

| DSL | JVM |
|---|---|
| `iconst(i: Int)` | `iconst_m1` through `iconst_5`, `bipush`, `sipush`, `ldc` (automatic) |
| `loadConstant(i: Int)` | optimized short-form when possible |
| `loadConstant(s: String)` | `ldc` (String) |
| `loadConstant(c: Char)` | `ldc` (Integer with char code) |
| `dup()` | `dup` |
| `pop()` | `pop` |

### Local variables

| DSL | JVM |
|---|---|
| `iload(name)` | `iload` (with auto-assigned slot) |
| `istore(name)` | `istore` |
| `aload(name)` | `aload` |
| `astore(name)` | `astore` |

### Method invocation

| DSL | JVM |
|---|---|
| `invokeVirtual(className, methodName, type)` | `invokevirtual` |
| `invokeSpecial(className, methodName, type)` | `invokespecial` |
| `invokeStatic(className, methodName, type)` | `invokestatic` |

### Field access

| DSL | JVM |
|---|---|
| `getStatic(className, fieldName, type)` | `getstatic` |
| `getField(className, fieldName, type)` | `getfield` |
| `putField(className, fieldName, type)` | `putfield` |

### Object operations

| DSL | JVM |
|---|---|
| `` `new`(className) `` | `new` |
| `` `return`() `` | `return` (void) |
| `ireturn()` | `ireturn` |
| `areturn()` | `areturn` |

### References to the constant pool

Use `fieldRef(className, fieldName, type)` and `methodRef(className, methodName, type)` to obtain pool references for use with `getStatic` / `invokeVirtual` etc.
