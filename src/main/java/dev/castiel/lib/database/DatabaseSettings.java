package dev.castiel.lib.database;

import java.io.File;

public final class DatabaseSettings {
    public enum Type {
        SQLITE, MYSQL
    }

    public final Type type;
    public final String host;
    public final int port;
    public final String database;
    public final String username;
    public final String password;
    public final File sqliteFile;

    private DatabaseSettings(Type type, String host, int port, String database, String username, String password, File sqliteFile) {
        this.type = type;
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
        this.sqliteFile = sqliteFile;
    }

    public static DatabaseSettings sqlite(File file) {
        return new DatabaseSettings(Type.SQLITE, "", 0, "", "", "", file);
    }

    public static DatabaseSettings mysql(String host, int port, String database, String username, String password) {
        return new DatabaseSettings(Type.MYSQL, host, port, database, username, password, null);
    }
}
