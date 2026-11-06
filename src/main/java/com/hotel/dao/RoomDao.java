package com.hotel.dao;

import com.hotel.entity.Room;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class RoomDao {
    public List<Room> findAll(Connection connection) throws SQLException {
        List<Room> rooms = new ArrayList<>();
        try (var statement = connection.prepareStatement(
                "SELECT id, room_number, room_type, daily_price, status FROM room ORDER BY room_number");
             var result = statement.executeQuery()) {
            while (result.next()) {
                rooms.add(new Room(result.getLong("id"), result.getString("room_number"),
                        result.getString("room_type"), result.getBigDecimal("daily_price"),
                        result.getString("status")));
            }
        }
        return rooms;
    }
}
