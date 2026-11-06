# 酒店预订管理系统

课程设计：Java Swing + Java JDBC + MySQL。第一天完成项目骨架、三张核心表、示例客房和数据库读取入口；登录和 Swing 主窗口从第二天实现。

## 环境

- JDK 21
- MySQL 8.0.16 或更高的 8.x 版本（使用 CHECK 约束）
- MySQL Connector/J 8.4.0
- Maven 可选；当前可直接使用 PowerShell 脚本编译

## 初始化数据库

在 MySQL 客户端或 Workbench 中依次执行 `sql/schema.sql`、`sql/seed.sql`，脚本不会删除已有数据。`CREATE TABLE IF NOT EXISTS` 不负责升级已有表结构。

建议在 MySQL 中创建仅能访问本项目数据库的本地账号（将下面密码替换为自己的密码）：

```sql
CREATE USER 'hotel_app'@'localhost' IDENTIFIED BY 'replace_with_your_password';
GRANT SELECT, INSERT, UPDATE, DELETE ON hotel_management.* TO 'hotel_app'@'localhost';
```

复制 `config/db.properties.example` 为 `config/db.properties`，填写数据库地址、账号和密码。实际配置已被 Git 忽略。程序应从项目根目录运行；也可以通过 JVM 参数 `-Dhotel.db.config=配置文件路径` 指定位置。

## 编译和运行

将官方 MySQL Connector/J 的 JAR 文件放到 `lib` 中，然后运行：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/build.ps1
powershell -ExecutionPolicy Bypass -File scripts/run.ps1
```

如果安装了 Maven，也可先运行：

```powershell
mvn compile dependency:copy-dependencies -DoutputDirectory=lib
java -cp "target/classes;lib/*" com.hotel.Main
```

初始化成功后，应显示“数据库连接成功，读取到 4 间客房”及四间示例客房。已有数据时数量可能不同。验证失败会以非零退出码结束，不打印数据库密码。

## 目录

- `src/main/java/com/hotel`：入口、entity 实体、dao 数据访问、util 工具；后续增加 ui 和 service。
- `sql`：建表与客房示例数据。
- `config`：配置模板；本地实际配置不提交。
- `scripts`：不依赖 Maven 的编译和运行脚本。

## 后续业务约定

一张订单对应一间房及一位主入住人。房态 AVAILABLE（空闲）、OCCUPIED（入住）；订单状态 RESERVED、CHECKED_IN、CHECKED_OUT、CANCELLED。订单保存成交房价，金额使用 BigDecimal。

未来的空房查询按日期检测有效订单重叠：新入住日期 < 已有离店日期，且新离店日期 > 已有入住日期。该跨记录约束将在预订业务中通过事务及锁定房间实现，单靠本日的建表约束不能阻止重叠预订。

第一天不包含实际预订、登录或退房功能。管理员密码将在登录阶段使用加盐哈希初始化，当前不预置明文或占位密码。
