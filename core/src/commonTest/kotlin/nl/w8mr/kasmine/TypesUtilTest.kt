package nl.w8mr.kasmine

import kotlin.test.*
import kotlin.test.Test

class TypesUtilTest {

    @Test
    fun `decodeHex converts hex string to bytes`() {
        val bytes = "cafebabe".decodeHex()
        assertContentEquals(
            byteArrayOf(0xca.toByte(), 0xfe.toByte(), 0xba.toByte(), 0xbe.toByte()),
            bytes,
        )
    }

    @Test
    fun `decodeHex strips whitespace`() {
        val bytes = "ca fe ba be".decodeHex()
        assertContentEquals(
            byteArrayOf(0xca.toByte(), 0xfe.toByte(), 0xba.toByte(), 0xbe.toByte()),
            bytes,
        )
    }

    @Test
    fun `decodeHex rejects odd length`() {
        assertFails { "caf".decodeHex() }
    }

    @Test
    fun `decodeHex empty string returns empty array`() {
        assertContentEquals(byteArrayOf(), "".decodeHex())
    }

    @Test
    fun `toHex converts bytes to hex string`() {
        val hex = byteArrayOf(0xca.toByte(), 0xfe.toByte(), 0xba.toByte(), 0xbe.toByte()).toHex()
        assertEquals("cafebabe", hex)
    }

    @Test
    fun `toHex handles single byte`() {
        assertEquals("0a", byteArrayOf(10).toHex())
        assertEquals("ff", byteArrayOf(0xff.toByte()).toHex())
    }

    @Test
    fun `decodeHex toHex round trip`() {
        val original = "0123456789abcdef"
        val bytes = original.decodeHex()
        assertEquals(original, bytes.toHex())
    }

    @Test
    fun `decodeHex handles all byte values`() {
        val all = (0..255).joinToString("") { "%02x".format(it.toByte()) }
        val bytes = all.decodeHex()
        assertEquals(256, bytes.size)
        for (i in 0..255) assertEquals(i.toByte(), bytes[i])
    }

    @Test
    fun `Opcode objects have correct opcode values`() {
        assertEquals(0x02u, Opcode.IConstM1.opcode)
        assertEquals(0x03u.toUByte(), Opcode.IConst0.opcode)
        assertEquals(0x04u, Opcode.IConst1.opcode)
        assertEquals(0x59u, Opcode.Dup.opcode)
        assertEquals(0x57u, Opcode.Pop.opcode)
        assertEquals(0xb1u, Opcode.Return.opcode)
        assertEquals(0xacu, Opcode.IReturn.opcode)
        assertEquals(0xb0u, Opcode.AReturn.opcode)
        assertEquals(0x3au, Opcode.AStore.opcode)
        assertEquals(0x19u, Opcode.ALoad.opcode)
        assertEquals(0x36u, Opcode.IStore.opcode)
        assertEquals(0x15u, Opcode.ILoad.opcode)
        assertEquals(0x10u, Opcode.BiPush.opcode)
        assertEquals(0x11u, Opcode.SiPush.opcode)
        assertEquals(0xb2u, Opcode.GetStatic.opcode)
        assertEquals(0xb4u, Opcode.GetField.opcode)
        assertEquals(0xb5u, Opcode.PutField.opcode)
        assertEquals(0xb6u, Opcode.InvokeVirtual.opcode)
        assertEquals(0xb7u, Opcode.InvokeSpecial.opcode)
        assertEquals(0xb8u, Opcode.InvokeStatic.opcode)
        assertEquals(0xbbu, Opcode.New.opcode)
        assertEquals(0x9au, Opcode.IfNotEqual.opcode)
        assertEquals(0x99u, Opcode.IfEqual.opcode)
        assertEquals(0xa7u, Opcode.Goto.opcode)
    }

    @Test
    fun `Instruction byteSizes are correct`() {
        assertEquals(1, Instruction.NoArgument(Opcode.IConst0).byteSize)
        assertEquals(2, Instruction.OneArgumentUByte(Opcode.ILoad, 0u).byteSize)
        assertEquals(2, Instruction.OneArgumentByte(Opcode.BiPush, 0).byteSize)
        assertEquals(3, Instruction.OneArgumentShort(Opcode.SiPush, 0).byteSize)
        assertEquals(3, Instruction.OneArgumentUShort(Opcode.Goto, 0u).byteSize)
        assertEquals(
            3,
            Instruction.OneArgumentPool(Opcode.GetStatic, ConstantPoolType.UTF8String("x")).byteSize,
        )
        assertEquals(
            5,
            Instruction.TwoArgumentPool(
                    Opcode.InvokeVirtual,
                    ConstantPoolType.UTF8String("x"),
                    ConstantPoolType.UTF8String("y"),
                )
                .byteSize,
        )
    }

    @Test
    fun `InstructionBlock accumulates byteSize`() {
        val block = InstructionBlock()
        block.add(Instruction.NoArgument(Opcode.IConst0))
        block.add(Instruction.OneArgumentUByte(Opcode.ILoad, 0u))
        assertEquals(3, block.byteSize)
        assertEquals(2, block.instructions.size)
    }

    @Test
    fun `InstructionBlock tracks byteSize with three-byte instruction`() {
        val block = InstructionBlock()
        block.add(Instruction.NoArgument(Opcode.IConst0))
        block.add(Instruction.OneArgumentShort(Opcode.SiPush, 100))
        assertEquals(4, block.byteSize)
    }
}
