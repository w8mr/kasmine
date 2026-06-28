package nl.w8mr.kasmine

import kotlin.test.*
import kotlin.test.Test

class TypeEffectTest {

    private fun effect(inst: Instruction) = inst.typeEffect { VerificationType.Top }

    @Test
    fun `IConstM1 through IConst5 push Integer`() {
        for (op in
            listOf(
                Opcode.IConstM1,
                Opcode.IConst0,
                Opcode.IConst1,
                Opcode.IConst2,
                Opcode.IConst3,
                Opcode.IConst4,
                Opcode.IConst5,
            )) {
            val e = effect(Instruction.NoArgument(op))
            assertEquals(emptyList(), e.pops)
            assertEquals(listOf(VerificationType.Integer), e.pushes)
        }
    }

    @Test
    fun `Return has no effects`() {
        val e = effect(Instruction.NoArgument(Opcode.Return))
        assertEquals(emptyList(), e.pops)
        assertEquals(emptyList(), e.pushes)
    }

    @Test
    fun `IReturn pops Integer`() {
        val e = effect(Instruction.NoArgument(Opcode.IReturn))
        assertEquals(listOf(VerificationType.Integer), e.pops)
        assertEquals(emptyList(), e.pushes)
    }

    @Test
    fun `AReturn pops Top`() {
        val e = effect(Instruction.NoArgument(Opcode.AReturn))
        assertEquals(listOf(VerificationType.Top), e.pops)
        assertEquals(emptyList(), e.pushes)
    }

    @Test
    fun `Dup pops Top pushes Top Top`() {
        val e = effect(Instruction.NoArgument(Opcode.Dup))
        assertEquals(listOf(VerificationType.Top), e.pops)
        assertEquals(listOf(VerificationType.Top, VerificationType.Top), e.pushes)
    }

    @Test
    fun `Pop pops Top`() {
        val e = effect(Instruction.NoArgument(Opcode.Pop))
        assertEquals(listOf(VerificationType.Top), e.pops)
        assertEquals(emptyList(), e.pushes)
    }

    @Test
    fun `ILoad pushes Integer`() {
        val e = effect(Instruction.OneArgumentUByte(Opcode.ILoad, 0u))
        assertEquals(emptyList(), e.pops)
        assertEquals(listOf(VerificationType.Integer), e.pushes)
    }

    @Test
    fun `IStore pops Integer`() {
        val e = effect(Instruction.OneArgumentUByte(Opcode.IStore, 0u))
        assertEquals(listOf(VerificationType.Integer), e.pops)
        assertEquals(emptyList(), e.pushes)
    }

    @Test
    fun `ALoad pushes Top`() {
        val e = effect(Instruction.OneArgumentUByte(Opcode.ALoad, 0u))
        assertEquals(emptyList(), e.pops)
        assertEquals(listOf(VerificationType.Top), e.pushes)
    }

    @Test
    fun `AStore pops Top`() {
        val e = effect(Instruction.OneArgumentUByte(Opcode.AStore, 0u))
        assertEquals(listOf(VerificationType.Top), e.pops)
        assertEquals(emptyList(), e.pushes)
    }

    @Test
    fun `BiPush pushes Integer`() {
        val e = effect(Instruction.OneArgumentByte(Opcode.BiPush, 42))
        assertEquals(emptyList(), e.pops)
        assertEquals(listOf(VerificationType.Integer), e.pushes)
    }

    @Test
    fun `SiPush pushes Integer`() {
        val e = effect(Instruction.OneArgumentShort(Opcode.SiPush, 1000))
        assertEquals(emptyList(), e.pops)
        assertEquals(listOf(VerificationType.Integer), e.pushes)
    }

    @Test
    fun `IfEqual pops Integer`() {
        val e = effect(Instruction.OneArgumentShort(Opcode.IfEqual, 0))
        assertEquals(listOf(VerificationType.Integer), e.pops)
        assertEquals(emptyList(), e.pushes)
    }

    @Test
    fun `IfNotEqual pops Integer`() {
        val e = effect(Instruction.OneArgumentShort(Opcode.IfNotEqual, 0))
        assertEquals(listOf(VerificationType.Integer), e.pops)
        assertEquals(emptyList(), e.pushes)
    }

    @Test
    fun `Goto has no effects`() {
        val e = effect(Instruction.OneArgumentShort(Opcode.Goto, 0))
        assertEquals(emptyList(), e.pops)
        assertEquals(emptyList(), e.pushes)
    }

    @Test
    fun `GetStatic pushes resolved type`() {
        val fieldRef =
            ConstantPoolType.FieldRef(
                ConstantPoolType.ClassEntry(ConstantPoolType.UTF8String("Foo")),
                ConstantPoolType.NameAndType(
                    ConstantPoolType.UTF8String("x"),
                    ConstantPoolType.UTF8String("I"),
                ),
            )
        val e =
            Instruction.OneArgumentPool(Opcode.GetStatic, fieldRef).typeEffect { cp ->
                when (cp) {
                    fieldRef -> VerificationType.Integer
                    else -> VerificationType.Top
                }
            }
        assertEquals(emptyList(), e.pops)
        assertEquals(listOf(VerificationType.Integer), e.pushes)
    }

    @Test
    fun `GetField pops Top pushes resolved type`() {
        val fieldRef =
            ConstantPoolType.FieldRef(
                ConstantPoolType.ClassEntry(ConstantPoolType.UTF8String("Foo")),
                ConstantPoolType.NameAndType(
                    ConstantPoolType.UTF8String("x"),
                    ConstantPoolType.UTF8String("I"),
                ),
            )
        val e =
            Instruction.OneArgumentPool(Opcode.GetField, fieldRef).typeEffect { cp ->
                when (cp) {
                    fieldRef -> VerificationType.Integer
                    else -> VerificationType.Top
                }
            }
        assertEquals(listOf(VerificationType.Top), e.pops)
        assertEquals(listOf(VerificationType.Integer), e.pushes)
    }

    @Test
    fun `PutField pops Top and field type`() {
        val fieldRef =
            ConstantPoolType.FieldRef(
                ConstantPoolType.ClassEntry(ConstantPoolType.UTF8String("Foo")),
                ConstantPoolType.NameAndType(
                    ConstantPoolType.UTF8String("x"),
                    ConstantPoolType.UTF8String("I"),
                ),
            )
        val e =
            Instruction.OneArgumentPool(Opcode.PutField, fieldRef).typeEffect { cp ->
                when (cp) {
                    fieldRef -> VerificationType.Integer
                    else -> VerificationType.Top
                }
            }
        assertContains(e.pops, VerificationType.Top)
        assertContains(e.pops, VerificationType.Integer)
        assertEquals(2, e.pops.size)
        assertEquals(emptyList(), e.pushes)
    }

    @Test
    fun `LoadConstant with string pushes Object String`() {
        val str = ConstantPoolType.ConstantString(ConstantPoolType.UTF8String("hello"))
        val e = effect(Instruction.OneArgumentPool(Opcode.LoadConstant, str))
        assertEquals(emptyList(), e.pops)
        assertEquals(listOf(VerificationType.Object("java/lang/String")), e.pushes)
    }

    @Test
    fun `LoadConstant with integer pushes Integer`() {
        val ci = ConstantPoolType.ConstantInteger(42)
        val e = effect(Instruction.OneArgumentPool(Opcode.LoadConstant, ci))
        assertEquals(emptyList(), e.pops)
        assertEquals(listOf(VerificationType.Integer), e.pushes)
    }

    @Test
    fun `New pushes Uninitialized`() {
        val clazz = ConstantPoolType.ClassEntry(ConstantPoolType.UTF8String("Foo"))
        val e = effect(Instruction.OneArgumentPool(Opcode.New, clazz))
        assertEquals(emptyList(), e.pops)
        assertEquals(1, e.pushes.size)
        assertTrue(e.pushes[0] is VerificationType.Uninitialized)
    }

    @Test
    fun `InvokeStatic with void return has no pushes`() {
        val methodRef =
            ConstantPoolType.MethodRef(
                ConstantPoolType.ClassEntry(ConstantPoolType.UTF8String("Foo")),
                ConstantPoolType.NameAndType(
                    ConstantPoolType.UTF8String("bar"),
                    ConstantPoolType.UTF8String("()V"),
                ),
            )
        val e =
            Instruction.OneArgumentPool(Opcode.InvokeStatic, methodRef).typeEffect { cp ->
                when (cp) {
                    methodRef -> VerificationType.Top
                    else -> VerificationType.Top
                }
            }
        assertEquals(emptyList(), e.pops)
        assertEquals(emptyList(), e.pushes)
    }

    @Test
    fun `InvokeStatic with return pushes result`() {
        val methodRef =
            ConstantPoolType.MethodRef(
                ConstantPoolType.ClassEntry(ConstantPoolType.UTF8String("Foo")),
                ConstantPoolType.NameAndType(
                    ConstantPoolType.UTF8String("bar"),
                    ConstantPoolType.UTF8String("()I"),
                ),
            )
        val e =
            Instruction.OneArgumentPool(Opcode.InvokeStatic, methodRef).typeEffect { cp ->
                when (cp) {
                    methodRef -> VerificationType.Integer
                    else -> VerificationType.Top
                }
            }
        assertEquals(emptyList(), e.pops)
        assertEquals(listOf(VerificationType.Integer), e.pushes)
    }

    @Test
    fun `OneArgumentUShort has no effects`() {
        val e = effect(Instruction.OneArgumentUShort(Opcode.SiPush, 0u))
        assertEquals(emptyList(), e.pops)
        assertEquals(emptyList(), e.pushes)
    }

    @Test
    fun `TwoArgumentPool has no effects`() {
        val e =
            effect(
                Instruction.TwoArgumentPool(
                    Opcode.InvokeVirtual,
                    ConstantPoolType.UTF8String("a"),
                    ConstantPoolType.UTF8String("b"),
                )
            )
        assertEquals(emptyList(), e.pops)
        assertEquals(emptyList(), e.pushes)
    }
}
