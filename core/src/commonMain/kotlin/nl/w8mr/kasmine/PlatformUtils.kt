package nl.w8mr.kasmine

// Common expect declarations for platform-specific functions

/**
 * Format a string with the given arguments.
 * This is a platform-specific implementation of the format function.
 */
expect fun String.format(vararg args: Any?): String

/**
 * Merge a key-value pair into a map, using the remapping function if the key already exists.
 * This is a platform-specific implementation of the merge function.
 */
expect fun <K, V> MutableMap<K, V>.merge(key: K, value: V, remappingFunction: (V, V) -> V): V?