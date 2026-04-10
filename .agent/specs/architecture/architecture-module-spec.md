# 模块化单体架构规范（Architecture Module Specification）

> **用途：** 定义模块化单体的 Maven 多模块组织结构  
> **适用场景：** 所有采用模块化单体 + COLA 架构的项目  
> **复用性：** ⭐⭐⭐⭐⭐ 所有项目可复用

---

## 一、项目整体结构

### 1.1 目录树

```
{project-name}/                          # 顶级父工程（pom 打包）
├── pom.xml                              # 统一管理版本、依赖、插件
├── common/                              # 公共基础模块
├── {business-1}/                        # 业务域 1（pom 打包）
│   ├── {business-1}-client/             # 接口契约层
│   ├── {business-1}-domain/             # 领域模型层
│   ├── {business-1}-application/        # 应用服务层
│   └── {business-1}-adaptor/            # API 接入层
├── {business-2}/                        # 业务域 2（pom 打包）
│   ├── {business-2}-client/
│   ├── {business-2}-domain/
│   ├── {business-2}-application/
│   └── {business-2}-adaptor/
└── start/                               # 启动模块（唯一 SpringBoot 入口）
```

### 1.2 模块命名规则

| 模块类型 | 命名格式 | 示例 |
|---------|---------|------|
| 业务域父模块 | `{business}` | `user`、`product`、`order` |
| Client 子模块 | `{business}-client` | `user-client` |
| Domain 子模块 | `{business}-domain` | `user-domain` |
| Application 子模块 | `{business}-application` | `user-application` |
| Adaptor 子模块 | `{business}-adaptor` | `user-adaptor` |

---

## 二、Maven 依赖规则（核心约束）

### 2.1 跨业务域依赖规则

> **铁律：任何外部业务域，只能依赖目标域的 `client` 模块**

**允许的依赖：**
- order-application → user-client ✅
- order-application → product-client ✅

**禁止的依赖：**
- order-application → user-domain ❌
- order-application → user-application ❌
- order-application → user-adaptor ❌

**原因：** Maven 没有依赖就无法导入类，编译期直接报错，从物理层面杜绝违规调用。

### 2.2 单业务域内部依赖规则

**依赖方向：**
```
adaptor → application → domain → client
```

**详细规则：**

| 模块 | 允许依赖 | 禁止依赖 |
|------|---------|---------|
| **client** | common | domain、application、adaptor、其他业务域 |
| **domain** | client（可选）、common | application、adaptor、其他业务域 |
| **application** | domain、client、common、**其他业务域的 client** | 其他业务域的 domain/application/adaptor |
| **adaptor** | application、client、common | domain、其他业务域的任何模块 |

### 2.3 启动模块依赖规则

**start 模块依赖：**
- ✅ 所有业务域的 `adaptor` 模块
- ✅ `common` 模块
- ❌ 不直接依赖 client、domain、application

---

## 三、包路径规范

### 3.1 基础包路径

```
com.{company}.{project}
```

### 3.2 各模块包路径

| 模块 | 包路径 | 示例 |
|------|--------|------|
| common | `com.{company}.{project}.common` | com.ecommerce.monolith.common |
| {business}-client | `com.{company}.{project}.{business}.client` | com.ecommerce.monolith.user.client |
| {business}-domain | `com.{company}.{project}.{business}.domain` | com.ecommerce.monolith.user.domain |
| {business}-application | `com.{company}.{project}.{business}.app` | com.ecommerce.monolith.user.app |
| {business}-adaptor | `com.{company}.{project}.{business}.adapter` | com.ecommerce.monolith.user.adapter |
| start | `com.{company}.{project}` | com.ecommerce.monolith |

---

## 四、POM 配置要点

### 4.1 顶级父 POM

**关键配置：**
- `packaging`: `pom`
- `modules`: 列出所有子模块（common、各业务域、start）
- `dependencyManagement`: 统一管理第三方依赖版本
- `properties`: 定义版本号变量

### 4.2 业务域父 POM

**关键配置：**
- `packaging`: `pom`
- `modules`: client、domain、application、adaptor
- `parent`: 指向顶级父 POM

### 4.3 子模块 POM

**关键配置：**
- `packaging`: `jar`（默认）
- `parent`: 指向业务域父 POM
- `dependencies`: 根据依赖规则声明

### 4.4 Start 模块 POM

**关键配置：**
- `packaging`: `jar`
- 依赖所有 adaptor 模块
- 配置 `spring-boot-maven-plugin` 插件

---

## 五、自动化检查机制

### 5.1 Maven Enforcer Plugin

**作用：** 编译期强制检查依赖规则

**检查项：**
- 禁止 application 层依赖其他业务域的 domain/application/adaptor
- 只允许依赖自己的 domain/application/client
- 只允许依赖其他业务域的 client

**效果：** 违反规则则构建失败，从编译期杜绝架构违规。

### 5.2 配置示例位置

在顶级父 POM 的 `<build><plugins>` 中配置 `maven-enforcer-plugin`。

具体配置参考项目中的实际配置文件。

---

## 六、数据库设计规范

### 6.1 表命名规范

**规则：表名加业务域前缀**

| 业务域 | 表前缀 | 示例 |
|--------|--------|------|
| 用户域 | `user_` | user_info、user_role |
| 商品域 | `product_` | product_info、product_sku |
| 订单域 | `order_` | order_info、order_item |

### 6.2 数据访问限制

**禁止事项：**
- ❌ 禁止跨业务域 JOIN 查询
- ❌ 禁止跨业务域事务

**正确做法：**
- ✅ 先查本域数据
- ✅ 通过接口调用获取其他域数据
- ✅ 使用最终一致性代替强一致性事务

---

## 七、关键原则总结

### 必须遵守的规则

1. ✅ 每个业务域包含 4 个子模块：client、domain、application、adaptor
2. ✅ 跨模块调用只能通过 client 接口
3. ✅ Domain 层是纯 POJO，不依赖框架
4. ✅ 表名加业务域前缀
5. ✅ 禁止跨模块 JOIN 和事务
6. ✅ 配置 Maven Enforcer Plugin 强制检查

### 禁止出现的错误

1. ❌ application 层依赖其他业务域的 domain/application/adaptor
2. ❌ 跨模块 JOIN 查询
3. ❌ 跨模块事务
4. ❌ 在 client 模块中写实现类
5. ❌ 在 domain 模块中使用 Spring 注解

---

## 八、与其他 Spec 的关系

**本 Spec 负责：**
- Maven 模块组织结构
- 依赖规则
- 包路径规范
- POM 配置

**不负责（由其他 Spec 定义）：**
- Client 层的具体内容 → 参考 `client-module-spec.md`
- Domain 层的具体内容 → 参考 `domain-module-spec.md`
- Application 层的具体内容 → 参考 `application-module-spec.md`
- Adaptor 层的具体内容 → 参考 `adaptor-module-spec.md`
- Infrastructure 层的具体内容 → 参考 `infrastructure-module-spec.md`
- 业务域划分原则 → 参考 `business/business-module-spec.md`
- 具体业务规则 → 参考 `business/{business}/{business}-domain-spec.md`

---

## 九、快速开始

**创建新业务域的步骤：**

1. 在顶级父 POM 的 `modules` 中添加新业务域
2. 创建业务域父模块（pom 打包）
3. 创建 4 个子模块：client、domain、application、adaptor
4. 配置各子模块的 POM 依赖（遵循本节第二条的依赖规则）
5. 创建对应的包路径（遵循本节第三条的包路径规范）
6. 在各子模块中实现具体内容（参考对应的分层 Spec）

**验证依赖合规性：**
```bash
mvn clean compile
```

如果依赖违规，Maven Enforcer Plugin 会报错并阻止构建。
