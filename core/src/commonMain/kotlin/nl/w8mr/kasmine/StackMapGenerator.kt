package nl.w8mr.kasmine

data class FlatInstruction(val instruction: Instruction, val offset: Int, val blockIndex: Int)

data class StackMapEntry(val offsetDelta: Int, val frame: Frame)

class StackMapGenerator(
    private val blocks: List<InstructionBlock>,
    private val methodSig: String,
    private val isStatic: Boolean,
    private val thisClassName: String,
) {
    private val allInstructions: List<FlatInstruction> by lazy { flattenInstructions() }
    private val offsetToIndex: Map<Int, Int> by lazy { allInstructions.mapIndexed { i, fi -> fi.offset to i }.toMap() }

    var maxStack: Int = 0
        private set
    var maxLocals: Int = 0
        private set

    fun hasBranches(): Boolean = blocks.any { it.jumpRef != null || it.jumpTarget != null }

    private fun flattenInstructions(): List<FlatInstruction> {
        val result = mutableListOf<FlatInstruction>()
        var offset = 0
        blocks.forEachIndexed { bi, block ->
            block.instructions.forEach { inst ->
                result.add(FlatInstruction(inst, offset, bi))
                offset += inst.byteSize
            }
        }
        return result
    }

    private fun findTargetOffset(targetBlock: InstructionBlock): Int {
        var offset = 0
        for (block in blocks) {
            if (block === targetBlock) return offset
            offset += block.byteSize
        }
        error("Target block not found")
    }

    private fun branchTargets(): Set<Int> {
        val targets = mutableSetOf<Int>()
        blocks.forEach { block ->
            block.jumpTarget?.let { it.block?.let { b -> targets.add(findTargetOffset(b)) } }
            block.jumpRef?.let { lambda ->
                val ref = lambda()
                ref.block?.let { targets.add(findTargetOffset(it)) }
            }
        }
        return targets
    }

    private fun initialFrame(): Frame {
        val frame = Frame()
        if (!isStatic) {
            frame.setLocal(0, VerificationType.Object(thisClassName))
        }
        val params = VerificationType.parameterTypesFromMethodDescriptor(methodSig)
        var slot = if (isStatic) 0 else 1
        for (p in params) {
            frame.setLocal(slot, p)
            slot += p.slots()
        }
        return frame
    }

    fun generate(): List<StackMapEntry> {
        val framesAtTargets = mutableMapOf<Int, Frame>()
        val worklist = ArrayDeque<Int>()

        val initial = initialFrame()
        maxLocals = initial.locals.size
        framesAtTargets[0] = initial.copy()
        worklist.add(0)

        while (worklist.isNotEmpty()) {
            val startOffset = worklist.removeFirst()
            val currentFrame = framesAtTargets[startOffset] ?: error("No frame for offset $startOffset")
            simulate(currentFrame, startOffset, targets, framesAtTargets, worklist)
        }

        if (!hasBranches()) return emptyList()

        val sortedTargets = targets.filter { it > 0 }.sorted()
        val entries = mutableListOf<StackMapEntry>()
        var prevOffset = 0
        for (target in sortedTargets) {
            val frame = framesAtTargets[target] ?: continue
            val delta = if (prevOffset == 0) target else (target - prevOffset - 1)
            entries.add(StackMapEntry(delta, frame))
            prevOffset = target
        }
        return entries
    }

    private val targets: Set<Int> by lazy { branchTargets() }

    private fun simulate(
        startFrame: Frame,
        startOffset: Int,
        targets: Set<Int>,
        framesAtTargets: MutableMap<Int, Frame>,
        worklist: ArrayDeque<Int>,
    ) {
        val startIdx = offsetToIndex[startOffset] ?: return
        var frame = startFrame.copy()
        var currentOffset = startOffset
        maxStack = maxOf(maxStack, frame.stack.size)

        for (i in startIdx until allInstructions.size) {
            val fi = allInstructions[i]
            if (fi.offset != currentOffset) break

            val inst = fi.instruction

            if (fi.offset > startOffset && fi.offset in targets) {
                val existing = framesAtTargets[fi.offset]
                if (existing != null) {
                    if (existing.merge(frame)) {
                        worklist.add(fi.offset)
                    }
                } else {
                    framesAtTargets[fi.offset] = frame.copy()
                    worklist.add(fi.offset)
                }
            }

            val effect = computeEffect(inst, fi.offset)
            for (expectedPop in effect.pops) {
                if (frame.stack.isNotEmpty()) {
                    frame.stack.removeAt(frame.stack.lastIndex)
                }
            }
            for (push in effect.pushes) {
                frame.push(push)
                if (push.slots() == 2) {
                    frame.push(VerificationType.Top)
                }
            }
            maxStack = maxOf(maxStack, frame.stack.size)

            if (inst is Instruction.OneArgumentUByte) {
                val localIdx = inst.value.toInt()
                when (inst.opcode) {
                    Opcode.IStore -> {
                        frame.setLocal(localIdx, VerificationType.Integer)
                        maxLocals = maxOf(maxLocals, frame.locals.size)
                    }
                    Opcode.AStore -> {
                        frame.setLocal(localIdx, VerificationType.Top)
                        maxLocals = maxOf(maxLocals, frame.locals.size)
                    }
                    Opcode.ILoad -> {
                        if (frame.stack.isNotEmpty()) {
                            val localType = frame.local(localIdx)
                            if (localType !is VerificationType.Top) {
                                frame.stack[frame.stack.lastIndex] = localType
                            }
                        }
                    }
                    Opcode.ALoad -> {
                        if (frame.stack.isNotEmpty()) {
                            val localType = frame.local(localIdx)
                            if (localType !is VerificationType.Top) {
                                frame.stack[frame.stack.lastIndex] = localType
                            }
                        }
                    }
                    else -> {}
                }
            }

            currentOffset = fi.offset + inst.byteSize

            if (inst is Instruction.OneArgumentShort && inst.opcode == Opcode.Goto) {
                recordBranchTarget(inst, frame, framesAtTargets, worklist)
                return
            }

            if (inst is Instruction.OneArgumentShort && (inst.opcode == Opcode.IfNotEqual || inst.opcode == Opcode.IfEqual)) {
                recordBranchTarget(inst, frame, framesAtTargets, worklist)
                continue
            }

            if (inst is Instruction.NoArgument &&
                (inst.opcode == Opcode.Return || inst.opcode == Opcode.IReturn || inst.opcode == Opcode.AReturn)) {
                return
            }

            if (currentOffset >= allInstructions.last().offset + allInstructions.last().instruction.byteSize) {
                return
            }
        }
    }

    private fun recordBranchTarget(
        inst: Instruction.OneArgumentShort,
        frame: Frame,
        framesAtTargets: MutableMap<Int, Frame>,
        worklist: ArrayDeque<Int>,
    ) {
        for (block in blocks) {
            if (block.instructions.lastOrNull() !== inst) continue
            val targetBlock = block.jumpTarget?.block ?: block.jumpRef?.let { it().block } ?: continue
            val targetOff = findTargetOffset(targetBlock)
            val existing = framesAtTargets[targetOff]
            val targetFrame = frame.copy()
            if (existing != null) {
                if (existing.merge(targetFrame)) {
                    worklist.add(targetOff)
                }
            } else {
                framesAtTargets[targetOff] = targetFrame
                worklist.add(targetOff)
            }
        }
    }

    private fun computeEffect(inst: Instruction, offset: Int): TypeEffect = when {
        inst is Instruction.OneArgumentPool && inst.opcode == Opcode.New ->
            TypeEffect(emptyList(), listOf(VerificationType.Uninitialized(offset.toUShort())))

        inst is Instruction.OneArgumentPool && (inst.opcode == Opcode.InvokeVirtual || inst.opcode == Opcode.InvokeSpecial) -> {
            val methodRef = inst.value as? ConstantPoolType.MethodRef
            val returnType = methodRef?.let { VerificationType.returnTypeFromMethodDescriptor(it.nameAndTypeRef.typeRef.value) } ?: VerificationType.Top
            val paramTypes = methodRef?.let { VerificationType.parameterTypesFromMethodDescriptor(it.nameAndTypeRef.typeRef.value) } ?: emptyList()
            val allPops = mutableListOf<VerificationType>()
            allPops.addAll(paramTypes.reversed())
            allPops.add(VerificationType.Top)
            val pushes = if (returnType is VerificationType.Top) emptyList() else listOf(returnType)
            TypeEffect(allPops, pushes)
        }

        inst is Instruction.OneArgumentPool && inst.opcode == Opcode.InvokeStatic -> {
            val methodRef = inst.value as? ConstantPoolType.MethodRef
            val returnType = methodRef?.let { VerificationType.returnTypeFromMethodDescriptor(it.nameAndTypeRef.typeRef.value) } ?: VerificationType.Top
            val paramTypes = methodRef?.let { VerificationType.parameterTypesFromMethodDescriptor(it.nameAndTypeRef.typeRef.value) } ?: emptyList()
            TypeEffect(paramTypes.reversed(), if (returnType is VerificationType.Top) emptyList() else listOf(returnType))
        }

        inst is Instruction.OneArgumentPool && (inst.opcode == Opcode.GetStatic || inst.opcode == Opcode.GetField) -> {
            val fieldRef = inst.value as? ConstantPoolType.FieldRef
            val fieldType = fieldRef?.let { VerificationType.fromFieldDescriptor(it.nameAndTypeRef.typeRef.value) } ?: VerificationType.Top
            val pops = if (inst.opcode == Opcode.GetField) listOf(VerificationType.Top) else emptyList()
            TypeEffect(pops, listOf(fieldType))
        }

        inst is Instruction.OneArgumentPool && inst.opcode == Opcode.PutField -> {
            val fieldRef = inst.value as? ConstantPoolType.FieldRef
            val fieldType = fieldRef?.let { VerificationType.fromFieldDescriptor(it.nameAndTypeRef.typeRef.value) } ?: VerificationType.Top
            TypeEffect(listOf(fieldType, VerificationType.Top), emptyList())
        }

        inst is Instruction.OneArgumentPool && inst.opcode == Opcode.LoadConstant -> {
            val t = when (inst.value) {
                is ConstantPoolType.ConstantString -> VerificationType.Object("java/lang/String")
                is ConstantPoolType.ConstantInteger -> VerificationType.Integer
                else -> VerificationType.Top
            }
            TypeEffect(emptyList(), listOf(t))
        }

        else -> inst.typeEffect { VerificationType.Top }
    }

    fun writeStackMap(out: ByteCodeWriter, cpMap: Map<ConstantPoolType, Int>, entries: List<StackMapEntry>) {
        val smtUtf8 = ConstantPoolType.UTF8String("StackMapTable")

        val smtWriter = ByteCodeWriter()
        smtWriter.ushort(entries.size)
        for (entry in entries) {
            writeFrame(smtWriter, entry, cpMap)
        }
        val smtBytes = smtWriter.toByteArray()

        out.ushort(cpMap[smtUtf8]!!)
        out.uint(smtBytes.size.toUInt())
        out.write(smtBytes)
    }

    private fun writeFrame(out: ByteCodeWriter, entry: StackMapEntry, cpMap: Map<ConstantPoolType, Int>) {
        val locals = trimTrailingTop(entry.frame.locals)
        val stack = entry.frame.stack

        out.ubyte(255u)
        out.ushort(entry.offsetDelta)
        out.ushort(locals.size)
        for (local in locals) {
            local.write(out, cpMap)
        }
        out.ushort(stack.size)
        for (s in stack) {
            s.write(out, cpMap)
        }
    }

    private fun trimTrailingTop(locals: List<VerificationType>): List<VerificationType> {
        var end = locals.size
        while (end > 0 && locals[end - 1] is VerificationType.Top) {
            end--
        }
        return locals.subList(0, end)
    }
}
