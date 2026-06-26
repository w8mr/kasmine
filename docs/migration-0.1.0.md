# Migration Guide: 0.0.5 → 0.1.0

## New Label / Block API

0.1.0 replaces `createTarget()` / `insertInstructionBlock()` / `instructionBlock { }` with a cleaner `label()` + lambda-based API.

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

### `insertInstructionBlock()` → `label { }`

**Before (0.0.5):**

```kotlin
val loop = createTarget()
val end = createTarget()

insertInstructionBlock(loop)
loadConstant(0)
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

loadConstant(0)
istore("i")
goto(loop)

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

`instructionBlock { }` wrappers are no longer needed. Write instructions directly in the method scope:

**Before:**

```kotlin
insertInstructionBlock(target)
instructionBlock {
    loadConstant(42)
    ireturn()
}
```

**After:**

```kotlin
target {
    loadConstant(42)
    ireturn()
}
```

### Lazy (lambda) refs

Branch instructions accept either a `BlockRef` directly or a lambda `{ BlockRef }`:

```kotlin
// Direct
ifequal(end)

// Lambda (lazy — useful for forward references)
ifequal { end }
```

## What Changed

| Old (0.0.5) | New (0.1.0) | Status |
|---|---|---|
| `createTarget()` | `label()` | **Replaced** |
| `insertInstructionBlock(target)` | `target { }` | **Replaced** |
| `instructionBlock { }` | direct method scope | **Removed** |
| `block { }` | `label()` + `label { }` | **Removed** |
| `self` | — | **Removed** |
| `goto(target)`, `ifequal(target)` (`InstructionBlock`) | `goto(ref)`, `ifequal(ref)` (`BlockRef` or lambda) | **Replaced** |

## Compatibility

0.0.5 code using the old API (`createTarget`, `insertInstructionBlock`, `instructionBlock { }`, `goto(InstructionBlock)`) will need updating. The changes are mechanical and covered in the examples above.
