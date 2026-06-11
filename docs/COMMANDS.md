# Commands

Define:

```java
@Command(value = "castiel", aliases = {"clib"})
@Permission(value = "castiel.admin", message = "&cNo permission.")
public final class CastielCommand {
  @SubCommand("")
  public void root(CommandSender sender) {}

  @SubCommand("give")
  @Permission("castiel.give")
  public void give(Player player, String target, int amount) {}
}
```

Register:

```java
lib.commands().noPermissionMessage(settings.noPermission.replace("%prefix%", settings.prefix));
lib.commands().permissionResolver((sender, node) -> Permissions.hasWildcard(sender, node));
lib.commands().register(new CastielCommand());
```

Commands are inserted into Bukkit's command map by reflection and do not require `plugin.yml` entries.

Permission behavior:

- A class-level `@Permission` applies to every subcommand unless a method has its own `@Permission`.
- Method-level `@Permission` applies only to that subcommand.
- `lib.commands().noPermissionMessage(...)` initializes the global denial message once for every `@Permission` without a custom annotation message.
- `@Permission(message = "...")` overrides the global message for that specific class or method.
- If neither message is set, the command is denied silently.
- Root commands use `@SubCommand` or `@SubCommand("")`.
- CastielLib does not set Bukkit command permission metadata and does not rely on Bukkit's command permission gate.
- Before invoking the command method, CastielLib manually checks the selected permission node against the sender's effective permissions.
- `lib.commands().permissionResolver(...)` can replace the permission backend entirely with a plugin-defined boolean check.
- Exact denies beat wildcard grants. Wildcards such as `customshop.*`, `customshop.sell.*`, and `*` are supported.
- The registry removes stale command-map entries for the same command and aliases before registering, which prevents old reload-era command objects from bypassing new annotation metadata.
- Tab completion hides subcommands the sender cannot use.
