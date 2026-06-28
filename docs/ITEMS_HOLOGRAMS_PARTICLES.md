# Items, Holograms, and Particles

CustomCrate extends CastielLib with three reusable utility areas:

- `dev.castiel.lib.items.ConfigItem` builds configurable item stacks with material/head parsing, names, lore, custom model data, and glow.
- `dev.castiel.lib.world.BlockStyle` applies a normal material or base64 player-head texture to a block.
- `dev.castiel.lib.hologram.HologramManager` spawns multi-line holograms using TextDisplay when available and ArmorStand fallback otherwise.
- `dev.castiel.lib.effects.ParticleEffectEngine` renders reusable scheduled particle animations around fixed world locations.

These APIs intentionally keep modern Minecraft calls reflective where practical so plugins can compile against an older Paper API while still using newer runtime features on current servers.

Holograms can be updated in place by forcing a respawn under the same id:

```java
lib.holograms().show("boss:live", location, options, placeholders, true);
```
