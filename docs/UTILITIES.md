# Utilities

These utility packages are based on repeated patterns across the commission plugins and are implemented generically in CastielLib.

Permissions:

```java
Permissions.require(sender, "customshop.sell.all", "&cNo permission.");
Permissions.hasWithAdmin(sender, "customauctions.sell", "customauctions.admin");
int limit = Permissions.highestNumbered(sender, "customshop.limit", 1, 100, 1);
```

Permission checks are manual: CastielLib reads effective permission attachments directly instead of calling `Command#setPermission(...)` or relying on Bukkit command metadata.
Command checks can also use a plugin-defined resolver with `lib.commands().permissionResolver(...)`.

Requirements:

```java
boolean allowed = Requirements.test(player, Arrays.asList(
  "permission: shop.vip",
  "!world: disabled_world",
  "gamemode: survival",
  "placeholder: %level% >= 10"
), Placeholders.of("level", 15));
```

Weighted rolls:

```java
Reward reward = new WeightedTable<Reward>()
  .add(commonReward, 80)
  .add(rareReward, 20)
  .roll();
```

Time:

```java
TimeFormats.formatSeconds(93784);       // 1d 2h 3m 4s
TimeFormats.parseMillis("1d 2h 30m");
TimeFormats.secondsUntilDaily("12:00", ZoneId.systemDefault());
```

Inventory and slots:

```java
List<Integer> slots = SlotParser.parse("10-16,19-25,28..34");
boolean fits = InventoryMath.canFit(player.getInventory(), item, 64);
InventoryMath.giveAtomic(player.getInventory(), item, 32);
```

Locations:

```java
String key = Locations.blockKey(block);      // world:x:y:z
String chunk = Locations.chunkKey(location); // world:x:z
Location parsed = Locations.parseBlockKey(key);
String keyed = Locations.keyedPrecise(location);       // namespace:key:x:y:z:yaw:pitch where available
Location parsedKeyed = Locations.parseFlexiblePrecise(keyed);
```

PDC string helpers:

```java
NamespacedKey key = PdcTags.key(plugin, "example_id");
PdcTags.setString(entity, key, "value");
String value = PdcTags.getString(itemMeta, key);
```
