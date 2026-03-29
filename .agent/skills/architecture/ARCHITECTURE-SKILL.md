# COLA 架构开发规范（总纲）

> 📚 **这是 COLA 架构 Skill 体系的根节点**，从宏观角度介绍整体架构。
> 
> **相关子 Skill：**
> - [OpenFeign 外部接口调用规范](./RESTCALL-SKILL) - 外部 REST API 集成
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

**外部服务调用规范：** 详见 [OpenFeign-SKILL](./RESTCALL-SKILL)

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

**重要原则：先按模块分，再按类名和包名分**

每个模块独立组织，包含该模块的完整命名规范（类名 + 包名）。

---

### 5.1 Client 模块（客户端接口层）

**职责：** 定义服务接口和 DTO，供外部调用。

#### 📦 包结构

**规则：先按业务分包，再按功能分包**

```
com.{company}.{project}.client
├── {business}              # 按业务分包（如 customer, order）
│   ├── api                 # 业务服务接口
│   └── dto                 # 业务 DTO
│       ├── command         # 命令对象
│       ├── query           # 查询对象
│       └── response        # 响应对象
└── common                  # 通用对象（跨业务）
    ├── api
    └── dto
```

#### 🏷️ 类命名规范

| 类型 | 命名规范 | 示例 |
|------|---------|------|
| Service 接口 | `XXXServiceI` | `CustomerServiceI` |
| Command | `XXXCmd` | `CustomerAddCmd` |
| Query | `XXXQry` | `CustomerListByNameQry` |
| DTO | `XXXDTO` | `CustomerDTO` |
| Response | `Response` / `XXXResponse` | `Response`, `PageResult` |

#### 💡 完整示例（客户管理）

```java
// 包：com.{company}.{project}.client.customer.api
public interface CustomerServiceI {
    Response addCustomer(CustomerAddCmd cmd);
    MultiResponse<CustomerDTO> listByName(CustomerListByNameQry qry);
}

// 包：com.{company}.{project}.client.customer.dto.command
@Data
public class CustomerAddCmd implements Command {
    private String name;
    private String email;
}

// 包：com.{company}.{project}.client.customer.dto.query
@Data
public class CustomerListByNameQry implements Query {
    private String name;
}

// 包：com.{company}.{project}.client.customer.dto
@Data
public class CustomerDTO implements Serializable {
    private Long id;
    private String name;
    private String email;
}
```

#### 💡 完整示例（订单管理）

```java
// 包：com.{company}.{project}.client.order.api
public interface OrderServiceI {
    Response createOrder(OrderCreateCmd cmd);
    OrderDTO getOrderById(Long orderId);
}

// 包：com.{company}.{project}.client.order.dto.command
@Data
public class OrderCreateCmd implements Command {
    private Long customerId;
    private List<OrderItemDTO> items;
}

// 包：com.{company}.{project}.client.order.dto
@Data
public class OrderDTO implements Serializable {
    private Long id;
    private String orderNo;
    private BigDecimal totalAmount;
}
```

---

### 5.2 Adapter 模块（适配层）

**职责：** 处理外部请求，参数校验，调用 App 层。

#### 📦 包结构

**规则：先按业务分包，再按功能分包**

```
com.{company}.{project}.adapter
├── {business}              # 按业务分包（如 customer, order）
│   └── web                 # Web 控制器
├── common                  # 通用控制器（可选）
│   └── web
├── wireless                # 无线端适配器（可选）
└── wap                     # WAP 端适配器（可选）
```

#### 🏷️ 类命名规范

| 类型 | 命名规范 | 示例 |
|------|---------|------|
| Controller | `XXXController` | `CustomerController` |
| Request/VO | `XXXRequest` / `XXXVO` | `CreateCustomerRequest`, `CustomerVO` |

#### 💡 完整示例（客户管理）

```java
// 包：com.{company}.{project}.adapter.customer.web
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

#### 💡 完整示例（订单管理）

```java
// 包：com.{company}.{project}.adapter.order.web
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    
    @Autowired
    private OrderServiceI orderService;
    
    @PostMapping
    public Response createOrder(@RequestBody CreateOrderRequest request) {
        OrderCreateCmd cmd = convertToCmd(request);
        return orderService.createOrder(cmd);
    }
}
```

---

### 5.3 App 模块（应用层）

**职责：** 实现 Client 层接口，协调领域模型，不包含核心业务逻辑。

#### 📦 包结构

**规则：先按业务分包，再按功能分包**

```
com.{company}.{project}.app
├── {business}              # 按业务分包（如 customer, order）
│   ├── executor            # 命令/查询执行器
│   ├── consumer            # 消息消费者（可选）
│   ├── scheduler           # 定时任务（可选）
│   └── service             # 应用服务实现
└── common                  # 通用执行器（跨业务）
    └── executor
```

#### 🏷️ 类命名规范

| 类型 | 命名规范 | 示例 |
|------|---------|------|
| Service 实现 | `XXXServiceImpl` | `CustomerServiceImpl` |
| Command 执行器 | `XXXCmdExe` | `CustomerAddCmdExe` |
| Query 执行器 | `XXXQryExe` | `CustomerListQryExe` |
| Consumer | `XXXConsumer` | `CustomerEventConsumer` |
| Scheduler | `XXXScheduler` | `CustomerSyncScheduler` |

#### 💡 完整示例（客户管理）

```java
// 包：com.{company}.{project}.app.customer.service
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

// 包：com.{company}.{project}.app.customer.executor
@Component
public class CustomerAddCmdExe {
    
    @Resource
    private CustomerGateway customerGateway;
    
    public Response execute(CustomerAddCmd cmd) {
        Customer customer = convertToEntity(cmd);
        customerGateway.create(customer);
        return Response.buildSuccess();
    }
}
```

---

### 5.4 Domain 模块（领域层）⭐

**职责：** 包含**核心业务逻辑**，领域模型定义，网关接口定义。

#### 📦 包结构

**规则：按业务子领域分包**

```
com.{company}.{project}.domain
├── {domain}                # 按领域分包（如 customer, order）
│   ├── model               # 领域实体
│   │   └── aggregate       # 聚合根
│   ├── ability             # 领域能力（Domain Service）
│   ├── gateway             # 领域网关接口
│   └── event               # 领域事件（可选）
└── model                   # 通用领域实体
```

#### 🏷️ 类命名规范

| 类型 | 命名规范 | 示例 |
|------|---------|------|
| Entity | `XXX` | `Customer`, `Order` |
| Value Object | `XXX` / `XXXValue` | `Address`, `Money` |
| Aggregate Root | `XXXAggregate` | `CustomerAggregate` |
| Domain Service | `XXXDomainService` | `CustomerDomainService` |
| Gateway 接口 | `XXXGateway` | `CustomerGateway` |
| Domain Event | `XXXEvent` | `CustomerCreatedEvent` |
| ID 对象 | `XXXId` | `CustomerId`, `OrderId` |

#### 💡 完整示例（客户领域）

```java
// 包：com.{company}.{project}.domain.customer.model
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

// 包：com.{company}.{project}.domain.customer.gateway
public interface CustomerGateway {
    Customer getById(Long id);
    void create(Customer customer);
    void update(Customer customer);
    void delete(Long id);
}

// 包：com.{company}.{project}.domain.customer.ability
@Component
public class CustomerDomainService {
    
    @Resource
    private CustomerGateway customerGateway;
    
    public Customer createCustomer(String name, String email) {
        // 复杂的领域逻辑
        Customer customer = new Customer();
        customer.setName(name);
        customer.setEmail(email);
        
        // 业务规则校验
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new BusinessException("邮箱格式不正确");
        }
        
        return customer;
    }
}
```

---

### 5.5 Infrastructure 模块（基础设施层）⭐

**职责：** 实现 Domain 层定义的网关接口，数据库访问，第三方服务调用。

#### 📦 包结构

**规则：按业务分包 + 功能分包**

```
com.{company}.{project}.infrastructure
├── {domain}                    # 按业务分包
│   ├── gatewayimpl             # 网关实现
│   ├── mapper                  # 数据库访问
│   │   └── dataobject          # DO 对象
│   ├── config                  # 配置类
│   └── converter               # 转换器
├── client                      # 外部服务调用（Feign Client）
│   ├── {service}Client         # Feign Client 接口
│   ├── dto                     # 外部 DTO
│   ├── fallback                # 熔断降级
│   └── config                  # Feign 配置
├── config                      # 全局配置
└── common                      # 通用实现
```

#### 🏷️ 类命名规范

| 类型 | 命名规范 | 示例 |
|------|---------|------|
| Gateway 实现 | `XXXGatewayImpl` | `CustomerGatewayImpl` |
| Mapper | `XXXMapper` | `CustomerMapper` |
| Data Object | `XXXDO` | `CustomerDO` |
| Converter | `XXXConverter` | `CustomerConverter` |
| Feign Client | `XXXClient` | `UserServiceClient` |
| FallbackFactory | `XXXFallbackFactory` | `UserServiceFallbackFactory` |
| Config | `XXXConfig` | `CustomerConfig`, `FeignClientConfig` |

#### 💡 完整示例（客户基础设施）

```java
// 包：com.{company}.{project}.infrastructure.customer.gatewayimpl
@Component
public class CustomerGatewayImpl implements CustomerGateway {
    
    @Resource
    private CustomerMapper customerMapper;
    
    @Override
    public Customer getById(Long id) {
        CustomerDO customerDO = customerMapper.selectById(id);
        return convertToEntity(customerDO);
    }
    
    @Override
    public void create(Customer customer) {
        CustomerDO customerDO = convertToDO(customer);
        customerMapper.insert(customerDO);
    }
}

// 包：com.{company}.{project}.infrastructure.customer.mapper
@Mapper
public interface CustomerMapper {
    CustomerDO selectById(Long id);
    List<CustomerDO> findByName(String name);
    int insert(CustomerDO customerDO);
    int update(CustomerDO customerDO);
    int deleteById(Long id);
}

// 包：com.{company}.{project}.infrastructure.customer.mapper.dataobject
@Data
@TableName("customer")
public class CustomerDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String email;
    private Date createTime;
    private Date updateTime;
}
```

#### 💡 完整示例（外部服务调用 - OpenFeign）

```java
// 包：com.{company}.{project}.infrastructure.client
@FeignClient(
    name = "user-service",
    url = "${external.user-service.base-url}",
    configuration = FeignClientConfig.class
)
public interface UserServiceClient {
    
    @GetMapping("/api/v2/users/{id}")
    ApiResponse<UserDTO> getUserById(@PathVariable("id") Long id);
    
    @PostMapping("/api/v2/users")
    ApiResponse<UserDTO> createUser(@RequestBody CreateUserRequest request);
}

// 包：com.{company}.{project}.infrastructure.client.dto
@Data
public class UserDTO implements Serializable {
    private Long id;
    private String name;
    private String email;
}

// 包：com.{company}.{project}.infrastructure.client.fallback
@Component
public class UserServiceFallbackFactory implements FallbackFactory<UserServiceClient> {
    @Override
    public UserServiceClient create(Throwable cause) {
        return new UserServiceClient() {
            @Override
            public ApiResponse<UserDTO> getUserById(Long id) {
                ApiResponse<UserDTO> response = new ApiResponse<>();
                response.setSuccess(false);
                response.setMessage("用户服务暂时不可用");
                return response;
            }
            // ... 其他方法
        };
    }
}
```

---

### 5.6 Start 模块（启动模块）

**职责：** 应用启动入口，全局配置项。

#### 📦 包结构

```
com.{company}.{project}
├── {Application}             # 启动类
└── config                    # 应用级配置（可选）
```

#### 🏷️ 类命名规范

| 类型 | 命名规范 | 示例 |
|------|---------|------|
| Application | `{Project}Application` | `HarnessStartApplication` |
| Config | `XXXConfig` | `SwaggerConfig`, `WebMvcConfig` |
| ExceptionHandler | `GlobalExceptionHandler` | `GlobalExceptionHandler` |

#### 💡 完整示例

```java
// 包：com.{company}.{project}
@SpringBootApplication
@ComponentScan(basePackages = "com.{company}.{project}")
@EnableFeignClients(basePackages = "com.{company}.{project}.infrastructure.client")
public class HarnessStartApplication {
    public static void main(String[] args) {
        SpringApplication.run(HarnessStartApplication.class, args);
    }
}

// 包：com.{company}.{project}.config
@Configuration
public class SwaggerConfig {
    @Bean
    public Docket createRestApi() {
        return new Docket(DocumentationType.OAS_30)
            .apiInfo(apiInfo())
            .select()
            .apis(RequestHandlerSelectors.basePackage("com.{company}.{project}.adapter.web"))
            .paths(PathSelectors.any())
            .build();
    }
}
```

---

### 5.7 Common 模块（通用模块）

**职责：** 常量、工具类、枚举、异常等通用组件。

#### 📦 包结构

```
com.{company}.{project}.common
├── constant                  # 常量定义
├── util                      # 工具类
├── enums                     # 枚举定义
└── exception                 # 异常类定义
```

#### 🏷️ 类命名规范

| 类型 | 命名规范 | 示例 |
|------|---------|------|
| Constant | `XXXConstant` / `XXXConst` | `ErrorCodeConstant`, `SystemConst` |
| Util | `XXXUtil` / `XXXUtils` | `DateUtil`, `BeanUtils` |
| Enum | `XXXEnum` | `CustomerStatusEnum`, `OrderStatusEnum` |
| Exception | `XXXException` | `BusinessException`, `SystemException` |
| ErrorCode | `ErrorCode` | `ErrorCode` |

#### 💡 完整示例

```java
// 包：com.{company}.{project}.common.constant
public class ErrorCodeConstant {
    public static final String SUCCESS = "200";
    public static final String BUSINESS_ERROR = "500";
    public static final String SYSTEM_ERROR = "501";
}

// 包：com.{company}.{project}.common.util
public class DateUtil {
    public static String format(Date date, String pattern) {
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        return sdf.format(date);
    }
}

// 包：com.{company}.{project}.common.enums
public enum CustomerStatusEnum {
    ACTIVE("活跃"),
    INACTIVE("未激活"),
    LOCKED("锁定");
    
    private final String description;
    
    CustomerStatusEnum(String description) {
        this.description = description;
    }
}

// 包：com.{company}.{project}.common.exception
public class BusinessException extends RuntimeException {
    private String code;
    
    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }
}
```

---

### 5.8 Extension 扩展点（可选）

**职责：** 处理业务的差异化需求，支持灵活扩展。

#### 📦 包结构

```
com.{company}.{project}.app.{business}
├── extension                 # 扩展点实现
└── extension.point           # 扩展点接口
```

#### 🏷️ 类命名规范

| 类型 | 命名规范 | 示例 |
|------|---------|------|
| Extension Point | `XXXExtPt` | `CustomerValidatorExtPt` |
| Extension 实现 | `XXX{Biz}Ext` | `CustomerValidatorVipExt` |

#### 💡 完整示例

```java
// 包：com.{company}.{project}.app.customer.extension.point
public interface CustomerValidatorExtPt extends ExtensionPointI {
    void validate(CustomerAddCmd cmd);
}

// 包：com.{company}.{project}.app.customer.extension
@Component
@Extension(bizId = "default")
public class CustomerValidatorDefaultExt implements CustomerValidatorExtPt {
    @Override
    public void validate(CustomerAddCmd cmd) {
        // 默认校验逻辑
    }
}

// 包：com.{company}.{project}.app.customer.extension
@Component
@Extension(bizId = "vip")
public class CustomerValidatorVipExt implements CustomerValidatorExtPt {
    @Override
    public void validate(CustomerAddCmd cmd) {
        // VIP 客户特殊校验逻辑
    }
}
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

- [OpenFeign 外部接口调用规范](./RESTCALL-SKILL)
- [项目 README](../../README.md)
- [快速开始指南](../../QUICKSTART.md)

---

## 九、总结

COLA 架构通过清晰的层次划分和严格的依赖管理，为业务系统开发提供了一套标准化的解决方案。

**记住：架构是手段，不是目的。** 不要为了架构而架构，要根据实际业务需求灵活运用。

**下一步：**
- 👉 需要调用外部 REST API？查看 [OpenFeign-SKILL](./RESTCALL-SKILL)
- 🔄 更多子 Skill 敬请期待...
