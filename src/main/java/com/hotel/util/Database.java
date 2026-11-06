package com.hotel.util;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class Database {
    private Database() {}

    public static Connection connect() throws IOException, SQLException {
        Path path = Path.of(System.getProperty("hotel.db.config", "config/db.properties"));
        Properties config = new Properties();
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            config.load(reader);
        }
        String url = required(config, "db.url");
        String username = required(config, "db.username");
        String password = config.getProperty("db.password");
        if (password == null) throw new IOException("数据库配置缺少 db.password");
        return DriverManager.getConnection(url, username, password);
    }

    private static String required(Properties config, String key) throws IOException {
        String value = config.getProperty(key);
        if (value == null || value.isBlank()) throw new IOException("数据库配置缺少 " + key);
        return value.trim();
    }
}
