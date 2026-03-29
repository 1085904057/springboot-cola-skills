# JetCache 缓存设计规范

> 📚 **这是 COLA 架构 Skill 体系的缓存规范分册**，指导如何在 COLA 架构中使用 JetCache 实现声明式缓存。
> 
> **关联文档：**
> - [ARCHITECTURE-SKILL](./ARCHITECTURE-SKILL) - 架构规范
> - [BUILD-SKILL](./BUILD-SKILL.md) - 项目构建规范
> - [DEVELOP-SKILL](./DEVELOP-SKILL.md) - 开发流程规范

---

## 一、元数据信息

### Name
`jetcache-cache-design`

### Description
基于 COLA 架构的 JetCache 缓存设计规范。覆盖缓存定位、配置管理、注解使用、最佳实践等，实现高性能、声明式的缓存管理。

### 适用角色
- 💻 后端开发工程师
- 🔧 技术负责人/架构师
- 📊 性能优化工程师

---

## 二、Overview（什么时候用）

### 适用场景

✅ **当你需要：**
- 为查询操作添加缓存（提升读取性能）
- 自动管理缓存更新和清除
- 实现多级缓存（本地 + 远程）
- 防止缓存穿透、击穿、雪崩
- 自动刷新热点数据缓存
- 批量查询优化

❌ **不适用场景：**
- 数据实时性要求极高（毫秒级）
- 缓存数据量极小（< 100 条）
- 简单的配置信息管理

### 在 COLA 架构中的位置

```
┌─────────────────────────────────────┐
│  Adapter（适配层）                  │  ← 接收请求
├─────────────────────────────────────┤
│  App（应用层）⭐                    │  ← 使用 @Cached 注解
│   - CustomerServiceImpl             │
│   - OrderServiceImpl                │
├─────────────────────────────────────┤
│  Domain（领域层）                   │  ← 纯业务逻辑（无缓存）
├─────────────────────────────────────┤
│  Infrastructure（基础设施层）       │  ← 提供缓存实现
│   - cache/config/RedisConfig        │
│   - cache/service/CacheService      │
└─────────────────────────────────────┘
```

**核心原则：**
- ✅ **缓存注解在 App 层** - 业务服务层声明缓存
- ✅ **缓存在 Infra 层实现** - Redis 配置、连接管理
- ✅ **Domain 层不感知缓存** - 保持领域模型纯粹性

---

## 三、JetCache 核心概念

### 3.1 什么是 JetCache？

JetCache 是阿里开源的分布式缓存框架，提供了：
- 🚀 **声明式缓存** - 注解方式，无需手动编写缓存逻辑
- 🎯 **多级缓存** - 本地缓存（Caffeine）+ 远程缓存（Redis）
- 🛡️ **高级特性** - 自动刷新、防穿透、防击穿
- 📊 **监控统计** - 命中率、QPS、响应时间

### 3.2 核心注解

| 注解 | 说明 | 使用场景 |
|------|------|---------|
| `@Cached` | 声明式缓存 | 查询操作 |
| `@Update` | 更新缓存 | 更新操作（自动更新缓存值） |
| `@Invalidate` | 清除缓存 | 删除操作 |
| `@CreateCache` | 编程式缓存 | 需要灵活控制的场景 |

### 3.3 缓存类型

```java
CacheType.LOCAL   // 仅本地缓存（Caffeine）
CacheType.REMOTE  // 仅远程缓存（Redis）
CacheType.BOTH    // 两级缓存（推荐）⭐
```

---

## 四、完整开发流程

### 4.1 添加依赖

**文件：** `harness-infrastructure/pom.xml`

```xml
<dependencies>
    <!-- JetCache Redis Starter -->
    <dependency>
        <groupId>com.alicp.jetcache</groupId>
        <artifactId>jetcache-starter-redis</artifactId>
        <version>2.7.5</version>
    </dependency>
    
    <!-- Caffeine（本地缓存，已包含在 jetcache-starter 中） -->
    <!-- 如果需要显式声明 -->
    <dependency>
        <groupId>com.github.ben-manes.caffeine</groupId>
        <artifactId>caffeine</artifactId>
    </dependency>
    
    <!-- FastJSON（序列化，可选但推荐） -->
    <dependency>
        <groupId>com.alibaba.fastjson2</groupId>
        <artifactId>fastjson2</artifactId>
    </dependency>
</dependencies>
```

---

### 4.2 配置文件

**文件：** `harness-start/src/main/resources/application.yml`

```yaml
jetcache:
  # 是否允许在方法调用链中使用缓存
  areaInCacheName: false
  
  # 统计支持（监控）
  statSupport:
    qps:
      enable: true          # 开启 QPS 统计
      periodSeconds: 10     # 统计周期 10 秒
  
  # 本地缓存配置
  local:
    default:
      type: caffeine
      keyConvertor: fastjson              # 键转换器
      valueEncoder: java                  # 值编码器
      valueDecoder: java                  # 值解码器
      limit: 10000                        # 最大缓存 1 万条
      
  # 远程缓存配置
  remote:
    default:
      type: redis
      keyConvertor: fastjson
      valueEncoder: java
      valueDecoder: java
      poolConfig:
        minIdle: 5                        # 最小空闲连接
        maxIdle: 20                       # 最大空闲连接
        maxTotal: 50                      # 最大总连接数
      host: ${redis.host:localhost}       # Redis 主机
      port: ${redis.port:6379}            # Redis 端口
      password: ${redis.password:}        # Redis 密码（可选）
      database: 0                         # Redis 数据库
```

**多环境配置示例：**

```yaml
# application-dev.yml
jetcache:
  remote:
    default:
      host: dev-redis.example.com
      port: 6379
      password: dev_password

# application-prod.yml
jetcache:
  remote:
    default:
      host: prod-redis-cluster.example.com
      port: 6379
      password: ${REDIS_PASSWORD}  # 从环境变量读取
      poolConfig:
        minIdle: 10
        maxIdle: 50
        maxTotal: 200
```

---

### 4.3 启动类配置

**文件：** `harness-start/src/main/java/com/harness/engineering/HarnessStartApplication.java`

```java
package com.harness.engineering;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.alicp.jetcache.anno.EnableMethodCache;

@SpringBootApplication
@EnableMethodCache(basePackages = "com.harness.engineering.app")  // ⭐ 启用方法缓存
public class HarnessStartApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(HarnessStartApplication.class, args);
    }
}
```

**重要参数说明：**
- `basePackages` - 指定哪些包下的类可以使用缓存注解
- 通常设置为 App 层的包路径

---

### 4.4 在 App 层使用缓存注解

#### 场景一：简单查询（基础用法）

**文件：** `harness-app/src/main/java/com/harness/engineering/app/customer/service/CustomerServiceImpl.java`

```java
package com.harness.engineering.app.customer.service;

import com.harness.engineering.client.customer.api.CustomerServiceI;
import com.harness.engineering.client.customer.dto.command.CreateCustomerCmd;
import com.harness.engineering.client.customer.dto.query.GetCustomerQry;
import com.harness.engineering.client.dto.Response;
import com.harness.engineering.client.dto.CustomerDTO;
import com.harness.engineering.domain.customer.gateway.CustomerGateway;
import com.harness.engineering.domain.customer.model.Customer;
import com.alicp.jetcache.anno.Cached;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CustomerServiceImpl implements CustomerServiceI {
    
    @Resource
    private CustomerGateway customerGateway;
    
    /**
     * 获取客户详情（带缓存）
     * 
     * @param qry 查询条件
     * @return 客户 DTO
     */
    @Override
    @Cached(
        name = "customer:",           // 缓存前缀
        key = "#qry.id",              // 缓存键（SpEL 表达式）
        expire = 3600,                // 过期时间 1 小时
        cacheType = CacheType.BOTH,   // 两级缓存
        localExpire = 60,             // 本地缓存 1 分钟
        syncLocal = true              // 同步本地缓存
    )
    public Response<CustomerDTO> getCustomer(GetCustomerQry qry) {
        log.info("Querying customer from database: {}", qry.getId());
        
        Customer customer = customerGateway.getById(qry.getId());
        if (customer == null) {
            return Response.failure("Customer not found");
        }
        
        CustomerDTO dto = convertToDTO(customer);
        return Response.success(dto);
    }
    
    private CustomerDTO convertToDTO(Customer customer) {
        // DTO 转换逻辑
        CustomerDTO dto = new CustomerDTO();
        BeanUtils.copyProperties(customer, dto);
        return dto;
    }
}
```

**关键参数说明：**
- `name` - 缓存名称前缀（会自动拼接 key）
- `key` - SpEL 表达式，从方法参数中提取缓存键
- `expire` - 远程缓存过期时间（秒）
- `localExpire` - 本地缓存过期时间（秒）
- `cacheType` - 缓存类型（BOTH = 本地 + 远程）
- `syncLocal` - 是否同步本地缓存（集群环境下建议开启）

---

#### 场景二：防缓存穿透

**问题：** 查询不存在的数据，导致每次请求都打到数据库

```java
@Override
@Cached(
    name = "customer:",
    key = "#qry.id",
    expire = 300,  // 5 分钟，空值缓存时间短一些
    cacheType = CacheType.BOTH,
    penetrationProtectConfig = @PenetrationProtectConfig(
        enable = true,              // ✅ 启用防穿透保护
        periodOfDetectedSilence = 1000,  // 检测静默期 1 秒
        thresholdOfSilence = 10     // 静默阈值 10
    )
)
public Response<CustomerDTO> getCustomer(GetCustomerQry qry) {
    Customer customer = customerGateway.getById(qry.getId());
    
    // 即使返回 null 或 failure，JetCache 也会缓存空值
    if (customer == null) {
        log.warn("Customer not found: {}", qry.getId());
        return Response.failure("Customer not found");
    }
    
    return Response.success(convertToDTO(customer));
}
```

**原理：**
- 当检测到大量请求同时查询一个不存在的 key 时
- JetCache 会自动缓存空值，防止数据库被打穿

---

#### 场景三：防缓存击穿（互斥锁）

**问题：** 热点数据过期瞬间，大量请求打到数据库

```java
@Override
@Cached(
    name = "hot_customer:",
    key = "#qry.id",
    expire = 3600,
    cacheType = CacheType.BOTH,
    syncLocal = true,
    lockExpire = 10  // ✅ 设置锁超时时间（秒）
)
public Response<CustomerDTO> getHotCustomer(GetCustomerQry qry) {
    // JetCache 会自动加互斥锁
    // 只有一个线程会重建缓存，其他线程等待
    Customer customer = customerGateway.getById(qry.getId());
    return Response.success(convertToDTO(customer));
}
```

**原理：**
- 当缓存失效时，第一个请求获得锁并重建缓存
- 其他请求等待锁释放，避免并发查询数据库

---

#### 场景四：自动刷新缓存

**场景：** 热点数据需要定期刷新，保持最新状态

```java
@Override
@Cached(
    name = "customer:",
    key = "#qry.id",
    expire = 3600,
    cacheType = CacheType.BOTH,
    refreshPolicy = @RefreshPolicy(
        stopRefreshAfterLastAccess = 3600,  // 1 小时无访问停止刷新
        refreshInterval = 300               // 每 5 分钟刷新一次
    )
)
public Response<CustomerDTO> getCustomer(GetCustomerQry qry) {
    log.info("Querying customer (with auto-refresh): {}", qry.getId());
    Customer customer = customerGateway.getById(qry.getId());
    return Response.success(convertToDTO(customer));
}
```

**工作原理：**
```
时间轴：
0min   - 首次查询，写入缓存
5min   - 后台线程自动刷新缓存
10min  - 后台线程自动刷新缓存
...
60min  - 缓存过期
65min  - 如果有访问，继续刷新；否则停止刷新
```

**好处：**
- ✅ 热点数据始终在缓存中
- ✅ 用户访问时不会触发缓存 miss
- ✅ 自动停止刷新，节省资源

---

#### 场景五：更新缓存

**方式一：使用 `@Update` 注解（推荐）**

```java
@Override
@Update(
    key = "#cmd.id",                // 缓存键
    expire = 3600,                  // 缓存时间
    invalidate = {"customer:list"}  // 同时清除列表缓存
)
public Response<Void> updateCustomer(UpdateCustomerCmd cmd) {
    log.info("Updating customer: {}", cmd.getId());
    
    // 执行业务逻辑
    Customer customer = customerGateway.getById(cmd.getId());
    customer.updateEmail(cmd.getEmail());
    customerGateway.update(customer);
    
    // JetCache 会自动用新值更新缓存
    // 无需手动清除缓存，也无需重新查询数据库
    return Response.success();
}
```

**优势：**
- ✅ 直接更新缓存，无需重新查询
- ✅ 减少数据库访问
- ✅ 性能更好

---

**方式二：使用 `@Invalidate` 清除缓存**

```java
@Override
@Invalidate(
    keys = {
        "customer:#cmd.id",      // 清除单个客户缓存
        "customer:list"          // 清除客户列表缓存
    }
)
public Response<Void> deleteCustomer(Long id) {
    log.info("Deleting customer: {}", id);
    
    customerGateway.delete(id);
    
    // JetCache 会自动清除指定的缓存
    return Response.success();
}
```

---

#### 场景六：批量查询缓存

**问题：** N+1 查询问题

```java
// ❌ 错误示例：循环调用单个查询
public MultiResponse<CustomerDTO> getCustomersByIds(List<Long> ids) {
    return ids.stream()
        .map(id -> getCustomer(id))  // 每次都查数据库
        .collect(Collectors.toList());
}

// ✅ 正确示例：批量查询 + 批量缓存
@Override
@Cached(
    name = "customer:batch:",
    key = "#ids",
    expire = 600,
    cacheType = CacheType.BOTH
)
public MultiResponse<CustomerDTO> getCustomersByIds(List<Long> ids) {
    log.info("Batch querying customers: {}", ids);
    
    // 一次性查询所有客户
    List<Customer> customers = customerGateway.getByIds(ids);
    
    return MultiResponse.of(convertToDTOList(customers));
}
```

**好处：**
- ✅ 一次查询多个，减少数据库压力
- ✅ 整体缓存，避免 N+1 问题
- ✅ 如果部分数据在缓存中，会自动合并

---

### 4.5 缓存配置类（可选）

**文件：** `harness-infrastructure/src/main/java/com/harness/engineering/infrastructure/cache/config/JetCacheConfig.java`

```java
package com.harness.engineering.infrastructure.cache.config;

import com.alicp.jetcache.anno.support.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class JetCacheConfig {
    
    /**
     * 自定义 RedisTemplate（可选）
     * JetCache 默认已经配置好了，这个是给自己代码用的
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        
        // Key 序列化
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        
        // Value 序列化
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        
        template.afterPropertiesSet();
        return template;
    }
}
```

---

## 五、完整示例：客户管理模块

### 5.1 目录结构

```
harness-engineering/
├── harness-app/
│   └── src/main/java/com/harness/engineering/app/customer/
│       └── service/
│           └── CustomerServiceImpl.java  ← 使用 @Cached 注解
│
├── harness-domain/
│   └── src/main/java/com/harness/engineering/domain/customer/
│       └── gateway/
│           └── CustomerGateway.java      ← 网关接口（无缓存）
│
└── harness-infrastructure/
    └── src/main/java/com/harness/engineering/infrastructure/
        ├── customer/gatewayimpl/
        │   └── CustomerGatewayImpl.java  ← 网关实现（无缓存）
        └── cache/config/
            └── JetCacheConfig.java       ← 缓存配置
```

---

### 5.2 完整代码示例

#### Domain 层（纯业务逻辑）

```java
// domain/customer/model/Customer.java
@Data
public class Customer {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private CustomerStatus status;
    
    // 领域行为
    public void activate() {
        this.status = CustomerStatus.ACTIVE;
    }
    
    public void updateEmail(String newEmail) {
        if (!isValidEmail(newEmail)) {
            throw new BusinessException("Invalid email format");
        }
        this.email = newEmail;
    }
    
    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }
}

// domain/customer/gateway/CustomerGateway.java
public interface CustomerGateway {
    Customer getById(Long id);
    List<Customer> getByIds(List<Long> ids);
    void create(Customer customer);
    void update(Customer customer);
    void delete(Long id);
    boolean existsByEmail(String email);
}
```

---

#### Infrastructure 层（数据访问）

```java
// infrastructure/customer/gatewayimpl/CustomerGatewayImpl.java
@Component
public class CustomerGatewayImpl implements CustomerGateway {
    
    @Resource
    private CustomerMapper customerMapper;
    
    @Override
    public Customer getById(Long id) {
        CustomerDO customerDO = customerMapper.selectById(id);
        return CustomerConverter.INSTANCE.toEntity(customerDO);
    }
    
    @Override
    public List<Customer> getByIds(List<Long> ids) {
        List<CustomerDO> customerDOs = customerMapper.selectBatchIds(ids);
        return CustomerConverter.INSTANCE.toEntities(customerDOs);
    }
    
    @Override
    public void create(Customer customer) {
        CustomerDO customerDO = CustomerConverter.INSTANCE.toDO(customer);
        customerMapper.insert(customerDO);
    }
    
    @Override
    public void update(Customer customer) {
        CustomerDO customerDO = CustomerConverter.INSTANCE.toDO(customer);
        customerMapper.updateById(customerDO);
    }
    
    @Override
    public void delete(Long id) {
        customerMapper.deleteById(id);
    }
    
    @Override
    public boolean existsByEmail(String email) {
        return customerMapper.existsByEmail(email);
    }
}
```

---

#### App 层（业务协调 + 缓存）⭐

```java
// app/customer/service/CustomerServiceImpl.java
@Service
@CatchAndLog
@Slf4j
public class CustomerServiceImpl implements CustomerServiceI {
    
    @Resource
    private CustomerGateway customerGateway;
    
    /**
     * 获取客户详情（带缓存）
     */
    @Override
    @Cached(
        name = "customer:",
        key = "#qry.id",
        expire = 3600,
        cacheType = CacheType.BOTH,
        localExpire = 60,
        syncLocal = true,
        penetrationProtectConfig = @PenetrationProtectConfig(enable = true)
    )
    public Response<CustomerDTO> getCustomer(GetCustomerQry qry) {
        log.info("Querying customer: {}", qry.getId());
        
        Customer customer = customerGateway.getById(qry.getId());
        if (customer == null) {
            return Response.failure("Customer not found");
        }
        
        return Response.success(convertToDTO(customer));
    }
    
    /**
     * 批量查询客户（带缓存）
     */
    @Override
    @Cached(
        name = "customer:batch:",
        key = "#ids",
        expire = 600,
        cacheType = CacheType.BOTH
    )
    public MultiResponse<CustomerDTO> getCustomersByIds(List<Long> ids) {
        log.info("Batch querying customers: count={}", ids.size());
        
        List<Customer> customers = customerGateway.getByIds(ids);
        List<CustomerDTO> dtos = convertToDTOList(customers);
        
        return MultiResponse.of(dtos);
    }
    
    /**
     * 创建客户（清除缓存）
     */
    @Override
    @Invalidate(keys = {"customer:list"})
    public Response<CustomerDTO> createCustomer(CreateCustomerCmd cmd) {
        log.info("Creating customer: {}", cmd.getName());
        
        // 业务校验
        if (customerGateway.existsByEmail(cmd.getEmail())) {
            return Response.failure("Email already exists");
        }
        
        // 创建客户
        Customer customer = new Customer();
        BeanUtils.copyProperties(cmd, customer);
        customerGateway.create(customer);
        
        // 转换为 DTO
        CustomerDTO dto = convertToDTO(customer);
        return Response.success(dto);
    }
    
    /**
     * 更新客户（更新缓存）
     */
    @Override
    @Update(
        key = "#cmd.id",
        expire = 3600,
        invalidate = {"customer:list"}
    )
    public Response<Void> updateCustomer(UpdateCustomerCmd cmd) {
        log.info("Updating customer: {}", cmd.getId());
        
        Customer customer = customerGateway.getById(cmd.getId());
        if (customer == null) {
            return Response.failure("Customer not found");
        }
        
        // 更新客户信息
        customer.updateEmail(cmd.getEmail());
        customerGateway.update(customer);
        
        return Response.success();
    }
    
    /**
     * 删除客户（清除缓存）
     */
    @Override
    @Invalidate(
        keys = {
            "customer:#id",
            "customer:list"
        }
    )
    public Response<Void> deleteCustomer(Long id) {
        log.info("Deleting customer: {}", id);
        
        customerGateway.delete(id);
        return Response.success();
    }
    
    // ========== 辅助方法 ==========
    
    private CustomerDTO convertToDTO(Customer customer) {
        CustomerDTO dto = new CustomerDTO();
        BeanUtils.copyProperties(customer, dto);
        return dto;
    }
    
    private List<CustomerDTO> convertToDTOList(List<Customer> customers) {
        return customers.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }
}
```

---

## 六、最佳实践总结

### ✅ 必须遵守的规则

1. **缓存注解只能在 App 层** - 不能在 Domain 层或 Infra 层使用
2. **Domain 层不能感知缓存** - 保持领域模型的纯粹性
3. **合理设置过期时间** - 根据数据特点设置不同的 TTL
4. **使用两级缓存** - 生产环境必须用 `CacheType.BOTH`
5. **开启本地缓存同步** - 集群环境下必须设置 `syncLocal = true`

---

### 🎯 推荐的缓存策略

#### 不同数据的缓存时间

| 数据类型 | 过期时间 | 说明 |
|---------|---------|------|
| 用户信息 | 30 分钟 - 1 小时 | 频繁访问，变化较少 |
| 商品信息 | 1 - 24 小时 | 根据更新频率调整 |
| 订单信息 | 10 - 30 分钟 | 实时性要求较高 |
| 配置信息 | 24 小时 | 几乎不变 |
| 统计数据 | 5 - 10 分钟 | 计算复杂，可接受延迟 |
| 空值 | 5 分钟 | 防穿透，时间要短 |

---

### 📊 缓存键设计规范

**命名格式：**
```
{业务前缀}:{子业务}:{ID}
```

**示例：**
```java
// 单个对象
name = "customer:"
key = "#id"
// 结果：customer:123

// 列表对象
name = "customer:list:"
key = "#pageNo + ':' + #pageSize"
// 结果：customer:list:1:10

// 嵌套对象
name = "order:items:"
key = "#orderId"
// 结果：order:items:456
```

---

### 🔧 高级技巧

#### 技巧一：条件缓存

```java
@Cached(
    name = "vip_customer:",
    key = "#id",
    expire = 3600,
    condition = "#level == CustomerLevel.VIP"  // 只缓存 VIP 客户
)
public Customer getCustomer(Long id, CustomerLevel level) {
    return customerGateway.getById(id);
}
```

---

#### 技巧二：缓存预热

```java
@Component
public class CacheWarmer implements ApplicationRunner {
    
    @Resource
    private CustomerServiceI customerService;
    
    @Override
    public void run(ApplicationArguments args) {
        // 应用启动时预热热点数据
        List<Long> hotCustomerIds = Arrays.asList(1L, 2L, 3L);
        
        hotCustomerIds.forEach(id -> {
            GetCustomerQry qry = new GetCustomerQry();
            qry.setId(id);
            customerService.getCustomer(qry);  // 触发缓存
        });
        
        log.info("Cache warmed for {} hot customers", hotCustomerIds.size());
    }
}
```

---

#### 技巧三：监控统计

```java
// 查看缓存命中率（通过 Actuator）
GET /actuator/metrics/jetcache.hitRate

// 查看 QPS
GET /actuator/metrics/jetcache.qps

// 在 Grafana 中展示
// 使用 JetCache 提供的 Prometheus Exporter
```

---

## 七、常见问题 FAQ

### Q1: 缓存和数据库一致性如何保证？

**A:** JetCache 提供多种方案：

**方案一：更新时自动更新缓存**
```java
@Update(key = "#id", expire = 3600)
public void updateCustomer(UpdateCustomerCmd cmd) {
    // 业务逻辑
    // JetCache 会自动用新值更新缓存
}
```

**方案二：更新时清除缓存**
```java
@Invalidate(keys = "customer:#id")
public void updateCustomer(UpdateCustomerCmd cmd) {
    // 业务逻辑
    // 下次查询时会重新加载
}
```

**方案三：设置较短过期时间**
```java
@Cached(name = "customer:", key = "#id", expire = 300)  // 5 分钟
```

**推荐：** 根据业务场景选择
- 实时性要求高 → 方案一
- 可以容忍短暂不一致 → 方案二
- 读多写少 → 方案三

---

### Q2: 如何处理缓存穿透？

**A:** 使用 JetCache 的防穿透配置

```java
@Cached(
    name = "customer:",
    key = "#id",
    expire = 300,
    penetrationProtectConfig = @PenetrationProtectConfig(
        enable = true,
        periodOfDetectedSilence = 1000,
        thresholdOfSilence = 10
    )
)
```

**原理：** 自动缓存空值，防止恶意攻击

---

### Q3: 如何处理缓存击穿？

**A:** 使用互斥锁

```java
@Cached(
    name = "hot_product:",
    key = "#id",
    expire = 3600,
    lockExpire = 10  // 设置锁超时时间
)
```

**原理：** 只有一个线程重建缓存，其他线程等待

---

### Q4: 如何处理缓存雪崩？

**A:** 随机化过期时间

```java
@Cached(
    name = "product:",
    key = "#id",
    expire = 3600 + new Random().nextInt(600)  // 1 小时 ±10 分钟
)
```

**原理：** 避免大量缓存同时过期

---

### Q5: 集群环境下如何保证本地缓存一致性？

**A:** 开启本地缓存同步

```java
@Cached(
    name = "customer:",
    key = "#id",
    cacheType = CacheType.BOTH,
    syncLocal = true  // ✅ 开启本地缓存同步
)
```

**原理：** 通过 Redis Pub/Sub 广播缓存变更

---

### Q6: 如何监控缓存命中率？

**A:** JetCache 内置统计功能

```yaml
# application.yml
jetcache:
  statSupport:
    qps:
      enable: true
      periodSeconds: 10
```

**查看方式：**
1. 日志输出（默认每 10 秒）
2. Actuator 端点
3. Grafana 面板（需配置 Prometheus）

---

### Q7: 如何选择本地缓存和远程缓存的比例？

**A:** 根据访问频率和数据量

**高频访问数据：**
- 本地缓存：1-5 分钟
- 远程缓存：30-60 分钟
- 示例：用户信息、配置信息

**低频访问数据：**
- 本地缓存：30 秒 -1 分钟
- 远程缓存：5-10 分钟
- 示例：统计数据、列表数据

---

## 八、性能优化建议

### 8.1 本地缓存大小控制

```yaml
jetcache:
  local:
    default:
      limit: 10000  # 每个 JVM 最多缓存 1 万条
```

**建议：**
- 内存充足：10000 - 50000
- 内存紧张：1000 - 5000
- 根据单体数据大小调整

---

### 8.2 Redis 连接池优化

```yaml
jetcache:
  remote:
    default:
      poolConfig:
        minIdle: 10      # 最小空闲连接
        maxIdle: 50      # 最大空闲连接
        maxTotal: 200    # 最大总连接数
```

**建议：**
- 开发环境：maxTotal = 50
- 测试环境：maxTotal = 100
- 生产环境：maxTotal = 200-500

---

### 8.3 批量查询优化

```java
// ❌ 避免：循环单个查询
for (Long id : ids) {
    getCustomer(id);  // N 次查询
}

// ✅ 推荐：批量查询
@Cached(name = "customer:batch:", key = "#ids")
public List<Customer> getCustomersByIds(List<Long> ids) {
    return customerGateway.getByIds(ids);  // 1 次查询
}
```

---

### 8.4 热点数据特殊处理

```java
@Cached(
    name = "hot_product:",
    key = "#id",
    expire = 3600,
    cacheType = CacheType.BOTH,
    localExpire = 300,           // 本地缓存 5 分钟
    syncLocal = true,            // 同步本地缓存
    refreshPolicy = @RefreshPolicy(
        stopRefreshAfterLastAccess = 7200,  // 2 小时无访问停止刷新
        refreshInterval = 60                // 1 分钟刷新一次
    )
)
```

---

## 九、JetCache vs Spring Cache 对比

### JetCache 的核心优势

#### 1️⃣ **功能更丰富**

| 功能 | JetCache | Spring Cache |
|------|----------|--------------|
| 多级缓存 | ✅ 原生支持 | ❌ 需要自己实现 |
| 自动刷新 | ✅ `@RefreshPolicy` | ❌ 不支持 |
| 防穿透保护 | ✅ `@PenetrationProtectConfig` | ❌ 不支持 |
| 防击穿锁 | ✅ `lockExpire` | ❌ 不支持 |
| 批量缓存 | ✅ 原生支持 | ❌ 需要自己实现 |
| 监控统计 | ✅ 内置 | ❌ 需要集成 |

---

#### 2️⃣ **性能更优**

根据阿里官方测试数据：

| 场景 | Spring Cache | JetCache | 提升 |
|------|-------------|----------|------|
| 单次查询 | 10ms | 8ms | 20% |
| 批量查询 | 100ms | 50ms | 50% |
| 高并发（1000 QPS） | 150ms | 80ms | 47% |
| 缓存击穿场景 | 500ms | 100ms | 80% |

**性能优势来源：**
- ✅ 本地缓存（Caffeine）减少网络开销
- ✅ 批量查询优化减少数据库压力
- ✅ 互斥锁机制避免缓存击穿
- ✅ 自动刷新减少缓存 miss

---

#### 3️⃣ **开箱即用**

**Spring Cache 需要自己实现的功能：**
```java
// ❌ 防穿透：需要手动缓存空值
if (result == null) {
    cache.put(key, NULL_OBJECT);  // 自己定义 NULL_OBJECT
}

// ❌ 自动刷新：需要自己写定时任务
@Scheduled(fixedRate = 300000)
public void refreshCache() {
    // 手动刷新逻辑
}

// ❌ 多级缓存：需要自己封装
public class MultiLevelCache {
    private LocalCache local;
    private RemoteCache remote;
    // 需要写很多同步逻辑
}
```

**JetCache 只需配置：**
```java
// ✅ 一行注解搞定
@Cached(
    name = "customer:",
    key = "#id",
    expire = 3600,
    cacheType = CacheType.BOTH,
    penetrationProtectConfig = @PenetrationProtectConfig(enable = true),
    refreshPolicy = @RefreshPolicy(refreshInterval = 300)
)
```

---

#### 4️⃣ **经过大规模验证**

- **双 11 验证** - 支撑天猫双 11 海量流量
- **高并发场景** - 百万级 QPS 验证
- **稳定性** - 多年生产环境打磨

---

#### 5️⃣ **学习成本相当**

**Spring Cache 核心注解（5 个）：**
```java
@Cacheable
@CachePut
@CacheEvict
@Caching
@CacheConfig
```

**JetCache 核心注解（4 个）：**
```java
@Cached
@Update
@Invalidate
@CreateCache
```

**结论：** 注解数量差不多，但 JetCache 功能强大得多！

---

### 总结：为什么选择 JetCache？

1. ✅ **同样简洁** - 注解方式，声明式缓存
2. ✅ **功能更强** - 自动刷新、防穿透、防击穿
3. ✅ **性能更好** - 多级缓存、批量优化
4. ✅ **开箱即用** - 无需自己造轮子
5. ✅ **经过验证** - 阿里双 11 实战检验

**代价：**
- ⚠️ 引入第三方依赖（但阿里维护，活跃度高）
- ⚠️ 需要学习少量新概念（但 1 小时就能掌握）

**性价比：** 极高！💯

---

## 十、参考资料

### 📚 相关 Skill
- [ARCHITECTURE-SKILL](./ARCHITECTURE-SKILL) - COLA 架构规范
- [BUILD-SKILL](./BUILD-SKILL.md) - Maven 构建规范
- [DEVELOP-SKILL](./DEVELOP-SKILL.md) - 开发流程规范
- [OpenFeign-SKILL](./OpenFeign-SKILL.md) - 外部接口调用规范

### 🔗 外部资源
- [JetCache 官方文档](https://github.com/alibaba/jetcache)
- [JetCache GitHub](https://github.com/alibaba/jetcache)
- [JetCache 中文 Wiki](https://www.yuque.com/jetcache/docs)
- [Caffeine 性能优化指南](https://github.com/ben-manes/caffeine/wiki)

### 🛠️ 工具推荐
- [Redis Desktop Manager](https://redisdesktop.com/) - Redis 可视化工具
- [Redisson](https://redisson.org/) - Redis 客户端（可与 JetCache 配合）
- [Grafana](https://grafana.com/) - 监控面板（展示缓存指标）

---

**最后提醒：缓存是双刃剑，用得好提升性能，用不好带来灾难。** 合理使用缓存策略，持续监控和优化！🎯
