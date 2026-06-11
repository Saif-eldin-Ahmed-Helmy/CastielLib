# Config

Create a POJO with defaults:

```java
public final class ShopConfig {
  @ConfigNode("RotatingShop.Enabled")
  public boolean enabled = true;

  @ConfigNode("RotatingShop.Size")
  public int size = 45;
}
```

Load it:

```java
ShopConfig config = lib.configs().load("shop.yml", ShopConfig.class);
```

If a path is missing, CastielLib writes the default value into the existing YAML file without replacing known values.
