# Commands

Define:

```java
@Command(value = "castiel", aliases = {"clib"})
@Permission("castiel.admin")
public final class CastielCommand {
  @SubCommand("")
  public void root(CommandSender sender) {}

  @SubCommand("give")
  public void give(Player player, String target, int amount) {}
}
```

Register:

```java
lib.commands().register(new CastielCommand());
```

Commands are inserted into Bukkit's command map by reflection and do not require `plugin.yml` entries.
