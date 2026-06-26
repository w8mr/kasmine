package nl.w8mr.kasmine

import kotlin.test.Test
import kotlin.test.*

class FrameTest {

    @Test fun `push and pop round trip`() {
        val f = Frame()
        f.push(VerificationType.Integer)
        assertEquals(VerificationType.Integer, f.pop())
    }

    @Test fun `pop returns most recently pushed`() {
        val f = Frame()
        f.push(VerificationType.Integer)
        f.push(VerificationType.Float)
        assertEquals(VerificationType.Float, f.pop())
        assertEquals(VerificationType.Integer, f.pop())
    }

    @Test fun `pop from empty stack throws`() {
        val f = Frame()
        assertFails { f.pop() }
    }

    @Test fun `pop count returns reversed order`() {
        val f = Frame()
        f.push(VerificationType.Integer)
        f.push(VerificationType.Float)
        f.push(VerificationType.Double)
        val popped = f.pop(2)
        assertEquals(listOf(VerificationType.Float, VerificationType.Double), popped)
        assertEquals(1, f.stack.size)
    }

    @Test fun `setLocal stores at index`() {
        val f = Frame()
        f.setLocal(0, VerificationType.Integer)
        assertEquals(VerificationType.Integer, f.local(0))
    }

    @Test fun `setLocal adds Top to fill gaps`() {
        val f = Frame()
        f.setLocal(2, VerificationType.Float)
        assertEquals(VerificationType.Top, f.local(0))
        assertEquals(VerificationType.Top, f.local(1))
        assertEquals(VerificationType.Float, f.local(2))
    }

    @Test fun `local out of bounds returns Top`() {
        val f = Frame()
        assertEquals(VerificationType.Top, f.local(99))
    }

    @Test fun `clearStack empties the stack`() {
        val f = Frame()
        f.push(VerificationType.Integer)
        f.push(VerificationType.Float)
        f.clearStack()
        assertTrue(f.stack.isEmpty())
    }

    @Test fun `copy is independent`() {
        val f = Frame()
        f.setLocal(0, VerificationType.Integer)
        f.push(VerificationType.Float)
        val copy = f.copy()
        f.setLocal(0, VerificationType.Float)
        f.pop()
        assertEquals(VerificationType.Integer, copy.local(0))
        assertEquals(1, copy.stack.size)
    }

    @Test fun `merge identical frame returns false`() {
        val f = Frame()
        f.setLocal(0, VerificationType.Integer)
        val g = Frame()
        g.setLocal(0, VerificationType.Integer)
        assertFalse(f.merge(g))
        assertEquals(VerificationType.Integer, f.local(0))
    }

    @Test fun `merge different locals returns true and updates`() {
        val f = Frame()
        f.setLocal(0, VerificationType.Integer)
        val g = Frame()
        g.setLocal(0, VerificationType.Float)
        assertTrue(f.merge(g))
        assertEquals(VerificationType.Top, f.local(0))
    }

    @Test fun `merge with larger frame extends locals`() {
        val f = Frame()
        f.setLocal(0, VerificationType.Integer)
        val g = Frame()
        g.setLocal(2, VerificationType.Float)
        assertTrue(f.merge(g))
        assertEquals(3, f.locals.size)
        assertEquals(VerificationType.Top, f.local(1))
        assertEquals(VerificationType.Float, f.local(2))
    }

    @Test fun `merge Object with Null keeps Object`() {
        val f = Frame()
        f.setLocal(0, VerificationType.Object("Foo"))
        val g = Frame()
        g.setLocal(0, VerificationType.Null)
        assertFalse(f.merge(g))
        assertEquals(VerificationType.Object("Foo"), f.local(0))
    }

    @Test fun `pop count requirement fails when stack too small`() {
        val f = Frame()
        f.push(VerificationType.Integer)
        assertFails { f.pop(3) }
    }
}
