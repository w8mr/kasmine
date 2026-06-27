# Kasmine Codebase Analysis

## 1. Current Instruction Opcodes Supported

The `Opcode` sealed class defines 23 opcodes across 7 instruction forms.

### Opcodes and DSL Methods

| Opcode | Byte | Mnemonic | DSL Method |
|--------|------|----------|------------|
| `IConstM1` | 0x02 | `iconst_m1` | (private) |
| `IConst0` | 0x03 | `iconst_0` | (private) |
| `IConst1` | 0x04 | `iconst_1` | (private) |
| `IConst2` | 0x05 | `iconst_2` | (private) |
| `IConst3` | 0x06 | `iconst_3` | (private) |
| `IConst4` | 0x07 | `iconst_4` | (private) |
| `IConst5` | 0x08 | `iconst_5` | (private) |
| `BiPush` | 0x10 | `bipush` | (private) |
| `SiPush` | 0x11 | `sipush` | (private) |
| `LoadConstant` | 0x12/0x13 | `ldc`/`ldc_w` | `loadConstant(...)` |
| `GetStatic` | 0xb2 | `getstatic` | `getStatic(...)` |
| `GetField` | 0xb4 | `getfield` | `getField(...)` |
| `PutField` | 0xb5 | `putfield` | `putField(...)` |
| `InvokeVirtual` | 0xb6 | `invokevirtual` | `invokeVirtual(...)` |
| `InvokeSpecial` | 0xb7 | `invokespecial` | `invokeSpecial(...)` |
| `InvokeStatic` | 0xb8 | `invokestatic` | `invokeStatic(...)` |
| `New` | 0xbb | `new` | `` `new`(...) `` |
| `Return` | 0xb1 | `return` | `` `return`() `` |
| `IReturn` | 0xac | `ireturn` | `ireturn()` |
| `AReturn` | 0xb0 | `areturn` | `areturn()` |
| `AStore` | 0x3a | `astore` | `astore(name)` |
| `ALoad` | 0x19 | `aload` | `aload(name)` |
| `IStore` | 0x36 | `istore` | `istore(name)` |
| `ILoad` | 0x15 | `iload` | `iload(name)` |
| `Dup` | 0x59 | `dup` | `dup()` |
| `Pop` | 0x57 | `pop` | `pop()` |
| `IfNotEqual` | 0x9a | `ifne` | `ifnotequal(...)` |
| `IfEqual` | 0x99 | `ifeq` | `ifequal(...)` |
| `Goto` | 0xa7 | `goto` | `goto(...)` |

### Instruction Form Variants

- `Instruction.NoArgument` — 1 byte
- `Instruction.OneArgumentUByte` — 2 bytes (iload, istore, aload, astore)
- `Instruction.OneArgumentByte` — 2 bytes (bipush)
- `Instruction.OneArgumentShort` — 3 bytes (sipush, goto, ifeq, ifne)
- `Instruction.OneArgumentUShort` — 3 bytes (defined but unused)
- `Instruction.OneArgumentPool` — 3 bytes (getstatic, getfield, putfield, invokevirtual, invokespecial, invokestatic, new, ldc)
- `Instruction.TwoArgumentPool` — 5 bytes (defined but unused)

### Public DSL Methods on `MethodDSL.DSL`

`loadConstant(Int)`, `loadConstant(String)`, `loadConstant(Char)`, `getStatic(...)`, `getField(...)`, `putField(...)`, `invokeVirtual(...)`, `invokeSpecial(...)`, `invokeStatic(...)`, `` `new`(...) ``, `` `return`() ``, `ireturn()`, `areturn()`, `dup()`, `pop()`, `astore(name)`, `aload(name)`, `istore(name)`, `iload(name)`, `ifequal(target)`, `ifnotequal(target)`, `goto(target)`, `ifequal(Short)`, `ifnotequal(Short)`, `goto(Short)`, `label()`, `block(...)`, `parameter(name)`.

---

## 2. What Is Missing vs. Standard JVM Instruction Set

Out of ~258 JVM opcodes, Kasmine supports 23. Major categories missing:

### Arithmetic (30+ opcodes)
- Integer: `iadd`, `isub`, `imul`, `idiv`, `irem`, `ineg`, `ishl`, `ishr`, `iushr`, `iand`, `ior`, `ixor`, `iinc`
- Long: `ladd`, `lsub`, `lmul`, `ldiv`, `lrem`, `lneg`, `lshl`, `lshr`, `lushr`, `land`, `lor`, `lxor`
- Float: `fadd`, `fsub`, `fmul`, `fdiv`, `frem`, `fneg`
- Double: `dadd`, `dsub`, `dmul`, `ddiv`, `drem`, `dneg`

### Comparison (10+ opcodes)
- `lcmp`, `fcmpl`, `fcmpg`, `dcmpl`, `dcmpg`
- `if_icmpeq`, `if_icmpne`, `if_icmplt`, `if_icmpge`, `if_icmpgt`, `if_icmple`
- `if_acmpeq`, `if_acmpne`, `ifnull`, `ifnonnull`

### Type Conversion (15 opcodes)
- `i2l`, `i2f`, `i2d`, `l2i`, `l2f`, `l2d`, `f2i`, `f2l`, `f2d`, `d2i`, `d2l`, `d2f`, `i2b`, `i2c`, `i2s`

### Load/Store for Other Types
- `lload`/`lstore`, `fload`/`fstore`, `dload`/`dstore`
- Compact forms: `iload_0`–`iload_3`, `istore_0`–`istore_3`, etc.
- `wide` prefix (extended local variable indices)

### Array Operations (20 opcodes)
- `newarray`, `anewarray`, `multianewarray`
- `arraylength`
- `iaload`/`iastore`, `laload`/`lastore`, `faload`/`fastore`, `daload`/`dastore`
- `aaload`/`aastore`, `baload`/`bastore`, `caload`/`castore`, `saload`/`sastore`

### Object Operations
- `instanceof`, `checkcast`
- `monitorenter`/`monitorexit`
- `athrow`

### Method Invocation
- `invokeinterface`, `invokedynamic`

### Return Types
- `lreturn`, `freturn`, `dreturn`

### Stack Manipulation
- `pop2`, `dup_x1`, `dup_x2`, `dup2`, `dup2_x1`, `dup2_x2`, `swap`

### Constant Loading
- `aconst_null`, `lconst_0`/`lconst_1`, `fconst_0`–`fconst_2`, `dconst_0`/`dconst_1`
- `ldc2_w` (long/double constant)

### LoadConstant Optimization
- The `ByteShortOpcode` mechanism exists but the narrow form (`ldc`) is commented out — always emits the wide form `ldc_w`.

---

## 3. TODO Comments in the Codebase

All 5 TODOs are in `Types.kt`, all related to the `wide` prefix instruction:

1. **`Instruction.OneArgumentUByte.write()`** — `// TODO wide`
2. **`Instruction.OneArgumentByte.write()`** — `// TODO wide`
3. **`Instruction.OneArgumentShort.write()`** — `// TODO wide`
4. **`Instruction.OneArgumentUShort.write()`** — `// TODO wide`
5. **`Instruction.OneArgumentPool.write()`** — `// TODO check how to deal with dynamic sizes?`

No `FIXME`, `HACK`, or `XXX` comments exist.

---

## 4. Current Type System Limitations

### Supported Verification Types (all 9 defined)
- `Top` (tag 0), `Integer` (tag 1), `Float` (tag 2), `Double` (tag 3), `Long` (tag 4)
- `Null` (tag 5), `UninitializedThis` (tag 6), `Object(className)` (tag 7), `Uninitialized(offset)` (tag 8)

### Dataflow Gaps
- **Float, Double, Long** — defined and parsed from descriptors, but the dataflow analysis in `StackMapGenerator.simulate()` only tracks `ILoad`/`IStore`/`ALoad`/`AStore`. No `F`/`D`/`L` load/store opcodes are handled, so tracking degenerates to `Top` for these types.
- **`slots()`** correctly returns 2 for `Long`/`Double`, and `push()` inserts extra `Top` for 2-slot types, but without load/store opcodes for those types the simulation can't transfer values properly between stack and locals.
- **`Frame.merge()`** handles Object, Null, and degenerate cases, but doesn't handle `Uninitialized` merging.

---

## 5. Test Coverage Gaps

Total: 136 tests across 8 files.

| File | Tests | Type |
|------|-------|------|
| `ClassBuilderTest.kt` | 20 | JVM integration |
| `StackMapGeneratorTest.kt` | 16 | JVM unit |
| `ByteCodeWriterJvmTest.kt` | 18 | JVM unit |
| `FrameTest.kt` | 14 | Common unit |
| `TypeEffectTest.kt` | 25 | Common unit |
| `VerificationTypeTest.kt` | 27 | Common unit |
| `TypesUtilTest.kt` | 12 | Common unit |
| `PlatformUtilsTest.kt` | 4 | Common unit |

### No Tests For
- **Instance methods** — all tested methods are `static`; no constructor `<init>` invocation
- **`new` instruction** — object creation, `dup` + `invokespecial <init>` pattern
- **`getfield` / `putfield`** — instance field access (only `getStatic` is tested)
- **`invokeSpecial`** — only `invokeVirtual` and `invokeStatic` are tested
- **String constants** via `ldc` — no test loads a string constant and verifies it
- **Exception handlers** — try/catch/finally
- **Method parameters** — `invokeStatic`/`invokeVirtual` with parameters
- **`lreturn`, `freturn`, `dreturn`** — not implemented
- **Array operations** — none implemented
- **Arithmetic** — none implemented
- **`TwoArgumentPool` / `OneArgumentUShort`** — defined but never used
- **Non-zero field access flags** — beyond `private`
- **Non-default superclass** — all tests extend `java/lang/Object`
- **Merge of `Uninitialized` types** — no `FrameTest` coverage

---

## 6. StackMapGenerator Limitations

1. **Always emits `full_frame` (tag 255)** — never uses compact forms (`SAME`, `APPEND`, `CHOP`, etc.)
2. **No Float/Double/Long tracking** — `simulate()` only handles `IStore`, `AStore`, `ILoad`, `ALoad`
3. **`areturn` always pops `Top`** — actual object type on stack is discarded
4. **No frame consistency verification** — generator computes maps but doesn't validate correctness
5. **`computeEffect()` vs `typeEffect()` duplication** — two places computing type effects that could diverge
6. **Dead code not handled** — unreachable targets get no frame, but code is still emitted
7. **No subroutine (jsr/ret) support** — not that anyone wants it

---

## 7. Status of Key Opcodes

| Opcode | Implemented | Tested |
|--------|-------------|--------|
| `invokevirtual` | Yes | Yes |
| `invokespecial` | Yes | **No** |
| `invokestatic` | Yes | Yes |
| `getstatic` | Yes | Yes |
| `getfield` | Yes | **No** |
| `putfield` | Yes | **No** |
| `new` | Yes | **No** |

All 7 are fully implemented. Four have zero test coverage.

---

## 8. Missing Class File Features

### Completely Missing
- **Interfaces** — `interfaces_count` hardcoded to 0
- **Inner Classes**, **Enclosing Method** — not emitted
- **Annotations** — runtime-visible/invisible on class/method/field
- **Generic Signatures** — `Signature` attribute not written (though `signature` field exists on `MethodDef`)
- **Bootstrap Methods** — no `invokedynamic` support
- **Source File / Debug** — `SourceFile`, `LineNumberTable`, `LocalVariableTable`
- **Module Info** — Java 9+ `Module` attribute
- **Nest Members** — Java 11+ nesting
- **Record**, **Sealed Classes** — newer features
- **Synthetic / Deprecated** attributes

### Constant Pool Types Missing
- `CONSTANT_Float` (04), `CONSTANT_Long` (05), `CONSTANT_Double` (06)
- `CONSTANT_MethodHandle` (15), `CONSTANT_MethodType` (16)
- `CONSTANT_Dynamic` (17), `CONSTANT_InvokeDynamic` (18)
- `CONSTANT_Module` (19), `CONSTANT_Package` (20)

### Partially Supported
- **Fields**: basic definitions work, but no field-level attributes
- **Methods**: `Code` attribute works, but no `LineNumberTable`, `LocalVariableTable`, `ExceptionTable`
- **Exception handlers**: hardcoded to 0

---

## 9. Build System and Publishing

### Current Setup
- Gradle with Kotlin Multiplatform plugin 2.1.10
- Targets: JVM (Java 21 toolchain) and JS (browser + nodejs)
- Test: JUnit 5 (JVM), `kotlin.test` (common/JS)
- Publishing: `com.vanniktech.maven.publish` 0.31.0 → Maven Central
- Coordinates: `nl.w8mr.kasmine:core:0.0.5`
- License: MIT

### Improvements Needed
1. **No CI/CD** — `.github/workflows/` is empty
2. **Single module** — could split `kasmine-core` from `kasmine-dsl`
3. **Java 21 toolchain** — requires JDK 21 to build, though library targets Java 7+
4. **No gradle.properties tuning** — missing `org.gradle.jvmargs`
5. **No Kotlin/Native targets** — macOS, iOS, Linux not configured
6. **No dependency locking** — `gradle.lockfile` absent

---

## 10. Documentation Gaps

### Existing Docs
- `README.md` — overview, quick start, roadmap (good)
- `docs/overview.md` — architecture, design decisions (good)
- `docs/class-builder.md` — DSL reference (good, but references non-existent methods)
- `docs/types.md` — types, opcodes, verification types (good)
- `docs/bytecode-writer.md` — low-level writer (good)
- `docs/platforms.md` — platform details (good)
- `docs/migration-0.1.0.md` — API migration guide (good)

### Gaps
1. **`Opcodes.md` incomplete** — only covers Kasmine's subset
2. **No tutorial** — step-by-step walkthrough
3. **No API reference** — `JavadocJar.Empty()` means no javadoc published
4. **No contribution guide** — no `CONTRIBUTING.md`
5. **No changelog** — no `CHANGELOG.md`
6. **No examples directory** — standalone example projects
7. **`docs/class-builder.md`** references `iconst1()` and `iadd()` that don't exist in current API
8. **No in-code KDoc** — minimal comments in source files
9. **No spec compliance docs** — no reference to target JVM spec edition
