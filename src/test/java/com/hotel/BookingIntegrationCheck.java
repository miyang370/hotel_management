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
            var stays = new com.hotel.service.StayService();
            try (var c = Database.connect(); var s = c.prepareStatement("UPDATE hotel_order SET status='CANCELLED' WHERE room_id=?")) {
                s.setLong(1,id); s.executeUpdate();
            }
            LocalDate today = LocalDate.now(ZoneId.of("Asia/Shanghai"));
            String cancelled = service.reserve(room, today, today.plusDays(1), "取消测试", "13800000000", "TEST004");
            long cancelledId = stays.search(cancelled).getFirst().id();
            stays.cancel(cancelledId);
            try { stays.checkIn(cancelledId); throw new AssertionError("取消订单不能入住"); } catch (IllegalArgumentException expected) { }
            String booked = service.reserve(room, today, today.plusDays(1), "入住测试", "13800000000", "TEST005");
            long bookedId = stays.search(booked).getFirst().id();
            stays.checkIn(bookedId);
            try { stays.checkIn(bookedId); throw new AssertionError("重复入住应拒绝"); } catch (IllegalArgumentException expected) { }
            try { stays.cancel(bookedId); throw new AssertionError("入住后不可取消"); } catch (IllegalArgumentException expected) { }
            try { service.directCheckIn(room, today, today.plusDays(1), "直接入住测试", "13800000000", "TEST006"); throw new AssertionError("占用房间不能直接入住"); } catch (IllegalArgumentException expected) { }
            try (var c = Database.connect()) {
                try (var s = c.prepareStatement("UPDATE hotel_order SET status='CHECKED_OUT' WHERE id=?")) { s.setLong(1,bookedId); s.executeUpdate(); }
                try (var s = c.prepareStatement("UPDATE room SET status='AVAILABLE' WHERE id=?")) { s.setLong(1,id); s.executeUpdate(); }
            }
            String direct = service.directCheckIn(room, today, today.plusDays(1), "直接入住测试", "13800000000", "TEST007");
            if (!stays.search(direct).getFirst().status().equals("CHECKED_IN")) throw new AssertionError("直接入住订单状态错误");
            try (var c = Database.connect(); var s = c.prepareStatement("SELECT status FROM room WHERE id=?")) {
                s.setLong(1,id); try(var r=s.executeQuery()) { r.next(); if(!r.getString(1).equals("OCCUPIED")) throw new AssertionError("房态未同步"); }
            }
            System.out.println("PASS: cancellation, check-in, duplicate rejection, direct check-in, room status");
            var checkout = new com.hotel.service.CheckoutService();
            long directId = stays.search(direct).getFirst().id();
            try { checkout.checkout(directId, new BigDecimal("999")); throw new AssertionError("金额变更必须重新确认"); } catch(IllegalArgumentException expected) { }
            if(!stays.search(direct).getFirst().status().equals("CHECKED_IN"))throw new AssertionError("失败后订单应保持在住");
            var bill = checkout.checkout(directId, new BigDecimal("100.00"));
            if(bill.nights()!=1 || bill.total().compareTo(new BigDecimal("100.00"))!=0)throw new AssertionError("同日退房须计一晚");
            try { checkout.checkout(directId, bill.total()); throw new AssertionError("重复退房必须拒绝"); } catch(IllegalArgumentException expected) { }
            try(var c=Database.connect();var s=c.prepareStatement("SELECT r.status,o.status,o.total_amount,o.actual_check_out FROM room r JOIN hotel_order o ON o.room_id=r.id WHERE o.id=?")) {
                s.setLong(1,directId);try(var r=s.executeQuery()){r.next();if(!r.getString(1).equals("AVAILABLE")||!r.getString(2).equals("CHECKED_OUT")||r.getBigDecimal(3).compareTo(bill.total())!=0||r.getObject(4)==null)throw new AssertionError("退房数据未同步");}
            }
            if(com.hotel.service.CheckoutService.nights(today.minusDays(3).atTime(23,0),today.atTime(8,0))!=3)throw new AssertionError("跨日计费错误");
            System.out.println("PASS: checkout amount, rollback, duplicate rejection, room release, multi-night billing");
            var queries=new com.hotel.service.OrderQueryService();
            var matches=queries.search(new com.hotel.service.OrderQueryService.Filter("直接入住测试","13800000000","TEST007",number,today,today,"CHECKED_OUT"));
            if(matches.size()!=1||matches.getFirst().id()!=directId||matches.getFirst().actualDeparture()==null||matches.getFirst().total().compareTo(bill.total())!=0)throw new AssertionError("组合查询或详情字段错误");
            if(!queries.search(new com.hotel.service.OrderQueryService.Filter("","","TEST007",number,today,today,"RESERVED")).isEmpty())throw new AssertionError("状态筛选错误");
            if(!queries.search(new com.hotel.service.OrderQueryService.Filter("","","TEST007",number,today.plusDays(1),today.plusDays(2),"")).isEmpty())throw new AssertionError("日期筛选错误");
            if(!queries.search(new com.hotel.service.OrderQueryService.Filter("%","","",number,null,null,"")).isEmpty())throw new AssertionError("通配符应按字面匹配");
            if(!com.hotel.service.OrderQueryService.mask("TEST007").equals("TE***07"))throw new AssertionError("证件号脱敏错误");
            System.out.println("PASS: combined guest filters, dates, status, detail fields, literal search, masking");
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
