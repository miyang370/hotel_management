package com.hotel.service;

import com.hotel.util.Database;
import java.sql.*;
import java.time.*;
import java.util.*;

public final class StayService {
    public record Order(long id, String number, String room, String guest, String phone,
                        LocalDate arrival, LocalDate departure, String status) {}
    public List<Order> search(String keyword) throws Exception {
        try (var c = Database.connect(); var s = c.prepareStatement("SELECT o.*,r.room_number FROM hotel_order o JOIN room r ON r.id=o.room_id WHERE (o.order_number LIKE ? OR o.guest_name LIKE ? OR r.room_number LIKE ?) ORDER BY o.id DESC")) {
            for (int i=1;i<=3;i++) s.setString(i, "%" + keyword + "%");
            try (var r=s.executeQuery()) {
                List<Order> rows=new ArrayList<>();
                while(r.next()) rows.add(new Order(r.getLong("id"), r.getString("order_number"), r.getString("room_number"), r.getString("guest_name"), r.getString("phone"), r.getObject("planned_check_in", LocalDate.class), r.getObject("planned_check_out", LocalDate.class), r.getString("status")));
                return rows;
            }
        }
    }
    public void cancel(long id) throws Exception { change(id, false); }
    public void checkIn(long id) throws Exception { change(id, true); }
    private void change(long id, boolean checkIn) throws Exception {
        try(var c=Database.connect()) {
            c.setAutoCommit(false);
            try {
                long room;
                try(var s=c.prepareStatement("SELECT room_id FROM hotel_order WHERE id=?")) {
                    s.setLong(1,id); try(var r=s.executeQuery()) { if(!r.next()) throw new IllegalArgumentException("订单不存在"); room=r.getLong(1); }
                }
                String roomStatus;
                try(var s=c.prepareStatement("SELECT status FROM room WHERE id=? FOR UPDATE")) {
                    s.setLong(1,room); try(var r=s.executeQuery()) { if(!r.next()) throw new IllegalArgumentException("客房不存在"); roomStatus=r.getString(1); }
                }
                LocalDate today=LocalDate.now(ZoneId.of("Asia/Shanghai")), out;
                try(var s=c.prepareStatement("SELECT status,planned_check_in,planned_check_out FROM hotel_order WHERE id=? FOR UPDATE")) {
                    s.setLong(1,id); try(var r=s.executeQuery()) {
                        if(!r.next() || !r.getString(1).equals("RESERVED")) throw new IllegalArgumentException("仅已预订订单可以取消或办理入住，请刷新");
                        if(checkIn && !today.equals(r.getObject(2,LocalDate.class))) throw new IllegalArgumentException("仅可在预订入住当天办理入住");
                        out=r.getObject(3,LocalDate.class);
                    }
                }
                if(checkIn) {
                    if(!roomStatus.equals("AVAILABLE")) throw new IllegalArgumentException("客房当前已入住，请先处理原入住订单");
                    try(var s=c.prepareStatement("SELECT id FROM hotel_order WHERE room_id=? AND id<>? AND (status='CHECKED_IN' OR (status='RESERVED' AND planned_check_in < ? AND planned_check_out > ?)) FOR UPDATE")) {
                        s.setLong(1,room); s.setLong(2,id); s.setObject(3,out); s.setObject(4,today);
                        try(var r=s.executeQuery()) { if(r.next()) throw new IllegalArgumentException("存在其他入住或冲突预订，无法入住"); }
                    }
                    try(var s=c.prepareStatement("UPDATE room SET status='OCCUPIED' WHERE id=?")) { s.setLong(1,room); s.executeUpdate(); }
                }
                try(var s=c.prepareStatement(checkIn ? "UPDATE hotel_order SET status='CHECKED_IN',actual_check_in=? WHERE id=?" : "UPDATE hotel_order SET status='CANCELLED' WHERE id=?")) {
                    if(checkIn) { s.setObject(1,LocalDateTime.now(ZoneId.of("Asia/Shanghai")).withNano(0)); s.setLong(2,id); } else s.setLong(1,id);
                    s.executeUpdate();
                }
                c.commit();
            } catch(Exception e) { try { c.rollback(); } catch(SQLException rollback) { e.addSuppressed(rollback); } throw e; }
        }
    }
}
