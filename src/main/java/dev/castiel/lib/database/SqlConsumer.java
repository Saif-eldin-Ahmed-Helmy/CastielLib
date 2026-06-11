package dev.castiel.lib.database;

import java.sql.PreparedStatement;
import java.sql.SQLException;

@FunctionalInterface
public interface SqlConsumer {
    void accept(PreparedStatement statement) throws SQLException;
}
