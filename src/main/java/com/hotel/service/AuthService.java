package com.hotel.service;

import com.hotel.dao.AdminDao;
import com.hotel.util.Database;
import com.hotel.util.Passwords;
import java.io.IOException;
import java.sql.SQLException;

public final class AuthService {
    private final AdminDao dao = new AdminDao();

    public boolean login(String username, char[] password) throws IOException, SQLException {
        try (var connection = Database.connect()) {
            String hash = dao.findPasswordHash(connection, username);
            return hash != null && Passwords.verify(password, hash);
        }
    }

    public void createAdmin(String username, char[] password) throws IOException, SQLException {
        if (!username.matches("[A-Za-z0-9_]{3,50}"))
            throw new IllegalArgumentException("账号须为 3～50 位字母、数字或下划线");
        if (password.length < 8 || password.length > 128)
            throw new IllegalArgumentException("密码长度须为 8～128 位");
        String hash = Passwords.hash(password);
        try (var connection = Database.connect()) {
            dao.insert(connection, username, hash);
        }
    }
}
