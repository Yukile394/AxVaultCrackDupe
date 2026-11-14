package com.artillexstudios.axvaults.libs.hikari;

import java.io.Closeable;
import java.sql.Connection;
import java.sql.SQLException;

/** Sadece derleme için stub. Jar'a girmez. */
public class HikariDataSource extends HikariConfig implements Closeable {
    public HikariDataSource(HikariConfig configuration) {
    }

    public Connection getConnection() throws SQLException {
        throw new UnsupportedOperationException("compile-only stub");
    }

    @Override
    public void close() {
    }
}
