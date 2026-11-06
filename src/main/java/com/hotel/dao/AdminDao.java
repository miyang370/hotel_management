package com.hotel.dao;

import java.sql.*;

public final class AdminDao {
    public String findPasswordHash(Connection connection, String username) throws SQLException {
        try (var query = connection.prepareStatement("SELECT password_hash FROM admin WHERE username = ?")) {
            query.setString(1, username);
            try (var result = query.executeQuery()) {
                return result.next() ? result.getString(1) : null;
            }
        }
    }

    public void insert(Connection connection, String username, String hash) throws SQLException {
        try (var query = connection.prepareStatement("INSERT INTO admin(username, password_hash) VALUES (?, ?)")) {
            query.setString(1, username);
            query.setString(2, hash);
            query.executeUpdate();
        }
    }
}
