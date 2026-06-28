package nl.w8mr.kasmine

// Common utility functions
fun String.decodeHex(): ByteArray {
    val trimmed = this.replace(Regex("\\s+"), "")
    check(trimmed.length % 2 == 0) { "Must have an even length" }

    return trimmed.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}

fun ByteArray.toHex(): String = joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

sealed class ConstantPoolType {
    data class UTF8String(val value: String) : ConstantPoolType() {
        override fun write(out: ByteCodeWriter, cpMap: Map<ConstantPoolType, Int>) {
            with(out) {
                +"01"
                val bytes = value.encodeToByteArray()
                ushort(bytes.size)
                write(bytes)
            }
        }
    }

    data class ConstantInteger(val value: Int) : ConstantPoolType() {
        override fun write(out: ByteCodeWriter, cpMap: Map<ConstantPoolType, Int>) {
            with(out) {
                +"03"
                uint(value.toUInt())
            }
        }
    }

    data class ConstantFloat(val value: Float) : ConstantPoolType() {
        override fun write(out: ByteCodeWriter, cpMap: Map<ConstantPoolType, Int>) {
            with(out) {
                +"04"
                uint(value.toBits().toUInt())
            }
        }
    }

    data class ConstantLong(val value: Long) : ConstantPoolType() {
        override fun write(out: ByteCodeWriter, cpMap: Map<ConstantPoolType, Int>) {
            with(out) {
                +"05"
                uint((value ushr 32).toUInt())
                uint(value.toUInt())
            }
        }
    }

    data class ConstantDouble(val value: Double) : ConstantPoolType() {
        override fun write(out: ByteCodeWriter, cpMap: Map<ConstantPoolType, Int>) {
            with(out) {
                +"06"
                uint((value.toBits() ushr 32).toUInt())
                uint(value.toBits().toUInt())
            }
        }
    }

    data class ClassEntry(val nameRef: UTF8String) : ConstantPoolType() {
        override fun write(out: ByteCodeWriter, cpMap: Map<ConstantPoolType, Int>) {
            out.instructionOneArgument("07", cpMap[nameRef]!!)
        }
    }

    data class ConstantString(val nameRef: UTF8String) : ConstantPoolType() {
        override fun write(out: ByteCodeWriter, cpMap: Map<ConstantPoolType, Int>) {
            out.instructionOneArgument("08", cpMap[nameRef]!!)
        }
    }

    data class NameAndType(val nameRef: UTF8String, val typeRef: UTF8String) : ConstantPoolType() {
        override fun write(out: ByteCodeWriter, cpMap: Map<ConstantPoolType, Int>) {
            out.instructionTwoArgument("0c", cpMap[nameRef]!!, cpMap[typeRef]!!)
        }
    }

    data class FieldRef(val classRef: ClassEntry, val nameAndTypeRef: NameAndType) :
        ConstantPoolType() {
        override fun write(out: ByteCodeWriter, cpMap: Map<ConstantPoolType, Int>) {
            out.instructionTwoArgument("09", cpMap[classRef]!!, cpMap[nameAndTypeRef]!!)
        }
    }

    data class MethodRef(val classRef: ClassEntry, val nameAndTypeRef: NameAndType) :
        ConstantPoolType() {
        override fun write(out: ByteCodeWriter, cpMap: Map<ConstantPoolType, Int>) {
            out.instructionTwoArgument("0a", cpMap[classRef]!!, cpMap[nameAndTypeRef]!!)
        }
    }

    abstract fun write(out: ByteCodeWriter, cpMap: Map<ConstantPoolType, Int>)
}

sealed class Opcode(val opcode: UByte, val name: String) {
    abstract class ByteShortOpcode(opcode1: UByte, val opcode2: UByte, name: String) :
        Opcode(opcode1, name)

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

    object LConst0 : Opcode(0x09u, "LConst0")

    object LConst1 : Opcode(0x0au, "LConst1")

    object FConst0 : Opcode(0x0bu, "FConst0")

    object FConst1 : Opcode(0x0cu, "FConst1")

    object FConst2 : Opcode(0x0du, "FConst2")

    object DConst0 : Opcode(0x0eu, "DConst0")

    object DConst1 : Opcode(0x0fu, "DConst1")

    object LLoad : Opcode(0x16u, "LLoad")

    object LStore : Opcode(0x37u, "LStore")

    object FLoad : Opcode(0x17u, "FLoad")

    object FStore : Opcode(0x38u, "FStore")

    object DLoad : Opcode(0x18u, "DLoad")

    object DStore : Opcode(0x39u, "DStore")

    object LReturn : Opcode(0xadu, "LReturn")

    object FReturn : Opcode(0xaeu, "FReturn")

    object DReturn : Opcode(0xafu, "DReturn")

    object LoadConstant2W : Opcode(0x14u, "LoadConstant2W")
}

class BlockRef {
    var block: InstructionBlock? = null
    val isBound: Boolean
        get() = block != null
}

class InstructionBlock {
    val instructions: MutableList<Instruction> = mutableListOf()
    var byteSize: Int = 0
    var maxStack: Int = 0
    var currentStack: Int = 0
    var jumpRef: (() -> BlockRef)? = null
    var jumpTarget: BlockRef? = null

    fun add(instruction: Instruction) {
        byteSize += instruction.byteSize
        instructions.add(instruction)
    }
}

typealias TypeResolver = (ConstantPoolType) -> VerificationType

data class TypeEffect(val pops: List<VerificationType>, val pushes: List<VerificationType>)

sealed interface Instruction {
    val opcode: Opcode
    val byteSize: Int

    fun write(out: ByteCodeWriter, cpMap: Map<ConstantPoolType, Int>)

    fun typeEffect(resolve: TypeResolver): TypeEffect

    interface OneArgument<T : Any> : Instruction {
        val value: T
    }

    interface TwoArgument<T : Any, R : Any> : Instruction {
        val value1: T
        val value2: R
    }

    data class NoArgument(override val opcode: Opcode) : Instruction {
        override fun write(out: ByteCodeWriter, cpMap: Map<ConstantPoolType, Int>) {
            out.ubyte(opcode.opcode)
        }

        override val byteSize: Int = 1

        override fun typeEffect(resolve: TypeResolver): TypeEffect =
            when (opcode) {
                Opcode.IConstM1,
                Opcode.IConst0,
                Opcode.IConst1,
                Opcode.IConst2,
                Opcode.IConst3,
                Opcode.IConst4,
                Opcode.IConst5 -> TypeEffect(emptyList(), listOf(VerificationType.Integer))
                Opcode.LConst0,
                Opcode.LConst1 -> TypeEffect(emptyList(), listOf(VerificationType.Long))
                Opcode.FConst0,
                Opcode.FConst1,
                Opcode.FConst2 -> TypeEffect(emptyList(), listOf(VerificationType.Float))
                Opcode.DConst0,
                Opcode.DConst1 -> TypeEffect(emptyList(), listOf(VerificationType.Double))
                Opcode.Dup ->
                    TypeEffect(
                        listOf(VerificationType.Top),
                        listOf(VerificationType.Top, VerificationType.Top),
                    )
                Opcode.Pop -> TypeEffect(listOf(VerificationType.Top), emptyList())
                Opcode.Return -> TypeEffect(emptyList(), emptyList())
                Opcode.IReturn -> TypeEffect(listOf(VerificationType.Integer), emptyList())
                Opcode.LReturn -> TypeEffect(listOf(VerificationType.Long), emptyList())
                Opcode.FReturn -> TypeEffect(listOf(VerificationType.Float), emptyList())
                Opcode.DReturn -> TypeEffect(listOf(VerificationType.Double), emptyList())
                Opcode.AReturn -> TypeEffect(listOf(VerificationType.Top), emptyList())
                else -> TypeEffect(emptyList(), emptyList())
            }
    }

    data class OneArgumentUByte(override val opcode: Opcode, override val value: UByte) :
        OneArgument<UByte> {
        override fun write(out: ByteCodeWriter, cpMap: Map<ConstantPoolType, Int>) {
            out.ubyte(opcode.opcode)
            out.ubyte(value)
            // TODO wide
        }

        override val byteSize: Int = 2

        override fun typeEffect(resolve: TypeResolver): TypeEffect =
            when (opcode) {
                Opcode.ILoad -> TypeEffect(emptyList(), listOf(VerificationType.Integer))
                Opcode.IStore -> TypeEffect(listOf(VerificationType.Integer), emptyList())
                Opcode.LLoad -> TypeEffect(emptyList(), listOf(VerificationType.Long))
                Opcode.LStore -> TypeEffect(listOf(VerificationType.Long), emptyList())
                Opcode.FLoad -> TypeEffect(emptyList(), listOf(VerificationType.Float))
                Opcode.FStore -> TypeEffect(listOf(VerificationType.Float), emptyList())
                Opcode.DLoad -> TypeEffect(emptyList(), listOf(VerificationType.Double))
                Opcode.DStore -> TypeEffect(listOf(VerificationType.Double), emptyList())
                Opcode.ALoad -> TypeEffect(emptyList(), listOf(VerificationType.Top))
                Opcode.AStore -> TypeEffect(listOf(VerificationType.Top), emptyList())
                else -> TypeEffect(emptyList(), emptyList())
            }
    }

    data class OneArgumentByte(override val opcode: Opcode, override val value: Byte) :
        OneArgument<Byte> {
        override fun write(out: ByteCodeWriter, cpMap: Map<ConstantPoolType, Int>) {
            out.ubyte(opcode.opcode)
            out.byte(value)
            // TODO wide
        }

        override val byteSize: Int = 2

        override fun typeEffect(resolve: TypeResolver): TypeEffect =
            when (opcode) {
                Opcode.BiPush -> TypeEffect(emptyList(), listOf(VerificationType.Integer))
                else -> TypeEffect(emptyList(), emptyList())
            }
    }

    data class OneArgumentShort(override val opcode: Opcode, override val value: Short) :
        OneArgument<Short> {
        override fun write(out: ByteCodeWriter, cpMap: Map<ConstantPoolType, Int>) {
            out.ubyte(opcode.opcode)
            out.short(value)
            // TODO wide
        }

        override val byteSize: Int = 3

        override fun typeEffect(resolve: TypeResolver): TypeEffect =
            when (opcode) {
                Opcode.SiPush -> TypeEffect(emptyList(), listOf(VerificationType.Integer))
                Opcode.Goto -> TypeEffect(emptyList(), emptyList())
                Opcode.IfNotEqual,
                Opcode.IfEqual -> TypeEffect(listOf(VerificationType.Integer), emptyList())
                else -> TypeEffect(emptyList(), emptyList())
            }
    }

    data class OneArgumentUShort(override val opcode: Opcode, override val value: UShort) :
        OneArgument<UShort> {
        override fun write(out: ByteCodeWriter, cpMap: Map<ConstantPoolType, Int>) {
            out.ubyte(opcode.opcode)
            out.ushort(value)
            // TODO wide
        }

        override val byteSize: Int = 3

        override fun typeEffect(resolve: TypeResolver): TypeEffect =
            TypeEffect(emptyList(), emptyList())
    }

    data class OneArgumentPool(override val opcode: Opcode, override val value: ConstantPoolType) :
        OneArgument<ConstantPoolType> {
        override fun write(out: ByteCodeWriter, cpMap: Map<ConstantPoolType, Int>) {
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

        override fun typeEffect(resolve: TypeResolver): TypeEffect =
            when (opcode) {
                Opcode.GetStatic -> TypeEffect(emptyList(), listOf(resolve(value)))
                Opcode.GetField -> TypeEffect(listOf(VerificationType.Top), listOf(resolve(value)))
                Opcode.PutField -> {
                    val fieldType = resolve(value)
                    TypeEffect(listOf(VerificationType.Top, fieldType), emptyList())
                }
                Opcode.InvokeVirtual,
                Opcode.InvokeSpecial -> {
                    val returnType = resolve(value)
                    val pops =
                        if (returnType is VerificationType.Top) listOf(VerificationType.Top)
                        else emptyList()
                    val pushes =
                        if (returnType is VerificationType.Top) emptyList() else listOf(returnType)
                    TypeEffect(pops, pushes)
                }
                Opcode.InvokeStatic -> {
                    val returnType = resolve(value)
                    val pushes =
                        if (returnType is VerificationType.Top) emptyList() else listOf(returnType)
                    TypeEffect(emptyList(), pushes)
                }
                Opcode.LoadConstant -> {
                    val t =
                        when (value) {
                            is ConstantPoolType.ConstantString ->
                                VerificationType.Object("java/lang/String")
                            is ConstantPoolType.ConstantInteger -> VerificationType.Integer
                            is ConstantPoolType.ConstantFloat -> VerificationType.Float
                            else -> VerificationType.Top
                        }
                    TypeEffect(emptyList(), listOf(t))
                }
                Opcode.LoadConstant2W -> {
                    val t =
                        when (value) {
                            is ConstantPoolType.ConstantLong -> VerificationType.Long
                            is ConstantPoolType.ConstantDouble -> VerificationType.Double
                            else -> VerificationType.Top
                        }
                    TypeEffect(emptyList(), listOf(t))
                }
                Opcode.New -> TypeEffect(emptyList(), listOf(VerificationType.Uninitialized(0u)))
                else -> TypeEffect(emptyList(), emptyList())
            }
    }

    data class TwoArgumentPool(
        override val opcode: Opcode,
        override val value1: ConstantPoolType,
        override val value2: ConstantPoolType,
    ) : TwoArgument<ConstantPoolType, ConstantPoolType> {
        override fun write(out: ByteCodeWriter, cpMap: Map<ConstantPoolType, Int>) {
            out.ubyte(opcode.opcode)
            val ref1 = cpMap[value1]!!
            out.ushort(ref1)
            val ref2 = cpMap[value2]!!
            out.ushort(ref2)
        }

        override val byteSize: Int = 5

        override fun typeEffect(resolve: TypeResolver): TypeEffect =
            TypeEffect(emptyList(), emptyList())
    }
}

data class FieldDef(
    val access: UShort,
    val name: ConstantPoolType.UTF8String,
    val type: ConstantPoolType.UTF8String,
)

data class ClassDef(
    val access: UShort,
    val classRef: ConstantPoolType.ClassEntry,
    val superClassRef: ConstantPoolType.ClassEntry,
    val fields: List<FieldDef>,
    val methods: List<MethodDef>,
)

data class MethodDef(
    val access: UShort,
    val methodName: ConstantPoolType.UTF8String,
    val methodSig: ConstantPoolType.UTF8String,
    val instructions: List<InstructionBlock>,
)
