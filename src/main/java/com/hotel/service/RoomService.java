package com.hotel.service;

import com.hotel.entity.Room;
import com.hotel.dao.RoomDao;
import com.hotel.util.Database;
import java.math.BigDecimal;
import java.util.List;
import java.sql.SQLException;

public final class RoomService {
    public List<Room> search(String number, String type, String status) throws Exception {
        try (var c = Database.connect()) {
            return new RoomDao().findAll(c).stream()
                .filter(r -> r.roomNumber().contains(number) && r.roomType().contains(type))
                .filter(r -> status.isEmpty() || r.status().equals(status)).toList();
        }
    }
    public void save(Long id, String number, String type, BigDecimal price) throws Exception {
        if (number.isBlank() || number.length() > 20) throw new IllegalArgumentException("房号不能为空且不能超过20位");
        if (type.isBlank() || type.length() > 50) throw new IllegalArgumentException("房型不能为空且不能超过50位");
        if (price.signum() <= 0 || price.scale() > 2 || price.compareTo(new BigDecimal("99999999.99")) > 0)
            throw new IllegalArgumentException("房价须大于0，最多两位小数且不超过99999999.99");
        try (var c = Database.connect(); var s = c.prepareStatement(id == null
                ? "INSERT INTO room(room_number,room_type,daily_price) VALUES (?,?,?)"
                : "UPDATE room SET room_number=?,room_type=?,daily_price=? WHERE id=?")) {
            s.setString(1, number); s.setString(2, type); s.setBigDecimal(3, price);
            if (id != null) s.setLong(4, id);
            if (s.executeUpdate() == 0) throw new IllegalArgumentException("客房不存在，请刷新列表");
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) throw new IllegalArgumentException("房号已存在，请使用其他房号");
            throw e;
        }
    }
    public void delete(long id) throws Exception {
        try (var c = Database.connect(); var s = c.prepareStatement("DELETE FROM room WHERE id=? AND status='AVAILABLE'")) {
            s.setLong(1, id);
            if (s.executeUpdate() == 0) throw new IllegalArgumentException("客房已入住或已不存在，无法删除");
        } catch (SQLException e) {
            if (e.getErrorCode() == 1451) throw new IllegalArgumentException("该客房有关联订单，不能删除");
            throw e;
        }
    }
}
