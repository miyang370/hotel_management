# 酒店预订管理系统

## 第三天：客房管理

登录后默认进入客房管理，可按房号、房型（包含匹配）和房态组合查询，支持重置条件、新增、选中修改、选中删除。表格不可直接编辑，使用统一表头、行高和隔行背景。数据库操作在 SwingWorker 后台执行。

房号最多20位、房型最多50位，均不能为空；房价必须大于0、最多两位小数。重复房号由数据库唯一约束保护并显示红色提示。有任意历史或当前订单关联的客房无法删除，已入住客房也无法删除。新增客房默认空闲，修改仅调整房号、房型、价格；房态由后续入住和退房业务自动维护，避免手动改为空闲导致重复入住。修改房价不更改订单中保存的成交房价。

验收步骤：新增一间客房 → 按房号查询 → 修改房型和价格 → 再次查询 → 尝试重复房号及无效金额 → 删除无订单的空闲客房。已有订单数据时，再验证关联订单的客房删除被阻止。

当前编译已通过；真实 MySQL 增删改查及界面交互仍需在配置本地数据库后验收。

课程设计：Java Swing + Java JDBC + MySQL。已完成第一天数据库骨架，以及第二天管理员登录、密码哈希初始化、主窗口导航和退出登录。五个业务页面目前为占位页面。

## 第二天：初始化管理员并启动

先完成下方的数据库、驱动和本地配置准备，再在项目根目录的 PowerShell 终端启动：

```powershell
.\scripts\run.ps1
```

首次使用点击登录窗口的“注册账号”，填写账号、密码和确认密码。注册成功返回登录窗口并自动填入账号，再输入密码登录。账号为 3～50 位字母、数字或下划线，密码为 8～128 位。重复账号会提示已注册，不覆盖已有密码。本课程设计注册的账号均为管理员。

也保留可选的终端初始化命令 `.\scripts\setup-admin.ps1`，无需与界面注册重复执行。

默认启动 Swing 登录窗口；登录成功进入主窗口，可切换客房管理、空房查询与预订、预订与入住、退房结算、客人与订单查询五个页面。业务操作将在后续阶段开发。退出登录会返回登录窗口。

数据库验证入口仍然保留：

```powershell
java -cp "target/classes;lib/*" com.hotel.Main --check-db
```

在 IDEA 中启动 Main 前，请将工作目录设为项目根目录，并重新加载 Maven 依赖。若使用 Maven，先执行下方的依赖复制命令，再使用初始化脚本。

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
java -cp "target/classes;lib/*" com.hotel.Main --check-db
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

实际预订和退房功能尚未实现。管理员密码使用 PBKDF2-HMAC-SHA256、随机盐和 210000 次迭代保存，不预置默认密码。
