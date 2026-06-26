package nl.w8mr.kasmine

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class ByteCodeWriterJvmTest {

    @Test fun `write bytes and toByteArray round trip`() {
        val w = ByteCodeWriter()
        w.write(byteArrayOf(0x01, 0x02, 0x03))
        assertArrayEquals(byteArrayOf(0x01, 0x02, 0x03), w.toByteArray())
    }

    @Test fun `ushort writes big endian`() {
        val w = ByteCodeWriter()
        w.ushort(0x1234)
        assertArrayEquals(byteArrayOf(0x12, 0x34), w.toByteArray())
    }

    @Test fun `ushort with UShort writes big endian`() {
        val w = ByteCodeWriter()
        w.ushort(0x1234u)
        assertArrayEquals(byteArrayOf(0x12, 0x34), w.toByteArray())
    }

    @Test fun `ushort zero`() {
        val w = ByteCodeWriter()
        w.ushort(0)
        assertArrayEquals(byteArrayOf(0x00, 0x00), w.toByteArray())
    }

    @Test fun `ushort max`() {
        val w = ByteCodeWriter()
        w.ushort(0xFFFF)
        assertArrayEquals(byteArrayOf(0xFF.toByte(), 0xFF.toByte()), w.toByteArray())
    }

    @Test fun `short positive`() {
        val w = ByteCodeWriter()
        w.short(0x1234.toShort())
        assertArrayEquals(byteArrayOf(0x12, 0x34), w.toByteArray())
    }

    @Test fun `short negative`() {
        val w = ByteCodeWriter()
        w.short((-1).toShort())
        assertArrayEquals(byteArrayOf(0xFF.toByte(), 0xFF.toByte()), w.toByteArray())
    }

    @Test fun `short negative value`() {
        val w = ByteCodeWriter()
        w.short((-1280).toShort())
        assertArrayEquals(byteArrayOf(0xFB.toByte(), 0x00), w.toByteArray())
    }

    @Test fun `ubyte writes single byte`() {
        val w = ByteCodeWriter()
        w.ubyte(0xABu)
        assertArrayEquals(byteArrayOf(0xAB.toByte()), w.toByteArray())
    }

    @Test fun `byte writes signed byte`() {
        val w = ByteCodeWriter()
        w.byte((-1).toByte())
        assertArrayEquals(byteArrayOf(0xFF.toByte()), w.toByteArray())
    }

    @Test fun `uint writes four bytes big endian`() {
        val w = ByteCodeWriter()
        w.uint(0x12345678u)
        assertArrayEquals(byteArrayOf(0x12, 0x34, 0x56, 0x78), w.toByteArray())
    }

    @Test fun `uint max`() {
        val w = ByteCodeWriter()
        w.uint(0xFFFFFFFFu)
        assertArrayEquals(byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte()), w.toByteArray())
    }

    @Test fun `string unary plus decodes hex and writes`() {
        val w = ByteCodeWriter()
        with(w) { +"cafebabe" }
        assertArrayEquals(byteArrayOf(0xca.toByte(), 0xfe.toByte(), 0xba.toByte(), 0xbe.toByte()), w.toByteArray())
    }

    @Test fun `string unary plus with whitespace`() {
        val w = ByteCodeWriter()
        with(w) { +"ca fe ba be" }
        assertArrayEquals(byteArrayOf(0xca.toByte(), 0xfe.toByte(), 0xba.toByte(), 0xbe.toByte()), w.toByteArray())
    }

    @Test fun `instructionOneArgument writes opcode plus two bytes big endian`() {
        val w = ByteCodeWriter()
        w.instructionOneArgument("b2", 0x0102)
        assertArrayEquals(byteArrayOf(0xb2.toByte(), 0x01, 0x02), w.toByteArray())
    }

    @Test fun `instructionTwoArgument writes opcode plus four bytes`() {
        val w = ByteCodeWriter()
        w.instructionTwoArgument("b6", 0x0001, 0x0002)
        assertArrayEquals(byteArrayOf(0xb6.toByte(), 0x00, 0x01, 0x00, 0x02), w.toByteArray())
    }

    @Test fun `chained calls accumulate`() {
        val w = ByteCodeWriter()
        w.ubyte(0x01u)
        w.ushort(0x0203)
        w.uint(0x04050607u)
        assertEquals(7, w.toByteArray().size)
    }

    @Test fun `toByteArray returns fresh copy on each call`() {
        val w = ByteCodeWriter()
        w.ubyte(0x01u)
        val a = w.toByteArray()
        w.ubyte(0x02u)
        val b = w.toByteArray()
        assertArrayEquals(byteArrayOf(0x01), a)
        assertArrayEquals(byteArrayOf(0x01, 0x02), b)
    }
}
