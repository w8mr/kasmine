# Architecture Overview

Kasmine generates valid JVM class files from Kotlin DSL descriptions. It handles the entire class file format: magic number, version, constant pool, access flags, interfaces, fields, methods, and the Code attribute.

## How It Works

1. **Define** a class using the `classBuilder` DSL — specify access flags, class name, superclass, fields, and methods with their bytecode instructions.
2. **Build** — the DSL collects constant pool entries (with automatic deduplication), method definitions, and instruction blocks.
3. **Write** — `ClassBuilder.write()` serializes everything into a valid `.class` file byte array, following the [JVM Class File Format](https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-4.html).
4. **Load** — on the JVM, `DynamicClassLoader.define()` loads the byte array as a live `Class<?>`.

## Package Structure

```
nl.w8mr.kasmine
├── ByteCodeWriter        (expect/actual — platform interface)
├── ClassBuilder          (DSL for class construction)
├── Types                 (constant pool types, opcodes, instructions)
├── TypeEffect            (instruction stack/pop effects for dataflow analysis)
├── VerificationType      (verification type hierarchy + Frame class)
├── StackMapGenerator     (forward dataflow analysis & StackMapTable frame generation)
├── PlatformUtils         (expect/actual — String.format and Map.merge)
├── DynamicClassLoader    (JVM only — loads generated classes)
```

## Module Layout

```
core/
├── src/
│   ├── commonMain/       — shared code (DSL, types, writer interface)
│   ├── jvmMain/          — JVM ByteCodeWriter, DynamicClassLoader
│   └── jsMain/           — JS ByteCodeWriter
```

## Key Design Decisions

- **No dependencies** — Kasmine is a zero-dependency library (only the Kotlin stdlib).
- **Constant pool is reference-counted** — entries use `MutableMap.merge` to count references, then the pool is sorted by frequency for optimal `cpMap` indices.
- **Jump offsets are recalculated** after all instructions are collected, so you can reference `InstructionBlock` targets before their byte sizes are known.
- **Instructions are data classes** — each instruction carries its own `byteSize`, enabling accurate offset computation without running the writer.
- **maxStack and maxLocals are computed from dataflow analysis** — `StackMapGenerator` simulates execution of all instructions, tracking operand stack height and local variable types across branches. No more hardcoded values.
- **StackMapTable is emitted automatically for class version ≥ 50** — required by the JVM for verification of version 51.0+ class files. The generator produces `full_frame` (tag 255) entries at every branch target.
- **Class file version is configurable** — set `version` on the DSL scope (default 51). Versions < 50 skip StackMapTable; versions ≥ 50 include it only when branching instructions are present.

## File Format Support

| Feature | Status |
|---|---|---|
| Class magic (`cafebabe`) | ✓ |
| Minor/major version | ✓ (configurable, default 51 = Java 7) |
| Constant pool | ✓ (UTF8, Integer, Class, String, NameAndType, FieldRef, MethodRef) |
| Access flags | ✓ |
| Fields | ✓ (with access, name, type) |
| Methods | ✓ (with Code attribute, computed maxStack/maxLocals) |
| StackMapTable | ✓ (full_frame at branch targets, emitted for version ≥ 50) |
| maxStack / maxLocals | ✓ (computed from forward dataflow analysis) |
| Interfaces | — (count set to 0) |
| Exceptions | — (count set to 0) |
| Bootstrap methods | — (not yet implemented) |
| Compact StackMapTable encoding | — (planned — currently full_frame only) |
| Arithmetic / comparison opcodes | — (planned) |
| invokedynamic | — (planned) |
| Exception handlers | — (planned) |
