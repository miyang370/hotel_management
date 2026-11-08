package com.hotel;

import com.hotel.entity.Room;
import com.hotel.service.BookingService;
import com.hotel.util.Database;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;
import java.util.concurrent.*;

/** Opt-in real database checks. Creates and removes only its own test room and orders. */
public final class BookingIntegrationCheck {
    public static void main(String[] args) throws Exception {
        long id;
        String number = "T" + UUID.randomUUID().toString().substring(0, 12);
        try (var c = Database.connect(); var s = c.prepareStatement(
                "INSERT INTO room(room_number,room_type,daily_price) VALUES (?,'冲突检测测试',100)", java.sql.Statement.RETURN_GENERATED_KEYS)) {
            s.setString(1, number); s.executeUpdate();
            try (var r = s.getGeneratedKeys()) { r.next(); id = r.getLong(1); }
        }
        Room room = new Room(id, number, "冲突检测测试", new BigDecimal("100.00"), "AVAILABLE");
        BookingService service = new BookingService();
        LocalDate d = LocalDate.now(ZoneId.of("Asia/Shanghai")).plusDays(10);
        try {
            service.reserve(room, d, d.plusDays(2), "测试客人", "13800000000", "TEST001");
            for (LocalDate[] dates : new LocalDate[][]{
                    {d, d.plusDays(2)}, {d.minusDays(1), d.plusDays(1)},
                    {d.plusDays(1), d.plusDays(3)}, {d.minusDays(1), d.plusDays(3)}}) {
                if (available(service, id, dates[0], dates[1])) throw new AssertionError("重叠房间被查询为可订");
                try { service.reserve(room, dates[0], dates[1], "测试", "13800000000", "TEST002"); throw new AssertionError("重叠预订未被拒绝"); }
                catch (IllegalArgumentException expected) { /* conflict */ }
            }
            if (!available(service, id, d.minusDays(1), d) || !available(service, id, d.plusDays(2), d.plusDays(3)))
                throw new AssertionError("相邻日期应允许预订");
            try (var c = Database.connect(); var s = c.prepareStatement("UPDATE hotel_order SET status=? WHERE room_id=?")) {
                for (String status : new String[]{"CANCELLED", "CHECKED_OUT", "CHECKED_IN"}) {
                    s.setString(1, status); s.setLong(2, id); s.executeUpdate();
                    if (available(service, id, d, d.plusDays(2)) == status.equals("CHECKED_IN")) throw new AssertionError("订单状态过滤错误");
                }
            }
            ExecutorService pool = Executors.newFixedThreadPool(2);
            CountDownLatch gate = new CountDownLatch(1);
            try {
                Callable<Boolean> submit = () -> {
                    gate.await();
                    try { service.reserve(room, d.plusDays(3), d.plusDays(4), "并发测试", "13800000000", "TEST003"); return true; }
                    catch (IllegalArgumentException expected) { return false; }
                };
                Future<Boolean> a = pool.submit(submit), b = pool.submit(submit); gate.countDown();
                if (a.get(30, TimeUnit.SECONDS) == b.get(30, TimeUnit.SECONDS)) throw new AssertionError("并发提交必须仅有一个成功");
            } finally { pool.shutdownNow(); pool.awaitTermination(15, TimeUnit.SECONDS); }
            System.out.println("PASS: overlap, adjacency, statuses, concurrent reservations");
        } finally {
            try (var c = Database.connect()) {
                try (var s = c.prepareStatement("DELETE FROM hotel_order WHERE room_id=?")) { s.setLong(1, id); s.executeUpdate(); }
                try (var s = c.prepareStatement("DELETE FROM room WHERE id=?")) { s.setLong(1, id); s.executeUpdate(); }
            }
        }
    }
    private static boolean available(BookingService service, long id, LocalDate in, LocalDate out) throws Exception {
        return service.available(in, out).stream().anyMatch(r -> r.id() == id);
    }
}
