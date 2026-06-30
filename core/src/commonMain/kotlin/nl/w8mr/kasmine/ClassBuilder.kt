package nl.w8mr.kasmine

fun classBuilder(init: ClassBuilder.ClassDSL.DSL.() -> Unit): ClassBuilder {
    val builder = ClassBuilder()
    builder.classDef(init)
    return builder
}

@Suppress("TooManyFunctions")
class ClassBuilder {
    private val constantPool = mutableMapOf<ConstantPoolType, Int>()
    lateinit var classDef: ClassDef
    var classVersion: Int = 51

    private fun utf8String(value: String) = addToPool(ConstantPoolType.UTF8String(value))

    private fun classEntry(value: String) =
        addToPool(ConstantPoolType.ClassEntry(utf8String(value)))

    fun constantString(value: String) =
        addToPool(ConstantPoolType.ConstantString(utf8String(value)))

    fun constantInteger(value: Int) = addToPool(ConstantPoolType.ConstantInteger(value))

    fun constantFloat(value: Float) = addToPool(ConstantPoolType.ConstantFloat(value))

    fun constantLong(value: Long) = addToPool(ConstantPoolType.ConstantLong(value))

    fun constantDouble(value: Double) = addToPool(ConstantPoolType.ConstantDouble(value))

    private fun nameAndType(name: String, type: String) =
        addToPool(ConstantPoolType.NameAndType(utf8String(name), utf8String(type)))

    private fun fieldRef(
        classRef: ConstantPoolType.ClassEntry,
        nameAndType: ConstantPoolType.NameAndType,
    ) = addToPool(ConstantPoolType.FieldRef(classRef, nameAndType))

    private fun fieldRef(classRef: ConstantPoolType.ClassEntry, name: String, type: String) =
        fieldRef(classRef, nameAndType(name, type))

    fun fieldRef(className: String, fieldName: String, type: String) =
        fieldRef(classEntry(className), fieldName, type)

    private fun methodRef(
        classRef: ConstantPoolType.ClassEntry,
        nameAndType: ConstantPoolType.NameAndType,
    ) = addToPool(ConstantPoolType.MethodRef(classRef, nameAndType))

    private fun methodRef(classRef: ConstantPoolType.ClassEntry, methodName: String, type: String) =
        methodRef(classRef, nameAndType(methodName, type))

    fun methodRef(className: String, methodName: String, type: String) =
        methodRef(classEntry(className), methodName, type)

    private fun classDef(
        access: UShort,
        classRef: ConstantPoolType.ClassEntry,
        superClassRef: ConstantPoolType.ClassEntry = classEntry("java/lang/Object"),
        fields: List<FieldDef>,
        methods: List<MethodDef>,
    ): ClassDef {
        classDef = ClassDef(access, classRef, superClassRef, fields, methods)
        return classDef
    }

    private fun classDef(
        access: UShort,
        className: String,
        superClassName: String = "java/lang/Object",
        fields: List<FieldDef> = emptyList(),
        methods: List<MethodDef>,
    ) = classDef(access, classEntry(className), classEntry(superClassName), fields, methods)

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
        classVersion = dsl.version
        val fieldDefs =
            classDsl.rawFields.map { (access, name, type) ->
                FieldDef(access, utf8String(name), utf8String(type))
            }
        return classDef(dsl.access, dsl.name, dsl.superClass, fieldDefs, classDsl.methods)
    }

    private inline fun <reified T : ConstantPoolType> addToPool(element: T): T {
        constantPool.merge(element, 1, Int::plus)
        return element
    }

    fun write(): ByteArray {
        val out = ByteCodeWriter()

        val hasBranches =
            classDef.methods.any { m ->
                m.instructions.any { it.jumpRef != null || it.jumpTarget != null }
            }
        val needsStackMap = classVersion >= 50 && hasBranches
        if (needsStackMap) {
            utf8String("StackMapTable")
            for (method in classDef.methods) {
                val generator =
                    StackMapGenerator(
                        blocks = method.instructions,
                        methodSig = method.methodSig.value,
                        isStatic = (method.access.toInt() and 0x0008) != 0,
                        thisClassName = classDef.classRef.nameRef.value,
                    )
                for (entry in generator.generate()) {
                    for (local in entry.frame.locals) {
                        if (local is VerificationType.Object) classEntry(local.className)
                    }
                    for (s in entry.frame.stack) {
                        if (s is VerificationType.Object) classEntry(s.className)
                    }
                }
            }
        }

        with(out) {
            +"cafebabe"
            ushort(0) // minor version
            ushort(classVersion) // major version
            val cpMap = writeConstantPool()

            ushort(classDef.access) // Super Public
            ushort(cpMap[classDef.classRef]!!) // main class
            ushort(cpMap[classDef.superClassRef]!!) // main class super
            ushort(0u) // interface count
            ushort(classDef.fields.size) // field count
            classDef.fields.forEach { field ->
                ushort(field.access)
                ushort(cpMap[field.name]!!)
                ushort(cpMap[field.type]!!)
                ushort(0) // attribute count
            }
            ushort(classDef.methods.size) // method count
            classDef.methods.forEach { method -> writeMethod(method, cpMap, needsStackMap) }
            ushort(0) // attribute count class
        }

        return out.toByteArray()
    }

    private fun ByteCodeWriter.writeConstantPool(): MutableMap<ConstantPoolType, Int> {
        val sortedEntries = constantPool.entries.sortedByDescending { it.value }.map { it.key }
        val cpMap = mutableMapOf<ConstantPoolType, Int>()
        var idx = 1
        for (entry in sortedEntries) {
            cpMap[entry] = idx
            idx++
            if (
                entry is ConstantPoolType.ConstantLong || entry is ConstantPoolType.ConstantDouble
            ) {
                idx++
            }
        }
        ushort(idx) // constantPoolSize + 1
        cpMap.entries.sortedBy { it.value }.forEach { it.key.write(this, cpMap) }
        return cpMap
    }

    private fun ByteCodeWriter.writeMethod(
        method: MethodDef,
        cpMap: MutableMap<ConstantPoolType, Int>,
        needsStackMap: Boolean,
    ) {
        ushort(method.access)
        ushort(cpMap[method.methodName]!!) // main class super
        ushort(cpMap[method.methodSig]!!) // method signature
        ushort(1u) // attribute count method
        ushort(cpMap[ConstantPoolType.UTF8String("Code")]!!) // reference to Code attribute
        val instWriter = ByteCodeWriter()
        method.instructions.forEach { block ->
            block.instructions.forEach { it.write(instWriter, cpMap) }
        }
        val instBytes = instWriter.toByteArray()

        val generator =
            StackMapGenerator(
                blocks = method.instructions,
                methodSig = method.methodSig.value,
                isStatic = (method.access.toInt() and 0x0008) != 0,
                thisClassName = classDef.classRef.nameRef.value,
            )
        val smtEntries = generator.generate()
        val smtWriter = ByteCodeWriter()
        if (needsStackMap) {
            generator.writeStackMap(smtWriter, cpMap, smtEntries)
        }
        val smtBytes = smtWriter.toByteArray()
        val hasCodeAttr = smtBytes.isNotEmpty()
        val codeAttrLen = (12 + instBytes.size + smtBytes.size).toUInt()

        uint(codeAttrLen) // code attribute bytes count
        ushort(generator.maxStack)
        ushort(generator.maxLocals)
        uint(instBytes.size.toUInt()) // code block bytes count
        write(instBytes)
        ushort(0) // exception count
        if (hasCodeAttr) {
            ushort(1) // attribute count method
            write(smtBytes)
        } else {
            ushort(0) // attribute count method
        }
    }

    inner class ClassDSL {
        val methods = mutableListOf<MethodDef>()
        val rawFields = mutableListOf<Triple<UShort, String, String>>()

        inner class DSL {
            lateinit var name: String
            var access: UShort = 33u
            var superClass: String = "java/lang/Object"
            var version: Int = 51

            fun field(access: UShort = 2u, name: String, type: String) {
                rawFields.add(Triple(access, name, type))
            }

            fun method(init: MethodDSL.DSL.() -> Unit) {
                val methodDsl = MethodDSL(this)
                val dsl = methodDsl.DSL()
                dsl.init()
                check(dsl.name.isNotEmpty())
                check(dsl.signature.isNotEmpty())
                recalculateJumps(methodDsl)
                val methodDef =
                    methodDef(dsl.access, dsl.name, dsl.signature, methodDsl.instructionBlocks)
                methods.add(methodDef)
            }

            private fun recalculateJumps(methodDsl: MethodDSL) {
                methodDsl.instructionBlocks.forEachIndexed { index, block ->
                    block.jumpTarget?.let { targetRef ->
                        val targetBlock = targetRef.block ?: error("Unbound BlockRef")
                        val targetIndex =
                            methodDsl.instructionBlocks.indexOfFirst { it === targetBlock }
                        val jump =
                            calculateJumpOffset(index, targetIndex, methodDsl.instructionBlocks)
                        updateJumpInstruction(block, jump)
                    }
                    block.jumpRef?.let { targetLambda ->
                        val targetRef = targetLambda()
                        val targetBlock = targetRef.block ?: error("Unbound BlockRef")
                        val targetIndex =
                            methodDsl.instructionBlocks.indexOfFirst { it === targetBlock }
                        val jump =
                            calculateJumpOffset(index, targetIndex, methodDsl.instructionBlocks)
                        updateJumpInstruction(block, jump)
                    }
                }
            }

            private fun calculateJumpOffset(
                fromIndex: Int,
                toIndex: Int,
                blocks: List<InstructionBlock>,
            ): Short {
                return if (toIndex > fromIndex) {
                    ((fromIndex + 1 until toIndex).sumOf { blocks[it].byteSize } + 3).toShort()
                } else {
                    (-(toIndex..fromIndex).sumOf { blocks[it].byteSize } + 3).toShort()
                }
            }

            private fun updateJumpInstruction(block: InstructionBlock, jump: Short) {
                when (val inst = block.instructions.last()) {
                    is Instruction.OneArgumentShort -> {
                        when (inst.opcode) {
                            Opcode.Goto,
                            Opcode.IfNotEqual,
                            Opcode.IfEqual ->
                                block.instructions[block.instructions.size - 1] =
                                    inst.copy(value = jump)
                            else -> error("should be jump")
                        }
                    }
                    else -> error("should be jump")
                }
            }
        }
    }

    inner class MethodDSL(val parentDSL: ClassDSL.DSL) {
        val instructionBlocks = mutableListOf<InstructionBlock>()
        var currentBlock: InstructionBlock? = null

        @Suppress("TooManyFunctions")
        inner class DSL {
            val parent: ClassDSL.DSL
                get() = parentDSL

            lateinit var name: String
            lateinit var signature: String
            var access: UShort = 9u
            var self: BlockRef = BlockRef()
                private set

            private val localVarMap = mutableMapOf<String, UByte>()

            private fun localVar(name: String): UByte {
                return localVarMap.getOrPut(name) { localVarMap.size.toUByte() }
            }

            fun parameter(name: String) {
                localVar(name)
            }

            private fun add(instruction: Instruction) {
                val block = currentBlock ?: InstructionBlock().apply { instructionBlocks.add(this) }
                block.add(instruction)
                currentBlock = block
            }

            fun label(): BlockRef = BlockRef()

            inner class LabelDelegate(private val ref: BlockRef) {
                operator fun getValue(
                    thisRef: DSL?,
                    property: kotlin.reflect.KProperty<*>,
                ): BlockRef = ref
            }

            inner class LabelProvider {
                operator fun provideDelegate(
                    thisRef: DSL?,
                    property: kotlin.reflect.KProperty<*>,
                ): LabelDelegate = LabelDelegate(BlockRef())
            }

            val label: LabelProvider
                get() = LabelProvider()

            /**
             * Creates a new instruction block for this label and runs [init] inside it. After the
             * lambda the current block is restored to its previous value. Use [nextBlock] if you
             * need to break the restore and start a fresh block.
             */
            operator fun BlockRef.invoke(init: DSL.() -> Unit) {
                val ib = InstructionBlock()
                this.block = ib
                instructionBlocks.add(ib)
                val prevBlock = currentBlock
                val prevSelf = self
                currentBlock = ib
                self = this
                this@DSL.init()
                currentBlock = prevBlock
                self = prevSelf
            }

            /**
             * Ends the current block so the next instruction starts a fresh block. Use this after
             * label blocks ([BlockRef.invoke]) when subsequent instructions should not be appended
             * to the pre-label block.
             */
            fun nextBlock() {
                currentBlock = null
            }

            fun block(init: DSL.() -> Unit): BlockRef {
                val ref = BlockRef().also { it.block = InstructionBlock() }
                instructionBlocks.add(ref.block!!)
                val prevBlock = currentBlock
                val prevSelf = self
                currentBlock = ref.block
                self = ref
                this.init()
                currentBlock = prevBlock
                self = prevSelf
                return ref
            }

            private fun getStatic(field: ConstantPoolType.FieldRef) =
                add(Instruction.OneArgumentPool(Opcode.GetStatic, field))

            fun getStatic(className: String, fieldName: String, type: String) =
                getStatic(fieldRef(className, fieldName, type))

            private fun loadConstant(constant: ConstantPoolType) =
                add(Instruction.OneArgumentPool(Opcode.LoadConstant, constant))

            private fun loadConstant2W(constant: ConstantPoolType) =
                add(Instruction.OneArgumentPool(Opcode.LoadConstant2W, constant))

            private fun iconstm1() = add(Instruction.NoArgument(Opcode.IConstM1))

            private fun iconst0() = add(Instruction.NoArgument(Opcode.IConst0))

            private fun iconst1() = add(Instruction.NoArgument(Opcode.IConst1))

            private fun iconst2() = add(Instruction.NoArgument(Opcode.IConst2))

            private fun iconst3() = add(Instruction.NoArgument(Opcode.IConst3))

            private fun iconst4() = add(Instruction.NoArgument(Opcode.IConst4))

            private fun iconst5() = add(Instruction.NoArgument(Opcode.IConst5))

            private fun fconst0() = add(Instruction.NoArgument(Opcode.FConst0))

            private fun fconst1() = add(Instruction.NoArgument(Opcode.FConst1))

            private fun fconst2() = add(Instruction.NoArgument(Opcode.FConst2))

            private fun lconst0() = add(Instruction.NoArgument(Opcode.LConst0))

            private fun lconst1() = add(Instruction.NoArgument(Opcode.LConst1))

            private fun dconst0() = add(Instruction.NoArgument(Opcode.DConst0))

            private fun dconst1() = add(Instruction.NoArgument(Opcode.DConst1))

            private fun bipush(value: Byte) = add(Instruction.OneArgumentByte(Opcode.BiPush, value))

            private fun sipush(value: Short) =
                add(Instruction.OneArgumentShort(Opcode.SiPush, value))

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

            fun loadConstant(value: Float) =
                when {
                    value == 0.0f -> fconst0()
                    value == 1.0f -> fconst1()
                    value == 2.0f -> fconst2()
                    else -> loadConstant(constantFloat(value))
                }

            fun loadConstant(value: Long) =
                when {
                    value == 0L -> lconst0()
                    value == 1L -> lconst1()
                    else -> loadConstant2W(constantLong(value))
                }

            fun loadConstant(value: Double) =
                when {
                    value == 0.0 -> dconst0()
                    value == 1.0 -> dconst1()
                    else -> loadConstant2W(constantDouble(value))
                }

            private fun invokeVirtual(method: ConstantPoolType.MethodRef) =
                add(Instruction.OneArgumentPool(Opcode.InvokeVirtual, method))

            fun invokeVirtual(className: String, methodName: String, type: String) =
                invokeVirtual(methodRef(className, methodName, type))

            private fun invokeSpecial(method: ConstantPoolType.MethodRef) =
                add(Instruction.OneArgumentPool(Opcode.InvokeSpecial, method))

            fun invokeSpecial(className: String, methodName: String, type: String) =
                invokeSpecial(methodRef(className, methodName, type))

            private fun putField(field: ConstantPoolType.FieldRef) =
                add(Instruction.OneArgumentPool(Opcode.PutField, field))

            fun putField(className: String, fieldName: String, type: String) =
                putField(fieldRef(className, fieldName, type))

            private fun getField(field: ConstantPoolType.FieldRef) =
                add(Instruction.OneArgumentPool(Opcode.GetField, field))

            fun getField(className: String, fieldName: String, type: String) =
                getField(fieldRef(className, fieldName, type))

            private fun invokeStatic(method: ConstantPoolType.MethodRef) =
                add(Instruction.OneArgumentPool(Opcode.InvokeStatic, method))

            fun invokeStatic(className: String, methodName: String, type: String) =
                invokeStatic(methodRef(className, methodName, type))

            @Suppress("ktlint:standard:function-naming")
            private fun `new`(clazz: ConstantPoolType.ClassEntry) =
                add(Instruction.OneArgumentPool(Opcode.New, clazz))

            @Suppress("ktlint:standard:function-naming")
            fun `new`(className: String) = `new`(classEntry(className))

            fun `return`() = add(Instruction.NoArgument(Opcode.Return))

            fun ireturn() = add(Instruction.NoArgument(Opcode.IReturn))

            fun lreturn() = add(Instruction.NoArgument(Opcode.LReturn))

            fun freturn() = add(Instruction.NoArgument(Opcode.FReturn))

            fun dreturn() = add(Instruction.NoArgument(Opcode.DReturn))

            fun areturn() = add(Instruction.NoArgument(Opcode.AReturn))

            fun astore(identifier: String) =
                add(Instruction.OneArgumentUByte(Opcode.AStore, localVar(identifier)))

            fun aload(identifier: String) =
                add(Instruction.OneArgumentUByte(Opcode.ALoad, localVar(identifier)))

            fun istore(identifier: String) =
                add(Instruction.OneArgumentUByte(Opcode.IStore, localVar(identifier)))

            fun iload(identifier: String) =
                add(Instruction.OneArgumentUByte(Opcode.ILoad, localVar(identifier)))

            fun lstore(identifier: String) =
                add(Instruction.OneArgumentUByte(Opcode.LStore, localVar(identifier)))

            fun lload(identifier: String) =
                add(Instruction.OneArgumentUByte(Opcode.LLoad, localVar(identifier)))

            fun fstore(identifier: String) =
                add(Instruction.OneArgumentUByte(Opcode.FStore, localVar(identifier)))

            fun fload(identifier: String) =
                add(Instruction.OneArgumentUByte(Opcode.FLoad, localVar(identifier)))

            fun dstore(identifier: String) =
                add(Instruction.OneArgumentUByte(Opcode.DStore, localVar(identifier)))

            fun dload(identifier: String) =
                add(Instruction.OneArgumentUByte(Opcode.DLoad, localVar(identifier)))

            fun dup() = add(Instruction.NoArgument(Opcode.Dup))

            fun pop() = add(Instruction.NoArgument(Opcode.Pop))

            fun ifnotequal(jump: Short) = add(Instruction.OneArgumentShort(Opcode.IfNotEqual, jump))

            fun ifequal(jump: Short) = add(Instruction.OneArgumentShort(Opcode.IfEqual, jump))

            fun goto(jump: Short) = add(Instruction.OneArgumentShort(Opcode.Goto, jump))

            fun goto(target: BlockRef) =
                addJump(Instruction.OneArgumentShort(Opcode.Goto, 0)) { target }

            fun goto(target: () -> BlockRef) =
                addJump(Instruction.OneArgumentShort(Opcode.Goto, 0), target)

            fun ifequal(target: BlockRef) =
                addJump(Instruction.OneArgumentShort(Opcode.IfEqual, 0)) { target }

            fun ifequal(target: () -> BlockRef) =
                addJump(Instruction.OneArgumentShort(Opcode.IfEqual, 0), target)

            fun ifnotequal(target: BlockRef) =
                addJump(Instruction.OneArgumentShort(Opcode.IfNotEqual, 0)) { target }

            fun ifnotequal(target: () -> BlockRef) =
                addJump(Instruction.OneArgumentShort(Opcode.IfNotEqual, 0), target)

            private fun addJump(instruction: Instruction, target: () -> BlockRef) {
                val block = currentBlock ?: InstructionBlock().also { instructionBlocks.add(it) }
                block.add(instruction)
                val ref = target()
                if (ref.isBound) {
                    block.jumpTarget = ref
                } else {
                    block.jumpRef = target
                }
                currentBlock = null
            }
        }
    }
}
