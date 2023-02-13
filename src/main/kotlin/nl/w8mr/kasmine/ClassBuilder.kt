package nl.w8mr.kasmine

fun classBuilder(init: ClassBuilder.ClassDSL.DSL.() -> Unit): ClassBuilder {
    val builder = ClassBuilder()
    builder.classDef(init)
    return builder
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

    fun constantInteger(value: Int) =
        addToPool(ConstantPoolType.ConstantInteger(value))

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

    fun classDef(init: ClassDSL.DSL.() -> Unit): ClassDef {
        val classDsl = ClassDSL()
        val dsl = classDsl.DSL()
        dsl.init()
        check(dsl.name.isNotEmpty())
        return classDef(dsl.access, dsl.name, dsl.superClass, classDsl.methods)
    }

    val localVarMap = mutableMapOf<String, UByte>()
    fun localVar(name: String): UByte {
        return localVarMap.getOrPut(name) { localVarMap.size.toUByte() }
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

            ushort(classDef.access) // Super Public
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
                ushort(10u) // TODO: calc max stack size
                ushort(10u) // TODO: calc max local var size
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


    inner class ClassDSL() {
        val methods = mutableListOf<MethodDef>()

        inner class DSL() {
            lateinit var name : String
            var access: UShort = 33u
            var superClass: String = "java/lang/Object"
            fun method(init: MethodDSL.DSL.() -> Unit) {
                val methodDsl = MethodDSL()
                val dsl = methodDsl.DSL()
                dsl.init()
                check(dsl.name.isNotEmpty())
                check(dsl.signature.isNotEmpty())
                val methodDef = methodDef(dsl.access, dsl.name, dsl.signature, methodDsl.instructions)
                methods.add(methodDef)
            }
        }

    }

    inner class MethodDSL() {
        val instructions = mutableListOf<Instruction>()

        inner class DSL() {
            lateinit var name : String
            lateinit var signature : String
            var access: UShort = 9u

            private fun add(instruction: Instruction) {
                instructions.add(instruction)
            }
            fun getStatic(field: ConstantPoolType.FieldRef) = add(Instruction.OneArgument(Opcode.GetStatic, field))
            fun getStatic(className: String, fieldName: String, type: String) = getStatic(fieldRef(className, fieldName, type))

            fun loadConstant(constant: ConstantPoolType) = add(Instruction.OneArgument(Opcode.LoadConstant, constant))
            fun loadConstant(string: String) = loadConstant(constantString(string))

            fun loadConstant(value: Int) = loadConstant(constantInteger(value))

            fun invokeVirtual(method: ConstantPoolType.MethodRef) = add(Instruction.OneArgument(Opcode.InvokeVirtual, method))
            fun invokeVirtual(className: String, methodName: String, type: String) = invokeVirtual(methodRef(className, methodName, type))

            fun invokeStatic(method: ConstantPoolType.MethodRef) = add(Instruction.OneArgument(Opcode.InvokeStatic, method))
            fun invokeStatic(className: String, methodName: String, type: String) = invokeStatic(methodRef(className, methodName, type))

            fun ret() = add(Instruction.NoArgument(Opcode.Ret))

            fun astore(identifier: String) = add(Instruction.OneArgumentConst(Opcode.AStore, localVar(identifier)))
            fun aload(identifier: String) = add(Instruction.OneArgumentConst(Opcode.ALoad, localVar(identifier)))

            fun istore(identifier: String) = add(Instruction.OneArgumentConst(Opcode.IStore, localVar(identifier)))
            fun iload(identifier: String) = add(Instruction.OneArgumentConst(Opcode.ILoad, localVar(identifier)))
            fun dup() = add(Instruction.NoArgument(Opcode.Dup))
            fun pop() = add(Instruction.NoArgument(Opcode.Pop))


        }
    }


}