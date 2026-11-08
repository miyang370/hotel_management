package com.hotel.service;

import com.hotel.entity.Room;
import com.hotel.util.Database;
import java.time.*;
import java.sql.*;
import java.util.*;

public final class BookingService {
    private static final String CONFLICT = "SELECT id FROM hotel_order WHERE room_id=? AND status IN ('RESERVED','CHECKED_IN') AND planned_check_in < ? AND planned_check_out > ?";
    public static void validateDates(LocalDate in, LocalDate out) {
        if (in == null || out == null || in.isBefore(LocalDate.now(ZoneId.of("Asia/Shanghai"))) || !out.isAfter(in) || out.getYear() > 9999)
            throw new IllegalArgumentException("入住日期不能早于今天，离店日期必须晚于入住日期");
    }
    public List<Room> available(LocalDate in, LocalDate out) throws Exception {
        validateDates(in, out);
        String sql = "SELECT r.* FROM room r WHERE NOT EXISTS (SELECT 1 FROM hotel_order o WHERE o.room_id=r.id AND o.status IN ('RESERVED','CHECKED_IN') AND o.planned_check_in < ? AND o.planned_check_out > ?) AND (r.status='AVAILABLE' OR ? > CURRENT_DATE) ORDER BY room_number";
        try (var c = Database.connect(); var s = c.prepareStatement(sql)) {
            s.setObject(1, out); s.setObject(2, in); s.setObject(3, in);
            try (var rs = s.executeQuery()) {
                List<Room> rooms = new ArrayList<>();
                while (rs.next()) rooms.add(new Room(rs.getLong("id"), rs.getString("room_number"), rs.getString("room_type"), rs.getBigDecimal("daily_price"), rs.getString("status")));
                return rooms;
            }
        }
    }
    public String reserve(Room room, LocalDate in, LocalDate out, String name, String phone, String document) throws Exception {
        validateDates(in, out);
        if (name.isBlank() || name.length() > 100) throw new IllegalArgumentException("姓名不能为空且最多100位");
        if (!phone.matches("[0-9+() -]{6,30}")) throw new IllegalArgumentException("请输入6～30位有效联系电话");
        if (!document.matches("[A-Za-z0-9-]{5,30}")) throw new IllegalArgumentException("证件号须为5～30位字母、数字或连字符");
        try (var c = Database.connect()) {
            c.setAutoCommit(false);
            try {
                try (var lock = c.prepareStatement("SELECT status,daily_price FROM room WHERE id=? FOR UPDATE")) {
                    lock.setLong(1, room.id());
                    try (var r = lock.executeQuery()) {
                        if (!r.next()) throw new IllegalArgumentException("客房已被删除，请重新查询");
                        if (!r.getBigDecimal("daily_price").equals(room.dailyPrice())) throw new IllegalArgumentException("房价已变化，请重新查询后预订");
                        if (in.equals(LocalDate.now(ZoneId.of("Asia/Shanghai"))) && !r.getString("status").equals("AVAILABLE"))
                            throw new IllegalArgumentException("该房间当前已入住，请选择其他客房");
                    }
                }
                try (var s = c.prepareStatement(CONFLICT + " FOR UPDATE")) {
                    s.setLong(1, room.id()); s.setObject(2, out); s.setObject(3, in);
                    try (var r = s.executeQuery()) { if (r.next()) throw new IllegalArgumentException("该客房在所选日期已有预订或入住安排，请重新查询"); }
                }
                String number = "B" + UUID.randomUUID().toString().replace("-", "");
                try (var s = c.prepareStatement("INSERT INTO hotel_order(order_number,room_id,guest_name,phone,id_number,planned_check_in,planned_check_out,daily_price,status) VALUES (?,?,?,?,?,?,?,?,'RESERVED')")) {
                    s.setString(1, number); s.setLong(2, room.id()); s.setString(3, name); s.setString(4, phone); s.setString(5, document);
                    s.setObject(6, in); s.setObject(7, out); s.setBigDecimal(8, room.dailyPrice()); s.executeUpdate();
                }
                c.commit(); return number;
            } catch (Exception e) { c.rollback(); throw e; }
        }
    }
}
