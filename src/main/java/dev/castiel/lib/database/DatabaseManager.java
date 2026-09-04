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
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

public final class DatabaseManager implements AutoCloseable {
    private final JavaPlugin plugin;
    private final HikariDataSource source;
    private final PendingOperations operations = new PendingOperations();

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
        if (!operations.register(future)) {
            return rejected(future);
        }
        schedule(future, () -> {
            try (Connection connection = source.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
                if (binder != null) {
                    binder.accept(statement);
                }
                future.complete(statement.executeUpdate());
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    public <T> CompletableFuture<List<T>> query(String sql, SqlConsumer binder, RowMapper<T> mapper) {
        CompletableFuture<List<T>> future = new CompletableFuture<>();
        if (!operations.register(future)) {
            return rejected(future);
        }
        schedule(future, () -> {
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
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    /** Runs connection-scoped work through the tracked async lifecycle. */
    public <T> CompletableFuture<T> execute(SqlFunction<T> operation) {
        CompletableFuture<T> future = new CompletableFuture<T>();
        if (operation == null) {
            future.completeExceptionally(new IllegalArgumentException("Database operation is required."));
            return future;
        }
        if (!operations.register(future)) {
            return rejected(future);
        }
        schedule(future, () -> {
            try (Connection connection = source.getConnection()) {
                future.complete(operation.apply(connection));
            } catch (Exception error) {
                future.completeExceptionally(error);
            }
        });
        return future;
    }

    public HikariDataSource source() {
        return source;
    }

    /** Waits for submitted operations to finish without rejecting new work. */
    public boolean flush(long timeout, TimeUnit unit) {
        return operations.awaitEmpty(timeout, unit);
    }

    /** Prevents new database operations from being submitted. */
    public void beginShutdown() {
        operations.beginClosing();
    }

    /** Returns whether this manager has begun shutting down. */
    public boolean isClosing() {
        return operations.isClosing();
    }

    /** Returns the number of submitted operations that have not completed. */
    public int pendingOperationCount() {
        return operations.size();
    }

    /** Drains pending work for a bounded interval and then closes the pool. */
    public boolean close(long timeout, TimeUnit unit) {
        beginShutdown();
        boolean drained = flush(timeout, unit);
        source.close();
        if (!drained) {
            plugin.getLogger().warning("Closed database with " + operations.size() + " incomplete operation(s).");
        }
        return drained;
    }

    @Override
    public void close() {
        close(5, TimeUnit.SECONDS);
    }

    private void schedule(CompletableFuture<?> future, Runnable operation) {
        try {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, operation);
        } catch (RuntimeException error) {
            future.completeExceptionally(error);
        }
    }

    private <T> CompletableFuture<T> rejected(CompletableFuture<T> future) {
        future.completeExceptionally(new RejectedExecutionException("Database manager is closing."));
        return future;
    }
}
