# ByteCodeWriter

`ByteCodeWriter` is the low-level interface for writing raw bytes that make up a JVM class file. It is declared as an `expect class` in `commonMain` with platform-specific `actual` implementations.

## Interface

```kotlin
expect class ByteCodeWriter() {
    operator fun String.unaryPlus()       // write hex string as bytes
    fun write(bytes: ByteArray)           // write raw byte array
    fun byte(value: Byte)                 // write single signed byte
    fun ubyte(value: UByte)               // write single unsigned byte
    fun ushort(value: Int)                // write 2 bytes (value must fit in UShort range)
    fun ushort(value: UShort)             // write 2 bytes
    fun short(value: Short)               // write 2 bytes, big-endian
    fun uint(value: UInt)                 // write 4 bytes, big-endian
    fun instructionOneArgument(opcode: String, value: Int)    // hex opcode + ushort
    fun instructionTwoArgument(opcode: String, value1: Int, value2: Int) // hex opcode + 2×ushort
    fun toByteArray(): ByteArray          // get all written bytes
}
```

## Hex String Syntax

The `+` unary operator on a `String` interprets the string as hexadecimal and writes the decoded bytes:

```kotlin
with(writer) {
    +"cafebabe"     // writes the 4-byte class magic
    +"01"           // writes a single byte 0x01 (CONSTANT_Utf8 tag)
}
```

## Big-Endian Encoding

All multi-byte values are written in **big-endian** (network) byte order, as required by the JVM class file format:

- `ushort(0x1234)` → bytes `[0x12, 0x34]`
- `uint(0x12345678)` → bytes `[0x12, 0x34, 0x56, 0x78]`

## Platform Implementations

### JVM (`ByteCodeWriterJvm.kt`)

Backed by `java.io.ByteArrayOutputStream`. Uses `assert` for UShort range validation.

### JavaScript (`ByteCodeWriterJs.kt`)

Backed by a `MutableList<Byte>`. Uses `require` instead of `assert` for validation. Useful for testing bytecode generation logic in JS environments or analyzing generated classes without a JVM.
