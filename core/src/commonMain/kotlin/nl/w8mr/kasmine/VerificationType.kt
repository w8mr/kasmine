package nl.w8mr.kasmine

sealed class VerificationType {
    object Top : VerificationType()
    object Integer : VerificationType()
    object Float : VerificationType()
    object Double : VerificationType()
    object Long : VerificationType()
    object Null : VerificationType()
    object UninitializedThis : VerificationType()
    data class Object(val className: String) : VerificationType() {
        override fun toString(): String = "Object($className)"
    }
    data class Uninitialized(val offset: UShort) : VerificationType() {
        override fun toString(): String = "Uninitialized($offset)"
    }

    fun tag(): Int = when (this) {
        Top -> 0
        Integer -> 1
        Float -> 2
        Double -> 3
        Long -> 4
        Null -> 5
        UninitializedThis -> 6
        is Object -> 7
        is Uninitialized -> 8
    }

    fun write(out: ByteCodeWriter, cpMap: Map<ConstantPoolType, Int>) {
        out.ubyte(tag().toUByte())
        when (this) {
            is Object -> {
                val idx = cpMap[ConstantPoolType.UTF8String(className)]
                require(idx != null) { "Class '$className' not in constant pool for stack map" }
                out.ushort(idx)
            }
            is Uninitialized -> out.ushort(offset.toInt())
            else -> {}
        }
    }

    fun merge(other: VerificationType): VerificationType {
        if (this == other) return this
        if (this is Top) return other
        if (other is Top) return this
        if (this is Object && other is Object) {
            return if (this.className == other.className) this
            else Object("java/lang/Object")
        }
        if (this is Object && other is Null) return this
        if (this is Null && other is Object) return other
        return Top
    }

    fun slots(): Int = when (this) {
        is Long, is Double -> 2
        else -> 1
    }

    companion object {
        fun fromFieldDescriptor(desc: String): VerificationType = when (desc.firstOrNull()) {
            'B', 'C', 'I', 'S', 'Z' -> Integer
            'F' -> Float
            'D' -> Double
            'J' -> Long
            'L' -> {
                val internal = desc.drop(1).dropLast(1)
                Object(internal)
            }
            '[' -> Object(desc)
            else -> Top
        }

        fun returnTypeFromMethodDescriptor(desc: String): VerificationType {
            val paren = desc.indexOf(')')
            val returnDesc = desc.substring(paren + 1)
            if (returnDesc.isEmpty() || returnDesc.first() == 'V') return Top
            return fromFieldDescriptor(returnDesc)
        }

        fun parameterTypesFromMethodDescriptor(desc: String): List<VerificationType> {
            val paren = desc.indexOf(')')
            val params = desc.substring(1, paren)
            val result = mutableListOf<VerificationType>()
            var i = 0
            while (i < params.length) {
                when (params[i]) {
                    'B', 'C', 'I', 'S', 'Z' -> {
                        result.add(Integer); i++
                    }
                    'F' -> { result.add(Float); i++ }
                    'D' -> { result.add(Double); i++ }
                    'J' -> { result.add(Long); i++ }
                    'L' -> {
                        val end = params.indexOf(';', i)
                        result.add(Object(params.substring(i + 1, end)))
                        i = end + 1
                    }
                    '[' -> {
                        var j = i
                        while (params[j] == '[') j++
                        if (params[j] == 'L') j = params.indexOf(';', j) + 1
                        else j++
                        result.add(Object(params.substring(i, j)))
                        i = j
                    }
                    else -> { result.add(Top); i++ }
                }
            }
            return result
        }
    }

    override fun toString(): String = when (this) {
        Top -> "Top"
        Integer -> "Integer"
        Float -> "Float"
        Double -> "Double"
        Long -> "Long"
        Null -> "Null"
        UninitializedThis -> "UninitializedThis"
        is Object -> "Object($className)"
        is Uninitialized -> "Uninitialized($offset)"
    }
}

class Frame(
    val locals: MutableList<VerificationType> = mutableListOf(),
    val stack: MutableList<VerificationType> = mutableListOf(),
) {
    fun local(index: Int): VerificationType =
        if (index < locals.size) locals[index] else VerificationType.Top

    fun setLocal(index: Int, type: VerificationType) {
        while (locals.size <= index) locals.add(VerificationType.Top)
        locals[index] = type
    }

    fun push(type: VerificationType) {
        stack.add(type)
    }

    fun pop(): VerificationType {
        check(stack.isNotEmpty()) { "Cannot pop from empty stack" }
        return stack.removeAt(stack.lastIndex)
    }

    fun pop(count: Int): List<VerificationType> {
        require(count <= stack.size) { "Cannot pop $count from stack of size ${stack.size}" }
        val result = mutableListOf<VerificationType>()
        repeat(count) { result.add(0, stack.removeAt(stack.lastIndex)) }
        return result
    }

    fun clearStack() {
        stack.clear()
    }

    fun copy(): Frame {
        val f = Frame(locals.toMutableList(), stack.toMutableList())
        return f
    }

    fun merge(other: Frame): Boolean {
        var changed = false
        val maxLocals = maxOf(locals.size, other.locals.size)
        while (locals.size < maxLocals) locals.add(VerificationType.Top)
        while (other.locals.size < maxLocals) other.locals.add(VerificationType.Top)
        for (i in 0 until maxLocals) {
            val merged = locals[i].merge(other.locals[i])
            if (merged != locals[i]) {
                locals[i] = merged
                changed = true
            }
        }
        return changed
    }
}
