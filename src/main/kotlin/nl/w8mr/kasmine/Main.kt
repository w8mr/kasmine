package nl.w8mr.kasmine

import jogamp.graph.font.typecast.ot.table.ClassDef
import java.io.ByteArrayOutputStream

fun String.decodeHex(): ByteArray {
    val trimmed = this.replace(Regex("\\s+"), "")
    check(trimmed.length % 2 == 0) { "Must have an even length" }

    return trimmed.chunked(2)
        .map { it.toInt(16).toByte() }
        .toByteArray()
}

fun ByteArray.toHex(): String = joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

sealed class ConstantPoolType {

    data class UTF8String(val value: String) : ConstantPoolType()
    data class ClassEntry(val nameRef: UTF8String) : ConstantPoolType()
    data class ConstantString(val nameRef: UTF8String) : ConstantPoolType()
    data class NameAndType(val nameRef: UTF8String, val typeRef: UTF8String) : ConstantPoolType()
    data class FieldRef(val classRef: ClassEntry, val nameAndTypeRef: NameAndType) : ConstantPoolType()
    data class MethodRef(val classRef: ClassEntry, val nameAndTypeRef: NameAndType) : ConstantPoolType()
}

data class ClassDef(val access: UShort, val classRef: ConstantPoolType.ClassEntry, val superClassRef: ConstantPoolType.ClassEntry)

class ByteCodeWriter() {
    private val out = ByteArrayOutputStream()
    private var constantPoolIndex: UShort = 1u

    operator fun String.unaryPlus() {
        out.writeBytes(this.decodeHex())
    }

    fun ubyte(value: UShort) {
        assert(value <= UByte.MAX_VALUE)
        ubyte(value.toUByte())
    }

    fun ubyte(value: Int) {
        assert(value <= UByte.MAX_VALUE.toInt())
        ubyte(value.toUByte())
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

    fun utf8String(value: String): UShort {
        +"01"
        val bytes = value.toByteArray(Charsets.UTF_8)
        ushort(bytes.size)
        out.write(bytes)
        val index = constantPoolIndex++
        return index
    }

    fun classEntry(value: String): UShort {
        +"07"
        val index = constantPoolIndex++
        ushort(constantPoolIndex)
        utf8String(value)
        return index
    }

    fun constantString(value: String): UShort {
        +"08"
        val index = constantPoolIndex++
        ushort(constantPoolIndex)
        utf8String(value)
        return index
    }

    fun fieldRef(classRef: UShort, nameAndTypeRef: UShort): UShort {
        +"09"
        val index = constantPoolIndex++
        ushort(classRef)
        ushort(nameAndTypeRef)
        return index
    }

    fun methodRef(classRef: UShort, nameAndTypeRef: UShort): UShort {
        +"0a"
        val index = constantPoolIndex++
        ushort(classRef)
        ushort(nameAndTypeRef)
        return index
    }

    fun NameAndType(name: String, type: String) {
        +"0C"
        val index = constantPoolIndex++
        ushort(constantPoolIndex)
        ushort((constantPoolIndex + 1u).toUShort())
        utf8String(name)
        utf8String(type)
    }

    fun getStatic(ref: UShort) {
        +"b2"
        ushort(ref)
    }

    fun loadConstant(ref: UShort) {
        +"12"
        ubyte(ref)
    }

    fun invokeVirtual(ref: UShort) {
        +"b6"
        ushort(ref)
    }

    fun ret() {
        +"b1"
    }

    fun fieldRefWithNameAndType(classRef: UShort, name: String, type: String): UShort {
        val index = fieldRef(classRef, (constantPoolIndex + 1u).toUShort())
        NameAndType(name, type)
        return index
    }

    fun methodRefWithNameAndType(classRef: UShort, name: String, type: String): UShort {
        val index = methodRef(classRef, (constantPoolIndex + 1u).toUShort())
        NameAndType(name, type)
        return index
    }

    fun toByteArray() =
        out.toByteArray()
}

class ClassBuilder() {
    val constantPool = mutableMapOf<ConstantPoolType, Int>()
    lateinit var classDef: nl.w8mr.kasmine.ClassDef

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

    fun classDef(access: UShort, classRef: ConstantPoolType.ClassEntry, superClassRef: ConstantPoolType.ClassEntry = classEntry("java/lang/Object")): nl.w8mr.kasmine.ClassDef {
        classDef = ClassDef(access, classRef, superClassRef)
        return classDef
    }

    fun classDef(access: UShort, className: String, superClassName: String = "java/lang/Object") =
        classDef(access, classEntry(className), classEntry(superClassName))

    private inline fun <reified T : ConstantPoolType> addToPool(element: T): T {
        constantPool.merge(element, 1, Int::plus)
        return element
    }
}

class DynamicClassLoader(parent: ClassLoader?) : ClassLoader(parent) {
    fun define(className: String?, bytecode: ByteArray): Class<*> {
        return super.defineClass(className, bytecode, 0, bytecode.size)
    }
}

fun main(vararg args: String) {
    with(ClassBuilder()) {
        //val cl = classEntry("HelloWorld")
        //val scl = classEntry("java/lang/Object")
        //   val sys = classEntry("java/lang/System")
        //   val ps = classEntry("java/io/PrintStream")
        val hello = constantString("Hello World")
        val out = fieldRef("java/lang/System", "out", "Ljava/io/PrintStream;")
        val println = methodRef("java/io/PrintStream", "println", "(Ljava/lang/String;)V")
        val main = utf8String("main")
        val mainSig = utf8String("([Ljava/lang/String;)V")
        val code = utf8String("Code")

        //methodDef()

        classDef(33u, "HelloWorld", "java/lang/Object")

        println(constantPool.size)
    }

    val out = ByteCodeWriter()

    with(out) {
        +"cafebabe"
        ushort(0) // minor version
        ushort(52) // major version
        ushort(22) // constantPoolSize + 1
        val cl = classEntry("HelloWorld")
        val scl = classEntry("java/lang/Object")
        val sys = classEntry("java/lang/System")
        val ps = classEntry("java/io/PrintStream")
        val hello = constantString("Hello World")
        val out = fieldRefWithNameAndType(sys, "out", "Ljava/io/PrintStream;")
        val println = methodRefWithNameAndType(ps, "println", "(Ljava/lang/String;)V")
        val main = utf8String("main")
        val mainSig = utf8String("([Ljava/lang/String;)V")
        val code = utf8String("Code")
        ushort(33) // Super Public
        ushort(cl) // main class
        ushort(scl) // main class super
        ushort(0u) // interface count
        ushort(0u) // field count
        ushort(1u) // method count
        ushort(9u) // Access modifier (public 0x001 static 0x008)
        ushort(main) // method name
        ushort(mainSig) // method signature
        ushort(1u) // attribute count
        ushort(code) // reference to Code attribute
        uint(21u) // code attribute bytes count
        ushort(2u) // max stack size
        ushort(1u) // max local var size
        uint(9u) // code block bytes count
        getStatic(out)
        loadConstant(hello)
        invokeVirtual(println)
        ret()
        ushort(0) // exception count
        ushort(0) // attribute count method
        ushort(0) // attribute count class
    }
    val actual = out.toByteArray().toHex()
    val expected =
        "cafebabe00000034001607000201000a48656c6c6f576f726c640700040100106a6176612f6c616e672f4f626a6563740700060100106a6176612f6c616e672f53797374656d0700080100136a6176612f696f2f5072696e7453747265616d08000a01000b48656c6c6f20576f726c64090005000c0c000d000e0100036f75740100154c6a6176612f696f2f5072696e7453747265616d3b0a000700100c001100120100077072696e746c6e010015284c6a6176612f6c616e672f537472696e673b29560100046d61696e010016285b4c6a6176612f6c616e672f537472696e673b2956010004436f646500210001000300000000000100090013001400010015000000150002000100000009b2000b1209b6000fb1000000000000"
    println(actual)
    println(expected)
    println(expected.startsWith(actual))

    val loader = DynamicClassLoader(Thread.currentThread().contextClassLoader)
    val helloWorldClass = loader.define("HelloWorld", out.toByteArray())
    helloWorldClass.getMethod("main", Array<String>::class.java).invoke(null, null)
}
/*
https://medium.com/@davethomas_9528/writing-hello-world-in-java-byte-code-34f75428e0ad
 */
