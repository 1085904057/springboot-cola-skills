# Client 层规范（Client Module Specification）

> **用途：** 定义 Client 层（接口契约层）的职责、内容、依赖规则  
> **适用场景：** 所有业务域的 Client 模块开发  
> **复用性：** ⭐⭐⭐⭐⭐ 所有业务域可复用

---

## 一、职责定位

**Client 层是业务域对外的唯一出口，定义服务契约。**

**核心职责：**
- 定义 Service 接口（供其他模块调用）
- 定义 DTO 对象（Command、Query、Response、DTO）
- 定义对外暴露的枚举和常量

**关键原则：**
- ✅ 只包含接口定义和数据对象
- ❌ 不包含任何实现逻辑
- ❌ 不依赖 Spring 等框架

---

## 二、应包含的内容

### 2.1 Service 接口

**位置：** `api/` 包

**命名规范：** `XXXServiceI`

**示例命名：**
- UserServiceI
- ProductServiceI
- OrderServiceI

**设计原则：**
- 接口方法使用 Command/Query 模式
- 写操作使用 Command 参数
- 读操作使用 Query 参数
- 返回值统一使用 Response 封装

---

### 2.2 DTO 对象

#### Command（命令对象）

**位置：** `dto/command/` 包

**命名规范：** `XXXCmd`

**用途：** 封装写操作的请求参数

**示例命名：**
- CreateUserCmd
- CreateOrderCmd
- DeductStockCmd

**设计原则：**
- 实现 Command 标记接口
- 包含校验注解（如 @NotBlank、@Email）
- 不包含业务逻辑

---

#### Query（查询对象）

**位置：** `dto/query/` 包

**命名规范：** `XXXQry`

**用途：** 封装读操作的请求参数

**示例命名：**
- GetUserQry
- ListProductsQry
- GetOrderByIdQry

**设计原则：**
- 实现 Query 标记接口
- 支持分页参数（pageNum、pageSize）
- 支持排序参数（sortField、sortOrder）

---

#### Response（响应对象）

**位置：** `dto/response/` 包或直接放在 `dto/` 包

**命名规范：** 
- 通用响应：`Response`
- 分页响应：`PageResult<T>`
- 列表响应：`MultiResponse<T>`

**设计原则：**
- 统一响应格式（code、message、data）
- 支持泛型
- 提供成功/失败的工厂方法

---

#### DTO（数据传输对象）

**位置：** `dto/` 包

**命名规范：** `XXXDTO`

**用途：** 在模块间传输数据

**示例命名：**
- UserDTO
- ProductDTO
- OrderDTO

**设计原则：**
- 实现 Serializable 接口
- 只包含数据字段，不包含业务逻辑
- 与 Domain Entity 分离

---

## 三、禁止的内容

### 3.1 禁止包含实现类

- ❌ ServiceImpl
- ❌ Executor
- ❌ 任何业务逻辑实现

**原因：** Client 层只定义契约，不包含实现。

---

### 3.2 禁止包含基础设施代码

- ❌ Mapper 接口
- ❌ DAO 接口
- ❌ Repository 接口

**原因：** 基础设施属于 Infrastructure 层。

---

### 3.3 禁止包含 Domain Entity

- ❌ 领域实体
- ❌ 聚合根
- ❌ 值对象

**原因：** Domain Entity 属于 Domain 层，Client 层使用 DTO。

---

### 3.4 禁止依赖 Spring 框架

- ❌ @Autowired
- ❌ @Service
- ❌ @Component
- ❌ 任何 Spring 注解

**原因：** Client 层应保持纯 Java，不依赖框架。

---

## 四、依赖规则

### 4.1 允许的依赖

- ✅ `common` 模块（工具类、常量、异常）
- ✅ JDK 标准库
- ✅ 校验注解库（如 javax.validation）

### 4.2 禁止的依赖

- ❌ 同业务域的 domain/application/adaptor
- ❌ 其他业务域的任何模块
- ❌ Spring 框架
- ❌ MyBatis、JPA 等持久化框架
- ❌ 任何第三方业务框架

---

## 五、包路径规范

```
com.{company}.{project}.{business}.client
├── api/                    # Service 接口
│   └── XXXServiceI.java
└── dto/                    # 数据传输对象
    ├── command/            # 命令对象
    │   └── XXXCmd.java
    ├── query/              # 查询对象
    │   └── XXXQry.java
    └── response/           # 响应对象（可选）
        └── XXXResponse.java
```

**注意：** DTO 也可以直接放在 `dto/` 包下，不强制分子包。

---

## 六、命名规范总结

| 类型 | 命名格式 | 示例 |
|------|---------|------|
| Service 接口 | `XXXServiceI` | UserServiceI |
| Command | `XXXCmd` | CreateUserCmd |
| Query | `XXXQry` | GetUserQry |
| DTO | `XXXDTO` | UserDTO |
| Response | `Response` / `XXXResponse` | Response、PageResult |

---

## 七、CQRS 模式应用

### 7.1 Command 和 Query 分离

**原则：**
- 写操作（增删改）使用 Command
- 读操作（查询）使用 Query
- Command 和 Query 不共用同一个对象

**好处：**
- 职责清晰
- 参数校验更精确
- 便于后续优化（如读写分离）

---

### 7.2 接口方法设计

**写操作方法：**
```
Response createUser(CreateUserCmd cmd)
Response updateUser(UpdateUserCmd cmd)
Response deleteUser(DeleteUserCmd cmd)
```

**读操作方法：**
```
Response<UserDTO> getUserById(GetUserQry qry)
MultiResponse<UserDTO> listUsers(ListUsersQry qry)
PageResult<UserDTO> pageUsers(PageUsersQry qry)
```

---

## 八、DTO 与 Entity 的区别

| 维度 | DTO | Entity |
|------|-----|--------|
| **所属层** | Client 层 | Domain 层 |
| **用途** | 模块间数据传输 | 领域模型 |
| **包含内容** | 仅数据字段 | 数据 + 业务行为 |
| **依赖框架** | 无 | 无（纯 POJO） |
| **可变性** | 通常可变 | 根据业务决定 |
| **校验** | 使用注解校验 | 使用方法校验 |

**转换关系：**
```
DTO（Client 层） ↔ Command/Query（Application 层） ↔ Entity（Domain 层）
```

---

## 九、与其他 Spec 的关系

**本 Spec 负责：**
- Client 层的职责和内容
- DTO 设计规范
- 依赖规则
- 包路径和命名规范

**不负责（由其他 Spec 定义）：**
- Maven 模块组织结构 → 参考 `architecture-module-spec.md`
- Domain 层的 Entity 设计 → 参考 `domain-module-spec.md`
- Application 层的 Service 实现 → 参考 `application-module-spec.md`
- 具体业务的接口定义 → 参考 `business/{business}/{business}-domain-spec.md`

---

## 十、快速检查清单

创建 Client 模块时，确认以下事项：

- [ ] 只包含接口和 DTO，没有实现类
- [ ] Service 接口命名为 `XXXServiceI`
- [ ] Command 命名为 `XXXCmd`，放在 `dto/command/` 包
- [ ] Query 命名为 `XXXQry`，放在 `dto/query/` 包
- [ ] DTO 命名为 `XXXDTO`，实现 Serializable
- [ ] 没有 Spring 注解
- [ ] 没有 Mapper/DAO 接口
- [ ] 没有 Domain Entity
- [ ] 只依赖 common 模块
- [ ] 包路径符合规范

---

## 十一、常见错误

### 错误 1：在 Client 层写实现类

**表现：** client 模块中有 ServiceImpl

**解决：** 实现类放到 application 模块

---

### 错误 2：Client 层依赖 Spring

**表现：** 使用 @Autowired、@Service 等注解

**解决：** 移除 Spring 依赖，保持纯 Java 接口

---

### 错误 3：Command 和 Query 混用

**表现：** 一个对象既用于创建又用于查询

**解决：** 分离为 CreateUserCmd 和 GetUserQry

---

### 错误 4：DTO 与 Entity 混淆

**表现：** Client 层直接使用 Domain Entity

**解决：** 定义独立的 DTO，在 application 层进行转换
