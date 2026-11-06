package com.hotel;

import com.hotel.dao.RoomDao;
import com.hotel.util.Database;
import java.io.IOException;
import java.sql.SQLException;

/** Day-one command-line database check; Swing login is planned for day two. */
public final class Main {
    public static void main(String[] args) {
        try (var connection = Database.connect()) {
            var rooms = new RoomDao().findAll(connection);
            System.out.println("数据库连接成功，读取到 " + rooms.size() + " 间客房。");
            for (var room : rooms) {
                System.out.printf("%s | %s | %s 元/晚 | %s%n", room.roomNumber(),
                        room.roomType(), room.dailyPrice(), room.status());
            }
        } catch (IOException e) {
            System.err.println("无法读取数据库配置：" + e.getMessage());
            System.err.println("请参考 README.md 创建 config/db.properties。");
            System.exit(1);
        } catch (SQLException e) {
            System.err.println("数据库验证失败，SQLState=" + e.getSQLState()
                    + "，错误码=" + e.getErrorCode());
            System.err.println("请检查 MySQL 服务、账号权限、建表脚本及 JDBC 驱动。");
            System.exit(1);
        }
    }
}
