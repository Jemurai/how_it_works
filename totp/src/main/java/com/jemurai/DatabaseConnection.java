package com.jemurai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

class DatabaseConnection {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final Connection connection;

    DatabaseConnection() throws SQLException {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException("Could not initialize driver: " + e.getMessage());
        }

        String url = "jdbc:postgresql://localhost/totp";
        this.connection = DriverManager.getConnection(url);
    }

    void writeSecret(String secret) {
        String query = "INSERT INTO tokens (encrypted_secret, user_id) VALUES (?, ?)";
        try(PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, secret);
            statement.setLong(2, 1);
            statement.executeUpdate();
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
    }

    String getSecret() {
        String query = "SELECT encrypted_secret FROM tokens WHERE user_id = 1";
        try(Statement statement = connection.createStatement()) {
            ResultSet result = statement.executeQuery(query);
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
