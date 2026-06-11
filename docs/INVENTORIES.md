# Inventories

Load menus from YAML:

```java
lib.inventories().placeholders((player, menuId, itemId) ->
  Placeholders.of("player", player.getName()).put("rotation_time", "12:00")
);
lib.inventories().load(new File(getDataFolder(), "menus.yml"));
lib.inventories().open(player, "RotatingShop");
```

Menu keys such as `ResetTimes`, `Rarities`, prices, stocks, and custom sections may exist in YAML. CastielLib consumes item display/click data and leaves domain-specific sections available for your plugin logic.
