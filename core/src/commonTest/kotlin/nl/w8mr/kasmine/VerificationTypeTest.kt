package nl.w8mr.kasmine

import kotlin.test.*
import kotlin.test.Test

class VerificationTypeTest {

    @Test
    fun `fromFieldDescriptor B C I S Z return Integer`() {
        listOf("B", "C", "I", "S", "Z").forEach { desc ->
            assertEquals(VerificationType.Integer, VerificationType.fromFieldDescriptor(desc))
        }
    }

    @Test
    fun `fromFieldDescriptor F returns Float`() {
        assertEquals(VerificationType.Float, VerificationType.fromFieldDescriptor("F"))
    }

    @Test
    fun `fromFieldDescriptor D returns Double`() {
        assertEquals(VerificationType.Double, VerificationType.fromFieldDescriptor("D"))
    }

    @Test
    fun `fromFieldDescriptor J returns Long`() {
        assertEquals(VerificationType.Long, VerificationType.fromFieldDescriptor("J"))
    }

    @Test
    fun `fromFieldDescriptor object type returns Object with internal name`() {
        assertEquals(
            VerificationType.Object("java/lang/String"),
            VerificationType.fromFieldDescriptor("Ljava/lang/String;"),
        )
    }

    @Test
    fun `fromFieldDescriptor array type returns Object with descriptor`() {
        assertEquals(VerificationType.Object("[I"), VerificationType.fromFieldDescriptor("[I"))
        assertEquals(
            VerificationType.Object("[[Ljava/lang/String;"),
            VerificationType.fromFieldDescriptor("[[Ljava/lang/String;"),
        )
    }

    @Test
    fun `fromFieldDescriptor void returns Top`() {
        assertEquals(VerificationType.Top, VerificationType.fromFieldDescriptor("V"))
    }

    @Test
    fun `fromFieldDescriptor empty string returns Top`() {
        assertEquals(VerificationType.Top, VerificationType.fromFieldDescriptor(""))
    }

    @Test
    fun `parameterTypesFromMethodDescriptor parses empty`() {
        assertTrue(VerificationType.parameterTypesFromMethodDescriptor("()V").isEmpty())
    }

    @Test
    fun `parameterTypesFromMethodDescriptor parses mixed types`() {
        val params = VerificationType.parameterTypesFromMethodDescriptor("(IJD)V")
        assertEquals(
            listOf(VerificationType.Integer, VerificationType.Long, VerificationType.Double),
            params,
        )
    }

    @Test
    fun `parameterTypesFromMethodDescriptor parses object and array`() {
        val params = VerificationType.parameterTypesFromMethodDescriptor("(ILFoo;D)V")
        assertEquals(
            listOf(
                VerificationType.Integer,
                VerificationType.Object("Foo"),
                VerificationType.Double,
            ),
            params,
        )
    }

    @Test
    fun `parameterTypesFromMethodDescriptor parses array params`() {
        val params =
            VerificationType.parameterTypesFromMethodDescriptor("([I[[Ljava/lang/String;)V")
        assertEquals(
            listOf(VerificationType.Object("[I"), VerificationType.Object("[[Ljava/lang/String;")),
            params,
        )
    }

    @Test
    fun `returnTypeFromMethodDescriptor returns Top for void`() {
        assertEquals(VerificationType.Top, VerificationType.returnTypeFromMethodDescriptor("()V"))
    }

    @Test
    fun `returnTypeFromMethodDescriptor returns Integer`() {
        assertEquals(
            VerificationType.Integer,
            VerificationType.returnTypeFromMethodDescriptor("()I"),
        )
    }

    @Test
    fun `returnTypeFromMethodDescriptor returns Object`() {
        assertEquals(
            VerificationType.Object("java/lang/String"),
            VerificationType.returnTypeFromMethodDescriptor("()Ljava/lang/String;"),
        )
    }

    @Test
    fun `tag values are correct`() {
        assertEquals(0, VerificationType.Top.tag())
        assertEquals(1, VerificationType.Integer.tag())
        assertEquals(2, VerificationType.Float.tag())
        assertEquals(3, VerificationType.Double.tag())
        assertEquals(4, VerificationType.Long.tag())
        assertEquals(5, VerificationType.Null.tag())
        assertEquals(6, VerificationType.UninitializedThis.tag())
        assertEquals(7, VerificationType.Object("x").tag())
        assertEquals(8, VerificationType.Uninitialized(0u).tag())
    }

    @Test
    fun `slots for Double and Long return 2`() {
        assertEquals(2, VerificationType.Double.slots())
        assertEquals(2, VerificationType.Long.slots())
    }

    @Test
    fun `slots for all other types return 1`() {
        assertEquals(1, VerificationType.Top.slots())
        assertEquals(1, VerificationType.Integer.slots())
        assertEquals(1, VerificationType.Float.slots())
        assertEquals(1, VerificationType.Null.slots())
        assertEquals(1, VerificationType.UninitializedThis.slots())
        assertEquals(1, VerificationType.Object("x").slots())
        assertEquals(1, VerificationType.Uninitialized(0u).slots())
    }

    @Test
    fun `merge same type returns same`() {
        assertEquals(
            VerificationType.Integer,
            VerificationType.Integer.merge(VerificationType.Integer),
        )
    }

    @Test
    fun `merge Top with X returns X`() {
        assertEquals(VerificationType.Integer, VerificationType.Top.merge(VerificationType.Integer))
    }

    @Test
    fun `merge X with Top returns X`() {
        assertEquals(VerificationType.Float, VerificationType.Float.merge(VerificationType.Top))
    }

    @Test
    fun `merge Object with Null returns Object`() {
        val obj = VerificationType.Object("Foo")
        assertEquals(obj, obj.merge(VerificationType.Null))
    }

    @Test
    fun `merge Null with Object returns Object`() {
        val obj = VerificationType.Object("Foo")
        assertEquals(obj, VerificationType.Null.merge(obj))
    }

    @Test
    fun `merge same Object classNames returns same`() {
        val obj = VerificationType.Object("Foo")
        assertEquals(obj, obj.merge(VerificationType.Object("Foo")))
    }

    @Test
    fun `merge different Object classNames returns java slash lang slash Object`() {
        val result = VerificationType.Object("Foo").merge(VerificationType.Object("Bar"))
        assertEquals(VerificationType.Object("java/lang/Object"), result)
    }

    @Test
    fun `merge incompatible types returns Top`() {
        assertEquals(VerificationType.Top, VerificationType.Integer.merge(VerificationType.Float))
    }

    @Test
    fun `toString returns expected format`() {
        assertEquals("Top", VerificationType.Top.toString())
        assertEquals("Integer", VerificationType.Integer.toString())
        assertEquals(
            "Object(java/lang/String)",
            VerificationType.Object("java/lang/String").toString(),
        )
        assertEquals("Uninitialized(42)", VerificationType.Uninitialized(42u).toString())
    }
}
