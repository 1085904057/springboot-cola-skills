# Spec 规范体系总纲

> **核心思想：业务 Spec 与架构 Spec 分离**
> - **架构 Spec**：所有应用开发场景可复用，与技术栈和架构模式相关（如 COLA 架构）
> - **业务 Spec**：特定业务领域的规则，与具体业务逻辑相关（如电商的订单、用户、商品）

---

## 📁 目录结构

```
.spec/
├── business/                       # 【业务维度规范】特定业务领域的规则
│   ├── business-module-spec.md     # ✅ 【全局业务总纲】模块化单体的业务域划分原则
│   ├── order/                      # 订单业务域
│   │   └── order-domain-spec.md    # ✅ 【订单域专属规范】订单的领域模型、业务规则、交互流程
│   ├── user/                       # 用户业务域
│   │   └── user-domain-spec.md     # ⏳ 【用户域专属规范】用户的领域模型、业务规则（待创建）
│   └── product/                    # 商品业务域
│       └── product-domain-spec.md  # ⏳ 【商品域专属规范】商品的领域模型、业务规则（待创建）
│
└── architecture/                       # 【架构维度规范】可复用的架构层规则
    ├── architecture-module-spec.md     # ✅ 【全局架构总纲】模块化单体的 Maven 多模块组织规范
    ├── client-module-spec.md           # ✅ 【Client 层规范】接口契约层的职责、内容、依赖规则
    ├── domain-module-spec.md           # ⏳ 【Domain 层规范】领域层的职责、内容、依赖规则（待创建）
    ├── application-module-spec.md      # ⏳ 【Application 层规范】应用层的职责、内容、依赖规则（待创建）
    ├── adaptor-module-spec.md          # ⏳ 【Adaptor 层规范】适配层的职责、内容、依赖规则（待创建）
    └── infrastructure-module-spec.md   # ⏳ 【Infrastructure 层规范】基础设施层的职责、内容、依赖规则（待创建）
```

**图例：**
- ✅ 已创建
- ⏳ 待创建

---

## 📋 Spec 文件说明

### 一、业务维度规范（Business Specs）

#### 1. `business/business-module-spec.md`
**用途：** 定义模块化单体中业务域的划分原则和通用规则  
**适用场景：** 任何需要划分业务域的项目  
**核心内容：**
- 如何识别和划分业务域（用户域、商品域、订单域等）
- 业务域之间的交互原则（只能通过接口调用）
- 业务域内部的子模块组织（client/domain/application/adaptor）
- 跨业务域调用的约束（禁止循环依赖、禁止直接访问实现）
- 数据库层面的业务隔离（表名前缀、禁止跨域 JOIN）

**示例场景：** 电商系统划分为用户、商品、订单三个业务域

---

#### 2. `business/order/order-domain-spec.md`
**用途：** 定义订单业务域的专属规范  
**适用场景：** 电商系统的订单模块开发  
**核心内容：**
- 订单领域的核心概念（订单、订单项、支付记录等）
- 订单的生命周期状态机（待支付、已支付、已发货、已完成、已取消）
- 订单的核心业务规则（库存校验、价格计算、优惠叠加等）
- 订单与其他域的交互流程（调用用户域获取用户信息、调用商品域扣减库存）
- 订单领域的关键业务流程（创建订单、取消订单、退款流程）

**不包含：** 技术实现细节、代码示例、架构分层规则

---

#### 3. `business/user/user-domain-spec.md`
**用途：** 定义用户业务域的专属规范  
**适用场景：** 电商系统的用户模块开发  
**核心内容：**
- 用户领域的核心概念（用户、角色、权限等）
- 用户认证和授权规则
- 用户信息的业务规则（邮箱唯一性、手机号格式等）
- 用户与其他域的交互（被订单域调用、被营销域调用）

---

#### 4. `business/product/product-domain-spec.md`
**用途：** 定义商品业务域的专属规范  
**适用场景：** 电商系统的商品模块开发  
**核心内容：**
- 商品领域的核心概念（SPU、SKU、分类、品牌等）
- 商品上下架规则
- 库存管理规则（扣减、回滚、预占）
- 商品价格计算规则
- 商品与其他域的交互（被订单域查询和扣减库存）

---

### 二、架构维度规范（Architecture Specs）

#### 1. `architecture/architecture-module-spec.md`
**用途：** 定义模块化单体的 Maven 多模块组织结构  
**适用场景：** 所有采用模块化单体架构的项目  
**核心内容：**
- 项目整体目录结构（顶级父工程、业务域父模块、子模块、启动模块）
- Maven 模块命名规范（{business}-client、{business}-domain 等）
- Maven 依赖规则（跨域只能依赖 client、单域内部依赖方向）
- 包路径规范（com.{company}.{project}.{business}.{layer}）
- POM 配置要点（父 POM、子模块 POM、启动模块 POM）
- 自动化检查机制（Maven Enforcer Plugin 配置）

**不包含：** 具体业务逻辑、Java 代码示例

**复用性：** ⭐⭐⭐⭐⭐ 所有 COLA 架构项目都可复用

---

#### 2. `architecture/client-module-spec.md`
**用途：** 定义 Client 层（接口契约层）的规范  
**适用场景：** 所有业务域的 Client 模块开发  
**核心内容：**
- Client 层的职责定位（对外唯一出口、服务契约定义）
- Client 层应包含的内容（Service 接口、DTO、Command、Query、Response）
- Client 层禁止的内容（实现类、Mapper、Domain Entity、Spring 注解）
- Client 层的依赖规则（仅依赖 common，不依赖任何其他模块）
- DTO 设计规范（Command/Query 分离、DTO 与 Entity 分离）
- 接口命名规范（XXXServiceI、XXXCmd、XXXQry、XXXDTO）
- 包路径规范（api/、dto/command/、dto/query/）

**不包含：** 具体业务的接口定义、Java 代码示例

**复用性：** ⭐⭐⭐⭐⭐ 所有业务域的 Client 层都遵循此规范

---

#### 3. `architecture/domain-module-spec.md`
**用途：** 定义 Domain 层（领域层）的规范  
**适用场景：** 所有业务域的 Domain 模块开发  
**核心内容：**
- Domain 层的职责定位（核心业务逻辑、领域模型）
- Domain 层应包含的内容（Entity、Value Object、Domain Service、Gateway 接口）
- Domain 层禁止的内容（Spring 注解、框架依赖、基础设施实现）
- Domain 层的依赖规则（纯 POJO，仅依赖 common，可选依赖 client）
- 领域模型设计规范（聚合根、值对象、领域行为）
- 网关接口设计原则（声明数据访问契约、依赖倒置）
- 包路径规范（model/、gateway/、ability/、event/）

**不包含：** 具体业务的领域模型、Java 代码示例

**复用性：** ⭐⭐⭐⭐⭐ 所有业务域的 Domain 层都遵循此规范

---

#### 4. `architecture/application-module-spec.md`
**用途：** 定义 Application 层（应用层）的规范  
**适用场景：** 所有业务域的 Application 模块开发  
**核心内容：**
- Application 层的职责定位（业务流程编排、协调领域模型）
- Application 层应包含的内容（Service 实现、Command/Query 执行器、Consumer、Scheduler）
- Application 层的依赖规则（依赖自己的 domain/client、可依赖其他域的 client）
- CQRS 模式应用（Command 执行器、Query 执行器分离）
- 跨域调用规范（通过 client 接口调用、禁止直接访问实现）
- 事务管理原则（本地事务、避免跨域事务）
- 包路径规范（service/、executor/cmd/、executor/qry/、consumer/）

**不包含：** 具体业务的流程实现、Java 代码示例

**复用性：** ⭐⭐⭐⭐⭐ 所有业务域的 Application 层都遵循此规范

---

#### 5. `architecture/adaptor-module-spec.md`
**用途：** 定义 Adaptor 层（适配层）的规范  
**适用场景：** 所有业务域的 Adaptor 模块开发  
**核心内容：**
- Adaptor 层的职责定位（接收外部请求、参数校验、调用 Application 层）
- Adaptor 层应包含的内容（Controller、Request/VO、参数转换器）
- Adaptor 层的依赖规则（依赖自己的 application/client，不依赖 domain）
- RESTful API 设计规范（URL 命名、HTTP 方法、状态码）
- 参数校验规范（使用 Validation 注解、统一异常处理）
- 返回值封装规范（统一 Response 格式）
- 包路径规范（web/、wireless/、wap/）

**不包含：** 具体业务的 API 定义、Java 代码示例

**复用性：** ⭐⭐⭐⭐⭐ 所有业务域的 Adaptor 层都遵循此规范

---

#### 6. `architecture/infrastructure-module-spec.md`
**用途：** 定义 Infrastructure 层（基础设施层）的规范  
**适用场景：** 所有业务域的 Infrastructure 模块开发  
**核心内容：**
- Infrastructure 层的职责定位（实现 Domain 层的网关接口、技术实现）
- Infrastructure 层应包含的内容（Gateway 实现、Mapper、DO、Converter、Config）
- Infrastructure 层的依赖规则（依赖自己的 domain、可依赖第三方框架）
- 数据对象转换规范（DTO ↔ DO ↔ Entity 转换）
- MyBatis Mapper 设计规范（Mapper 接口、DO 对象、XML 映射）
- 外部服务调用规范（OpenFeign Client、熔断降级）
- 包路径规范（gatewayimpl/、mapper/、mapper/dataobject/、client/、config/）

**不包含：** 具体业务的 Gateway 实现、Java 代码示例

**复用性：** ⭐⭐⭐⭐⭐ 所有业务域的 Infrastructure 层都遵循此规范

---

## 🎯 Spec 使用指南

### 如何使用这些 Spec？

#### 场景 1：创建新的业务域（如"营销域"）

**步骤：**
1. 阅读 `architecture/architecture-module-spec.md` → 了解 Maven 模块组织结构
2. 阅读 `architecture/client-module-spec.md` → 创建 marketing-client 模块
3. 阅读 `architecture/domain-module-spec.md` → 创建 marketing-domain 模块
4. 阅读 `architecture/application-module-spec.md` → 创建 marketing-application 模块
5. 阅读 `architecture/adaptor-module-spec.md` → 创建 marketing-adaptor 模块
6. 创建 `business/marketing/marketing-domain-spec.md` → 定义营销域的专属业务规则

**依赖的 Spec：** 5 个架构 Spec + 1 个业务 Spec

---

#### 场景 2：在订单域中实现"创建订单"功能

**步骤：**
1. 阅读 `business/order/order-domain-spec.md` → 了解订单创建的业务规则
2. 阅读 `architecture/client-module-spec.md` → 定义 CreateOrderCmd 和 OrderServiceI
3. 阅读 `architecture/domain-module-spec.md` → 设计 Order 实体和 OrderGateway
4. 阅读 `architecture/application-module-spec.md` → 实现 CreateOrderCmdExe
5. 阅读 `architecture/adaptor-module-spec.md` → 创建 OrderController
6. 阅读 `architecture/infrastructure-module-spec.md` → 实现 OrderGatewayImpl

**依赖的 Spec：** 1 个业务 Spec + 5 个架构 Spec

---

#### 场景 3：订单域调用用户域获取用户信息

**步骤：**
1. 阅读 `business/business-module-spec.md` → 了解跨域调用原则
2. 阅读 `architecture/application-module-spec.md` → 了解如何通过 client 接口调用
3. 确保 order-application 的 pom.xml 依赖 user-client（参考 `architecture/architecture-module-spec.md`）
4. 在 order-application 中注入 UserServiceI 接口调用

**依赖的 Spec：** 1 个业务总纲 + 2 个架构 Spec

---

### Spec 之间的关系

```
业务开发流程：
业务 Spec（做什么） + 架构 Spec（怎么做） = 完整实现

示例：
order-domain-spec.md（订单业务规则）
    ↓ 结合
client-module-spec.md（Client 层规范）
domain-module-spec.md（Domain 层规范）
application-module-spec.md（Application 层规范）
adaptor-module-spec.md（Adaptor 层规范）
infrastructure-module-spec.md（Infrastructure 层规范）
    ↓ 生成
完整的订单模块代码
```

---

## ✅ Spec 设计原则

### 1. 单一职责
- 每个 Spec 只关注一个层面或一个业务域
- 架构 Spec 不包含业务逻辑
- 业务 Spec 不包含技术实现

### 2. 精简聚焦
- 每个 Spec 控制在 200-300 行以内
- 只写规则和原则，不写代码示例
- 用表格和列表代替长篇大论

### 3. 可复用性
- 架构 Spec 可在所有项目中复用
- 业务 Spec 可在同类型业务中参考

### 4. 独立性
- 每个 Spec 可独立阅读和理解
- Spec 之间通过引用关联，不重复内容

### 5. 可执行性
- Spec 中的规则应该是可验证的
- 可通过 Maven Enforcer、ArchUnit 等工具自动化检查

---

## 🔄 维护建议

1. **架构 Spec 稳定**：一旦确定，很少修改
2. **业务 Spec 迭代**：随业务发展逐步完善
3. **新增业务域**：只需添加对应的业务 Spec，无需修改架构 Spec
4. **版本管理**：Spec 文件纳入 Git 版本控制
5. **团队共识**：定期 Review Spec，确保团队理解一致