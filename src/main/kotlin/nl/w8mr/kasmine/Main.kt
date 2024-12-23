package nl.w8mr.kasmine

import java.io.ByteArrayOutputStream
import java.io.File

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
                val bytes = value.toByteArray(Charsets.UTF_8)
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

    object InvokeVirtual : Opcode(0xb6u, "InvokeVirtual")

    object InvokeStatic : Opcode(0xb8u, "InvokeStatic")

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

    object IfEqual : Opcode(0x99u, "IfNotEqual")

    object Goto : Opcode(0xa7u, "Goto")
}

class InstructionBlock {
    val instructions: MutableList<Instruction> = mutableListOf()
    var byteSize: Int = 0
    var maxStack: Int = 0
    var currentStack: Int = 0

    private fun add(instruction: Instruction) {
        byteSize += instruction.byteSize
        instructions.add(instruction)
    }
}

sealed class Instruction(open val opcode: Opcode) {
    abstract fun write(
        out: ByteCodeWriter,
        cpMap: Map<ConstantPoolType, Int>,
    )

    abstract val byteSize: Int

    data class NoArgument(override val opcode: Opcode) : Instruction(opcode) {
        override fun write(
            out: ByteCodeWriter,
            cpMap: Map<ConstantPoolType, Int>,
        ) {
            out.ubyte(opcode.opcode)
        }

        override val byteSize: Int = 1
    }

    data class OneArgumentUByte(override val opcode: Opcode, val value: UByte) : Instruction(opcode) {
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

    data class OneArgumentByte(override val opcode: Opcode, val value: Byte) : Instruction(opcode) {
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

    data class OneArgumentShort(override val opcode: Opcode, val value: Short) : Instruction(opcode) {
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

    data class OneArgumentUShort(override val opcode: Opcode, val value: UShort) : Instruction(opcode) {
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

    data class OneArgument(override val opcode: Opcode, val arg1: ConstantPoolType) : Instruction(opcode) {
        override fun write(
            out: ByteCodeWriter,
            cpMap: Map<ConstantPoolType, Int>,
        ) {
            val ref1 = cpMap[arg1]!!
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

    data class TwoArgument(override val opcode: Opcode, val arg1: ConstantPoolType, val arg2: ConstantPoolType) : Instruction(opcode) {
        override fun write(
            out: ByteCodeWriter,
            cpMap: Map<ConstantPoolType, Int>,
        ) {
            out.ubyte(opcode.opcode)
            val ref1 = cpMap[arg1]!!
            out.ushort(ref1)
            val ref2 = cpMap[arg2]!!
            out.ushort(ref2)
        }

        override val byteSize: Int = 5
    }
}

data class ClassDef(val access: UShort, val classRef: ConstantPoolType.ClassEntry, val superClassRef: ConstantPoolType.ClassEntry, val methods: List<MethodDef>)

data class MethodDef(val access: UShort, val methodName: ConstantPoolType.UTF8String, val methodSig: ConstantPoolType.UTF8String, val instructions: List<Instruction>)

class ByteCodeWriter {
    private val out = ByteArrayOutputStream()

    operator fun String.unaryPlus() {
        out.writeBytes(this.decodeHex())
    }

    fun write(bytes: ByteArray) {
        out.write(bytes)
    }

    fun byte(value: Byte) {
        out.write(value.toInt())
    }

    fun ubyte(value: UByte) {
        out.write(value.toInt())
    }

    fun ushort(value: Int) {
        assert(value <= UShort.MAX_VALUE.toInt())
        ushort(value.toUShort())
    }

    fun ushort(value: UShort) {
        out.write(value.toInt() shr 8)
        out.write(value.toInt())
    }

    fun short(value: Short) {
        out.write(if (value < 0) (value.toInt() shr 8) and 128 else (value.toInt() shr 8))
        out.write(value.toInt())
    }

    fun uint(value: UInt) {
        out.write((value shr 24).toInt())
        out.write((value shr 16).toInt())
        out.write((value shr 8).toInt())
        out.write((value shr 0).toInt())
    }

    fun instructionOneArgument(
        opcode: String,
        value: Int,
    ) {
        +opcode
        ushort(value)
    }

    fun instructionTwoArgument(
        opcode: String,
        value1: Int,
        value2: Int,
    ) {
        +opcode
        ushort(value1)
        ushort(value2)
    }

    fun toByteArray() = out.toByteArray()
}

class DynamicClassLoader(parent: ClassLoader?) : ClassLoader(parent) {
    fun define(
        className: String?,
        bytecode: ByteArray,
    ): Class<*> {
        return super.defineClass(className, bytecode, 0, bytecode.size)
    }
}

fun main() {
    val clazz =
        classBuilder {
            name = "Script"
            method {
                name = "main"
                signature = "([Ljava/lang/String;)V"
                getStatic("java/lang/System", "out", "Ljava/io/PrintStream;")
                loadConstant("Hello World")
                invokeVirtual("java/io/PrintStream", "println", "(Ljava/lang/String;)V")
                `return`()
            }
        }
    val bytes = clazz.write()
    File("/Users/TU23DC/Script.class").writeBytes(bytes)

    val actual = bytes.toHex()
    val expected =
        "cafebabe0000003400160100106a6176612f6c616e672f53797374656d0700010100036f75740100154c6a6176612f696f2f5072696e7453747265616d3b0c00030004090002000501000b48656c6c6f20576f726c640800070100136a6176612f696f2f5072696e7453747265616d0700090100077072696e746c6e010015284c6a6176612f6c616e672f537472696e673b29560c000b000c0a000a000d010004436f64650100046d61696e010016285b4c6a6176612f6c616e672f537472696e673b295601000a48656c6c6f576f726c640700120100106a6176612f6c616e672f4f626a6563740700140021001300150000000000010009001000110001000f00000015000a000a00000009b200061208b6000eb1000000000000"
    println(actual)
    println(expected)
    check(expected.startsWith(actual))

    val loader = DynamicClassLoader(Thread.currentThread().contextClassLoader)
    val ScriptClass = loader.define("Script", bytes)
    ScriptClass.getMethod("main", Array<String>::class.java).invoke(null, null)
}

/*
https://medium.com/@davethomas_9528/writing-hello-world-in-java-byte-code-34f75428e0ad
 */
