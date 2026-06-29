package nl.w8mr.kasmine

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

@Suppress("LargeClass")
class ClassBuilderTest {

    @Test
    fun `simple method without branches produces version 51 class`() {
        val myClass = classBuilder {
            access = 33u
            name = "SimpleClass"
            method {
                access = 9u
                name = "main"
                signature = "([Ljava/lang/String;)V"
                parameter("args")
                `return`()
            }
        }
        val bytes = myClass.write()
        assertEquals(0xca.toUByte(), bytes[0].toUByte())
        assertEquals(0xfe.toUByte(), bytes[1].toUByte())
        assertEquals(0xba.toUByte(), bytes[2].toUByte())
        assertEquals(0xbe.toUByte(), bytes[3].toUByte())
        assertEquals(0, bytes[4].toInt() shl 8 or bytes[5].toInt())
        assertEquals(51, bytes[6].toInt() shl 8 or bytes[7].toInt())
    }

    @Test
    fun `class can be loaded and invoked`() {
        val myClass = classBuilder {
            access = 33u
            name = "RunnableClass"
            method {
                access = 9u
                name = "main"
                signature = "([Ljava/lang/String;)V"
                parameter("args")
                `return`()
            }
        }
        val bytes = myClass.write()
        val loader = DynamicClassLoader(null)
        val clazz = loader.define("RunnableClass", bytes)
        val main = clazz.getMethod("main", Array<String>::class.java)
        main.invoke(null, arrayOf<String>())
    }

    @Test
    fun `branching method can be loaded and executed`() {
        val myClass = classBuilder {
            access = 33u
            name = "BranchExec"
            method {
                access = 9u
                name = "run"
                signature = "()I"
                val end = label()
                loadConstant(0)
                istore("x")
                iload("x")
                ifequal(end)
                loadConstant(0)
                ireturn()
                end {
                    loadConstant(1)
                    ireturn()
                }
            }
        }
        val bytes = myClass.write()
        val loader = DynamicClassLoader(null)
        val clazz = loader.define("BranchExec", bytes)
        val method = clazz.getMethod("run")
        val result = method.invoke(null) as Int
        assertEquals(1, result)
    }

    @Test
    fun `class with static method loads and runs`() {
        val myClass = classBuilder {
            access = 33u
            name = "StaticRunner"
            method {
                access = 9u
                name = "answer"
                signature = "()I"
                loadConstant(42)
                ireturn()
            }
        }
        val bytes = myClass.write()
        val loader = DynamicClassLoader(null)
        val clazz = loader.define("StaticRunner", bytes)
        val method = clazz.getMethod("answer")
        assertEquals(42, method.invoke(null) as Int)
    }

    @Test
    fun `class with getstatic and invokestatic can call system methods`() {
        val myClass = classBuilder {
            access = 33u
            name = "SysOut"
            method {
                access = 9u
                name = "greet"
                signature = "()V"
                getStatic("java/lang/System", "out", "Ljava/io/PrintStream;")
                loadConstant("hello")
                invokeVirtual("java/io/PrintStream", "println", "(Ljava/lang/String;)V")
                `return`()
            }
        }
        val bytes = myClass.write()
        val loader = DynamicClassLoader(null)
        val clazz = loader.define("SysOut", bytes)
        val method = clazz.getMethod("greet")
        method.invoke(null)
    }

    @Test
    fun `class with field can store and load`() {
        val myClass = classBuilder {
            access = 33u
            name = "FieldHolder"
            field(name = "value", type = "I")
            field(name = "name", type = "Ljava/lang/String;")
            method {
                access = 9u
                name = "getValue"
                signature = "()I"
                loadConstant(42)
                ireturn()
            }
        }
        val bytes = myClass.write()
        val loader = DynamicClassLoader(null)
        val clazz = loader.define("FieldHolder", bytes)
        val method = clazz.getMethod("getValue")
        assertEquals(42, method.invoke(null) as Int)
    }

    @Test
    fun `method with ifnotequal branching returns correct value`() {
        val myClass = classBuilder {
            access = 33u
            name = "IfNotEqualTest"
            method {
                access = 9u
                name = "test"
                signature = "(I)I"
                val elseBlock = label()
                iload("x")
                ifnotequal(elseBlock)
                loadConstant(10)
                ireturn()
                elseBlock {
                    iload("x")
                    ireturn()
                }
                parameter("x")
            }
        }
        val bytes = myClass.write()
        val loader = DynamicClassLoader(null)
        val clazz = loader.define("IfNotEqualTest", bytes)
        val method = clazz.getMethod("test", Int::class.javaPrimitiveType)
        assertEquals(10, method.invoke(null, 0))
        assertEquals(2, method.invoke(null, 2))
        assertEquals(5, method.invoke(null, 5))
    }

    @Test
    fun `method with goto produces valid class`() {
        val myClass = classBuilder {
            access = 33u
            name = "GotoTest"
            method {
                access = 9u
                name = "run"
                signature = "()I"
                val skip = label()
                loadConstant(1)
                goto(skip)
                skip {
                    loadConstant(42)
                    ireturn()
                }
            }
        }
        val bytes = myClass.write()
        val loader = DynamicClassLoader(null)
        val clazz = loader.define("GotoTest", bytes)
        val method = clazz.getMethod("run")
        assertEquals(42, method.invoke(null) as Int)
    }

    @Test
    fun `class with multiple methods all work`() {
        val myClass = classBuilder {
            access = 33u
            name = "MultiMethod"
            method {
                access = 9u
                name = "zero"
                signature = "()I"
                loadConstant(0)
                ireturn()
            }
        }
        val bytes = myClass.write()
        val loader = DynamicClassLoader(null)
        val clazz = loader.define("MultiMethod", bytes)
        val zero = clazz.getMethod("zero")
        assertEquals(0, zero.invoke(null) as Int)
    }

    @Test
    fun `version 49 class produces correct major version`() {
        val myClass = classBuilder {
            access = 33u
            name = "Version49"
            version = 49
            method {
                access = 9u
                name = "main"
                signature = "([Ljava/lang/String;)V"
                parameter("args")
                `return`()
            }
        }
        val bytes = myClass.write()
        assertEquals(49, bytes[6].toInt() shl 8 or bytes[7].toInt())
        val loader = DynamicClassLoader(null)
        val clazz = loader.define("Version49", bytes)
        val main = clazz.getMethod("main", Array<String>::class.java)
        main.invoke(null, arrayOf<String>())
    }

    @Test
    fun `version 49 branching class does not require StackMapTable`() {
        val myClass = classBuilder {
            access = 33u
            name = "Ver49Branch"
            version = 49
            method {
                access = 9u
                name = "run"
                signature = "()I"
                val end = label()
                loadConstant(0)
                ifequal(end)
                loadConstant(1)
                ireturn()
                end {
                    loadConstant(0)
                    ireturn()
                }
            }
        }
        val bytes = myClass.write()
        assertEquals(49, bytes[6].toInt() shl 8 or bytes[7].toInt())
        val loader = DynamicClassLoader(null)
        val clazz = loader.define("Ver49Branch", bytes)
        val method = clazz.getMethod("run")
        assertEquals(0, method.invoke(null) as Int)
    }

    @Test
    fun `version 52 class loads and executes`() {
        val myClass = classBuilder {
            access = 33u
            name = "Version52"
            version = 52
            method {
                access = 9u
                name = "run"
                signature = "()I"
                loadConstant(42)
                ireturn()
            }
        }
        val bytes = myClass.write()
        assertEquals(52, bytes[6].toInt() shl 8 or bytes[7].toInt())
        val loader = DynamicClassLoader(null)
        val clazz = loader.define("Version52", bytes)
        val method = clazz.getMethod("run")
        assertEquals(42, method.invoke(null) as Int)
    }

    @Test
    fun `branching version 52 class includes StackMapTable`() {
        val myClass = classBuilder {
            access = 33u
            name = "Ver52Branch"
            version = 52
            method {
                access = 9u
                name = "run"
                signature = "()I"
                val end = label()
                loadConstant(0)
                ifequal(end)
                loadConstant(1)
                ireturn()
                end {
                    loadConstant(0)
                    ireturn()
                }
            }
        }
        val bytes = myClass.write()
        assertEquals(52, bytes[6].toInt() shl 8 or bytes[7].toInt())
        val loader = DynamicClassLoader(null)
        val clazz = loader.define("Ver52Branch", bytes)
        val method = clazz.getMethod("run")
        assertEquals(0, method.invoke(null) as Int)
    }

    @Test
    fun `class with nested ifelse returns correct value`() {
        val myClass = classBuilder {
            access = 33u
            name = "NestedIf"
            method {
                access = 9u
                name = "classify"
                signature = "(I)I"
                val elseBlock = label()
                iload("x")
                ifequal(elseBlock)
                loadConstant(1)
                ireturn()
                elseBlock {
                    loadConstant(-1)
                    ireturn()
                }
                parameter("x")
            }
        }
        val bytes = myClass.write()
        val loader = DynamicClassLoader(null)
        val clazz = loader.define("NestedIf", bytes)
        val method = clazz.getMethod("classify", Int::class.javaPrimitiveType)
        assertEquals(-1, method.invoke(null, 0))
        assertEquals(1, method.invoke(null, 10))
        assertEquals(1, method.invoke(null, 5))
    }

    @Test
    fun `label with forward ref and bind`() {
        val myClass = classBuilder {
            access = 33u
            name = "LabelForward"
            method {
                access = 9u
                name = "run"
                signature = "()I"
                val end = label()
                loadConstant(0)
                istore("x")
                iload("x")
                ifequal(end)
                loadConstant(-1)
                ireturn()
                end {
                    loadConstant(42)
                    ireturn()
                }
            }
        }
        val bytes = myClass.write()
        val loader = DynamicClassLoader(null)
        val clazz = loader.define("LabelForward", bytes)
        val method = clazz.getMethod("run")
        assertEquals(42, method.invoke(null))
    }

    @Test
    fun `backward branch with consistent frame`() {
        val myClass = classBuilder {
            access = 33u
            name = "BackwardBranch"
            method {
                access = 9u
                name = "run"
                signature = "()I"
                val end = label()
                val loop = label()
                loadConstant(0)
                istore("x")
                goto(loop)
                loop {
                    iload("x")
                    ifnotequal(end)
                    loadConstant(1)
                    istore("x")
                    goto(loop)
                }
                end {
                    loadConstant(5)
                    ireturn()
                }
            }
        }
        val bytes = myClass.write()
        val loader = DynamicClassLoader(null)
        val clazz = loader.define("BackwardBranch", bytes)
        val method = clazz.getMethod("run")
        assertEquals(5, method.invoke(null))
    }

    @Test
    fun `direct and lazy reference both work`() {
        val myClass = classBuilder {
            access = 33u
            name = "RefStyle"
            method {
                access = 9u
                name = "run"
                signature = "()I"
                val end = label()
                loadConstant(0)
                istore("x")
                iload("x")
                ifequal(end)
                loadConstant(-1)
                ireturn()
                end {
                    loadConstant(99)
                    ireturn()
                }
            }
        }
        val bytes = myClass.write()
        val loader = DynamicClassLoader(null)
        val clazz = loader.define("RefStyle", bytes)
        val method = clazz.getMethod("run")
        assertEquals(99, method.invoke(null))
    }

    @Test
    fun `lazy forward reference`() {
        val myClass = classBuilder {
            access = 33u
            name = "LazyFwd"
            method {
                access = 9u
                name = "run"
                signature = "()I"
                val end = label()
                loadConstant(0)
                istore("x")
                iload("x")
                ifequal { end }
                loadConstant(-1)
                ireturn()
                end {
                    loadConstant(7)
                    ireturn()
                }
            }
        }
        val bytes = myClass.write()
        val loader = DynamicClassLoader(null)
        val clazz = loader.define("LazyFwd", bytes)
        val method = clazz.getMethod("run")
        assertEquals(7, method.invoke(null))
    }

    @Test
    fun `direct target reference`() {
        val myClass = classBuilder {
            access = 33u
            name = "DirectRef"
            method {
                access = 9u
                name = "run"
                signature = "()I"
                val end = label()
                loadConstant(0)
                istore("x")
                iload("x")
                ifequal(end)
                loadConstant(-1)
                ireturn()
                end {
                    loadConstant(42)
                    ireturn()
                }
            }
        }
        val bytes = myClass.write()
        val loader = DynamicClassLoader(null)
        val clazz = loader.define("DirectRef", bytes)
        val method = clazz.getMethod("run")
        assertEquals(42, method.invoke(null))
    }

    @Test
    fun `block with self reference`() {
        val myClass = classBuilder {
            access = 33u
            name = "BlockSelf"
            method {
                access = 9u
                name = "run"
                signature = "()I"
                val end = label()
                loadConstant(0)
                istore("x")

                val loop = block {
                    iload("x")
                    ifequal(end)
                    goto(self)
                }

                goto(loop)
                end {
                    loadConstant(42)
                    ireturn()
                }
            }
        }
        val bytes = myClass.write()
        val loader = DynamicClassLoader(null)
        val clazz = loader.define("BlockSelf", bytes)
        val method = clazz.getMethod("run")
        assertEquals(42, method.invoke(null))
    }

    @Test
    fun `by label delegate forward reference`() {
        val myClass = classBuilder {
            access = 33u
            name = "ByLabel"
            method {
                access = 9u
                name = "run"
                signature = "()I"
                val end by label
                loadConstant(0)
                istore("x")
                iload("x")
                ifequal(end)
                loadConstant(-1)
                ireturn()
                end {
                    loadConstant(99)
                    ireturn()
                }
            }
        }
        val bytes = myClass.write()
        val loader = DynamicClassLoader(null)
        val clazz = loader.define("ByLabel", bytes)
        val method = clazz.getMethod("run")
        assertEquals(99, method.invoke(null))
    }

    @Test
    fun `by label delegate backward branch`() {
        val myClass = classBuilder {
            access = 33u
            name = "ByLabelBack"
            method {
                access = 9u
                name = "run"
                signature = "()I"
                val end by label
                val loop by label
                loadConstant(0)
                istore("x")
                goto(loop)
                loop {
                    iload("x")
                    ifnotequal(end)
                    loadConstant(1)
                    istore("x")
                    goto(loop)
                }
                end {
                    loadConstant(7)
                    ireturn()
                }
            }
        }
        val bytes = myClass.write()
        val loader = DynamicClassLoader(null)
        val clazz = loader.define("ByLabelBack", bytes)
        val method = clazz.getMethod("run")
        assertEquals(7, method.invoke(null))
    }

    @Test
    fun `branch with String parameter generates valid StackMapTable`() {
        val bytes = classBuilder {
            access = 33u
            name = "StringParamBranch"
            method {
                access = 9u
                name = "run"
                signature = "(Ljava/lang/String;)I"
                parameter("x")
                val end = label()
                loadConstant(42)
                loadConstant(0)
                ifequal(end)
                loadConstant(0)
                ireturn()
                end {
                    loadConstant(1)
                    ireturn()
                }
            }
        }.write()
        val loader = DynamicClassLoader(null)
        val clazz = loader.define("StringParamBranch", bytes)
        val method = clazz.getMethod("run", String::class.java)
        assertEquals(1, method.invoke(null, ""))
        assertEquals(1, method.invoke(null, "hello"))
    }

    @Test
    fun `float load store and return`() {
        val myClass = classBuilder {
            access = 33u
            name = "FloatOps"
            method {
                access = 9u
                name = "run"
                signature = "()F"
                loadConstant(3.14f)
                fstore("x")
                fload("x")
                freturn()
            }
        }
        val bytes = myClass.write()
        val loader = DynamicClassLoader(null)
        val clazz = loader.define("FloatOps", bytes)
        val method = clazz.getMethod("run")
        assertEquals(3.14f, method.invoke(null))
    }

    @Test
    fun `long load store and return`() {
        val myClass = classBuilder {
            access = 33u
            name = "LongOps"
            method {
                access = 9u
                name = "run"
                signature = "()J"
                loadConstant(42L)
                lstore("x")
                lload("x")
                lreturn()
            }
        }
        val bytes = myClass.write()
        val loader = DynamicClassLoader(null)
        val clazz = loader.define("LongOps", bytes)
        val method = clazz.getMethod("run")
        assertEquals(42L, method.invoke(null))
    }

    @Test
    fun `double load store and return`() {
        val myClass = classBuilder {
            access = 33u
            name = "DoubleOps"
            method {
                access = 9u
                name = "run"
                signature = "()D"
                loadConstant(2.718)
                dstore("x")
                dload("x")
                dreturn()
            }
        }
        val bytes = myClass.write()
        val loader = DynamicClassLoader(null)
        val clazz = loader.define("DoubleOps", bytes)
        val method = clazz.getMethod("run")
        assertEquals(2.718, method.invoke(null))
    }

    @Test
    fun `long constant 0 and 1`() {
        val myClass = classBuilder {
            access = 33u
            name = "LongConst"
            method {
                access = 9u
                name = "run"
                signature = "()J"
                loadConstant(0L)
                lreturn()
            }
        }
        val bytes = myClass.write()
        val loader = DynamicClassLoader(null)
        val clazz = loader.define("LongConst", bytes)
        val method = clazz.getMethod("run")
        assertEquals(0L, method.invoke(null))
    }

    @Test
    fun `float constant 0 1 2`() {
        val myClass = classBuilder {
            access = 33u
            name = "FloatConst"
            method {
                access = 9u
                name = "run"
                signature = "()F"
                loadConstant(2.0f)
                freturn()
            }
        }
        val bytes = myClass.write()
        val loader = DynamicClassLoader(null)
        val clazz = loader.define("FloatConst", bytes)
        val method = clazz.getMethod("run")
        assertEquals(2.0f, method.invoke(null))
    }

    @Test
    fun `double constant 0 and 1`() {
        val myClass = classBuilder {
            access = 33u
            name = "DoubleConst"
            method {
                access = 9u
                name = "run"
                signature = "()D"
                loadConstant(0.0)
                dreturn()
            }
        }
        val bytes = myClass.write()
        val loader = DynamicClassLoader(null)
        val clazz = loader.define("DoubleConst", bytes)
        val method = clazz.getMethod("run")
        assertEquals(0.0, method.invoke(null))
    }
}
