package dev.castiel.lib.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class DatabaseManager implements AutoCloseable {
    private final JavaPlugin plugin;
    private final HikariDataSource source;

    public DatabaseManager(JavaPlugin plugin, DatabaseSettings settings) {
        this.plugin = plugin;
        HikariConfig config = new HikariConfig();
        if (settings.type == DatabaseSettings.Type.SQLITE) {
            config.setJdbcUrl("jdbc:sqlite:" + settings.sqliteFile.getAbsolutePath());
            config.setMaximumPoolSize(1);
        } else {
            config.setJdbcUrl("jdbc:mysql://" + settings.host + ":" + settings.port + "/" + settings.database + "?useSSL=false&autoReconnect=true");
            config.setUsername(settings.username);
            config.setPassword(settings.password);
        }
        config.setPoolName(plugin.getName() + "-CastielPool");
        this.source = new HikariDataSource(config);
    }

    public CompletableFuture<Integer> update(String sql, SqlConsumer binder) {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection connection = source.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
                if (binder != null) {
                    binder.accept(statement);
                }
                future.complete(statement.executeUpdate());
            } catch (SQLException e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    public <T> CompletableFuture<List<T>> query(String sql, SqlConsumer binder, RowMapper<T> mapper) {
        CompletableFuture<List<T>> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection connection = source.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
                if (binder != null) {
                    binder.accept(statement);
                }
                try (ResultSet results = statement.executeQuery()) {
                    List<T> mapped = new ArrayList<>();
                    while (results.next()) {
                        mapped.add(mapper.map(results));
                    }
                    future.complete(mapped);
                }
            } catch (SQLException e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    public HikariDataSource source() {
        return source;
    }

    @Override
    public void close() {
        source.close();
    }
}
