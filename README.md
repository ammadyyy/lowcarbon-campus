# 低碳校园资源循环流转桌面系统

> 2026 软件构造课程分组实验 · 迭代二交付物
> Java 8 兼容 · Swing（迭代三）· SQLite · Maven · JUnit 5

本项目面向校园场景，让闲置教材、数码配件、文体用品等物品在学生之间**发布、检索、收藏、流转**，用可运行的桌面程序演示"资源循环"的完整业务闭环。迭代二完成了**实体业务模块 + 数据库访问层 + JUnit 单元测试 + 基础数据存取**，可在控制台完整跑通"注册 → 登录 → 发布 → 检索 → 收藏 → 上下架 → 统计"流程。

---

## 1. 快速开始

### 1.1 环境

| 项目 | 要求 | 本机验证环境 |
| --- | --- | --- |
| JDK | 8 及以上（源码按 Java 8 编译，可在 8/11/17/21 运行） | JDK 21.0.10 与 JDK 1.8.0_212 均验证通过 |
| 构建工具 | Maven 3.6+ | Maven 3.9.9（分别配 JDK 21 与 JDK 8 编译打包，两次均通过） |
| 数据库 | SQLite（由 `sqlite-jdbc` 驱动内嵌，无需安装） | sqlite-jdbc 3.45.3.0 |

### 1.2 从源码构建

```bash
cd lowcarbon-campus
mvn clean package
```

产物：

- `target/lowcarbon-campus-0.2.0.jar` —— 含 SQLite 驱动的可执行 fat jar（可直接 `java -jar` 运行）
- `target/surefire-reports/` —— 单元测试报告（HTML + TXT）

> 首次构建需要联网下载依赖（sqlite-jdbc、slf4j、junit 等，约 15 MB）。只想跑测试用 `mvn test`，只打包用 `mvn clean package -DskipTests`。
> 仓库不提交 `target/`（构建产物）与 `data/`（运行时数据库），克隆后先构建再运行。

### 1.3 运行

```bash
java -jar target/lowcarbon-campus-0.2.0.jar demo   # 自动演示 6 个业务场景
java -jar target/lowcarbon-campus-0.2.0.jar        # 控制台交互菜单
```

首次运行会自动创建 `data/campus.db`、建表、写入分类与管理员账号，并依次演示 6 个模块。重复执行不会产生重复数据（脚本内置幂等判断）。

### 1.4 账号与编码

默认管理员：`admin / admin123`（首次登录后请修改密码）。演示账号：`demo_owner / demo1234`、`demo_buyer / demo1234`。

若控制台中文乱码，加编码参数启动：`java "-Dfile.encoding=UTF-8" -jar target/lowcarbon-campus-0.2.0.jar demo`。

---

## 2. 目录结构

```
lowcarbon-campus/
├── pom.xml                          Maven 配置（Java 8 release、shade fat jar、surefire）
├── db/
│   ├── schema.sql                   建表脚本（7 张表 + 索引 + 分类初始数据）
│   └── seed_data.sql                内置数据（管理员账号 + 分类）
├── docs/
│   ├── 迭代一与迭代二说明.docx        需求、UML、数据库设计、测试用例与结论
│   ├── 作业要求原文.txt              作业说明（从 docx 提取，便于检索）
│   └── 运行结果-*.txt                本机 JDK 21 / JDK 8 的实际运行输出
└── src/
    ├── main/java/com/campus/lowcarbon/
    │   ├── App.java                 程序入口（初始化 → 播种数据 → 控制台/demo）
    │   ├── ConsoleApp.java          控制台交互菜单（迭代三替换为 Swing）
    │   ├── DemoRunner.java          6 段业务演示
    │   ├── config/AppConfig.java    配置读取（系统属性 > config.properties > 默认值）
    │   ├── common/                  异常体系、PBKDF2 口令、通用校验器
    │   ├── domain/                  实体与值对象（User/Item/Category + 4 个枚举）
    │   ├── dao/                     DAO 接口、查询对象、单例、工厂、模板方法、排序策略
    │   │   └── jdbc/                三个 JDBC 实现（JdbcUserDao/JdbcItemDao/JdbcCategoryDao）
    │   └── service/                 业务服务（UserService/ItemService/CategoryService/DataSeeder）
    ├── main/resources/db/schema.sql 打包进 jar 的建表脚本
    └── test/java/com/campus/lowcarbon/
        ├── common/                  口令与校验器测试
        ├── domain/                  实体与状态机测试
        ├── service/                 业务规则测试（内存 DAO 替身）
        ├── dao/                     JDBC 集成测试（@TempDir 临时库）
        └── support/                 TestContext + 三个内存 DAO 替身
```

---

## 3. 架构与设计

### 3.1 分层

```
App / ConsoleApp（表现层，迭代三换 Swing）
        ↓ 只依赖 service
service（业务规则：注册登录、发布校验、状态机、权限、收藏）
        ↓ 只依赖 dao 接口
dao（DAO 接口 + ItemQuery + DaoFactory）/ dao.jdbc（JDBC 实现）
        ↓ 只依赖 domain + common
domain（不可变实体、值对象、枚举与状态机）
common（异常、口令、校验、配置）
```

依赖方向单向向下，`service` 与 `dao` 之间靠接口解耦：单元测试用 `InMemoryUserDao/ItemDao/CategoryDao` 替换真实实现，因此业务规则测试**不启动数据库**，秒级完成。

### 3.2 设计模式落地（作业要求的"至少 3 种"）

| 模式 | 落点 | 解决的问题 |
| --- | --- | --- |
| 单例 | `dao/Database.java`（双重检查锁 + `volatile`） | 全局唯一连接门面，避免连接参数散落各处 |
| 工厂 | `dao/DaoFactory.java` | 集中创建 DAO，测试时可整体替换实现 |
| 模板方法 | `dao/BaseJdbcDao.java`（`withConnection`/`inTransaction`/`generatedKey`） | 把获取连接、提交、回滚、关流等样板代码收敛到父类 |
| 策略 | `dao/ItemSortStrategy` + `dao/SortOrder`（`NEWEST/PRICE_ASC/PRICE_DESC/POPULAR`） | 排序规则可替换，新增排序无需改 DAO 的 SQL 拼接逻辑 |
| 建造者 | `User/Item/Category/ItemQuery/ItemDraft` 的 Builder | 多参数构造 + 不可变对象 + 构造期校验 |

### 3.3 防御性编程要点

- **实体不可变**：所有字段 `final`，修改一律返回新对象（`withXxx`/`toBuilder`），避免共享可变状态。
- **构造期校验**：`Builder.build()` 内校验标题长度、价格精度、信用分范围、用户名正则等，非法对象根本创建不出来。
- **异常分层**：`BusinessException`（带 `code`）→ `ValidationException/DuplicateException/NotFoundException/AuthException/PermissionException/DaoException`，上层按类型给出提示，不用 `e.getMessage().contains(...)` 判断。
- **状态机**：`ItemStatus.canTransitionTo()` 只允许 `在架→下架/交易中`、`交易中→完成/在架`、`下架→在架`，非法迁移直接抛异常。
- **权限校验**：`Item.ensureOwnedBy()` 保证只有发布者能改/下架/删除；管理员封禁规则单独校验。
- **事务**：收藏关系与 `favorite_count` 在同一事务内增减，异常自动回滚（`inTransaction` + `addSuppressed`）。
- **SQL 注入防护**：全部使用 `PreparedStatement` 参数化；模糊查询对 `% _ \` 做转义并使用 `ESCAPE '\'`。
- **口令安全**：PBKDF2-HMAC-SHA256、120000 次迭代、16 字节随机盐、256 位摘要，`MessageDigest.isEqual` 常量时间比较，登录失败信息不区分"用户不存在/口令错误"。

### 3.4 数据库设计（7 张表）

| 表 | 用途 | 迭代二状态 |
| --- | --- | --- |
| `users` | 账号、PBKDF2 散列、角色、信用分、封禁 | ✅ 使用 |
| `categories` | 物品分类（管理员维护） | ✅ 使用 |
| `items` | 闲置物品、价格、流转方式、成色、状态、标签、收藏数 | ✅ 使用 |
| `favorites` | 用户收藏（联合主键，级联删除） | ✅ 使用 |
| `exchange_records` | 供需流转记录 | 迭代三接入 |
| `reviews` | 交易评价与信用分 | 迭代三接入 |
| `operation_logs` | 关键操作审计 | 迭代四接入 |

约束与索引：用户名/分类名 `UNIQUE`；`role/status/trade_type/condition_level` 用 `CHECK` 限定取值；`price >= 0`、`credit_score BETWEEN 0 AND 100`；外键 `ON DELETE CASCADE`（删除用户级联删除其物品与收藏）；`items` 建 owner/category/status/title 四个索引。

---

## 4. 功能清单（迭代二）

| 模块 | 能力 |
| --- | --- |
| 账号 | 注册（用户名/口令/联系方式校验）、登录、改密码、更新联系方式、信用分调整、封禁/解封、管理员注册、按 id/用户名查询、用户列表 |
| 物品 | 发布（分类/标题/描述/价格/流转方式/成色/标签）、修改（权限 + 可编辑状态校验）、下架、重新上架、删除（交易中禁止）、按 id 查询、我的物品 |
| 检索 | 关键词（标题/描述/标签模糊匹配）、分类、价格区间、流转方式、状态、排序（最新/价格升降/最热）、分页（limit/offset）、总数统计 |
| 收藏 | 收藏、取消收藏、是否已收藏、我的收藏、收藏计数（事务保证一致） |
| 统计 | 用户数、物品总数、在架物品数、分类数 |
| 数据 | 首次启动自动建库建表、播种管理员与 5 个分类、`db/schema.sql` 与 `db/seed_data.sql` 可手工执行 |

控制台菜单：游客（注册/登录/退出），会员（浏览在架物品/关键词检索/发布/我的物品/收藏/我的收藏/改密码/退出登录）。

---

## 5. 测试

```bash
cd lowcarbon-campus
mvn test
```

当前结果：**60 个用例全部通过**（`Tests run: 60, Failures: 0, Errors: 0`）。

| 测试类 | 用例数 | 覆盖内容 |
| --- | --- | --- |
| `PasswordsTest` | 7 | 散列格式、盐随机性、正确/错误口令、非法编码串、随机口令 |
| `ValidatorsTest` | 8 | 用户名/口令/价格/文本长度/标签规范化 |
| `UserDomainTest` | 6 | Builder 校验、封禁、信用分裁剪、不可变 withXxx |
| `ItemDomainTest` | 5 | 标题/价格校验、状态机合法与非法迁移、权限异常 |
| `UserServiceTest` | 9 | 注册重复、登录失败一致性、封禁、改密码、信用分、管理员规则 |
| `ItemServiceTest` | 8 | 发布校验、越权、上下架状态机、交易中禁删、收藏规则、检索 |
| `JdbcUserDaoTest` | 4 | 增删改查、唯一约束翻译、封禁持久化 |
| `JdbcItemDaoTest` | 9 | 关键词/价格/类型筛选、排序、分页、状态过滤、更新、收藏事务、删除 |
| `DatabaseSchemaTest` | 4 | 7 张表建表、幂等初始化、外键级联、目录自动创建 |

---

## 6. 迭代路线

| 迭代 | 内容 | 状态 |
| --- | --- | --- |
| 迭代一（1-2 周） | 需求分析、UML 类图/时序图、数据库表设计、模块划分与分工 | ✅ 完成（见 `docs/迭代一与迭代二说明.docx`） |
| 迭代二（3-4 周） | 实体业务模块、数据库访问层、JUnit 单元测试、基础数据存取 | ✅ 本次交付 |
| 迭代三（5-6 周） | Swing GUI、供需沟通与流转记录、评价与信用分、集成测试 | ⏳ 计划中 |
| 迭代四（7-8 周） | 代码重构、CSV 导出与统计报表、打包发布、报告与 PPT | ⏳ 计划中 |

---

## 7. 说明与限制

- 源码统一 UTF-8 编码，`pom.xml` 已固定 `sourceEncoding=UTF-8`，surefire 也传入了 `-Dfile.encoding=UTF-8`。若在旧版控制台看到日志乱码，加 `-Dfile.encoding=UTF-8` 启动即可（不含中文的界面逻辑不受影响）。
- 迭代二的表现层是控制台（`ConsoleApp`），与 Swing 无关，迭代三只需把菜单替换为窗口并复用现有 service。
- `tags` 目前以逗号分隔存于 `items` 一列，是刻意的简化；迭代四重构时可拆为标签表并加分页缓存。
- 仓库只收录源码、脚本与文档：`target/`（jar 与测试报告）和 `data/campus.db`（运行时数据库）已写入 `.gitignore`，`mvn clean package` 可随时重新生成；首次运行数据库会自动建库建表，不需要提交二进制库文件。
