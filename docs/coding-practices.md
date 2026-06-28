# Coding Practices

## Gradle & Build

### Yarn dependency resolution

**Prefer editing `yarn.lock` over `build.gradle.kts` resolutions.**
Resolutions in `build.gradle.kts` via `YarnRootExtension.resolution()` are the most explicit approach, but they create OR entries in `yarn.lock` (e.g. `webpack@5.101.3, webpack@5.104.1:`) that must be manually bumped when a newer patch is released.

Instead, patch `yarn.lock` directly:
1. Run `./gradlew :kotlinUpgradeYarnLock` with the resolution temporarily in place to get the patched version into the lockfile
2. Remove the gradle resolution
3. Edit the OR entry in `yarn.lock` back to just the original semver range
4. Run `./gradlew :kotlinUpgradeYarnLock` again to verify stability (no changes)

This way the lockfile uses the patched version under the original semver constraint, so future `yarn upgrade` picks up newer patches automatically. The patches can be removed entirely when upstream (Kotlin JS plugin) ships updated transitive dependencies.

### Maven publishing plugin

Apply `com.vanniktech.maven.publish` in the **subproject** `build.gradle.kts`, not in root. The root project only declares the version with `apply false`. The root project is not a Kotlin multiplatform project, so applying the plugin there produces a "No compatible plugin found" warning.

### Kotlin compiler version

Keep `languageVersion` and `apiVersion` at 2.1 even when using a newer compiler (2.4.0). This ensures downstream consumers on older Kotlin versions can depend on the published artifacts without breakage.
