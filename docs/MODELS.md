# Vanilla Models

Package:

```java
dev.castiel.lib.models
```

CastielLib can load and spawn vanilla-only custom models. No resource pack,
textures, CustomModelData mapping, pack hosting, or player pack prompt is used.

Supported loaders:

- CustomMobs-style YAML bones.
- CustomExtras Treasure flat model parts.
- Blockbench `.bbmodel` cube geometry with vanilla material mapping.

Example:

```java
Map<String, String> materials = new HashMap<>();
materials.put("crystal", "EMERALD_BLOCK");
materials.put("outer_shell", "DEEPSLATE");

ModelDefinition model = lib.models().loadBlockbench(file, materials, "STONE");
SpawnedModel spawned = lib.models().spawn(
        model,
        location,
        ModelSpawnOptions.defaults().persistent(true)
);
```

Driven mob models:

```java
SpawnedModel model = lib.models().spawnDriven(definition, mob, ModelSpawnOptions.defaults());

// In the plugin's own tick loop:
model.syncToDriver(mob);
```

Compatibility:

- Display entities are used only when the runtime server supports them.
- Older servers fail clearly unless `allowArmorStandFallback(true)` is set.
- ArmorStand fallback is degraded and cannot reproduce Display-entity per-axis
  cuboid scaling.

Blockbench notes:

- Geometry is imported; textures are not.
- Material mapping precedence is cube name, then group name, then texture name,
  then default material.
- Unsupported mesh features should be simplified to cubes before import.
