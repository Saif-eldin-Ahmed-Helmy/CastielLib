# CastielLib

CastielLib is a Gradle-built Java utility library for Paper/Spigot plugin development. It packages the repeated boilerplate used across modern Minecraft plugins into one reusable foundation: commands, config loading, colors, actions, inventories, items, sounds, database access, holograms, particle effects, permissions, requirements, weighted rolls, time formatting, inventory math, and location serialization.

The library is designed for plugin code that needs to stay practical across older and newer server APIs. It compiles against Paper `1.16.5`, targets Java 8 bytecode, uses XSeries for cross-version names where useful, and uses reflection for newer APIs such as custom model data, TextDisplay holograms, and display transformations.

## Features

- Annotation command framework with `@Command`, `@SubCommand`, `@Permission`, argument binding, aliases, tab filtering, and stale command-map cleanup.
- Manual permission enforcement that does not use Bukkit command permission metadata.
- Initializable global no-permission message for all `@Permission` checks.
- Annotation-backed YAML config loading with automatic missing-key insertion.
- HikariCP-backed SQLite/MySQL access with all work scheduled asynchronously through Bukkit.
- Iridium-style colors: legacy codes, hex, `<SOLID>`, `<GRADIENT>`, and `<RAINBOW>`.
- Config action parser for commands, messages, broadcasts, titles, action bars, sounds, particles, and custom tags.
- YAML inventory parser plus programmatic managed and paginated menus.
- Configurable item stacks with XSeries material parsing, custom model data, lore, glow, and base64/url player heads.
- XSeries-backed sound helper.
- Holograms using TextDisplay when available and ArmorStand fallback otherwise.
- Scheduled particle effect engine with many built-in shapes/styles.
- Utility packages for permissions, requirements, weighted random selection, time, inventory math, slots, block styles, and location keys.

## Requirements

- Java 8-compatible runtime target.
- Gradle wrapper included.
- Paper/Spigot plugin project.
- XSeries and HikariCP are declared as Gradle API dependencies.

Build CastielLib:

```powershell
.\gradlew.bat build --no-daemon
```

Linux/macOS:

```bash
./gradlew build --no-daemon
```

Primary artifact:

```text
build/libs/CastielLib-1.0.0.jar
```

## Gradle

CastielLib itself uses:

```groovy
plugins {
    id "java-library"
    id "com.gradleup.shadow" version "8.3.6"
}

group = "dev.castiel"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
    withSourcesJar()
    withJavadocJar()
}

dependencies {
    compileOnly "com.destroystokyo.paper:paper-api:1.16.5-R0.1-SNAPSHOT"
    api "com.zaxxer:HikariCP:4.0.3"
    api "com.github.cryptomorin:XSeries:13.7.0"
}
```

If you use the built jar directly inside a plugin, put it on your compile classpath and shade it into the plugin jar, or load it as a separate plugin/library depending on your server architecture.

Example plugin-side shadow setup:

```groovy
dependencies {
    implementation files("libs/CastielLib-1.0.0.jar")
}

shadowJar {
    relocate "dev.castiel.lib", "your.plugin.libs.castiel"
}
```

## Bootstrap

Create one CastielLib instance in `onEnable`.

```java
public final class ExamplePlugin extends JavaPlugin {
    private CastielLib lib;

    @Override
    public void onEnable() {
        this.lib = CastielLib.bind(this);

        lib.commands()
                .noPermissionMessage("&cYou do not have permission.")
                .permissionResolver((sender, node) -> Permissions.hasWildcard(sender, node));

        lib.commands().register(new ExampleCommand());
    }

    @Override
    public void onDisable() {
        if (lib != null) {
            lib.shutdown();
        }
    }
}
```

`shutdown()` stops particle effects, removes holograms, and closes the database if one was configured.

## Core API

```java
CastielLib lib = CastielLib.bind(this);

lib.plugin();      // JavaPlugin
lib.configs();     // ConfigManager
lib.actions();     // ActionRegistry
lib.inventories(); // InventoryManager
lib.commands();    // CommandRegistry
lib.particles();   // ParticleEffectEngine
lib.holograms();   // HologramManager
lib.database();    // DatabaseManager after configuration
```

## Commands

Package:

```java
dev.castiel.lib.commands
```

Define commands with annotations:

```java
@Command(value = "castiel", aliases = {"clib"}, description = "CastielLib demo command.")
public final class CastielCommand {
    @SubCommand("")
    public void root(CommandSender sender) {
        sender.sendMessage(Colors.color("&aCastielLib is running."));
    }

    @SubCommand("give")
    @Permission("castiel.give")
    public void give(Player player, String targetName, int amount) {
        if (player == null) {
            return;
        }
        player.sendMessage(Colors.color("&aGiving " + amount + " to " + targetName));
    }

    @SubCommand("reload")
    @Permission(value = "castiel.admin", message = "&cOnly admins can reload.")
    public void reload(CommandSender sender) {
        sender.sendMessage(Colors.color("&aReloaded."));
    }
}
```

Register once:

```java
lib.commands().register(new CastielCommand());
```

Configure global no-permission handling once:

```java
lib.commands().noPermissionMessage("&cYou do not have permission.");
```

Replace the permission backend if needed:

```java
lib.commands().permissionResolver((sender, node) -> {
    if (!(sender instanceof Player)) {
        return true;
    }
    return Permissions.hasWildcard(sender, node);
});
```

Command behavior:

- `@Command` is placed on the command class.
- `@SubCommand("")` or bare `@SubCommand` is the root command.
- `@Permission` on the class applies to every subcommand.
- `@Permission` on a method overrides class-level permission for that subcommand.
- `@Permission(message = "...")` overrides the global no-permission message.
- If no global or annotation message is set, denial is silent.
- CastielLib removes stale command-map entries for the same command and aliases before registration.
- CastielLib does not call `Command#setPermission(...)`.
- CastielLib manually checks the selected subcommand permission before invoking the method.
- Tab completion hides subcommands that fail the resolver.

Supported argument bindings:

```java
CommandSender sender
Player player        // null when sender is not a player
String value
int value
Integer value
String[] remaining
@Sender CommandSender sender
```

## Permissions

Package:

```java
dev.castiel.lib.permissions
```

Helpers:

```java
boolean exact = Permissions.has(sender, "plugin.node");
boolean wildcard = Permissions.hasWildcard(sender, "plugin.node.deep");
boolean admin = Permissions.hasWithAdmin(sender, "plugin.sell", "plugin.admin");
boolean any = Permissions.hasAny(sender, "plugin.one", "plugin.two");
boolean all = Permissions.hasAll(sender, "plugin.one", "plugin.two");

if (!Permissions.require(sender, "plugin.use", "&cNo permission.")) {
    return;
}

int limit = Permissions.highestNumbered(sender, "customshop.limit", 1, 100, 1);
```

Manual permission behavior:

- Reads `sender.getEffectivePermissions()` directly.
- Exact permission values are checked first.
- Exact deny beats wildcard grant.
- Supports `*`, `plugin.*`, `plugin.section.*`, etc.
- Non-player senders are allowed by default.
- If no explicit effective permission exists, player op status is used as fallback.
- A custom command resolver can replace this behavior entirely.

## Configs

Package:

```java
dev.castiel.lib.config
```

Create a config class with defaults:

```java
public final class ShopSettings {
    @ConfigNode("Messages.Prefix")
    public String prefix = "&8[&bShop&8] ";

    @ConfigNode("Messages.No-Permission")
    public String noPermission = "%prefix%&cYou do not have permission.";

    @ConfigNode("Database.Type")
    public String databaseType = "SQLITE";

    @ConfigNode("Shop.Size")
    public int shopSize = 45;
}
```

Load it:

```java
ShopSettings settings = lib.configs().load("config.yml", ShopSettings.class);
```

Behavior:

- Loads from the plugin data folder.
- Creates missing files.
- Reads annotated fields from YAML paths.
- If a path is missing, writes the Java field default into the file.
- Existing values are preserved.

Direct YAML access:

```java
FileConfiguration config = lib.configs().yaml("menus.yml");
config.set("Example.Enabled", true);
lib.configs().save("menus.yml", config);
```

## Database

Package:

```java
dev.castiel.lib.database
```

SQLite:

```java
DatabaseManager database = new DatabaseManager(
        this,
        DatabaseSettings.sqlite(new File(getDataFolder(), "data.db"))
);

lib.database(database);
```

MySQL:

```java
lib.database(new DatabaseManager(
        this,
        DatabaseSettings.mysql("localhost", 3306, "minecraft", "user", "password")
));
```

Create tables:

```java
lib.database().update(
        "CREATE TABLE IF NOT EXISTS players(uuid VARCHAR(36) PRIMARY KEY, coins INT NOT NULL)",
        null
);
```

Insert/update:

```java
lib.database().update(
        "INSERT INTO players(uuid, coins) VALUES(?, ?) ON DUPLICATE KEY UPDATE coins = ?",
        statement -> {
            statement.setString(1, player.getUniqueId().toString());
            statement.setInt(2, 100);
            statement.setInt(3, 100);
        }
);
```

Query:

```java
CompletableFuture<List<Integer>> future = lib.database().query(
        "SELECT coins FROM players WHERE uuid = ?",
        statement -> statement.setString(1, player.getUniqueId().toString()),
        result -> result.getInt("coins")
);

future.thenAccept(values -> {
    int coins = values.isEmpty() ? 0 : values.get(0);
});
```

Rules:

- Uses HikariCP.
- SQLite pool size is `1`.
- Query/update methods return `CompletableFuture`.
- Database work is always scheduled through `Bukkit.getScheduler().runTaskAsynchronously`.
- No raw threads are created.
- Close through `lib.shutdown()` or `database.close()`.

## Colors

Package:

```java
dev.castiel.lib.text
```

Usage:

```java
player.sendMessage(Colors.color("&aGreen"));
player.sendMessage(Colors.color("&#55EFC4Mint"));
player.sendMessage(Colors.color("<SOLID:GOLD>Gold text</SOLID>"));
player.sendMessage(Colors.color("<GRADIENT:00FFFF>Gradient</GRADIENT:ADD8E6>"));
player.sendMessage(Colors.color("<RAINBOW>Rainbow</RAINBOW>"));
player.sendMessage(Colors.color("<RAINBOW:0.75>Soft rainbow</RAINBOW>"));
```

Supported syntax:

- `&a`, `&l`, `&r`, and other legacy codes.
- `&#RRGGBB`.
- `#{RRGGBB}`.
- `<SOLID:RRGGBB>text</SOLID>`.
- `<SOLID:GOLD>text</SOLID>`.
- `<SOLID:#RRGGBB>` as an inline color insertion.
- `<GRADIENT:FROM>text</GRADIENT:TO>`.
- `<RAINBOW>text</RAINBOW>`.
- `<RAINBOW:0.75>text</RAINBOW>`.
- Iridium-style `<RAINBOW75>text</RAINBOW>` saturation.

Named colors include:

```text
WHITE, BLACK, RED, GREEN, BLUE, YELLOW, CYAN, AQUA,
MAGENTA, PINK, ORANGE, GOLD, GRAY, GREY, DARK_GRAY, DARK_GREY
```

Compatibility:

- Uses Bungee `ChatColor.of(Color)` or `ChatColor.of(String)` if available.
- Falls back to closest legacy color where hex is not available.
- Preserves formatting codes inside gradients and rainbows.

## Placeholders

Package:

```java
dev.castiel.lib.util
```

Usage:

```java
Placeholders placeholders = Placeholders.empty()
        .put("player", player.getName())
        .put("price", 250)
        .put("stock", 10);

String line = placeholders.apply("&a%player% bought item for %price%");
```

Shortcut:

```java
Placeholders placeholders = Placeholders.of("player", player.getName());
```

Placeholders are literal `%key%` replacements.

## Actions

Package:

```java
dev.castiel.lib.actions
```

Run actions:

```java
List<String> actions = Arrays.asList(
        "{message} &aHello %player%",
        "{sound} ENTITY_PLAYER_LEVELUP",
        "{particle} END_ROD ring"
);

lib.actions().run(player, actions, Placeholders.of("player", player.getName()));
```

Built-in tags:

```text
{console} <command>
{player} <command>
{message} <text>
{broadcast} <text>
{title} <Title>;<Subtitle>
{action} <text>
{sound} <XSound or Bukkit Sound>
{close}
{particle} <Particle> <shape>
```

Examples:

```yaml
Actions:
  - "{console} give %player% diamond 1"
  - "{player} spawn"
  - "{message} <GRADIENT:00FFFF>Purchased!</GRADIENT:ADD8E6>"
  - "{broadcast} &e%player% bought a rare item."
  - "{title} &aSuccess;&7Purchase complete"
  - "{action} &a+%price% coins"
  - "{sound} ENTITY_PLAYER_LEVELUP"
  - "{particle} END_ROD ring"
  - "{close}"
```

Register custom actions:

```java
lib.actions().register("coins", (context, payload) -> {
    String parsed = context.placeholders.apply(payload);
    context.player.sendMessage(Colors.color("&eCoins action: " + parsed));
});
```

Then use:

```yaml
Actions:
  - "{coins} give %player% 100"
```

## Sounds

Package:

```java
dev.castiel.lib.sounds
```

Usage:

```java
Sound sound = Sounds.match("ENTITY_PLAYER_LEVELUP");
Sounds.play(player, "ENTITY_PLAYER_LEVELUP", 1.0f, 1.0f);
```

Behavior:

- Resolves through XSeries `XSound`.
- Falls back to Bukkit `Sound.valueOf`.
- Returns `false` when a sound cannot be resolved or no player is provided.

## YAML Inventories

Package:

```java
dev.castiel.lib.inventory
```

Load menus:

```java
lib.inventories().placeholders((player, menuId, itemId) -> {
    return Placeholders.empty()
            .put("player", player.getName())
            .put("rotation_time", "12:00")
            .put("personal_stock", 2);
});

lib.inventories().load(new File(getDataFolder(), "menus.yml"));
lib.inventories().open(player, "RotatingShop");
```

Example YAML:

```yaml
RotatingShop:
  Enabled: true
  Size: 45
  Title: "<GRADIENT:00FFFF>&lRotating Shop</GRADIENT:ADD8E6> &8| &fLimited Items"
  ResetTimes:
    - "00:00"
    - "12:00"
  Rarities:
    Awesome:
      Chance: 5
      DisplayName: "&6Awesome"
  DecorationItems:
    Rotating-Shop:
      Slot: 4
      Material: CLOCK
      Custom-Model-Data: 0
      Name: "<SOLID:55EFC4>&lBack To Main Shop</SOLID>"
      Lore:
        - "&8| &7Next Rotation: &#55EFC4%rotation_time%"
      Actions:
        - "{close}"
        - "{player} playtime shop"
  Items:
    Elytra:
      Slots: [20, 21, 22, 23, 24]
      Material: ELYTRA
      Custom-Model-Data: 0
      Name: "&8> &aWinged Freedom &8<"
      Lore:
        - "&8| &aPersonal Stock: %personal_stock%"
        - "&8| &eClick here to buy"
      Glow: true
      Actions:
        - "{console} give %player% elytra 1"
        - "{sound} ENTITY_PLAYER_LEVELUP"
        - "{particle} END_ROD ring"
```

Inventory behavior:

- `DecorationItems` and `Items` are parsed.
- `Slot` supports one slot.
- `Slots` supports multiple slots.
- `Material` is resolved through XSeries-backed item helpers.
- `Name` and `Lore` are passed through placeholders and `Colors.color`.
- `Actions` are run on click.
- Other YAML sections such as `ResetTimes`, `Rarities`, prices, stock, and custom plugin data are left for your plugin to interpret.

## Programmatic Menus

Create a simple managed menu:

```java
ItemStack close = new ConfigItem(
        "BARRIER",
        1,
        "&cClose",
        Collections.singletonList("&7Click to close."),
        0,
        false
).build();

ManagedMenu.create("example", 27, "&8Example Menu")
        .button(13, MenuButton.clickable(close, (player, event) -> player.closeInventory()))
        .open(player);
```

Decorative items:

```java
ManagedMenu.create("decor", 27, "&8Decor")
        .item(0, borderItem)
        .item(8, borderItem)
        .open(player);
```

Paginated menu:

```java
List<Integer> contentSlots = SlotParser.parse("10-16,19-25,28-34");

PaginatedMenu<String> menu = new PaginatedMenu<String>(
        "players",
        54,
        "&8Players &7(%page%/%pages%)",
        contentSlots,
        name -> new ConfigItem("PLAYER_HEAD", 1, "&a" + name, Collections.emptyList(), 0, false).build(),
        (player, name) -> player.sendMessage(Colors.color("&aClicked " + name))
);

menu.navigation(45, previousItem, 53, nextItem);
menu.open(player, playerNames, 0);
```

## Inventory Utilities

Slots:

```java
List<Integer> slots = SlotParser.parse("10-16,19-25,28..34");
List<Integer> rectangle = SlotParser.rectangle(10, 3, 7);
```

Inventory math:

```java
boolean canFit = InventoryMath.canFit(player.getInventory(), item, 64);
int count = InventoryMath.countSimilar(player.getInventory(), item);
int removed = InventoryMath.removeSimilar(player.getInventory(), item, 32);
Map<Integer, ItemStack> leftovers = InventoryMath.give(player.getInventory(), item, 64);
boolean allGiven = InventoryMath.giveAtomic(player.getInventory(), item, 64);
```

## Items

Package:

```java
dev.castiel.lib.items
```

Build an item from config-style data:

```java
ConfigItem item = new ConfigItem(
        "DIAMOND_SWORD",
        1,
        "<GRADIENT:00FFFF>&lStarter Sword</GRADIENT:ADD8E6>",
        Arrays.asList("&7Damage: &f10", "&eClick to claim"),
        0,
        true,
        true
);

ItemStack stack = item.build(Placeholders.of("player", player.getName()));
```

Read from YAML:

```yaml
Reward:
  Material: DIAMOND
  Amount: 3
  Name: "&bDiamonds"
  Lore:
    - "&7A shiny reward."
  Custom-Model-Data: 0
  Glow: true
  Hide-Attributes: true
```

```java
ConfigItem item = ConfigItem.from(section, "STONE", "&fItem");
ItemStack stack = item.build();
```

Player heads:

```java
ItemStack head1 = ItemStacks.playerHead("base64:<texture>", 1);
ItemStack head2 = ItemStacks.playerHead("texture:<texture>", 1);
ItemStack head3 = ItemStacks.playerHead("https://textures.minecraft.net/texture/...", 1);
```

Resolve materials:

```java
Material material = ItemStacks.resolveMaterial("ELYTRA", Material.STONE);
ItemStack stack = ItemStacks.fromMaterialOrTexture("DIAMOND", 32);
```

Compare a config item visually:

```java
boolean matches = item.visuallyMatches(player.getInventory().getItemInMainHand(), Placeholders.empty());
```

## Holograms

Package:

```java
dev.castiel.lib.hologram
```

Create from code:

```java
HologramOptions options = new HologramOptions(
        true,
        Arrays.asList("&b&lCrate", "&7Right click to open", "&e%keys% keys"),
        1.65,
        0.28,
        1.0f
);

lib.holograms().show(
        "crate:spawn",
        location,
        options,
        Placeholders.of("keys", 3)
);
```

Remove:

```java
lib.holograms().remove("crate:spawn");
lib.holograms().clear();
```

Read options from YAML:

```yaml
Hologram:
  Enabled: true
  Lines:
    - "&b&lCrate"
    - "&7Right click to open"
  Y-Offset: 1.65
  Line-Spacing: 0.28
  Scale: 1.0
```

```java
HologramOptions options = HologramOptions.from(section.getConfigurationSection("Hologram"));
```

Behavior:

- Uses TextDisplay where available.
- Falls back to invisible ArmorStand names.
- Refreshes around chunk loads, joins, teleports, and world changes.
- Clears on `lib.shutdown()`.

## Particle Effects

Package:

```java
dev.castiel.lib.effects
```

Start an effect:

```java
ParticleEffectConfig config = new ParticleEffectConfig(
        true,
        ParticleEffectStyle.GALAXY,
        "ENCHANTMENT_TABLE",
        "END_ROD",
        28,
        2,
        1.15,
        1.75,
        0.01,
        0.55
);

lib.particles().put("crate:spawn", block.getLocation(), config);
```

Remove/clear:

```java
lib.particles().remove("crate:spawn");
lib.particles().clear();
lib.particles().stop();
```

YAML:

```yaml
Effect:
  Enabled: true
  Style: GALAXY
  Particle: ENCHANTMENT_TABLE
  Secondary-Particle: END_ROD
  Density: 28
  Interval: 2
  Radius: 1.15
  Height: 1.75
  Speed: 0.01
  Y-Offset: 0.55
```

```java
ParticleEffectConfig config = ParticleEffectConfig.from(section.getConfigurationSection("Effect"));
```

Styles:

```text
HALO, RINGS, SPIRAL, DOUBLE_SPIRAL, HELIX, DNA, VORTEX, FOUNTAIN,
ORBIT, GALAXY, CROWN, BEACON, COMET, PULSE, AURORA, STAR, HEART,
CUBE, PYRAMID, NOVA, RAIN, SNOWGLOBE, FLAME_SWIRL, PORTAL,
ENCHANTED, TOTEM_BURST, DRIP, WAVE, CORKSCREW, FIREWORK
```

Performance behavior:

- One scheduler task renders all registered targets.
- Effects skip unloaded chunks.
- Effects render only if a player is within range.
- Stops and clears on `lib.shutdown()`.

## Action Particle Shapes

The action tag `{particle}` uses `ParticleShape`.

```yaml
Actions:
  - "{particle} END_ROD ring"
  - "{particle} FLAME sphere"
  - "{particle} VILLAGER_HAPPY point"
```

Supported simple shapes:

```text
point, ring, sphere
```

## Block Styles

Package:

```java
dev.castiel.lib.world.BlockStyle
```

Apply normal material:

```java
new BlockStyle("CHEST").apply(location, BlockFace.NORTH);
```

Apply textured head block:

```java
new BlockStyle("base64:<texture>").apply(location, BlockFace.SOUTH);
```

From YAML:

```yaml
Block:
  Material: CHEST
```

```java
BlockStyle style = BlockStyle.from(section.getConfigurationSection("Block"), "CHEST");
style.apply(block, player.getFacing());
```

Behavior:

- Normal materials are resolved through `ItemStacks.resolveMaterial`.
- Texture values become player heads.
- Directional/rotatable block data is oriented when possible.

## Locations

Package:

```java
dev.castiel.lib.world.Locations
```

Usage:

```java
String blockKey = Locations.blockKey(block);       // world:x:y:z
String blockKey2 = Locations.blockKey(location);   // world:x:y:z
String chunkKey = Locations.chunkKey(location);    // world:x:z
String precise = Locations.precise(location);      // world:x:y:z:yaw:pitch

Location blockLocation = Locations.parseBlockKey(blockKey);
Location preciseLocation = Locations.parsePrecise(precise);
```

These helpers are useful for crate locations, protected blocks, crop chunks, shop blocks, and serialized world positions.

## Requirements

Package:

```java
dev.castiel.lib.requirements
```

Evaluate config conditions:

```java
boolean allowed = Requirements.test(
        player,
        Arrays.asList(
                "permission: shop.vip",
                "!world: disabled_world",
                "gamemode: survival",
                "placeholder: %level% >= 10"
        ),
        Placeholders.of("level", 15)
);
```

Supported condition types:

```text
permission: node
perm: node
any-permission: node.one,node.two
any-perm: node.one,node.two
world: world_name
gamemode: survival
game-mode: adventure
op: true
placeholder: %value% >= 10
compare: some text contains thing
```

Inversion:

```text
!permission: blocked.node
!world: disabled_world
```

Comparison operators:

```text
>=, <=, ==, !=, >, <, contains, starts-with, ends-with
```

## Weighted Random

Package:

```java
dev.castiel.lib.random
```

Manual table:

```java
String reward = new WeightedTable<String>()
        .add("common", 80)
        .add("rare", 15)
        .add("legendary", 5)
        .roll();
```

Using `Weighted`:

```java
public final class Reward implements Weighted {
    private final String id;
    private final double chance;

    public Reward(String id, double chance) {
        this.id = id;
        this.chance = chance;
    }

    @Override
    public double weight() {
        return chance;
    }
}

Reward reward = WeightedTable.fromWeighted(rewards).roll();
```

Behavior:

- Zero and negative weights are ignored.
- `roll()` returns `null` when no positive entries exist.
- Useful for crates, rotating shops, rarity rolls, fishing loot, mob drops, and vote rewards.

## Time Formats

Package:

```java
dev.castiel.lib.time
```

Usage:

```java
String compact = TimeFormats.formatSeconds(93784);       // 1d 2h 3m 4s
String full = TimeFormats.formatSeconds(65, true);       // 0w 0d 0h 1m 5s
long millis = TimeFormats.parseMillis("1d 2h 30m");
long millis2 = TimeFormats.parseMillis("01:30:00");
long untilNoon = TimeFormats.secondsUntilDaily("12:00", ZoneId.systemDefault());
```

Supported duration units:

```text
w, d, h, m, s
```

Examples:

```text
1w 2d
3h 30m
45s
01:30:00
```

## Full Shop Example

```java
public final class ShopPlugin extends JavaPlugin {
    private CastielLib lib;
    private ShopSettings settings;

    @Override
    public void onEnable() {
        this.lib = CastielLib.bind(this);
        this.settings = lib.configs().load("config.yml", ShopSettings.class);

        lib.commands()
                .noPermissionMessage(settings.noPermission.replace("%prefix%", settings.prefix))
                .permissionResolver((sender, node) -> Permissions.hasWildcard(sender, node));

        lib.actions().register("coins", (context, payload) -> {
            context.player.sendMessage(Colors.color("&eCoins action: " + payload));
        });

        lib.inventories().placeholders((player, menuId, itemId) -> Placeholders.empty()
                .put("player", player.getName())
                .put("balance", 1000)
                .put("stock", 5));

        lib.inventories().load(new File(getDataFolder(), "menus.yml"));
        lib.commands().register(new ShopCommand(lib));
    }

    @Override
    public void onDisable() {
        if (lib != null) {
            lib.shutdown();
        }
    }
}
```

```java
@Command(value = "shop", aliases = {"shops"})
public final class ShopCommand {
    private final CastielLib lib;

    public ShopCommand(CastielLib lib) {
        this.lib = lib;
    }

    @SubCommand("")
    public void open(Player player) {
        if (player == null) {
            return;
        }
        lib.inventories().open(player, "MainShop");
    }

    @SubCommand("reload")
    @Permission("shop.admin")
    public void reload(CommandSender sender) {
        sender.sendMessage(Colors.color("&aShop reloaded."));
    }
}
```

## Design Rules

CastielLib follows these rules internally:

- No raw `new Thread()` usage.
- Heavy or blocking work should use Bukkit scheduler async tasks.
- Cross-version item/sound names should resolve through XSeries where practical.
- Modern APIs should be reflected when compiling against older Paper APIs.
- Plugin-specific business logic should stay in the plugin; CastielLib provides reusable primitives.
- Config-driven systems should preserve plugin-specific YAML sections rather than over-owning the file.

## Documentation Map

Additional module docs are available in `docs/`:

- `docs/CONFIG.md`
- `docs/DATABASE.md`
- `docs/ACTIONS.md`
- `docs/COLORS.md`
- `docs/COMMANDS.md`
- `docs/INVENTORIES.md`
- `docs/ITEMS_HOLOGRAMS_PARTICLES.md`
- `docs/UTILITIES.md`
- `ARCHITECTURE.md`

## Build Verification

Run:

```powershell
.\gradlew.bat build --no-daemon
```

Expected outputs:

```text
build/libs/CastielLib-1.0.0.jar
build/libs/CastielLib-1.0.0-sources.jar
build/libs/CastielLib-1.0.0-javadoc.jar
```

The project targets Java 8 bytecode. Newer JDKs may print warnings that Java 8 source/target values are obsolete; those warnings do not indicate a failed build.
