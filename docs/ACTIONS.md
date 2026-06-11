# Actions

Run config actions:

```java
lib.actions().run(player, actions, Placeholders.of("price", 40));
```

Built-ins:

- `{console} eco give %player% 100`
- `{player} spawn`
- `{message} <GRADIENT:00FFFF>Hello</GRADIENT:ADD8E6>`
- `{message} <SOLID:GOLD>Premium</SOLID>`
- `{message} <RAINBOW>Rotating Shop</RAINBOW>`
- `{broadcast} &aA shop rotated`
- `{title} Title;Subtitle`
- `{action} Action bar text`
- `{sound} ENTITY_PLAYER_LEVELUP`
- `{close}`
- `{particle} END_ROD ring`

Register custom tags:

```java
lib.actions().register("coins", (ctx, payload) -> {});
```
