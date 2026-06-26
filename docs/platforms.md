# Platform Support

Kasmine uses Kotlin Multiplatform's `expect`/`actual` mechanism to provide platform-specific implementations.

## JVM

| File | Description |
|---|---|
| `ByteCodeWriterJvm.kt` | `ByteArrayOutputStream`-backed writer |
| `DynamicClassLoader.kt` | Runtime class loader (extends `java.lang.ClassLoader`) |
| `PlatformUtilsJvm.kt` | Delegates `String.format` to `java.lang.String.format` |

The JVM target requires **Java 11+** (toolchain configured in `build.gradle.kts`), with bytecode targeting **Java 21** for the K2 compiler.

### DynamicClassLoader

```kotlin
val loader = DynamicClassLoader(parentClassLoader)
val clazz: Class<*> = loader.define("com.example.MyClass", bytecode)
```

Wraps `ClassLoader.defineClass()` — the standard JVM mechanism for loading classes from byte arrays.

## JavaScript

| File | Description |
|---|---|
| `ByteCodeWriterJs.kt` | `MutableList<Byte>`-backed writer |
| `PlatformUtilsJS.kt` | Custom `String.format` implementation with positional argument replacement |

Targets both browser and Node.js environments. The JS implementation is useful for:

- Testing bytecode generation logic in JS test suites
- Analyzing or serializing class files in browser environments
- Server-side bytecode generation on Node.js

### Notable Differences from JVM

- Uses `require` instead of `assert` for parameter validation
- `String.format` is implemented manually (supports `%s`, `%d`, `%f`, `%02x` patterns)
- No `DynamicClassLoader` — class loading is only available on JVM

## Common Code

All DSL logic, type definitions, opcode enums, and the class file serialization algorithm in `ClassBuilder` live in `commonMain` and are shared across both platforms.

## Adding a New Platform

To add support for a new Kotlin/Native target (e.g., Apple Silicon, Linux):

1. Add the target in `core/build.gradle.kts` under the `kotlin` block
2. Create a new source set directory (e.g., `nativeMain`)
3. Provide `actual class ByteCodeWriter` using the native platform's byte buffer APIs
4. Provide `actual fun String.format` and `actual fun MutableMap.merge` implementations
