# Colors

Use `Colors.color(text)` anywhere text is sent to players. Inventories and actions call it automatically.

Supported syntax:

- Legacy: `&aGreen &lBold`
- Hex: `&#55EFC4Mint`
- Hex insertion alias: `#{55EFC4}Mint`
- Solid insertion: `<SOLID:55EFC4>Mint text`
- Solid wrapper: `<SOLID:55EFC4>Mint text</SOLID>`
- Solid with named color: `<SOLID:GOLD>Gold text</SOLID>`
- Gradient: `<GRADIENT:00FFFF>Gradient text</GRADIENT:ADD8E6>`
- Bold gradient: `<GRADIENT:00FFFF>&lBold gradient</GRADIENT:ADD8E6>`
- Rainbow: `<RAINBOW>Rainbow text</RAINBOW>`
- Rainbow saturation: `<RAINBOW:0.75>Soft rainbow</RAINBOW>`
- Iridium rainbow syntax: `<RAINBOW75>Soft rainbow</RAINBOW>`

Color names are intentionally small and stable: `WHITE`, `BLACK`, `RED`, `GREEN`, `BLUE`, `YELLOW`, `CYAN`, `AQUA`, `MAGENTA`, `PINK`, `ORANGE`, `GOLD`, `GRAY`, `GREY`, `DARK_GRAY`, `DARK_GREY`.

Legacy formatting codes inside solid, gradient, and rainbow text are preserved and do not consume gradient steps.
Formatting immediately before a solid, gradient, or rainbow tag is inherited as well, so `&l<GRADIENT:00FFFF>Bold</GRADIENT:ADD8E6>` is also supported.
