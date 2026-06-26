# Migration Guide: 0.0.5 → 0.1.0

## New Label / Block API

0.1.0 introduces a cleaner control-flow API based on `label()` and `block { }`, replacing the `createTarget()` / `insertInstructionBlock()` pair. The old API still works — migration is optional.

### `createTarget()` → `label()`

**Before (0.0.5):**

```kotlin
val end = createTarget()
insertInstructionBlock(end)
```

**After (0.1.0):**

```kotlin
val end = label()
end { /* instructions at target */ }
```

### `insertInstructionBlock()` → `block { }` or `label { }`

**Before (0.0.5):**

```kotlin
val loop = createTarget()
val end = createTarget()

insertInstructionBlock(loop)
iconst0()
istore("i")

// loop body
insertInstructionBlock(loop)
iload("i")
iconst1()
iadd()
istore("i")
goto(loop)

insertInstructionBlock(end)
```

**After (0.1.0):**

```kotlin
val loop = label()
val end = label()

block {
    iconst0()
    istore("i")
    goto(loop)
}

loop {
    iload("i")
    iconst1()
    iadd()
    istore("i")
    goto(loop)
}

end { /* ... */ }
```

### `instructionBlock { }` wrappers

`instructionBlock { }` wrappers that exist solely to group code after `insertInstructionBlock` are no longer necessary. Use `block { }` for explicit grouping, or skip it entirely and let `label { }` hold the code:

**Before:**

```kotlin
val after = createTarget()
ifequal(after)
loadConstant(-1)
ireturn()
insertInstructionBlock(after)
instructionBlock {
    loadConstant(42)
    ireturn()
}
```

**After (equivalent):**

```kotlin
val after = label()
ifequal(after)
loadConstant(-1)
ireturn()
after {
    loadConstant(42)
    ireturn()
}
```

### `BlockRef` return value from `block { }`

`block { }` returns a `BlockRef` pointing to that block, so you can assign it in one step:

```kotlin
val loop = block {
    // loop body
}
// loop can now be used in branch instructions
goto(loop)
```

### `self` for self-referencing blocks

Inside a `block { }` or `label { }` body, `self` is a `BlockRef` pointing to the current block:

```kotlin
loop {
    iload("x")
    iconst1()
    iadd()
    istore("x")
    iload("x")
    iconst5()
    if_icmpeq { self }  // jump back to loop head
}
```

### Lazy (lambda) refs

Branch instructions accept either a `BlockRef` directly or a lambda `{ BlockRef }`:

```kotlin
// Direct
ifequal(end)
goto(end)

// Lambda (lazy)
ifequal { end }
goto { end }
```

The lambda style is useful for forward references where `end` is declared before its target block.

## What Changed

| Old (0.0.5) | New (0.1.0) | Status |
|---|---|---|
| `createTarget()` | `label()` | **New** — old API still works |
| `insertInstructionBlock(target)` | `target { }` or `block { }` | **New** — old API still works |
| `instructionBlock { }` | `block { }` (optional wrapper) | **Still works** |
| `goto(target)`, `ifequal(target)` | `goto(ref)`, `ifequal(ref)` + lambda overloads | **Extended** — old forms still work |
| — | `self` inside blocks | **New** |
| — | `block { }` returns `BlockRef` | **New** |

## Compatibility

All 0.0.5 code continues to compile and run unchanged under 0.1.0. There are no breaking changes to any existing API.
