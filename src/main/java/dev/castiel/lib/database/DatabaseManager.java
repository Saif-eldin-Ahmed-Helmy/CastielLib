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
    private static final String SQLITE_DRIVER = "org.sqlite.JDBC";
    private final JavaPlugin plugin;
    private final HikariDataSource source;
    private final PendingOperations operations = new PendingOperations();

    public DatabaseManager(JavaPlugin plugin, DatabaseSettings settings) {
        this.plugin = plugin;
        HikariConfig config = new HikariConfig();
        if (settings.type == DatabaseSettings.Type.SQLITE) {
            prepareSqliteFile(settings.sqliteFile);
            config.setJdbcUrl("jdbc:sqlite:" + settings.sqliteFile.getAbsolutePath());
            // Named explicitly rather than left to DriverManager/ServiceLoader
            // discovery: the bundled driver lives in this plugin's own
            // classloader, which the JDBC service lookup performed by Hikari
            // does not necessarily search. Without this the pool fails with
            // "No suitable driver" and the plugin cannot enable at all.
            config.setDriverClassName(SQLITE_DRIVER);
            // Forces Hikari's query-based liveness check instead of
            // Connection.isValid(), which pre-3.8 SQLite drivers leave
            // abstract. Costs nothing on a modern driver and keeps an
            // unexpected one from aborting pool startup outright.
            config.setConnectionTestQuery("SELECT 1");
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

    /**
     * Creates the parent directory of a SQLite file before Hikari opens the
     * pool. A plugin's data folder does not exist on a first run until
     * something writes to it, and SQLite reports the resulting failure as an
     * opaque connection error rather than a missing-directory one.
     */
    private static void prepareSqliteFile(java.io.File file) {
        if (file == null) return;
        java.io.File parent = file.getAbsoluteFile().getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs() && !parent.isDirectory()) {
            throw new IllegalStateException("Unable to create database directory: " + parent);
        }
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
