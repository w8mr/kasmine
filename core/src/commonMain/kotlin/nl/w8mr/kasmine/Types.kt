package nl.w8mr.kasmine

// Common utility functions
fun String.decodeHex(): ByteArray {
    val trimmed = this.replace(Regex("\\s+"), "")
    check(trimmed.length % 2 == 0) { "Must have an even length" }

    return trimmed.chunked(2)
        .map { it.toInt(16).toByte() }
        .toByteArray()
}

fun ByteArray.toHex(): String = joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

sealed class ConstantPoolType {
    data class UTF8String(val value: String) : ConstantPoolType() {
        override fun write(
            out: ByteCodeWriter,
            cpMap: Map<ConstantPoolType, Int>,
        ) {
            with(out) {
                +"01"
                val bytes = value.encodeToByteArray()
                ushort(bytes.size)
                write(bytes)
            }
        }
    }

    data class ConstantInteger(val value: Int) : ConstantPoolType() {
        override fun write(
            out: ByteCodeWriter,
            cpMap: Map<ConstantPoolType, Int>,
        ) {
            with(out) {
                +"03"
                uint(value.toUInt())
            }
        }
    }

    data class ClassEntry(val nameRef: UTF8String) : ConstantPoolType() {
        override fun write(
            out: ByteCodeWriter,
            cpMap: Map<ConstantPoolType, Int>,
        ) {
            out.instructionOneArgument("07", cpMap[nameRef]!!)
        }
    }

    data class ConstantString(val nameRef: UTF8String) : ConstantPoolType() {
        override fun write(
            out: ByteCodeWriter,
            cpMap: Map<ConstantPoolType, Int>,
        ) {
            out.instructionOneArgument("08", cpMap[nameRef]!!)
        }
    }

    data class NameAndType(val nameRef: UTF8String, val typeRef: UTF8String) : ConstantPoolType() {
        override fun write(
            out: ByteCodeWriter,
            cpMap: Map<ConstantPoolType, Int>,
        ) {
            out.instructionTwoArgument("0c", cpMap[nameRef]!!, cpMap[typeRef]!!)
        }
    }

    data class FieldRef(val classRef: ClassEntry, val nameAndTypeRef: NameAndType) : ConstantPoolType() {
        override fun write(
            out: ByteCodeWriter,
            cpMap: Map<ConstantPoolType, Int>,
        ) {
            out.instructionTwoArgument("09", cpMap[classRef]!!, cpMap[nameAndTypeRef]!!)
        }
    }

    data class MethodRef(val classRef: ClassEntry, val nameAndTypeRef: NameAndType) : ConstantPoolType() {
        override fun write(
            out: ByteCodeWriter,
            cpMap: Map<ConstantPoolType, Int>,
        ) {
            out.instructionTwoArgument("0a", cpMap[classRef]!!, cpMap[nameAndTypeRef]!!)
        }
    }

    abstract fun write(
        out: ByteCodeWriter,
        cpMap: Map<ConstantPoolType, Int>,
    )
}

sealed class Opcode(val opcode: UByte, val name: String) {
    abstract class ByteShortOpcode(opcode1: UByte, val opcode2: UByte, name: String) : Opcode(opcode1, name)

    object IConstM1 : Opcode(0x02u, "IConstM1")

    object IConst0 : Opcode(0x03u, "IConst0")

    object IConst1 : Opcode(0x04u, "IConst1")

    object IConst2 : Opcode(0x05u, "IConst2")

    object IConst3 : Opcode(0x06u, "IConst3")

    object IConst4 : Opcode(0x07u, "IConst4")

    object IConst5 : Opcode(0x08u, "IConst5")

    object BiPush : Opcode(0x10u, "BiPush")

    object SiPush : Opcode(0x11u, "SiPush")

    object GetStatic : Opcode(0xb2u, "GetStatic")

    object LoadConstant : ByteShortOpcode(0x13u, 0x12u, "LoadConstant")

    object GetField : Opcode(0xb4u, "GetField")

    object PutField : Opcode(0xb5u, "PutField")

    object InvokeVirtual : Opcode(0xb6u, "InvokeVirtual")

    object InvokeSpecial : Opcode(0xb7u, "InvokeSpecial")

    object InvokeStatic : Opcode(0xb8u, "InvokeStatic")

    object New : Opcode(0xbbu, "New")

    object Return : Opcode(0xb1u, "Return")

    object IReturn : Opcode(0xacu, "IReturn")

    object AReturn : Opcode(0xb0u, "AReturn")

    object AStore : Opcode(0x3au, "AStore")

    object ALoad : Opcode(0x19u, "ALoad")

    object IStore : Opcode(0x36u, "IStore")

    object ILoad : Opcode(0x15u, "ILoad")

    object Dup : Opcode(0x59u, "Dup")

    object Pop : Opcode(0x57u, "Pop")

    object IfNotEqual : Opcode(0x9au, "IfNotEqual")

    object IfEqual : Opcode(0x99u, "IfEqual")

    object Goto : Opcode(0xa7u, "Goto")
}

class InstructionBlock {
    val instructions: MutableList<Instruction> = mutableListOf()
    var byteSize: Int = 0
    var maxStack: Int = 0
    var currentStack: Int = 0
    var target: InstructionBlock? = null

    fun add(instruction: Instruction) {
        byteSize += instruction.byteSize
        instructions.add(instruction)
    }
}

sealed interface Instruction {
    val opcode: Opcode
    val byteSize: Int

    fun write(
        out: ByteCodeWriter,
        cpMap: Map<ConstantPoolType, Int>,
    )

    interface OneArgument<T: Any>: Instruction {
        val value: T
    }

    interface TwoArgument<T: Any, R: Any>: Instruction {
        val value1: T
        val value2: R
    }

    data class NoArgument(override val opcode: Opcode) : Instruction {
        override fun write(
            out: ByteCodeWriter,
            cpMap: Map<ConstantPoolType, Int>,
        ) {
            out.ubyte(opcode.opcode)
        }

        override val byteSize: Int = 1
    }

    data class OneArgumentUByte(override val opcode: Opcode, override val value: UByte) : OneArgument<UByte> {
        override fun write(
            out: ByteCodeWriter,
            cpMap: Map<ConstantPoolType, Int>,
        ) {
            out.ubyte(opcode.opcode)
            out.ubyte(value)
            // TODO wide
        }

        override val byteSize: Int = 2
    }

    data class OneArgumentByte(override val opcode: Opcode, override val value: Byte) : OneArgument<Byte> {
        override fun write(
            out: ByteCodeWriter,
            cpMap: Map<ConstantPoolType, Int>,
        ) {
            out.ubyte(opcode.opcode)
            out.byte(value)
            // TODO wide
        }

        override val byteSize: Int = 2
    }

    data class OneArgumentShort(override val opcode: Opcode, override val value: Short) : OneArgument<Short> {
        override fun write(
            out: ByteCodeWriter,
            cpMap: Map<ConstantPoolType, Int>,
        ) {
            out.ubyte(opcode.opcode)
            out.short(value)
            // TODO wide
        }

        override val byteSize: Int = 3
    }

    data class OneArgumentUShort(override val opcode: Opcode, override val value: UShort) : OneArgument<UShort> {
        override fun write(
            out: ByteCodeWriter,
            cpMap: Map<ConstantPoolType, Int>,
        ) {
            out.ubyte(opcode.opcode)
            out.ushort(value)
            // TODO wide
        }

        override val byteSize: Int = 3
    }

    data class OneArgumentPool(override val opcode: Opcode, override val value: ConstantPoolType) : OneArgument<ConstantPoolType> {
        override fun write(
            out: ByteCodeWriter,
            cpMap: Map<ConstantPoolType, Int>,
        ) {
            val ref1 = cpMap[value]!!
// TODO check how to deal with dynamic sizes?
//            if ((opcode is Opcode.ByteShortOpcode) && (ref1 <= 256)) {
//                out.ubyte(opcode.opcode2)
//                out.ubyte(ref1.toUByte())
//            } else {
            out.ubyte(opcode.opcode)
            out.ushort(ref1)
//            }
        }

        override val byteSize: Int = 3
    }

    data class TwoArgumentPool(override val opcode: Opcode, override val value1: ConstantPoolType, override val value2: ConstantPoolType) : TwoArgument<ConstantPoolType, ConstantPoolType> {
        override fun write(
            out: ByteCodeWriter,
            cpMap: Map<ConstantPoolType, Int>,
        ) {
            out.ubyte(opcode.opcode)
            val ref1 = cpMap[value1]!!
            out.ushort(ref1)
            val ref2 = cpMap[value2]!!
            out.ushort(ref2)
        }

        override val byteSize: Int = 5
    }
}

data class FieldDef(val access: UShort, val name: ConstantPoolType.UTF8String, val type: ConstantPoolType.UTF8String)

data class ClassDef(val access: UShort, val classRef: ConstantPoolType.ClassEntry, val superClassRef: ConstantPoolType.ClassEntry, val fields: List<FieldDef>, val methods: List<MethodDef>)

data class MethodDef(val access: UShort, val methodName: ConstantPoolType.UTF8String, val methodSig: ConstantPoolType.UTF8String, val instructions: List<InstructionBlock>)



