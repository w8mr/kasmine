# Types

The `Types.kt` file defines the core data types used throughout Kasmine: constant pool entries, JVM opcodes, and instruction representations.

## Constant Pool Types

Each constant pool entry extends `ConstantPoolType` and knows how to serialize itself via the `write(out, cpMap)` method.

| Type | Tag | Description |
|---|---|---|
| `UTF8String` | `01` | UTF-8 encoded string literal |
| `ConstantInteger` | `03` | 4-byte integer constant |
| `ClassEntry` | `07` | Class or interface reference (points to UTF8String) |
| `ConstantString` | `08` | String constant (points to UTF8String) |
| `NameAndType` | `0c` | Name and type descriptor pair (two UTF8String refs) |
| `FieldRef` | `09` | Field reference (ClassEntry + NameAndType) |
| `MethodRef` | `0a` | Method reference (ClassEntry + NameAndType) |

All entries are added to the pool via `ClassBuilder.addToPool()`, which uses `MutableMap.merge` to reference-count entries. During serialization, the pool is sorted by reference count (most-referenced first) to produce compact indices.

## Opcodes

The `Opcode` sealed class enumerates supported JVM opcodes. Each opcode carries its `opcode` byte and a `name`.

| Object | Value | Instruction |
|---|---|---|
| `IConstM1` | `0x02` | Push -1 |
| `IConst0`–`IConst5` | `0x03`–`0x08` | Push 0–5 |
| `BiPush` | `0x10` | Push byte |
| `SiPush` | `0x11` | Push short |
| `LoadConstant` | `0x12`/`0x13` | LDC (wide/narrow) |
| `GetStatic` | `0xb2` | Get static field |
| `GetField` | `0xb4` | Get instance field |
| `PutField` | `0xb5` | Put instance field |
| `InvokeVirtual` | `0xb6` | Invoke virtual method |
| `InvokeSpecial` | `0xb7` | Invoke special method |
| `InvokeStatic` | `0xb8` | Invoke static method |
| `New` | `0xbb` | Create new object |
| `Return` | `0xb1` | Return void |
| `IReturn` | `0xac` | Return int |
| `AReturn` | `0xb0` | Return reference |
| `AStore` | `0x3a` | Store reference in local var |
| `ALoad` | `0x19` | Load reference from local var |
| `IStore` | `0x36` | Store int in local var |
| `ILoad` | `0x15` | Load int from local var |
| `Dup` | `0x59` | Duplicate top stack word |
| `Pop` | `0x57` | Pop top stack word |
| `IfNotEqual` | `0x9a` | Branch if not equal (`ifne`) |
| `IfEqual` | `0x99` | Branch if equal (`ifeq`) |
| `Goto` | `0xa7` | Unconditional branch |

`Opcode.ByteShortOpcode` (used by `LoadConstant`) carries a second opcode byte for the narrow form (e.g., `ldc` vs `ldc_w`). Currently only the wide form is emitted.

## Instructions

The `Instruction` sealed interface hierarchy represents individual bytecode instructions:

| Type | byteSize | Description |
|---|---|---|
| `NoArgument` | 1 | Single opcode byte (e.g., `dup`, `return`) |
| `OneArgumentUByte` | 2 | Opcode + unsigned byte argument |
| `OneArgumentByte` | 2 | Opcode + signed byte argument |
| `OneArgumentShort` | 3 | Opcode + short argument (used for jumps) |
| `OneArgumentUShort` | 3 | Opcode + unsigned short argument |
| `OneArgumentPool` | 3 | Opcode + constant pool index (ushort) |
| `TwoArgumentPool` | 5 | Opcode + two constant pool indices |

Each instruction implements `write(out, cpMap)` for serialization and exposes a pre-computed `byteSize` for offset calculation.

## Instruction Blocks

`InstructionBlock` groups instructions and tracks:

- `byteSize` — total size of all instructions in the block
- `target` — optional jump target reference for offset recalculation

Actual `maxStack` and `maxLocals` values are computed by `StackMapGenerator` during the write phase via forward dataflow analysis across all blocks.

Jump instructions use `Instruction.OneArgumentShort` with a placeholder offset of `0`. After all blocks are collected, `ClassBuilder.recalculateJumps()` computes the actual offsets.

## Type Effects

Every instruction carries a `TypeEffect(pops: Int, pushes: Int)` describing its operand stack behavior, obtained via `instruction.typeEffect(resolver)`. This powers the `StackMapGenerator` dataflow analysis:

| Instruction | Pops | Pushes |
|---|---|---|
| `iconst`, `bipush`, `sipush`, `ldc` | 0 | 1 |
| `dup` | 1 | 2 |
| `pop` | 1 | 0 |
| `ifeq`, `ifne` | 1 | 0 |
| `goto` | 0 | 0 |
| `iload`, `aload` | 0 | 1 |
| `istore`, `astore` | 1 | 0 |
| `ireturn`, `areturn` | 1 | 0 |
| `return` | 0 | 0 |
| `invokestatic` | varies | varies (parsed from descriptor) |
| `getstatic` | 0 | 1 |
| `invokevirtual`, `invokespecial` | varies | varies |
| `new` | 0 | 1 |
| `getfield` | 1 | 1 |
| `putfield` | 2 | 0 |

## Verification Types

The `VerificationType` sealed hierarchy models JVM verification types used in StackMapTable frames:

| Type | Tag | Description |
|---|---|---|
| `Top` | 0 | Uninitialized / unused value |
| `Integer` | 1 | int, short, byte, boolean, char |
| `Float` | 2 | float |
| `Double` | 3 | double (occupies 2 slots) |
| `Long` | 4 | long (occupies 2 slots) |
| `Null` | 5 | null reference |
| `UninitializedThis` | 6 | uninitialized `this` |
| `Object` | 7 | Reference type (carries constant pool index) |
| `Uninitialized` | 8 | Uninitialized object (carries offset of `new` instruction) |

`Frame` wraps a list of local verification types and a stack verification type list, with `push`/`pop`/`setLocal`/`merge` operations for use during dataflow simulation.

## Helper Data Classes

```kotlin
data class FieldDef(access: UShort, name: UTF8String, type: UTF8String)
data class ClassDef(access: UShort, classRef: ClassEntry, superClassRef: ClassEntry, fields: List<FieldDef>, methods: List<MethodDef>)
data class MethodDef(access: UShort, methodName: UTF8String, methodSig: UTF8String, instructions: List<InstructionBlock>)
```

## Utility Functions

```kotlin
fun String.decodeHex(): ByteArray    // "01 02" → bytes [0x01, 0x02]
fun ByteArray.toHex(): String         // bytes [0x01, 0x02] → "0102"
```
