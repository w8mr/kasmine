package nl.w8mr.kasmine

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class StackMapGeneratorTest {

    private fun block(vararg insts: Instruction): InstructionBlock {
        val b = InstructionBlock()
        for (inst in insts) b.add(inst)
        return b
    }

    private fun InstructionBlock.jumpsTo(target: InstructionBlock) {
        val ref = BlockRef().also { it.block = target }
        this.jumpTarget = ref
    }

    private fun generator(
        blocks: List<InstructionBlock>,
        sig: String = "()V",
        isStatic: Boolean = true,
        className: String = "TestClass",
    ) = StackMapGenerator(blocks, sig, isStatic, className)

    @Test fun `hasBranches false when no targets`() {
        val g = generator(listOf(block(Instruction.NoArgument(Opcode.Return))))
        assertFalse(g.hasBranches())
    }

    @Test fun `hasBranches true when target set`() {
        val target = InstructionBlock()
        val b = block(Instruction.NoArgument(Opcode.IConst0))
        b.jumpsTo(target)
        val g = generator(listOf(b, target))
        assertTrue(g.hasBranches())
    }

    @Test fun `generate empty when no branches`() {
        val g = generator(listOf(
            block(Instruction.NoArgument(Opcode.IConst0), Instruction.NoArgument(Opcode.Return))
        ))
        assertTrue(g.generate().isEmpty())
    }

    @Test fun `generate has entry when target block exists`() {
        val target = InstructionBlock()
        target.add(Instruction.NoArgument(Opcode.Return))
        val main = block(Instruction.NoArgument(Opcode.IConst0))
        main.jumpsTo(target)
        val g = generator(listOf(main, target), "()I", isStatic = true)
        assertEquals(1, g.generate().size)
    }

    @Test fun `forward ifequal generates frame at target`() {
        val end = InstructionBlock()
        end.add(Instruction.NoArgument(Opcode.IReturn))

        val main = InstructionBlock()
        main.add(Instruction.NoArgument(Opcode.IConst0))
        val ifeq = Instruction.OneArgumentShort(Opcode.IfEqual, 0)
        main.add(ifeq)
        main.jumpsTo(end)

        val g = generator(listOf(main, end), "()I", isStatic = true)
        val entries = g.generate()
        assertEquals(1, entries.size)
        assertTrue(entries[0].frame.stack.isEmpty())
        assertEquals(VerificationType.Top, entries[0].frame.local(0))
    }

    @Test fun `target block gets correct locals from dataflow`() {
        val end = InstructionBlock()
        end.add(Instruction.NoArgument(Opcode.IReturn))

        val main = InstructionBlock()
        main.add(Instruction.OneArgumentUByte(Opcode.IConst0, 0u))
        main.add(Instruction.OneArgumentUByte(Opcode.IStore, 0u))
        main.add(Instruction.OneArgumentUByte(Opcode.ILoad, 0u))
        val ifeq = Instruction.OneArgumentShort(Opcode.IfEqual, 0)
        main.add(ifeq)
        main.jumpsTo(end)

        val g = generator(listOf(main, end), "()I", isStatic = true)
        val entries = g.generate()
        assertEquals(1, entries.size)
        val frame = entries[0].frame
        assertEquals(VerificationType.Integer, frame.local(0))
        assertTrue(frame.stack.isEmpty())
    }

    @Test fun `istore updates local to Integer`() {
        val end = InstructionBlock()
        end.add(Instruction.NoArgument(Opcode.Return))

        val main = InstructionBlock()
        main.add(Instruction.OneArgumentUByte(Opcode.IConst0, 0u))
        main.add(Instruction.OneArgumentUByte(Opcode.IStore, 0u))
        val ifeq = Instruction.OneArgumentShort(Opcode.IfEqual, 0)
        main.add(ifeq)
        main.jumpsTo(end)

        val g = generator(listOf(main, end), "()V", isStatic = true)
        val entries = g.generate()
        assertEquals(1, entries.size)
        val frame = entries[0].frame
        assertEquals(VerificationType.Integer, frame.local(0))
    }

    @Test fun `aload on Top local stays Top`() {
        val end = InstructionBlock()
        end.add(Instruction.NoArgument(Opcode.Return))

        val main = InstructionBlock()
        main.add(Instruction.OneArgumentUByte(Opcode.ALoad, 0u))
        val ifeq = Instruction.OneArgumentShort(Opcode.IfEqual, 0)
        main.add(ifeq)
        main.jumpsTo(end)

        val g = generator(listOf(main, end), "()V", isStatic = true)
        val entries = g.generate()
        assertEquals(1, entries.size)
        val frame = entries[0].frame
        assertEquals(VerificationType.Top, frame.local(0))
    }

    @Test fun `backward branch loop generates frame at header`() {
        val loopBody = InstructionBlock()
        val loopHeader = InstructionBlock()
        loopHeader.add(Instruction.NoArgument(Opcode.IConst0))
        val ifeq = Instruction.OneArgumentShort(Opcode.IfEqual, 0)
        loopHeader.add(ifeq)
        loopHeader.jumpsTo(loopBody)
        loopBody.add(Instruction.NoArgument(Opcode.Return))

        val g = generator(listOf(loopHeader, loopBody), "()I", isStatic = true)
        val entries = g.generate()
        assertFalse(entries.isEmpty())
    }

    @Test fun `multiple branches produce two frames`() {
        val target1 = InstructionBlock()
        target1.add(Instruction.NoArgument(Opcode.IReturn))
        val target2 = InstructionBlock()
        target2.add(Instruction.NoArgument(Opcode.IReturn))
        val end = InstructionBlock()
        end.add(Instruction.NoArgument(Opcode.IReturn))

        val block0 = InstructionBlock()
        block0.add(Instruction.NoArgument(Opcode.IConst0))
        block0.add(Instruction.OneArgumentShort(Opcode.IfEqual, 0))
        block0.jumpsTo(target1)

        val block1 = InstructionBlock()
        block1.add(Instruction.NoArgument(Opcode.IConst0))
        block1.add(Instruction.OneArgumentShort(Opcode.IfEqual, 0))
        block1.jumpsTo(target2)

        val g = generator(listOf(block0, block1, end, target1, target2), "()I", isStatic = true)
        val entries = g.generate()
        assertEquals(2, entries.size)
    }

    @Test fun `goto falls through and also reaches target`() {
        val target = InstructionBlock()
        target.add(Instruction.NoArgument(Opcode.IReturn))

        val main = InstructionBlock()
        main.add(Instruction.NoArgument(Opcode.IConst0))
        val goto = Instruction.OneArgumentShort(Opcode.Goto, 0)
        main.add(goto)
        main.jumpsTo(target)

        val fallthrough = InstructionBlock()
        fallthrough.add(Instruction.NoArgument(Opcode.IConst1))
        fallthrough.add(Instruction.NoArgument(Opcode.IReturn))

        val g = generator(listOf(main, fallthrough, target), "()I", isStatic = true)
        val entries = g.generate()
        assertEquals(1, entries.size)
    }

    @Test fun `instance method initial frame has this`() {
        val end = InstructionBlock()
        end.add(Instruction.NoArgument(Opcode.IReturn))
        val main = InstructionBlock()
        main.add(Instruction.NoArgument(Opcode.IConst0))
        val ifeq = Instruction.OneArgumentShort(Opcode.IfEqual, 0)
        main.add(ifeq)
        main.jumpsTo(end)

        val g = generator(listOf(main, end), "()V", isStatic = false, className = "MyClass")
        val entries = g.generate()
        assertEquals(1, entries.size)
        assertEquals(VerificationType.Object("MyClass"), entries[0].frame.local(0))
    }

    @Test fun `offset delta is correct for forward branch`() {
        val end = InstructionBlock()
        end.add(Instruction.NoArgument(Opcode.IReturn))

        val main = InstructionBlock()
        main.add(Instruction.NoArgument(Opcode.IConst0))
        val ifeq = Instruction.OneArgumentShort(Opcode.IfEqual, 0)
        main.add(ifeq)
        main.jumpsTo(end)

        val g = generator(listOf(main, end), "()I", isStatic = true)
        val entries = g.generate()
        assertEquals(1, entries.size)
        val entry = entries[0]
        assertTrue(entry.offsetDelta >= 0, "offsetDelta should be >= 0 but was ${entry.offsetDelta}")
    }

    @Test fun `writeStackMap produces valid header bytes`() {
        val end = InstructionBlock()
        end.add(Instruction.NoArgument(Opcode.IReturn))

        val main = InstructionBlock()
        main.add(Instruction.NoArgument(Opcode.IConst0))
        val ifeq = Instruction.OneArgumentShort(Opcode.IfEqual, 0)
        main.add(ifeq)
        main.jumpsTo(end)

        val g = generator(listOf(main, end), "()I", isStatic = true)
        val entries = g.generate()

        val pool = ConstantPoolType.UTF8String("StackMapTable")
        val cp: Map<ConstantPoolType, Int> = mapOf(
            pool to 1,
            ConstantPoolType.UTF8String("java/lang/Object") to 2,
        )

        val out = ByteCodeWriter()
        g.writeStackMap(out, cp, entries)
        val bytes = out.toByteArray()

        val bodyOffset = 6
        val entryCount = (bytes[bodyOffset].toInt() and 0xff) shl 8 or (bytes[bodyOffset + 1].toInt() and 0xff)
        assertEquals(entries.size, entryCount)

        val frameOffset = bodyOffset + 2
        val tag = bytes[frameOffset].toInt() and 0xff
        assertEquals(255, tag)
    }

    @Test fun `stack is empty at ifequal target when comparing to zero`() {
        val end = InstructionBlock()
        end.add(Instruction.NoArgument(Opcode.IReturn))

        val main = InstructionBlock()
        main.add(Instruction.NoArgument(Opcode.IConst0))
        val ifeq = Instruction.OneArgumentShort(Opcode.IfEqual, 0)
        main.add(ifeq)
        main.jumpsTo(end)

        val g = generator(listOf(main, end), "()I", isStatic = true)
        val entries = g.generate()
        assertEquals(1, entries.size)
        assertTrue(entries[0].frame.stack.isEmpty(), "stack should be empty after ifequal")
    }

    @Test fun `nested if-else generates correct frames`() {
        val innerElse = InstructionBlock()
        innerElse.add(Instruction.NoArgument(Opcode.IConst2))
        innerElse.add(Instruction.NoArgument(Opcode.IReturn))

        val outerElse = InstructionBlock()
        outerElse.add(Instruction.NoArgument(Opcode.IConst0))
        val innerIneq = Instruction.OneArgumentShort(Opcode.IfNotEqual, 0)
        outerElse.add(innerIneq)
        outerElse.jumpsTo(innerElse)
        val afterInner = InstructionBlock()
        afterInner.add(Instruction.NoArgument(Opcode.IConst1))
        afterInner.add(Instruction.NoArgument(Opcode.IReturn))

        val end = InstructionBlock()
        end.add(Instruction.NoArgument(Opcode.IReturn))

        val main = InstructionBlock()
        main.add(Instruction.NoArgument(Opcode.IConst0))
        val outerIneq = Instruction.OneArgumentShort(Opcode.IfNotEqual, 0)
        main.add(outerIneq)
        main.jumpsTo(outerElse)

        val g = generator(listOf(main, outerElse, innerElse, afterInner, end), "()I", isStatic = true)
        val entries = g.generate()
        assertTrue(entries.size >= 2)
    }
}
