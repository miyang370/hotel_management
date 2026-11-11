package com.hotel.service;

import com.hotel.util.Database;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public final class OrderQueryService {
    public record Filter(String name, String phone, String document, String room, LocalDate from, LocalDate to, String status) {}
    public record Record(long id, String number, String room, String name, String phone, String document,
                         LocalDate arrival, LocalDate departure, LocalDateTime actualArrival,
                         LocalDateTime actualDeparture, BigDecimal price, BigDecimal total, String status) {}
    public static String mask(String value) {
        if(value == null || value.isEmpty()) return "";
        if(value.length() <= 4) return "*".repeat(value.length());
        return value.substring(0,2) + "*".repeat(value.length()-4) + value.substring(value.length()-2);
    }
    public static String statusLabel(String status) {
        return switch(status) { case "RESERVED" -> "已预订"; case "CHECKED_IN" -> "已入住"; case "CHECKED_OUT" -> "已退房"; case "CANCELLED" -> "已取消"; default -> status; };
    }
    private static String literal(String value) { return "%" + value.replace("!","!!").replace("%","!%").replace("_","!_") + "%"; }
    public List<Record> search(Filter f) throws Exception {
        if(f.from()!=null && f.to()!=null && f.to().isBefore(f.from())) throw new IllegalArgumentException("结束日期不能早于开始日期");
        StringBuilder sql=new StringBuilder("SELECT o.*,r.room_number FROM hotel_order o JOIN room r ON r.id=o.room_id WHERE o.guest_name LIKE ? ESCAPE '!' AND o.phone LIKE ? ESCAPE '!' AND o.id_number LIKE ? ESCAPE '!' AND r.room_number LIKE ? ESCAPE '!'");
        List<Object> params=new ArrayList<>(List.of(literal(f.name()),literal(f.phone()),literal(f.document()),literal(f.room())));
        if(f.from()!=null){sql.append(" AND o.planned_check_in>=?");params.add(f.from());}
        if(f.to()!=null){sql.append(" AND o.planned_check_in<=?");params.add(f.to());}
        if(!f.status().isEmpty()){sql.append(" AND o.status=?");params.add(f.status());}
        sql.append(" ORDER BY o.id DESC");
        try(var c=Database.connect();var s=c.prepareStatement(sql.toString())) {
            for(int i=0;i<params.size();i++)s.setObject(i+1,params.get(i));
            try(var r=s.executeQuery()) {
                List<Record> result=new ArrayList<>();
                while(r.next())result.add(new Record(r.getLong("id"),r.getString("order_number"),r.getString("room_number"),r.getString("guest_name"),r.getString("phone"),r.getString("id_number"),r.getObject("planned_check_in",LocalDate.class),r.getObject("planned_check_out",LocalDate.class),r.getObject("actual_check_in",LocalDateTime.class),r.getObject("actual_check_out",LocalDateTime.class),r.getBigDecimal("daily_price"),r.getBigDecimal("total_amount"),r.getString("status")));
                return result;
            }
        }
    }
}
