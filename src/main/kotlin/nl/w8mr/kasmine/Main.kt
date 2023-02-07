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
        override fun write(out: ByteCodeWriter, cpMap: Map<ConstantPoolType, Int>) {
            with(out) {
                +"01"
                val bytes = value.toByteArray(Charsets.UTF_8)
                out.ushort(bytes.size)
                out.write(bytes)
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

    data class FieldRef(val classRef: ClassEntry, val nameAndTypeRef: NameAndType) : ConstantPoolType() {
        override fun write(out: ByteCodeWriter, cpMap: Map<ConstantPoolType, Int>) {
            out.instructionTwoArgument("09", cpMap[classRef]!!, cpMap[nameAndTypeRef]!!)
        }
    }

    data class MethodRef(val classRef: ClassEntry, val nameAndTypeRef: NameAndType) : ConstantPoolType() {
        override fun write(out: ByteCodeWriter, cpMap: Map<ConstantPoolType, Int>) {
            out.instructionTwoArgument("0a", cpMap[classRef]!!, cpMap[nameAndTypeRef]!!)
        }
    }

    abstract fun write(out: ByteCodeWriter, cpMap: Map<ConstantPoolType, Int>)
}

sealed class Opcode(val opcode: UByte, val name: String) {
    abstract class ByteShortOpcode(opcode1: UByte, val opcode2: UByte, name: String) : Opcode(opcode1, name)
    object GetStatic : Opcode(0xb2u, "GetStatic")
    object LoadConstant : ByteShortOpcode(0x13u, 0x12u, "LoadConstant")
    object InvokeVirtual : Opcode(0xb6u, "InvokeVirtual")
    object InvokeStatic : Opcode(0xb8u, "InvokeStatic")
    object Ret : Opcode(0xb1u, "Return")
}
sealed class Instruction(open val opcode: Opcode) {

    abstract fun write(out: ByteCodeWriter, cpMap: Map<ConstantPoolType, Int>)
    data class NoArgument(override val opcode: Opcode) : Instruction(opcode) {
        override fun write(out: ByteCodeWriter, cpMap: Map<ConstantPoolType, Int>) {
            out.ubyte(opcode.opcode)
        }
    }

    data class OneArgument(override val opcode: Opcode, val arg1: ConstantPoolType) : Instruction(opcode) {
        override fun write(out: ByteCodeWriter, cpMap: Map<ConstantPoolType, Int>) {
            val ref1 = cpMap[arg1]!!
            if ((opcode is Opcode.ByteShortOpcode) && (ref1 <= 256)) {
                out.ubyte(opcode.opcode2)
                out.ubyte(ref1.toUByte())
            } else {
                out.ubyte(opcode.opcode)
                out.ushort(ref1)
            }
        }
    }
    data class TwoArgument(override val opcode: Opcode, val arg1: ConstantPoolType, val arg2: ConstantPoolType) : Instruction(opcode) {
        override fun write(out: ByteCodeWriter, cpMap: Map<ConstantPoolType, Int>) {
            out.ubyte(opcode.opcode)
            val ref1 = cpMap[arg1]!!
            out.ushort(ref1)
            val ref2 = cpMap[arg2]!!
            out.ushort(ref2)
        }
    }
}

data class ClassDef(val access: UShort, val classRef: ConstantPoolType.ClassEntry, val superClassRef: ConstantPoolType.ClassEntry, val methods: List<MethodDef>)
data class MethodDef(val access: UShort, val methodName: ConstantPoolType.UTF8String, val methodSig: ConstantPoolType.UTF8String, val instructions: List<Instruction>)

class ByteCodeWriter() {
    private val out = ByteArrayOutputStream()

    operator fun String.unaryPlus() {
        out.writeBytes(this.decodeHex())
    }

    fun write(bytes: ByteArray) {
        out.write(bytes)
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

    fun uint(value: UInt) {
        out.write((value shr 24).toInt())
        out.write((value shr 16).toInt())
        out.write((value shr 8).toInt())
        out.write((value shr 0).toInt())
    }

    fun instructionOneArgument(opcode: String, value: Int) {
        +opcode
        ushort(value)
    }

    fun instructionTwoArgument(opcode: String, value1: Int, value2: Int) {
        +opcode
        ushort(value1)
        ushort(value2)
    }

    fun toByteArray() =
        out.toByteArray()
}

class ClassBuilder() {
    val constantPool = mutableMapOf<ConstantPoolType, Int>()
    lateinit var classDef: ClassDef

    fun utf8String(value: String) =
        addToPool(ConstantPoolType.UTF8String(value))

    fun classEntry(value: String) =
        addToPool(ConstantPoolType.ClassEntry(utf8String(value)))

    fun constantString(value: String) =
        addToPool(ConstantPoolType.ConstantString(utf8String(value)))

    fun nameAndType(name: String, type: String) =
        addToPool(ConstantPoolType.NameAndType(utf8String(name), utf8String(type)))

    fun fieldRef(classRef: ConstantPoolType.ClassEntry, nameAndType: ConstantPoolType.NameAndType) =
        addToPool(ConstantPoolType.FieldRef(classRef, nameAndType))

    fun fieldRef(classRef: ConstantPoolType.ClassEntry, name: String, type: String) =
        fieldRef(classRef, nameAndType(name, type))

    fun fieldRef(className: String, fieldName: String, type: String) =
        fieldRef(classEntry(className), fieldName, type)

    fun methodRef(classRef: ConstantPoolType.ClassEntry, nameAndType: ConstantPoolType.NameAndType) =
        addToPool(ConstantPoolType.MethodRef(classRef, nameAndType))

    fun methodRef(classRef: ConstantPoolType.ClassEntry, methodName: String, type: String) =
        methodRef(classRef, nameAndType(methodName, type))

    fun methodRef(className: String, methodName: String, type: String) =
        methodRef(classEntry(className), methodName, type)

    fun classDef(access: UShort, classRef: ConstantPoolType.ClassEntry, superClassRef: ConstantPoolType.ClassEntry = classEntry("java/lang/Object"), methods: List<MethodDef>): ClassDef {
        classDef = ClassDef(access, classRef, superClassRef, methods)
        return classDef
    }

    fun classDef(access: UShort, className: String, superClassName: String = "java/lang/Object", methods: List<MethodDef>) =
        classDef(access, classEntry(className), classEntry(superClassName), methods)

    fun methodDef(access: UShort, methodName: ConstantPoolType.UTF8String, methodSig: ConstantPoolType.UTF8String, instructions: List<Instruction>) =
        MethodDef(access, methodName, methodSig, instructions)

    fun methodDef(access: UShort, methodName: String, methodSig: String, instructions: List<Instruction>): MethodDef {
        if (instructions.isNotEmpty()) utf8String("Code")
        return methodDef(access, utf8String(methodName), utf8String(methodSig), instructions)
    }

    private inline fun <reified T : ConstantPoolType> addToPool(element: T): T {
        constantPool.merge(element, 1, Int::plus)
        return element
    }

    fun write(): ByteArray {
        val out = ByteCodeWriter()

        with(out) {
            +"cafebabe"
            ushort(0) // minor version
            ushort(52) // major version
            ushort(constantPool.size + 1) // constantPoolSize + 1
            val cpMap = constantPool.entries.sortedByDescending { it.value }.map { it.key }.withIndex().associate { it.value to (it.index + 1) }
            cpMap.entries.sortedBy { it.value }.forEach { it.key.write(out, cpMap) }

            ushort(33) // Super Public
            ushort(cpMap[classDef.classRef]!!) // main class
            ushort(cpMap[classDef.superClassRef]!!) // main class super
            ushort(0u) // interface count
            ushort(0u) // field count
            ushort(classDef.methods.size) // method count
            classDef.methods.forEach { method ->
                ushort(method.access)
                ushort(cpMap[method.methodName]!!) // main class super
                ushort(cpMap[method.methodSig]!!) // method signature
                ushort(1u) // attribute count method
                ushort(cpMap[ConstantPoolType.UTF8String("Code")]!!) // reference to Code attribute
                val instWriter = ByteCodeWriter()
                method.instructions.forEach { it.write(instWriter, cpMap) }
                val instBytes = instWriter.toByteArray()

                uint((instBytes.size + 12).toUInt()) // code attribute bytes count
                ushort(2u) // max stack size
                ushort(1u) // max local var size
                uint(instBytes.size.toUInt()) // code block bytes count
                out.write(instBytes)
                ushort(0) // exception count
                ushort(0) // attribute count method
            }
            ushort(0) // attribute count class
        }

        val toByteArray = out.toByteArray()

        return toByteArray
    }
}

class InstructionBuilder(val classBuilder: ClassBuilder) {
    private val instructions = mutableListOf<Instruction>()

    private fun fieldRef(className: String, fieldName: String, type: String): ConstantPoolType.FieldRef =
        classBuilder.fieldRef(className, fieldName, type)

    private fun constantString(value: String) =
        classBuilder.constantString(value)

    private fun methodRef(className: String, methodName: String, type: String) =
        classBuilder.methodRef(className, methodName, type)

    private fun add(instruction: Instruction) {
        instructions.add(instruction)
    }
    fun getStatic(field: ConstantPoolType.FieldRef) = add(Instruction.OneArgument(Opcode.GetStatic, field))
    fun getStatic(className: String, fieldName: String, type: String) = getStatic(fieldRef(className, fieldName, type))

    fun loadConstant(string: ConstantPoolType.ConstantString) = add(Instruction.OneArgument(Opcode.LoadConstant, string))
    fun loadConstant(string: String) = loadConstant(constantString(string))
    fun invokeVirtual(method: ConstantPoolType.MethodRef) = add(Instruction.OneArgument(Opcode.InvokeVirtual, method))
    fun invokeVirtual(className: String, methodName: String, type: String) = invokeVirtual(methodRef(className, methodName, type))

    fun invokeStatic(method: ConstantPoolType.MethodRef) = add(Instruction.OneArgument(Opcode.InvokeStatic, method))
    fun invokeStatic(className: String, methodName: String, type: String) = invokeStatic(methodRef(className, methodName, type))

    fun ret() = add(Instruction.NoArgument(Opcode.Ret))

    fun instructions() = instructions.toList()
}

class DynamicClassLoader(parent: ClassLoader?) : ClassLoader(parent) {
    fun define(className: String?, bytecode: ByteArray): Class<*> {
        return super.defineClass(className, bytecode, 0, bytecode.size)
    }
}

fun main(vararg args: String) {
    val builder = ClassBuilder()
    with(builder) {
        classDef(
            33u,
            "HelloWorld",
            "java/lang/Object",
            listOf(
                methodDef(
                    9u,
                    "main",
                    "([Ljava/lang/String;)V",
                    with(InstructionBuilder(builder)) {
                        getStatic("java/lang/System", "out", "Ljava/io/PrintStream;")
                        loadConstant("Hello World")
                        invokeVirtual("java/io/PrintStream", "println", "(Ljava/lang/String;)V")
                        ret()
                        instructions()
                    }
                )
            )
        )
    }
    val bytes = builder.write()
    File("/Users/TU23DC/HelloWorld.class").writeBytes(bytes)

    val actual = bytes.toHex()
    val expected =
        "cafebabe0000003400160100106a6176612f6c616e672f53797374656d0700010100036f75740100154c6a6176612f696f2f5072696e7453747265616d3b0c00030004090002000501000b48656c6c6f20576f726c640800070100136a6176612f696f2f5072696e7453747265616d0700090100077072696e746c6e010015284c6a6176612f6c616e672f537472696e673b29560c000b000c0a000a000d010004436f64650100046d61696e010016285b4c6a6176612f6c616e672f537472696e673b295601000a48656c6c6f576f726c640700120100106a6176612f6c616e672f4f626a6563740700140021001300150000000000010009001000110001000f000000150002000100000009b200061208b6000eb1000000000000"
    println(actual)
    println(expected)
    check(expected.startsWith(actual))

    val loader = DynamicClassLoader(Thread.currentThread().contextClassLoader)
    val helloWorldClass = loader.define("HelloWorld", bytes)
    helloWorldClass.getMethod("main", Array<String>::class.java).invoke(null, null)
}
/*
https://medium.com/@davethomas_9528/writing-hello-world-in-java-byte-code-34f75428e0ad
 */
