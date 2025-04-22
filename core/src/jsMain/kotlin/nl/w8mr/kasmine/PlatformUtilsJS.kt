package nl.w8mr.kasmine

/**
 * JavaScript implementation of the format function.
 */
actual fun String.format(vararg args: Any?): String {
    var result = this
    args.forEachIndexed { index, arg ->
        result = result.replace("%${index + 1}\$s", arg.toString())
            .replace("%s", arg.toString(), true)
            .replace("%${index + 1}\$d", arg.toString())
            .replace("%d", arg.toString(), true)
            .replace("%${index + 1}\$f", arg.toString())
            .replace("%f", arg.toString(), true)
    }

    // Handle %02x format specifically for ByteArray.toHex()
    if (result.contains("%02x") && args.isNotEmpty() && args[0] is Byte) {
        val byte = args[0] as Byte
        val hex = byte.toInt().and(0xFF).toString(16).padStart(2, '0')
        result = result.replace("%02x", hex)
    }

    return result
}

/**
 * JavaScript implementation of the merge function.
 */
actual fun <K, V> MutableMap<K, V>.merge(key: K, value: V, remappingFunction: (V, V) -> V): V? {
    val oldValue = this[key]
    val newValue = if (oldValue == null) {
        value
    } else {
        remappingFunction(oldValue, value)
    }
    this[key] = newValue
    return newValue
}
