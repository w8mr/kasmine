package nl.w8mr.kasmine

actual class ByteCodeWriter {
    private val buffer = mutableListOf<Byte>()

    actual operator fun String.unaryPlus() {
        buffer.addAll(this.decodeHex().toList())
    }

    actual fun write(bytes: ByteArray) {
        buffer.addAll(bytes.toList())
    }

    actual fun byte(value: Byte) {
        buffer.add(value)
    }

    actual fun ubyte(value: UByte) {
        buffer.add(value.toByte())
    }

    actual fun ushort(value: Int) {
        // JS doesn't have assert, so we'll use require instead
        require(value <= UShort.MAX_VALUE.toInt()) { "Value exceeds UShort.MAX_VALUE" }
        ushort(value.toUShort())
    }

    actual fun ushort(value: UShort) {
        buffer.add((value.toInt() shr 8).toByte())
        buffer.add(value.toByte())
    }

    actual fun short(value: Short) {
        if (value < 0) {
            buffer.add(((65536 + value.toInt()) shr 8).toByte())
            buffer.add(((65536 + value.toInt()) and 255).toByte())
        } else {
            buffer.add((value.toInt() shr 8).toByte())
            buffer.add(value.toByte())
        }
    }

    actual fun uint(value: UInt) {
        buffer.add((value shr 24).toByte())
        buffer.add((value shr 16).toByte())
        buffer.add((value shr 8).toByte())
        buffer.add((value shr 0).toByte())
    }

    actual fun instructionOneArgument(
        opcode: String,
        value: Int,
    ) {
        +opcode
        ushort(value)
    }

    actual fun instructionTwoArgument(
        opcode: String,
        value1: Int,
        value2: Int,
    ) {
        +opcode
        ushort(value1)
        ushort(value2)
    }

    actual fun toByteArray(): ByteArray = buffer.toByteArray()
}