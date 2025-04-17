package nl.w8mr.kasmine

import java.io.ByteArrayOutputStream

actual class ByteCodeWriter {
    private val out = ByteArrayOutputStream()

    actual operator fun String.unaryPlus() {
        out.writeBytes(this.decodeHex())
    }

    actual fun write(bytes: ByteArray) {
        out.write(bytes)
    }

    actual fun byte(value: Byte) {
        out.write(value.toInt())
    }

    actual fun ubyte(value: UByte) {
        out.write(value.toInt())
    }

    actual fun ushort(value: Int) {
        assert(value <= UShort.MAX_VALUE.toInt())
        ushort(value.toUShort())
    }

    actual fun ushort(value: UShort) {
        out.write(value.toInt() shr 8)
        out.write(value.toInt())
    }

    actual fun short(value: Short) {
        if (value < 0) {
            out.write((65536 + value.toInt()) shr 8)
            out.write((65536 + value.toInt()) and 255)
        } else {
            out.write(value.toInt() shr 8)
            out.write(value.toInt())
        }
    }

    actual fun uint(value: UInt) {
        out.write((value shr 24).toInt())
        out.write((value shr 16).toInt())
        out.write((value shr 8).toInt())
        out.write((value shr 0).toInt())
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

    actual fun toByteArray() = out.toByteArray()
}