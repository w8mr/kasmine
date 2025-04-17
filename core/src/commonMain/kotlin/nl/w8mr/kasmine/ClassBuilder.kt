package nl.w8mr.kasmine

import kotlin.apply

fun classBuilder(init: ClassBuilder.ClassDSL.DSL.() -> Unit): ClassBuilder {
    val builder = ClassBuilder()
    builder.classDef(init)
    return builder
}

class ClassBuilder {
    private val constantPool = mutableMapOf<ConstantPoolType, Int>()
    lateinit var classDef: ClassDef

    private fun utf8String(value: String) = addToPool(ConstantPoolType.UTF8String(value))

    private fun classEntry(value: String) = addToPool(ConstantPoolType.ClassEntry(utf8String(value)))

    fun constantString(value: String) = addToPool(ConstantPoolType.ConstantString(utf8String(value)))

    fun constantInteger(value: Int) = addToPool(ConstantPoolType.ConstantInteger(value))

    private fun nameAndType(
        name: String,
        type: String,
    ) = addToPool(ConstantPoolType.NameAndType(utf8String(name), utf8String(type)))

    private fun fieldRef(
        classRef: ConstantPoolType.ClassEntry,
        nameAndType: ConstantPoolType.NameAndType,
    ) = addToPool(ConstantPoolType.FieldRef(classRef, nameAndType))

    private fun fieldRef(
        classRef: ConstantPoolType.ClassEntry,
        name: String,
        type: String,
    ) = fieldRef(classRef, nameAndType(name, type))

    fun fieldRef(
        className: String,
        fieldName: String,
        type: String,
    ) = fieldRef(classEntry(className), fieldName, type)

    private fun methodRef(
        classRef: ConstantPoolType.ClassEntry,
        nameAndType: ConstantPoolType.NameAndType,
    ) = addToPool(ConstantPoolType.MethodRef(classRef, nameAndType))

    private fun methodRef(
        classRef: ConstantPoolType.ClassEntry,
        methodName: String,
        type: String,
    ) = methodRef(classRef, nameAndType(methodName, type))

    fun methodRef(
        className: String,
        methodName: String,
        type: String,
    ) = methodRef(classEntry(className), methodName, type)

    private fun classDef(
        access: UShort,
        classRef: ConstantPoolType.ClassEntry,
        superClassRef: ConstantPoolType.ClassEntry =
            classEntry(
                "java/lang/Object",
            ),
        methods: List<MethodDef>,
    ): ClassDef {
        classDef = ClassDef(access, classRef, superClassRef, methods)
        return classDef
    }

    private fun classDef(
        access: UShort,
        className: String,
        superClassName: String = "java/lang/Object",
        methods: List<MethodDef>,
    ) = classDef(access, classEntry(className), classEntry(superClassName), methods)

    private fun methodDef(
        access: UShort,
        methodName: ConstantPoolType.UTF8String,
        methodSig: ConstantPoolType.UTF8String,
        instructions: List<InstructionBlock>,
    ) = MethodDef(access, methodName, methodSig, instructions)

    fun methodDef(
        access: UShort,
        methodName: String,
        methodSig: String,
        instructions: List<InstructionBlock>,
    ): MethodDef {
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

    private inline fun <reified T : ConstantPoolType> addToPool(element: T): T {
        constantPool.merge(element, 1, Int::plus)
        return element
    }

    fun write(): ByteArray {
        val out = ByteCodeWriter()

        with(out) {
            +"cafebabe"
            ushort(0) // minor version
            ushort(49) // major version
            ushort(constantPool.size + 1) // constantPoolSize + 1
            val cpMap =
                constantPool.entries.sortedByDescending { it.value }
                    .map { it.key }.withIndex().associate { it.value to (it.index + 1) }
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
                method.instructions.forEach { block -> block.instructions.forEach { it.write(instWriter, cpMap) } }
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

    inner class ClassDSL {
        val methods = mutableListOf<MethodDef>()

        inner class DSL {
            lateinit var name: String
            var access: UShort = 33u
            var superClass: String = "java/lang/Object"

            fun method(init: MethodDSL.DSL.() -> Unit) {
                val methodDsl = MethodDSL(this)
                val dsl = methodDsl.DSL()
                dsl.init()
                check(dsl.name.isNotEmpty())
                check(dsl.signature.isNotEmpty())
                recalculateJumps(methodDsl)
                val methodDef = methodDef(dsl.access, dsl.name, dsl.signature, methodDsl.instructionBlocks)
                methods.add(methodDef)
            }

            private fun recalculateJumps(methodDsl: MethodDSL) {
                methodDsl.instructionBlocks.forEachIndexed { index, block ->
                    when (block.target) {
                        null -> {}
                        else -> {
                            val targetIndex = methodDsl.instructionBlocks.indexOfFirst { it == block.target }
                            val jump = if (targetIndex > index) {
                                ((index + 1 until targetIndex).sumOf { methodDsl.instructionBlocks[it].byteSize } + 3).toShort()
                            } else {
                                (-(targetIndex .. index).sumOf { methodDsl.instructionBlocks[it].byteSize } + 3).toShort()
                            }
                            when (val inst = block.instructions.last()) {
                                is Instruction.OneArgumentShort -> {
                                    when (inst.opcode) {
                                        Opcode.Goto, Opcode.IfNotEqual, Opcode.IfEqual -> block.instructions[block.instructions.size - 1] =
                                            inst.copy(value = jump)

                                        else -> error("should be jump")
                                    }
                                }
                                else -> error("should be jump")
                            }
                        }
                    }
                }
            }
        }
    }

    inner class MethodDSL(val parentDSL: ClassDSL.DSL) {
        val instructionBlocks = mutableListOf<InstructionBlock>()
        var currentBlock: InstructionBlock? = null

        inner class DSL {
            val parent: ClassDSL.DSL get() = parentDSL
            lateinit var name: String
            lateinit var signature: String
            var access: UShort = 9u

            private val localVarMap = mutableMapOf<String, UByte>()

            private fun localVar(name: String): UByte {
                return localVarMap.getOrPut(name) { localVarMap.size.toUByte() }
            }

            private fun add(instruction: Instruction) {
                check(currentBlock?.target == null) { "Cannot add instruction to block which already has a target block"}
                val block = when (val block = currentBlock) {
                    null -> InstructionBlock().apply { instructionBlocks.add(this) }
                    else -> block
                }
                block.add(instruction)
                currentBlock = block
            }

            fun createTarget() = InstructionBlock()
            fun insertInstructionBlock(codeBlock: InstructionBlock) {
                instructionBlocks.add(codeBlock)
                currentBlock = codeBlock
            }
            private fun addJump(instruction: Instruction, targetBlock: InstructionBlock = InstructionBlock()): InstructionBlock {
                val block = when (val block = currentBlock) {
                    null -> InstructionBlock().apply { instructionBlocks.add(this) }
                    else -> block
                }
                block.add(instruction)
                block.target = targetBlock
                currentBlock = null
                return targetBlock
            }

            private fun getStatic(field: ConstantPoolType.FieldRef) = add(Instruction.OneArgumentPool(Opcode.GetStatic, field))

            fun getStatic(
                className: String,
                fieldName: String,
                type: String,
            ) = getStatic(fieldRef(className, fieldName, type))

            private fun loadConstant(constant: ConstantPoolType) = add(Instruction.OneArgumentPool(Opcode.LoadConstant, constant))

            private fun iconstm1() = add(Instruction.NoArgument(Opcode.IConstM1))

            private fun iconst0() = add(Instruction.NoArgument(Opcode.IConst0))

            private fun iconst1() = add(Instruction.NoArgument(Opcode.IConst1))

            private fun iconst2() = add(Instruction.NoArgument(Opcode.IConst2))

            private fun iconst3() = add(Instruction.NoArgument(Opcode.IConst3))

            private fun iconst4() = add(Instruction.NoArgument(Opcode.IConst4))

            private fun iconst5() = add(Instruction.NoArgument(Opcode.IConst5))

            private fun bipush(value: Byte) = add(Instruction.OneArgumentByte(Opcode.BiPush, value))

            private fun sipush(value: Short) = add(Instruction.OneArgumentShort(Opcode.SiPush, value))

            fun loadConstant(string: String) = loadConstant(constantString(string))

            fun loadConstant(value: Char) = loadConstant(value.code)

            fun loadConstant(value: Int) =
                when (value) {
                    0 -> iconst0()
                    1 -> iconst1()
                    2 -> iconst2()
                    3 -> iconst3()
                    4 -> iconst4()
                    5 -> iconst5()
                    -1 -> iconstm1()
                    in 6..127 -> bipush(value.toByte())
                    in -128..-2 -> bipush(value.toByte())
                    in 128..32767 -> sipush(value.toShort())
                    in -32768..-129 -> sipush(value.toShort())
                    else -> loadConstant(constantInteger(value))
                }

            private fun invokeVirtual(method: ConstantPoolType.MethodRef) = add(Instruction.OneArgumentPool(Opcode.InvokeVirtual, method))

            fun invokeVirtual(
                className: String,
                methodName: String,
                type: String,
            ) = invokeVirtual(methodRef(className, methodName, type))

            private fun invokeStatic(method: ConstantPoolType.MethodRef) = add(Instruction.OneArgumentPool(Opcode.InvokeStatic, method))

            fun invokeStatic(
                className: String,
                methodName: String,
                type: String,
            ) = invokeStatic(methodRef(className, methodName, type))

            fun `return`() = add(Instruction.NoArgument(Opcode.Return))

            fun ireturn() = add(Instruction.NoArgument(Opcode.IReturn))

            fun areturn() = add(Instruction.NoArgument(Opcode.AReturn))

            fun astore(identifier: String) = add(Instruction.OneArgumentUByte(Opcode.AStore, localVar(identifier)))

            fun aload(identifier: String) = add(Instruction.OneArgumentUByte(Opcode.ALoad, localVar(identifier)))

            fun istore(identifier: String) = add(Instruction.OneArgumentUByte(Opcode.IStore, localVar(identifier)))

            fun iload(identifier: String) = add(Instruction.OneArgumentUByte(Opcode.ILoad, localVar(identifier)))

            fun dup() = add(Instruction.NoArgument(Opcode.Dup))

            fun pop() = add(Instruction.NoArgument(Opcode.Pop))

            fun ifnotequal(targetBlock: InstructionBlock) = addJump(Instruction.OneArgumentShort(Opcode.IfNotEqual, 0), targetBlock)
            fun ifnotequal(jump: Short) = add(Instruction.OneArgumentShort(Opcode.IfNotEqual, jump))

            fun ifequal(targetBlock: InstructionBlock) = addJump(Instruction.OneArgumentShort(Opcode.IfEqual, 0), targetBlock)
            fun ifequal(jump: Short) = add(Instruction.OneArgumentShort(Opcode.IfEqual, jump))

            fun goto(targetBlock: InstructionBlock) = addJump(Instruction.OneArgumentShort(Opcode.Goto, 0),targetBlock)
            fun goto(jump: Short) = add(Instruction.OneArgumentShort(Opcode.Goto, jump))
        }
    }
}


/*
https://medium.com/@davethomas_9528/writing-hello-world-in-java-byte-code-34f75428e0ad
 */
