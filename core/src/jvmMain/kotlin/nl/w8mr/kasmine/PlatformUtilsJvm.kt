package nl.w8mr.kasmine

/** JVM implementation of the format function. */
actual fun String.format(vararg args: Any?): String = java.lang.String.format(this, *args)

/** JVM implementation of the merge function. */
actual fun <K, V> MutableMap<K, V>.merge(key: K, value: V, remappingFunction: (V, V) -> V): V? {
    // For JVM, we can use the built-in merge function
    val oldValue = this[key]
    val newValue =
        if (oldValue == null) {
            value
        } else {
            remappingFunction(oldValue, value)
        }
    this[key] = newValue
    return newValue
}
