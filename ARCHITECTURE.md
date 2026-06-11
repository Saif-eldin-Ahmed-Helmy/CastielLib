# CastielLib Architecture

CastielLib is a Gradle Java library for Paper/Spigot plugins. Bind once in `onEnable`:

```java
CastielLib lib = CastielLib.bind(this);
```

Modules:

- `dev.castiel.lib.config`: annotation-backed YAML loader. Missing fields marked with `@ConfigNode` are injected into existing files with defaults.
- `dev.castiel.lib.database`: HikariCP wrapper for SQLite/MySQL. All query/update work is dispatched through `Bukkit.getScheduler().runTaskAsynchronously`.
- `dev.castiel.lib.actions`: config string action parser. Built-ins: `{console}`, `{player}`, `{message}`, `{broadcast}`, `{title}`, `{action}`, `{sound}`, `{close}`, `{particle}`.
- `dev.castiel.lib.inventory`: YAML menu builder with static decoration items, dynamic items, placeholders, click actions, custom model data, glow flags, and arbitrary extra YAML sections left available to plugin-specific code.
- `dev.castiel.lib.commands`: reflection command map registration with `@Command`, `@SubCommand`, `@Permission`, and simple argument binding.
- `dev.castiel.lib.text`: legacy, hex, solid, gradient, and rainbow color resolver with Iridium-style tags.
- `dev.castiel.lib.sounds`: XSeries-backed sound resolver for legacy and modern Bukkit sound names.

Compatibility strategy:

- Compile against Paper 1.16.5 API for a broad midpoint API.
- Resolve modern APIs like custom model data and action bars reflectively.
- Use XSeries at runtime for cross-version material/sound parsing, falling back to Bukkit enums when needed.
- Avoid raw threads; async work uses Bukkit scheduler.

Build:

```bash
gradle build
```
