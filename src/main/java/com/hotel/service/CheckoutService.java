package com.hotel.service;

import com.hotel.util.Database;
import java.math.BigDecimal;
import java.sql.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

public final class CheckoutService {
    public record Bill(long id, String number, String room, String guest, LocalDateTime arrival,
                       LocalDate plannedDeparture, BigDecimal price, long nights, BigDecimal total) {}
    public static long nights(LocalDateTime arrival, LocalDateTime departure) {
        if (arrival == null || departure.isBefore(arrival)) throw new IllegalArgumentException("实际入住时间异常，无法结算");
        return Math.max(1, ChronoUnit.DAYS.between(arrival.toLocalDate(), departure.toLocalDate()));
    }
    private Bill bill(ResultSet r, LocalDateTime now) throws SQLException {
        LocalDateTime in = r.getObject("actual_check_in", LocalDateTime.class);
        long count = nights(in, now);
        BigDecimal price = r.getBigDecimal("daily_price");
        return new Bill(r.getLong("id"), r.getString("order_number"), r.getString("room_number"), r.getString("guest_name"),
                in, r.getObject("planned_check_out", LocalDate.class), price, count, price.multiply(BigDecimal.valueOf(count)));
    }
    public List<Bill> search(String keyword) throws Exception {
        try (var c = Database.connect(); var s = c.prepareStatement("SELECT o.*,r.room_number FROM hotel_order o JOIN room r ON r.id=o.room_id WHERE o.status='CHECKED_IN' AND (o.guest_name LIKE ? OR r.room_number LIKE ? OR o.order_number LIKE ?) ORDER BY o.id DESC")) {
            for(int i=1;i<=3;i++) s.setString(i,"%"+keyword+"%");
            try(var r=s.executeQuery()) {
                List<Bill> result=new ArrayList<>(); LocalDateTime now=LocalDateTime.now(ZoneId.of("Asia/Shanghai"));
                while(r.next()) result.add(bill(r,now)); return result;
            }
        }
    }
    public Bill checkout(long id, BigDecimal confirmedTotal) throws Exception {
        try(var c=Database.connect()) {
            c.setAutoCommit(false);
            try {
                long room;
                try(var s=c.prepareStatement("SELECT room_id FROM hotel_order WHERE id=?")) {
                    s.setLong(1,id); try(var r=s.executeQuery()) {if(!r.next())throw new IllegalArgumentException("订单不存在");room=r.getLong(1);}
                }
                try(var s=c.prepareStatement("SELECT status FROM room WHERE id=? FOR UPDATE")) {
                    s.setLong(1,room); try(var r=s.executeQuery()) {if(!r.next()||!r.getString(1).equals("OCCUPIED"))throw new IllegalArgumentException("房态异常，请先核查");}
                }
                LocalDateTime now=LocalDateTime.now(ZoneId.of("Asia/Shanghai")); Bill result;
                try(var s=c.prepareStatement("SELECT o.*,r.room_number FROM hotel_order o JOIN room r ON r.id=o.room_id WHERE o.id=? FOR UPDATE")) {
                    s.setLong(1,id); try(var r=s.executeQuery()) {
                        if(!r.next()||!r.getString("status").equals("CHECKED_IN"))throw new IllegalArgumentException("仅在住订单可结算，禁止重复退房");
                        result=bill(r,now);
                    }
                }
                if(confirmedTotal==null||result.total().compareTo(confirmedTotal)!=0)throw new IllegalArgumentException("结算金额已变化，请刷新后重新确认");
                try(var s=c.prepareStatement("SELECT id FROM hotel_order WHERE room_id=? AND id<>? AND status='CHECKED_IN' FOR UPDATE")) {
                    s.setLong(1,room);s.setLong(2,id);try(var r=s.executeQuery()){if(r.next())throw new IllegalArgumentException("该房间存在其他在住订单，请核查后退房");}
                }
                try(var s=c.prepareStatement("UPDATE hotel_order SET status='CHECKED_OUT',actual_check_out=?,total_amount=? WHERE id=?")) {
                    s.setObject(1,now);s.setBigDecimal(2,result.total());s.setLong(3,id);s.executeUpdate();
                }
                try(var s=c.prepareStatement("UPDATE room SET status='AVAILABLE' WHERE id=?")){s.setLong(1,room);s.executeUpdate();}
                c.commit();return result;
            }catch(Exception e){try{c.rollback();}catch(SQLException rollback){e.addSuppressed(rollback);}throw e;}
        }
    }
}
