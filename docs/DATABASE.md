# Database

SQLite:

```java
DatabaseManager db = new DatabaseManager(this, DatabaseSettings.sqlite(new File(getDataFolder(), "data.db")));
lib.database(db);
```

MySQL:

```java
lib.database(new DatabaseManager(this, DatabaseSettings.mysql("localhost", 3306, "plugin", "user", "pass")));
```

Usage:

```java
lib.database().update("INSERT INTO users(uuid, coins) VALUES(?, ?)", ps -> {
  ps.setString(1, uuid.toString());
  ps.setInt(2, 100);
});
```

Queries return `CompletableFuture<List<T>>` and are always scheduled asynchronously through Bukkit.
