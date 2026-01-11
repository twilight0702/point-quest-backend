# PointQuest Backend

PointQuest 积分任务与兑换系统的后端服务，覆盖任务提交、积分账户与流水、商品兑换、购物车结算、站内消息与管理端能力。

## 功能范围
- 任务：发布、提交、审核、发放积分
- 积分：账户余额与积分流水
- 商城：奖池、商品、库存、订单
- 购物车：Redis 临时购物车与结算事务
- 消息：站内信与未读计数
- 管理端：任务、奖励、库存、奖池、消息（订单可选）

## 技术栈
- Java 21、Spring Boot 4.x
- MySQL 8、Redis 6
- MyBatis-Plus
- Spring Security + JWT（HttpOnly Cookie）
- MinIO（可选）

## 目录结构
- `src/main/java/com/twilight/pointquestbackend`：后端代码
- `src/main/resources`：运行配置与 MyBatis XML
- `src/test/java/com/twilight/pointquestbackend`：测试
- `sql/`：数据库建表与初始化脚本
- `docs/`：设计与需求文档

## 本地运行
1) 准备依赖：Java 21、MySQL 8、Redis 6（可选 MinIO）
2) 初始化数据库：
   - 建表：`sql/schema.sql`
   - 初始化数据：`sql/init_data.sql`
   - 或直接执行：`sql/point_quest.sql`
3) 创建本地配置：
   - 复制 `src/main/resources/application.yml.template`
     到 `src/main/resources/application.yml`
4) 启动服务：
```bash
./mvnw spring-boot:run
```

## 配置说明
`application.yml` 已被 Git 忽略，请用环境变量覆盖敏感配置。

常用环境变量：
- `POINTQUEST_DB_URL`
- `POINTQUEST_DB_USERNAME` / `POINTQUEST_DB_PASSWORD`
- `POINTQUEST_REDIS_HOST` / `POINTQUEST_REDIS_PORT` / `POINTQUEST_REDIS_PASSWORD`
- `POINTQUEST_STORAGE_PROVIDER`
- `POINTQUEST_MINIO_ENDPOINT` / `POINTQUEST_MINIO_ACCESS_KEY` / `POINTQUEST_MINIO_SECRET_KEY`
- `POINTQUEST_JWT_SECRET` / `POINTQUEST_JWT_EXPIRES_MINUTES`

## 常用命令
```bash
./mvnw test
./mvnw package
```

## 相关文档
- 设计稿：`docs/design.md`
- 需求文档：`docs/requiremen.md`
- 开发步骤：`docs/backend_step.md`
