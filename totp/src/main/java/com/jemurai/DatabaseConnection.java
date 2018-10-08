package com.jemurai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

public class DatabaseConnection {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final Connection connection;

    public DatabaseConnection() throws SQLException {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException("Could not initialize driver: " + e.getMessage());
        }

        String url = "jdbc:postgresql://localhost/totp";
        this.connection = DriverManager.getConnection(url);
    }

    public int writeSecret(String secret, long userId) {
        String query = "INSERT INTO tokens (encrypted_secret, user_id) VALUES (?, ?)";
        try(PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, secret);
            statement.setLong(2, userId);
            return statement.executeUpdate();
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            return -1;
        }
    }

    public String getSecret(long userId) {
        String query = "SELECT FROM tokens WHERE user_id = ?";
        try(PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setLong(1, userId);
            ResultSet result = statement.executeQuery();
            if (result.next()) {
                return result.getString(1);
            } else {
                return null;
            }
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }
}
