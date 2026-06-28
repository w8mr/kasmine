package nl.w8mr.kasmine

import kotlin.test.*
import kotlin.test.Test

class PlatformUtilsTest {

    @Test
    fun `String format substitutes single value`() {
        assertEquals("hello world", "hello %s".format("world"))
    }

    @Test
    fun `MutableMap merge adds new key`() {
        val map = mutableMapOf<String, Int>()
        map.merge("a", 1) { a, b -> a + b }
        assertEquals(1, map["a"])
    }

    @Test
    fun `MutableMap merge combines existing`() {
        val map = mutableMapOf("a" to 1)
        map.merge("a", 2) { a, b -> a + b }
        assertEquals(3, map["a"])
    }

    @Test
    fun `MutableMap merge returns merged value`() {
        val map = mutableMapOf("a" to 1)
        val result = map.merge("a", 2) { a, b -> a + b }
        assertEquals(3, result)
    }
}
