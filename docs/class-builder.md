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

## Control Flow

### Labels / Jump Targets

There are two ways to create a label reference:

#### `label()` function

```kotlin
val end = label()

loadConstant(0)
istore("x")
iload("x")
ifequal(end)       // forward reference — end block hasn't been created yet
loadConstant(-1)
ireturn()

end {
    loadConstant(42)
    ireturn()
}
```

#### `by label` delegate

```kotlin
val end by label

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

Both forms are equivalent. `label()` returns a `BlockRef` directly; `by label` uses a Kotlin property delegate.

Pass a `BlockRef` directly to branch instructions, or use a lambda for lazy resolution:

```kotlin
// Direct reference
ifequal(end)
goto(loop)

// Lambda (lazy — useful for forward references)
ifequal { end }
goto { loop }
```

For backward jumps, the target block is already defined, so both forms work:

```kotlin
val loop = label()

loadConstant(0)
istore("x")
goto(loop)

loop {
    iload("x")
    ifnotequal(end)
    loadConstant(1)
    istore("x")
    goto(loop)       // backward jump — loop is already bound
}

val end = label()
loadConstant(5)
ireturn()
```

| DSL Function | JVM Instruction | Accepts |
|---|---|---|
| `ifequal(target)` | `ifeq` | `BlockRef`, `() -> BlockRef` |
| `ifnotequal(target)` | `ifne` | `BlockRef`, `() -> BlockRef` |
| `goto(target)` | `goto` | `BlockRef`, `() -> BlockRef` |

Direct offset overloads are also available (`ifequal(offset: Short)`, etc.).

## Instruction Methods

### Stack operations

| DSL | JVM |
|---|---|
| `loadConstant(i: Int)` | `iconst_m1`–`iconst_5`, `bipush`, `sipush`, `ldc` (automatic) |
| `loadConstant(f: Float)` | `fconst_0`–`fconst_2`, `ldc` (automatic) |
| `loadConstant(l: Long)` | `lconst_0`–`lconst_1`, `ldc2_w` (automatic) |
| `loadConstant(d: Double)` | `dconst_0`–`dconst_1`, `ldc2_w` (automatic) |
| `loadConstant(s: String)` | `ldc` (String) |
| `loadConstant(c: Char)` | `ldc` (Integer with char code) |
| `dup()` | `dup` |
| `pop()` | `pop` |

### Local variables

| DSL | JVM |
|---|---|
| `iload(name)` / `istore(name)` | `iload` / `istore` (int) |
| `lload(name)` / `lstore(name)` | `lload` / `lstore` (long, 2 slots) |
| `fload(name)` / `fstore(name)` | `fload` / `fstore` (float) |
| `dload(name)` / `dstore(name)` | `dload` / `dstore` (double, 2 slots) |
| `aload(name)` / `astore(name)` | `aload` / `astore` (object ref) |

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

### Return instructions

| DSL | JVM |
|---|---|
| `` `return`() `` | `return` (void) |
| `ireturn()` | `ireturn` (int) |
| `lreturn()` | `lreturn` (long) |
| `freturn()` | `freturn` (float) |
| `dreturn()` | `dreturn` (double) |
| `areturn()` | `areturn` (object ref) |

### Object operations

| DSL | JVM |
|---|---|
| `` `new`(className) `` | `new` |

### References to the constant pool

Use `fieldRef(className, fieldName, type)` and `methodRef(className, methodName, type)` to obtain pool references for use with `getStatic` / `invokeVirtual` etc.
