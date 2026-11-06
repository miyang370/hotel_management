USE hotel_management;
-- 可重复执行，不覆盖已有客房信息。客房示例不包含真实个人数据。
INSERT INTO room (room_number, room_type, daily_price)
SELECT '101', '单人间', 168.00 WHERE NOT EXISTS (SELECT 1 FROM room WHERE room_number = '101');
INSERT INTO room (room_number, room_type, daily_price)
SELECT '102', '标准双人间', 228.00 WHERE NOT EXISTS (SELECT 1 FROM room WHERE room_number = '102');
INSERT INTO room (room_number, room_type, daily_price)
SELECT '201', '大床房', 268.00 WHERE NOT EXISTS (SELECT 1 FROM room WHERE room_number = '201');
INSERT INTO room (room_number, room_type, daily_price)
SELECT '202', '家庭房', 368.00 WHERE NOT EXISTS (SELECT 1 FROM room WHERE room_number = '202');
-- 管理员账号在第二天实现密码哈希和登录功能时初始化。
