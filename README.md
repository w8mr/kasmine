# Kasmine — JVM Bytecode Writer for Kotlin

[![Maven Central](https://img.shields.io/maven-central/v/nl.w8mr.kasmine/core)](https://central.sonatype.com/artifact/nl.w8mr.kasmine/core)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.10-purple)](https://kotlinlang.org)
[![Java](https://img.shields.io/badge/Java-11%2B-orange)](https://adoptium.net)

Kasmine is a **Kotlin Multiplatform** library for writing JVM bytecode dynamically at runtime. It provides a DSL for defining classes, methods, fields, and bytecode instructions directly in Kotlin, enabling advanced use cases like runtime class generation, dynamic proxies, and bytecode manipulation.

## Features

- **DSL-based class construction** — define classes, methods, and instructions in idiomatic Kotlin
- **Automatic constant pool management** — deduplication and reference counting of constant pool entries
- **Jump target resolution** — automatic calculation of forward and backward branch offsets
- **Configurable class file version** — set any version from 45.0 (Java 1.1) to 65.0 (Java 21) via the `version` property
- **StackMapTable generation** — forward dataflow analysis computes stack map frames (full_frame) for version ≥ 50; automatically skipped for older versions
- **Computed maxStack / maxLocals** — dataflow analysis tracks operand stack and local variable usage, eliminating hardcoded values
- **Runtime class loading** — load generated classes via `DynamicClassLoader`
- **133+ unit tests** — coverage spanning common and platform-specific code, including verified class loading on JVM
- **Multiplatform** — runs on JVM and JavaScript (JS targets generate bytecode for analysis/serialization)
- **Published to Maven Central** — no custom repositories needed

## Quick Start

```kotlin
val myClass = classBuilder {
    access = 33u           // public final
    name = "MyDynamicClass"
    version = 52           // Java 8 (default: 51 — Java 7)

    method {
        access = 9u        // public static
        name = "main"
        signature = "([Ljava/lang/String;)V"

        instructionBlock {
            getstatic("java/lang/System", "out", "Ljava/io/PrintStream;")
            ldc("Hello from Kasmine!")
            invokevirtual("java/io/PrintStream", "println", "(Ljava/lang/String;)V")
            return_()
        }
    }
}

val bytecode = myClass.write()

val clazz = DynamicClassLoader(null).define("MyDynamicClass", bytecode)
val mainMethod = clazz.getMethod("main", Array<String>::class.java)
mainMethod.invoke(null, arrayOf<String>())
```

## Dependencies

Add to your `build.gradle.kts`:

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("nl.w8mr.kasmine:core")
}
```

## Roadmap

| Area | Status |
|---|---|
| Compact StackMapTable frames (SAME, APPEND, CHOP) | Planned — currently always emits full_frame (tag 255) |
| Full type tracking (Float, Double, Long) in dataflow | Planned — currently only Integer and Object references are tracked |
| Arithmetic / comparison opcodes (iadd, isub, ixor, if_icmpeq, etc.) | Planned |
| invokedynamic support | Planned |
| Exception handlers | Planned |
| Interfaces (implements) | Planned |
| Bootstrap methods | Planned |
| Instruction-level maxStack tracking on InstructionBlock | Planned — fields exist but aren't updated |
| `areturn` with null and Object tracking | Planned — currently treated as top-of-stack pop |

## Documentation

- [Overview](docs/overview.md) — architecture and key concepts
- [Class Builder DSL](docs/class-builder.md) — building classes with the DSL
- [ByteCodeWriter](docs/bytecode-writer.md) — low-level bytecode writing
- [Types](docs/types.md) — constant pool types, opcodes, and instructions
- [Platform Support](docs/platforms.md) — JVM and JS implementations

## License

MIT License — see [LICENSE](https://opensource.org/license/mit).
