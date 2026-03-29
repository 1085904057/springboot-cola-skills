# COLA 架构开发规范（总纲）

> 📚 **这是 COLA 架构 Skill 体系的根节点**，从宏观角度介绍整体架构。
> 
> **相关子 Skill：**
> - [OpenFeign 外部接口调用规范](./OpenFeign-SKILL.md) - 外部 REST API 集成
> - 🔄 更多子 Skill 待补充...

---

## 一、COLA 架构概述

### 1.1 什么是 COLA

COLA (Clean Object-Oriented and Layered Architecture) 是由阿里巴巴张建飞提出的**整洁面向对象分层架构**。

**核心目标：**
- 提供可落地的业务代码结构规范
- 延缓代码腐烂速度
- 提升团队开发效率

### 1.2 COLA 4.0 组成

1. **COLA 架构（Archetype）**：应用的代码模板和结构规范 ⭐ **本 Skill 重点**
2. **COLA 组件（Components）**：通用组件库（扩展点、异常处理等）

### 1.3 核心原则

- **单一职责**：每层只做自己的事
- **依赖倒置**：Domain 层通过网关解耦 Infra
- **CQRS**：命令和查询分离
- **业务优先**：先按业务分包，再按功能分包
- **纯 POJO**：Domain 层不依赖框架

---

## 二、COLA 分层架构

### 2.1 整体架构图

```
┌─────────────────────────────────────────┐
│         Adapter 层 (适配层)               │ ← Controller、参数校验
└─────────────────────────────────────────┘
                    ↓ 调用
┌─────────────────────────────────────────┐
│          App 层 (应用层)                  │ ← 协调领域模型、流程编排
└─────────────────────────────────────────┘
                    ↓ 调用
┌─────────────────────────────────────────┐
│        Domain 层 (领域层)                 │ ← 核心业务逻辑、领域模型
└─────────────────────────────────────────┘
                    ↑ 实现
┌─────────────────────────────────────────┐
│       Infra 层 (基础设施层)               │ ← 数据库、中间件、外部服务 ⭐
└─────────────────────────────────────────┘
```

**Client SDK**（独立于分层之外，用于服务对外透出）：
```
client/
├── api/    # 服务接口定义
└── dto/    # 数据传输对象
```

### 2.2 各层职责速查表

| 层次 | 职责 | 包含内容 | 依赖框架 |
|------|------|---------|---------|
| **Adapter** | 接收请求、参数校验 | Controller、参数转换 | Spring MVC |
| **Client** | 接口定义、DTO | API 接口、Command/Query/DTO | 无 |
| **App** | 业务协调、流程编排 | Executor、Consumer | Spring |
| **Domain** | **核心业务逻辑** | Entity、DomainService、Gateway | **纯 POJO** |
| **Infra** | 基础设施实现 | GatewayImpl、Mapper、Config | Spring、MyBatis 等 |

---

## 三、各层核心规范

### 3.1 Adapter 层（适配层）

**职责：**处理外部请求，参数校验，调用 App 层。

```java
@RestController
@RequestMapping("/api/customers")
public class CustomerController {
    
    @Autowired
    private CustomerServiceI customerService;
    
    @GetMapping
    public Response listCustomerByName(@RequestParam String name) {
        CustomerListByNameQry qry = new CustomerListByNameQry();
        qry.setName(name);
        return customerService.listByName(qry);
    }
}
```

**关键原则：**
- ✅ 只负责适配，不包含业务逻辑
- ✅ 可以调用多个 App 层服务

---

### 3.2 Client 层（客户端接口层）

**职责：**定义服务接口和 DTO，供外部调用。

```java
// client/api/CustomerServiceI.java
public interface CustomerServiceI {
    Response addCustomer(CustomerAddCmd cmd);
    MultiResponse<CustomerDTO> listByName(CustomerListByNameQry qry);
}

// client/dto/CustomerDTO.java
@Data
public class CustomerDTO implements Serializable {
    private Long id;
    private String name;
    private String email;
}
```

**关键原则：**
- ✅ 仅包含接口定义和 DTO，无实现
- ✅ DTO 必须实现 `Serializable`
- ✅ Command 和 Query 分离（CQRS 模式）

---

### 3.3 App 层（应用层）

**职责：**实现 Client 层接口，协调领域模型，不包含核心业务逻辑。

```java
@Service
@CatchAndLog
public class CustomerServiceImpl implements CustomerServiceI {
    
    @Resource
    private CustomerAddCmdExe customerAddCmdExe;
    
    @Override
    public Response addCustomer(CustomerAddCmd cmd) {
        return customerAddCmdExe.execute(cmd);
    }
}
```

**关键原则：**
- ✅ **先按业务分包，再按功能分包**
- ✅ Command 和 Query 分离处理
- ✅ 不包含核心业务逻辑，只做协调

---

### 3.4 Domain 层（领域层）⭐

**职责：**包含**核心业务逻辑**，领域模型定义，网关接口定义。

```java
// domain/model/Customer.java
@Data
public class Customer {
    private Long id;
    private String name;
    private String email;
    
    // 领域行为
    public void activate() {
        // 激活客户的业务逻辑
    }
}

// domain/gateway/CustomerGateway.java
public interface CustomerGateway {
    Customer getById(Long id);
    void create(Customer customer);
}
```

**关键原则：**
- ✅ **纯 POJO，不依赖 Spring 等框架**
- ✅ 包含核心业务逻辑和业务规则
- ✅ 通过网关接口与基础设施层解耦

---

### 3.5 Infra 层（基础设施层）⭐

**职责：**实现 Domain 层定义的网关接口，数据库访问，第三方服务调用。

```java
// infrastructure/gatewayimpl/CustomerGatewayImpl.java
@Component
public class CustomerGatewayImpl implements CustomerGateway {
    
    @Resource
    private CustomerMapper customerMapper;
    
    @Override
    public Customer getById(Long id) {
        CustomerDO customerDO = customerMapper.selectById(id);
        return convertToEntity(customerDO);
    }
}
```

**关键原则：**
- ✅ 实现 Domain 层定义的网关接口
- ✅ 负责 DTO ↔ DO ↔ Entity 的转换
- ✅ 依赖 Spring、MyBatis 等框架

**外部服务调用规范：** 详见 [OpenFeign-SKILL](./OpenFeign-SKILL.md)

---

## 四、依赖方向规则

### 4.1 依赖倒置原则

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

### 4.2 Maven 模块依赖

```xml
<!-- adapter 依赖 client -->
<dependency>
    <groupId>com.example</groupId>
    <artifactId>demo-client</artifactId>
</dependency>

<!-- app 依赖 client 和 domain -->
<dependency>
    <groupId>com.example</groupId>
    <artifactId>demo-client</artifactId>
</dependency>
<dependency>
    <groupId>com.example</groupId>
    <artifactId>demo-domain</artifactId>
</dependency>

<!-- infra 依赖 domain -->
<dependency>
    <groupId>com.example</groupId>
    <artifactId>demo-domain</artifactId>
</dependency>
```

---

## 五、命名规范

### 5.1 类命名规范

| 类型 | 命名规范 | 示例 |
|------|---------|------|
| Controller | `XXXController` | `CustomerController` |
| Service 接口 | `XXXServiceI` | `CustomerServiceI` |
| Command | `XXXCmd` | `CustomerAddCmd` |
| Query | `XXXQry` | `CustomerListByNameQry` |
| DTO | `XXXDTO` | `CustomerDTO` |
| DO | `XXXDO` | `CustomerDO` |
| Entity | `XXX` | `Customer` |
| Gateway 接口 | `XXXGateway` | `CustomerGateway` |
| Mapper | `XXXMapper` | `CustomerMapper` |

### 5.2 包命名规范（按模块详细版）

**基础包名：** `com.harness.engineering.{module}`

#### 📦 Client 模块（客户端接口层）

```
com.harness.engineering.client.api          # 服务接口定义
com.harness.engineering.client.dto          # 数据传输对象
com.harness.engineering.client.dto.command  # 命令对象（写操作）
com.harness.engineering.client.dto.query    # 查询对象（读操作）
com.harness.engineering.client.dto.response # 响应对象
```

**示例：**
```
com.harness.engineering.client.api.CustomerServiceI
com.harness.engineering.client.dto.CustomerDTO
com.harness.engineering.client.dto.command.CustomerAddCmd
com.harness.engineering.client.dto.query.CustomerListQry
com.harness.engineering.client.dto.response.PageResult
```

---

#### 📦 Adapter 模块（适配层）

```
com.harness.engineering.adapter.web           # Web 控制器
com.harness.engineering.adapter.wireless      # 无线端适配器（可选）
com.harness.engineering.adapter.wap           # WAP 端适配器（可选）
```

**示例：**
```
com.harness.engineering.adapter.web.CustomerController
com.harness.engineering.adapter.web.OrderController
com.harness.engineering.adapter.wireless.MobileCustomerController
```

---

#### 📦 App 模块（应用层）

```
# 按业务分包 + 功能分包
com.harness.engineering.app.{business}.executor     # 命令/查询执行器
com.harness.engineering.app.{business}.consumer     # 消息消费者（可选）
com.harness.engineering.app.{business}.scheduler    # 定时任务（可选）
com.harness.engineering.app.{business}.service      # 应用服务实现

# 通用执行器（跨业务）
com.harness.engineering.app.common.executor
```

**示例（客户管理）：**
```
com.harness.engineering.app.customer.executor.CustomerAddCmdExe
com.harness.engineering.app.customer.executor.CustomerListQryExe
com.harness.engineering.app.customer.executor.CustomerDeleteCmdExe
com.harness.engineering.app.customer.consumer.CustomerEventConsumer
com.harness.engineering.app.customer.service.CustomerServiceImpl
```

**示例（订单管理）：**
```
com.harness.engineering.app.order.executor.OrderCreateCmdExe
com.harness.engineering.app.order.executor.OrderQueryExe
com.harness.engineering.app.order.service.OrderServiceImpl
```

---

#### 📦 Domain 模块（领域层）⭐

```
# 按业务子领域分包
com.harness.engineering.domain.{domain}.model         # 领域实体
com.harness.engineering.domain.{domain}.model.aggregate # 聚合根
com.harness.engineering.domain.{domain}.ability       # 领域能力（Domain Service）
com.harness.engineering.domain.{domain}.gateway       # 领域网关接口
com.harness.engineering.domain.{domain}.event         # 领域事件（可选）

# 通用领域对象
com.harness.engineering.domain.model                  # 通用领域实体
com.harness.engineering.domain.gateway                # 通用领域网关
com.harness.engineering.domain.ability                # 通用领域能力
```

**示例（客户领域）：**
```
com.harness.engineering.domain.customer.model.Customer
com.harness.engineering.domain.customer.model.CustomerId
com.harness.engineering.domain.customer.model.aggregate.CustomerAggregate
com.harness.engineering.domain.customer.ability.CustomerDomainService
com.harness.engineering.domain.customer.ability.CustomerValidator
com.harness.engineering.domain.customer.gateway.CustomerGateway
com.harness.engineering.domain.customer.event.CustomerCreatedEvent
```

**示例（订单领域）：**
```
com.harness.engineering.domain.order.model.Order
com.harness.engineering.domain.order.model.OrderItem
com.harness.engineering.domain.order.gateway.OrderGateway
com.harness.engineering.domain.order.ability.OrderDomainService
```

---

#### 📦 Infrastructure 模块（基础设施层）⭐

```
# 按业务分包 + 功能分包
com.harness.engineering.infrastructure.{domain}.gatewayimpl   # 网关实现
com.harness.engineering.infrastructure.{domain}.mapper        # 数据库访问
com.harness.engineering.infrastructure.{domain}.config        # 配置类
com.harness.engineering.infrastructure.{domain}.converter     # 转换器

# 通用基础设施
com.harness.engineering.infrastructure.config                 # 全局配置
com.harness.engineering.infrastructure.common                 # 通用实现

# 外部服务调用（Feign Client）
com.harness.engineering.infrastructure.client                 # Feign Client 声明
com.harness.engineering.infrastructure.client.dto             # 外部 DTO
com.harness.engineering.infrastructure.client.fallback        # 熔断降级
com.harness.engineering.infrastructure.client.config          # Feign 配置

# 数据对象
com.harness.engineering.infrastructure.mapper.dataobject      # DO 对象
```

**示例（客户基础设施）：**
```
com.harness.engineering.infrastructure.customer.gatewayimpl.CustomerGatewayImpl
com.harness.engineering.infrastructure.customer.mapper.CustomerMapper
com.harness.engineering.infrastructure.customer.mapper.dataobject.CustomerDO
com.harness.engineering.infrastructure.customer.config.CustomerConfig
com.harness.engineering.infrastructure.customer.converter.CustomerConverter
```

**示例（外部用户服务调用）：**
```
com.harness.engineering.infrastructure.client.UserServiceClient
com.harness.engineering.infrastructure.client.dto.UserDTO
com.harness.engineering.infrastructure.client.dto.CreateUserRequest
com.harness.engineering.infrastructure.client.fallback.UserServiceFallbackFactory
com.harness.engineering.infrastructure.config.FeignClientConfig
```

---

#### 📦 Start 模块（启动模块）

```
com.harness.engineering                             # 启动类
com.harness.engineering.config                      # 应用级配置（可选）
```

**示例：**
```
com.harness.engineering.HarnessStartApplication
com.harness.engineering.config.SwaggerConfig
com.harness.engineering.config.GlobalExceptionHandler
```

---

#### 📦 特殊场景包名

**扩展点（Extension）：**
```
com.harness.engineering.app.{business}.extension        # 扩展点实现
com.harness.engineering.app.{business}.extension.point  # 扩展点接口

# 示例：客户校验扩展点
com.harness.engineering.app.customer.extension.CustomerValidatorDefaultExt
com.harness.engineering.app.customer.extension.CustomerValidatorVipExt
com.harness.engineering.app.customer.extension.point.CustomerValidatorExtPt
```

**常量、工具类：**
```
com.harness.engineering.common.constant       # 常量定义
com.harness.engineering.common.util           # 工具类
com.harness.engineering.common.enums          # 枚举定义

# 示例
com.harness.engineering.common.constant.ErrorCodeConstant
com.harness.engineering.common.util.DateUtil
com.harness.engineering.common.enums.CustomerStatusEnum
```

**异常处理：**
```
com.harness.engineering.common.exception      # 异常类定义

# 示例
com.harness.engineering.common.exception.BusinessException
com.harness.engineering.common.exception.SystemException
com.harness.engineering.common.exception.ErrorCode
```

---

## 六、开发流程指引

### 6.1 新增功能的标准流程

1. **定义 DTO**（client/dto）- 请求/响应对象
2. **定义接口**（client/api）- Service 接口方法
3. **实现领域模型**（domain/model）- 实体、聚合根
4. **定义网关接口**（domain/gateway）- 需要的数据操作
5. **实现 App 层**（app/executor）- Command/Query 执行器
6. **实现 Service**（app/ServiceImpl）- 实现 Client 接口
7. **实现网关**（infrastructure/gatewayimpl）- 实现 Domain 网关
8. **实现 Mapper**（infrastructure/mapper）- 数据库操作
9. **实现 Controller**（adapter/web）- RESTful 接口

### 6.2 快速开始

参考项目示例：
- Demo Controller: `harness-adapter/src/main/java/.../DemoController.java`
- Demo Service: `harness-app/src/main/java/.../DemoServiceImpl.java`
- Demo Gateway: `harness-domain/src/main/java/.../DemoGateway.java`
- Demo GatewayImpl: `harness-infrastructure/src/main/java/.../DemoGatewayImpl.java`

---

## 七、最佳实践总结

### ✅ 必须遵守的规则

1. **Domain 层必须是纯 POJO** - 不依赖 Spring 等框架
2. **先按业务分包，再按功能分包** - 便于理解和维护
3. **Command 和 Query 分离** - CQRS 模式
4. **网关接口定义在 Domain 层** - 依赖倒置
5. **Infra 层实现网关接口** - 具体技术实现

### ❌ 禁止出现的错误

1. ❌ 在 App 层写核心业务逻辑
2. ❌ Domain 层依赖 Spring 注解
3. ❌ 先按功能分包，再按业务分包
4. ❌ 在 Adapter 层直接调用 Infra 层
5. ❌ 网关接口定义在 Infra 层

### 🎯 高质量代码特征

- 清晰的层次结构
- 明确的职责划分
- 统一的命名规范
- 完善的注释文档
- 合理的依赖方向

---

## 八、Skill 体系导航

### 📚 COLA 架构 Skill 树

```
COLA-SKILL（总纲）
├── OpenFeign-SKILL（外部接口调用） ⭐ 已创建
├── 🔒 MyBatis-SKILL（数据库访问） - 待创建
├── 🔒 Redis-SKILL（缓存设计） - 待创建
├── 🔒 MQ-SKILL（消息队列） - 待创建
├── 🔒 Exception-SKILL（异常处理） - 待创建
└── 🔒 Extension-SKILL（扩展点设计） - 待创建
```

### 🔗 相关资源

- [OpenFeign 外部接口调用规范](./OpenFeign-SKILL.md)
- [项目 README](../../README.md)
- [快速开始指南](../../QUICKSTART.md)

---

## 九、总结

COLA 架构通过清晰的层次划分和严格的依赖管理，为业务系统开发提供了一套标准化的解决方案。

**记住：架构是手段，不是目的。** 不要为了架构而架构，要根据实际业务需求灵活运用。

**下一步：**
- 👉 需要调用外部 REST API？查看 [OpenFeign-SKILL](./OpenFeign-SKILL.md)
- 🔄 更多子 Skill 敬请期待...
