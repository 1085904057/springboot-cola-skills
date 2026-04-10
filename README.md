# Harness Engineering Platform

基于 COLA (Clean Object-Oriented and Layered Architecture) 架构的业务系统工程

## 📚 架构介绍

COLA 是阿里巴巴张建飞提出的**整洁面向对象分层架构**，本项目的代码结构严格遵循 COLA 4.0 规范。

### 核心优势

- ✅ **清晰的分层架构**：每层职责明确，易于理解和维护
- ✅ **依赖倒置原则**：通过网关接口实现领域层与基础设施层的解耦
- ✅ **CQRS 模式**：Command 和 Query 分离，提升代码可读性
- ✅ **业务优先分包**：先按业务分包，再按功能分包
- ✅ **统一的技术栈**：Spring Boot 3.x + MyBatis Plus + MySQL

## 🏗️ 项目结构

```
{project}/
├── {project}-client/          # Client SDK - 服务接口定义和 DTO
│   ├── src/main/java/
│   │   └── com.{company}.{project}.client/
│   │       ├── api/         # 服务接口定义
│   │       └── dto/         # 数据传输对象 (Command/Query/Response)
│   └── pom.xml
│
├── {project}-adapter/         # Adapter 层 - 适配层（Controller）
│   ├── src/main/java/
│   │   └── com.{company}.{project}.adapter/
│   │       └── web/         # Web 控制器
│   └── pom.xml
│
├── {project}-app/             # App 层 - 应用层（业务协调）
│   ├── src/main/java/
│   │   └── com.{company}.{project}.app/
│   │       ├── executor/    # 命令/查询执行器
│   │       ├── consumer/    # 消息消费者（可选）
│   │       ├── scheduler/   # 定时任务（可选）
│   │       └── service/     # 应用服务实现
│   └── pom.xml
│
├── {project}-domain/          # Domain 层 - 领域层（核心业务逻辑）
│   ├── src/main/java/
│   │   └── com.{company}.{project}.domain/
│   │       ├── model/       # 领域实体
│   │       ├── ability/     # 领域能力（DomainService）
│   │       └── gateway/     # 领域网关接口
│   └── pom.xml
│
├── {project}-infrastructure/  # Infra 层 - 基础设施层
│   ├── src/main/java/
│   │   └── com.{company}.{project}.infrastructure/
│   │       ├── gatewayimpl/ # 网关实现
│   │       ├── mapper/      # 数据库访问层
│   │       ├── config/      # 配置类
│   │       └── converter/   # 转换器
│   └── pom.xml
│
├── {project}-start/           # Start 层 - 应用启动模块
│   ├── src/main/java/
│   │   └── com.harness.engineering/
│   │       └── HarnessStartApplication.java
│   ├── src/main/resources/
│   │   └── application.yml
│   └── pom.xml
│
└── pom.xml                  # 父 POM - 统一依赖管理
```

## 📦 技术栈

### 核心框架
- **Java 21**
- **Spring Boot 3.2.0**
- **COLA Components 4.4.0**

### 数据存储
- **MyBatis Plus 3.5.5**
- **MySQL 8.0.33**
- **Druid 1.2.20**（连接池）

### 工具库
- **Lombok 1.18.30**
- **MapStruct 1.5.5.Final**（对象映射）
- **Hutool 5.8.24**（工具类）
- **FastJSON2 2.0.42**（JSON 处理）
- **Guava 32.1.3**（Google 工具集）

## 🚀 快速开始

### 前置要求
- JDK 21+
- Maven 3.6+
- MySQL 8.0+

### 构建项目

```bash
cd {project}
mvn clean install
```

### 运行应用

```bash
cd {project}-start
mvn spring-boot:run
```

### 访问示例接口

```bash
curl http://localhost:8080/api/demo/welcome
```

预期响应：
```json
{
  "code": "SUCCESS",
  "message": "操作成功",
  "data": "Hello from COLA Architecture!",
  "success": true
}
```

## 📖 开发指南

### 新增功能的标准流程

1. **定义 DTO**（{project}-client/dto）
   - 创建 Command/Query 对象
   - 创建 Response DTO

2. **定义接口**（{project}-client/api）
   - 在 Service 接口中添加方法定义

3. **实现领域模型**（{project}-domain/model）
   - 定义领域实体
   - 实现领域行为

4. **定义网关接口**（{project}-domain/gateway）
   - 定义数据操作接口

5. **实现 App 层**（{project}-app/executor）
   - 实现 Command 执行器
   - 实现 Query 执行器

6. **实现 Service**（{project}-app/service）
   - 实现 Client 层定义的接口

7. **实现网关**（{project}-infrastructure/gatewayimpl）
   - 实现 Domain 层定义的网关接口

8. **实现 Controller**（{project}-adapter/web）
   - 定义 RESTful 接口
   - 调用 App 层服务

### 命名规范

| 类型 | 命名规范 | 示例 |
|------|---------|------|
| Controller | `XXXController` | `CustomerController` |
| Service 接口 | `XXXServiceI` | `CustomerServiceI` |
| Service 实现 | `XXXServiceImpl` | `CustomerServiceImpl` |
| Command | `XXXCmd` | `CustomerAddCmd` |
| Query | `XXXQry` | `CustomerListByNameQry` |
| DTO | `XXXDTO` | `CustomerDTO` |
| DO | `XXXDO` | `CustomerDO` |
| Entity | `XXX` | `Customer` |
| Gateway 接口 | `XXXGateway` | `CustomerGateway` |
| Gateway 实现 | `XXXGatewayImpl` | `CustomerGatewayImpl` |

## 🔗 依赖关系

```
Adapter → Client → App → Domain ← Infra
                              ↑
                              |
                          实现网关
```

**核心规则：**
- ✅ 上层可以调用下层
- ✅ **下层不能依赖上层**
- ✅ Domain 层通过网关接口与 Infra 层解耦
- ✅ Infra 层实现 Domain 层定义的网关接口

## 📚 参考文档

- [COLA 架构官方文档](https://github.com/alibaba/COLA)
- [如何保证同事的代码不会腐烂？一文带你了解 COLA 架构](https://cloud.tencent.com/developer/article/1971122)
- [COLA-SKILL.md](./.agent/skills/architecture/ARCHITECTURE-SKILL) - 详细的 COLA 架构 Skill 文档

## 👥 团队协作

本项目使用 COLA 架构作为统一的开发规范，确保：
- 代码结构清晰，易于维护
- 团队成员都能快速上手
- 延缓代码腐烂速度
- 提升整体开发效率

## 📝 License

Copyright © 2026 Harness Engineering Team
