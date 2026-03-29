# OpenFeign 外部接口调用开发规范

## 一、元数据信息

### Name
`openfeign-external-api-integration`

### Description
在 COLA 架构的 Infrastructure 层使用 OpenFeign 进行外部 REST API 调用的标准开发规范。包含 Feign Client 声明、连接池配置、统一异常处理、熔断降级等最佳实践。

---

## 二、Overview（什么时候用）

### 适用场景

✅ **当你需要调用外部 HTTP/REST API 时：**
- 调用第三方服务（如短信服务、支付服务、邮件服务）
- 微服务之间的 HTTP 通信
- 调用外部系统提供的 RESTful 接口
- 需要声明式、接口化的 HTTP 客户端

✅ **当你希望：**
- 代码简洁，避免模板代码
- 自动负载均衡（集成 Spring Cloud LoadBalancer）
- 快速实现熔断降级（集成 Sentinel/Hystrix）
- 统一的配置管理（超时、重试、连接池）

❌ **不适用场景：**
- 简单的单次 HTTP 调用（可用 RestTemplate）
- 需要精细控制 HTTP 请求细节
- 响应式编程场景（考虑 WebClient）

### 在 COLA 架构中的位置

```
COLA 分层架构：
┌─────────────────────────────────┐
│   Adapter 层 (Controller)        │
├─────────────────────────────────┤
│   App 层 (Service/Executor)      │
├─────────────────────────────────┤
│   Domain 层 (Model/Gateway)      │ ← 定义网关接口
├─────────────────────────────────┤
│   Infra 层                       │
│   ├─ gatewayimpl/               │ ← 实现网关（调用 Feign Client）
│   ├─ client/                    │ ← Feign Client 声明 ⭐
│   └─ config/                    │ ← Feign 配置 ⭐
└─────────────────────────────────┘
```

---

## 三、怎么用（完整开发流程）

### Step 1: 添加依赖

#### 1.1 父 pom.xml 统一管理版本

```xml
<properties>
    <!-- Spring Cloud 版本 -->
    <spring-cloud.version>2023.0.0</spring-cloud.version>
    
    <!-- OpenFeign 相关版本 -->
    <feign-httpclient.version>13.2.1</feign-httpclient.version>
    <httpclient5.version>5.3</httpclient5.version>
</properties>

<dependencyManagement>
    <dependencies>
        <!-- Spring Cloud 依赖管理 -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>${spring-cloud.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
        
        <!-- Feign Apache HttpClient 支持 -->
        <dependency>
            <groupId>io.github.openfeign</groupId>
            <artifactId>feign-httpclient</artifactId>
            <version>${feign-httpclient.version}</version>
        </dependency>
        
        <!-- Apache HttpClient5 -->
        <dependency>
            <groupId>org.apache.httpcomponents.client5</groupId>
            <artifactId>httpclient5</artifactId>
            <version>${httpclient5.version}</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

#### 1.2 harness-infrastructure/pom.xml 添加依赖

```xml
<dependencies>
    <!-- OpenFeign Starter -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-openfeign</artifactId>
    </dependency>
    
    <!-- Feign Apache HttpClient 支持（启用连接池） -->
    <dependency>
        <groupId>io.github.openfeign</groupId>
        <artifactId>feign-httpclient</artifactId>
    </dependency>
    
    <!-- Apache HttpClient5 -->
    <dependency>
        <groupId>org.apache.httpcomponents.client5</groupId>
        <artifactId>httpclient5</artifactId>
    </dependency>
    
    <!-- 可选：Sentinel 熔断降级 -->
    <!--
    <dependency>
        <groupId>com.alibaba.cloud</groupId>
        <artifactId>spring-cloud-starter-alibaba-sentinel</artifactId>
    </dependency>
    -->
</dependencies>
```

---

### Step 2: 配置文件（application.yml）

#### 2.1 基础配置（harness-start/src/main/resources/application.yml）

```yaml
feign:
  client:
    config:
      # 全局默认配置
      default:
        connectTimeout: 5000          # 连接超时 5 秒
        readTimeout: 10000            # 读取超时 10 秒
        loggerLevel: BASIC            # 日志级别：NONE/BASIC/HEADERS/FULL
        
        # 重试配置
        retryer:
          enabled: true
          period: 1000               # 重试间隔 1 秒
          maxPeriod: 5000            # 最大重试间隔 5 秒
          maxAttempts: 3             # 最大重试次数
      
      # 特定服务的独立配置
      user-service:
        connectTimeout: 3000
        readTimeout: 8000
        loggerLevel: HEADERS
      
      sms-service:
        connectTimeout: 2000
        readTimeout: 5000
  
  # Apache HttpClient 配置（启用连接池）
  httpclient:
    enabled: true
    
    # 连接池参数（重要！）
    pool:
      enabled: true
      maxTotal: 200        # 最大连接总数
      maxPerRoute: 20      # 每个路由的最大连接数
      idleTimeout: 30000   # 空闲连接超时 30 秒
      timeToLive: 60000    # 连接存活时间 60 秒
    
    # 连接超时配置
    connection:
      requestTimeout: 5000  # 连接请求超时
      connectTimeout: 5000  # 连接建立超时
      socketTimeout: 10000  # Socket 读写超时
  
  # 压缩配置（可选）
  compression:
    request:
      enabled: true
      mime-types: text/xml,application/xml,application/json
      min-request-size: 2048  # 最小压缩大小 2KB
    response:
      enabled: true

# 外部服务地址配置
external:
  user-service:
    base-url: https://api.example.com
  sms-service:
    base-url: https://sms.example.com
    app-key: ${SMS_APP_KEY:your-app-key}
```

---

### Step 3: 创建 Feign Client（harness-infrastructure/client/）

#### 3.1 标准示例

```java
package com.harness.engineering.infrastructure.client;

import com.harness.engineering.infrastructure.client.dto.ApiResponse;
import com.harness.engineering.infrastructure.client.dto.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * 用户服务 Feign Client
 * 
 * <p>用于调用外部用户管理系统的 REST API</p>
 * 
 * <strong>API 文档：</strong>
 * <ul>
 *     <li>官方文档：<a href="https://docs.example.com/user-api">https://docs.example.com/user-api</a></li>
 *     <li>Swagger UI：<a href="https://api.example.com/swagger-ui.html">https://api.example.com/swagger-ui.html</a></li>
 *     <li>Postman Collection: <code>User API v2.postman_collection.json</code></li>
 * </ul>
 * 
 * <strong>认证方式：</strong>Bearer Token (JWT)
 * 
 * <strong>限流策略：</strong>
 * <ul>
 *     <li>普通接口：100 次/分钟</li>
 *     <li>写接口：20 次/分钟</li>
 * </ul>
 * 
 * @author Harness Engineering Team
 * @since 2026-03-28
 * @see UserDTO
 * @see ApiResponse
 */
@FeignClient(
    name = "user-service",
    url = "${external.user-service.base-url}",
    configuration = FeignClientConfig.class,
    fallbackFactory = UserServiceFallbackFactory.class  // 可选：熔断降级
)
public interface UserServiceClient {
    
    /**
     * 根据 ID 获取用户信息
     * 
     * <p><strong>API 端点：</strong>GET /api/v2/users/{id}</p>
     * 
     * <p><strong>请求参数：</strong></p>
     * <ul>
     *     <li>id - 用户唯一标识（必填）</li>
     * </ul>
     * 
     * <p><strong>返回字段：</strong></p>
     * <ul>
     *     <li>id - 用户 ID</li>
     *     <li>name - 用户姓名</li>
     *     <li>email - 邮箱地址</li>
     *     <li>phone - 手机号码</li>
     *     <li>status - 用户状态（ACTIVE/INACTIVE/LOCKED）</li>
     * </ul>
     * 
     * <p><strong>错误码：</strong></p>
     * <ul>
     *     <li>404 - 用户不存在</li>
     *     <li>401 - 认证失败</li>
     *     <li>429 - 请求频率超限</li>
     * </ul>
     * 
     * @param id 用户 ID
     * @return 用户信息响应对象
     * @throws BusinessException 当用户不存在或认证失败时抛出
     */
    @GetMapping("/api/v2/users/{id}")
    ApiResponse<UserDTO> getUserById(@PathVariable("id") Long id);
    
    /**
     * 创建新用户
     * 
     * <p><strong>API 端点：</strong>POST /api/v2/users</p>
     * 
     * <p><strong>请求体字段：</strong></p>
     * <ul>
     *     <li>name - 用户姓名（必填，最大 50 字符）</li>
     *     <li>email - 邮箱（必填，唯一）</li>
     *     <li>phone - 手机号（可选）</li>
     * </ul>
     * 
     * <p><strong>返回字段：</strong></p>
     * <ul>
     *     <li>id - 新创建的用户 ID</li>
     *     <li>createTime - 创建时间</li>
     * </ul>
     * 
     * <p><strong>错误码：</strong></p>
     * <ul>
     *     <li>400 - 参数错误</li>
     *     <li>409 - 邮箱已存在</li>
     * </ul>
     * 
     * @param request 创建用户请求
     * @return 创建结果
     */
    @PostMapping("/api/v2/users")
    ApiResponse<UserDTO> createUser(@RequestBody CreateUserRequest request);
    
    /**
     * 更新用户信息
     * 
     * <p><strong>API 端点：</strong>PUT /api/v2/users/{id}</p>
     * 
     * @param id 用户 ID
     * @param request 更新请求
     * @return 更新结果
     */
    @PutMapping("/api/v2/users/{id}")
    ApiResponse<Void> updateUser(
        @PathVariable("id") Long id,
        @RequestBody UpdateUserRequest request
    );
    
    /**
     * 删除用户
     * 
     * <p><strong>API 端点：</strong>DELETE /api/v2/users/{id}</p>
     * 
     * @param id 用户 ID
     * @return 删除结果
     */
    @DeleteMapping("/api/v2/users/{id}")
    ApiResponse<Void> deleteUser(@PathVariable("id") Long id);
    
    /**
     * 分页查询用户列表
     * 
     * <p><strong>API 端点：</strong>GET /api/v2/users</p>
     * 
     * <p><strong>查询参数：</strong></p>
     * <ul>
     *     <li>page - 页码（从 1 开始，默认 1）</li>
     *     <li>size - 每页大小（默认 20，最大 100）</li>
     *     <li>name - 按名称模糊查询（可选）</li>
     *     <li>status - 按状态筛选（可选）</li>
     * </ul>
     * 
     * @param page 页码
     * @param size 每页大小
     * @param name 名称（可选）
     * @param status 状态（可选）
     * @return 分页结果
     */
    @GetMapping("/api/v2/users")
    ApiResponse<PageResult<UserDTO>> listUsers(
        @RequestParam(value = "page", defaultValue = "1") Integer page,
        @RequestParam(value = "size", defaultValue = "20") Integer size,
        @RequestParam(value = "name", required = false) String name,
        @RequestParam(value = "status", required = false) String status
    );
}
```

#### 3.2 DTO 定义（harness-infrastructure/client/dto/）

```java
package com.harness.engineering.infrastructure.client.dto;

import lombok.Data;
import java.io.Serializable;

/**
 * 统一 API 响应包装类
 * 
 * @param <T> 数据类型
 */
@Data
public class ApiResponse<T> implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 状态码
     */
    private Integer code;
    
    /**
     * 消息
     */
    private String message;
    
    /**
     * 数据
     */
    private T data;
    
    /**
     * 是否成功
     */
    private Boolean success;
    
    /**
     * 时间戳
     */
    private Long timestamp;
    
    /**
     * 判断是否成功
     */
    public boolean isSuccess() {
        return this.success != null && this.success;
    }
}

// ============================================

package com.harness.engineering.infrastructure.client.dto;

import lombok.Data;
import java.io.Serializable;

/**
 * 用户数据传输对象
 */
@Data
public class UserDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 用户 ID
     */
    private Long id;
    
    /**
     * 用户姓名
     */
    private String name;
    
    /**
     * 邮箱
     */
    private String email;
    
    /**
     * 手机号
     */
    private String phone;
    
    /**
     * 用户状态：ACTIVE/INACTIVE/LOCKED
     */
    private String status;
    
    /**
     * 创建时间
     */
    private Long createTime;
    
    /**
     * 更新时间
     */
    private Long updateTime;
}

// ============================================

package com.harness.engineering.infrastructure.client.dto;

import lombok.Data;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 创建用户请求
 */
@Data
public class CreateUserRequest implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 用户姓名（必填，最大 50 字符）
     */
    @NotBlank(message = "姓名不能为空")
    @Size(max = 50, message = "姓名最大长度 50 字符")
    private String name;
    
    /**
     * 邮箱（必填）
     */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;
    
    /**
     * 手机号（可选）
     */
    private String phone;
}

// ============================================

package com.harness.engineering.infrastructure.client.dto;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

/**
 * 分页结果
 * 
 * @param <T> 数据类型
 */
@Data
public class PageResult<T> implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 当前页码
     */
    private Integer page;
    
    /**
     * 每页大小
     */
    private Integer size;
    
    /**
     * 总记录数
     */
    private Long total;
    
    /**
     * 总页数
     */
    private Integer totalPages;
    
    /**
     * 数据列表
     */
    private List<T> items;
    
    /**
     * 是否有上一页
     */
    public boolean hasPrevious() {
        return page > 1;
    }
    
    /**
     * 是否有下一页
     */
    public boolean hasNext() {
        return page < totalPages;
    }
}
```

---

### Step 4: 配置类（harness-infrastructure/config/）

```java
package com.harness.engineering.infrastructure.config;

import feign.Logger;
import feign.RequestInterceptor;
import feign.Retryer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Feign Client 通用配置
 * 
 * <p>所有 Feign Client 共享的配置</p>
 * 
 * @author Harness Engineering Team
 */
@Slf4j
@Configuration
public class FeignClientConfig {
    
    /**
     * 配置日志级别
     * 
     * NONE - 不记录日志
     * BASIC - 仅记录请求方法和 URL
     * HEADERS - 记录请求方法和 URL + 请求头
     * FULL - 记录请求和响应的详细信息（包括 body）
     */
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }
    
    /**
     * 配置重试机制
     */
    @Bean
    public Retryer feignRetryer() {
        // 参数：period(初始间隔), maxPeriod(最大间隔), maxAttempts(最大尝试次数)
        return new Retryer.Default(1000, 5000, 3);
    }
    
    /**
     * 请求拦截器 - 统一添加认证头
     * 
     * @param apiSecret 从配置文件读取
     */
    @Value("${external.api.secret:}")
    private String apiSecret;
    
    @Bean
    public RequestInterceptor authRequestInterceptor() {
        return template -> {
            // 添加认证头
            if (!apiSecret.isEmpty()) {
                template.header("Authorization", "Bearer " + apiSecret);
            }
            
            // 添加统一请求头
            template.header("Content-Type", "application/json");
            template.header("User-Agent", "Harness-Engineering-Platform/1.0");
            
            // 添加请求追踪 ID（用于日志追踪）
            String requestId = generateRequestId();
            template.header("X-Request-ID", requestId);
            
            log.debug("Feign request interceptor - RequestID: {}, URL: {}", 
                requestId, template.url());
        };
    }
    
    /**
     * 生成请求追踪 ID
     */
    private String generateRequestId() {
        return "REQ-" + System.currentTimeMillis() + "-" + 
               Thread.currentThread().getId();
    }
}
```

---

### Step 5: 熔断降级工厂（可选但推荐）

```java
package com.harness.engineering.infrastructure.fallback;

import com.harness.engineering.infrastructure.client.UserServiceClient;
import com.harness.engineering.infrastructure.client.dto.ApiResponse;
import com.harness.engineering.infrastructure.client.dto.CreateUserRequest;
import com.harness.engineering.infrastructure.client.dto.PageResult;
import com.harness.engineering.infrastructure.client.dto.UpdateUserRequest;
import com.harness.engineering.infrastructure.client.dto.UserDTO;
import feign.hystrix.FallbackFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 用户服务熔断降级工厂
 * 
 * <p>当外部服务不可用时的降级处理</p>
 * 
 * @author Harness Engineering Team
 */
@Slf4j
@Component
public class UserServiceFallbackFactory implements FallbackFactory<UserServiceClient> {
    
    @Override
    public UserServiceClient create(Throwable cause) {
        log.error("UserService 触发熔断降级", cause);
        
        return new UserServiceClient() {
            
            @Override
            public ApiResponse<UserDTO> getUserById(Long id) {
                log.warn("getUserById 降级处理 - id: {}", id);
                ApiResponse<UserDTO> response = new ApiResponse<>();
                response.setSuccess(false);
                response.setCode(503);
                response.setMessage("用户服务暂时不可用，请稍后重试");
                response.setData(null);
                response.setTimestamp(System.currentTimeMillis());
                return response;
            }
            
            @Override
            public ApiResponse<UserDTO> createUser(CreateUserRequest request) {
                log.warn("createUser 降级处理");
                ApiResponse<UserDTO> response = new ApiResponse<>();
                response.setSuccess(false);
                response.setCode(503);
                response.setMessage("用户服务暂时不可用，请稍后重试");
                return response;
            }
            
            @Override
            public ApiResponse<Void> updateUser(Long id, UpdateUserRequest request) {
                log.warn("updateUser 降级处理 - id: {}", id);
                ApiResponse<Void> response = new ApiResponse<>();
                response.setSuccess(false);
                response.setCode(503);
                response.setMessage("用户服务暂时不可用，请稍后重试");
                return response;
            }
            
            @Override
            public ApiResponse<Void> deleteUser(Long id) {
                log.warn("deleteUser 降级处理 - id: {}", id);
                ApiResponse<Void> response = new ApiResponse<>();
                response.setSuccess(false);
                response.setCode(503);
                response.setMessage("用户服务暂时不可用，请稍后重试");
                return response;
            }
            
            @Override
            public ApiResponse<PageResult<UserDTO>> listUsers(Integer page, Integer size, 
                                                              String name, String status) {
                log.warn("listUsers 降级处理");
                ApiResponse<PageResult<UserDTO>> response = new ApiResponse<>();
                response.setSuccess(false);
                response.setCode(503);
                response.setMessage("用户服务暂时不可用，请稍后重试");
                return response;
            }
        };
    }
}
```

---

### Step 6: 在 Gateway 实现中使用

```java
package com.harness.engineering.infrastructure.gatewayimpl;

import com.harness.engineering.domain.gateway.ExternalUserGateway;
import com.harness.engineering.domain.model.User;
import com.harness.engineering.infrastructure.client.UserServiceClient;
import com.harness.engineering.infrastructure.client.dto.ApiResponse;
import com.harness.engineering.infrastructure.client.dto.UserDTO;
import com.harness.engineering.infrastructure.converter.UserConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 外部用户网关实现
 * 
 * @author Harness Engineering Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExternalUserGatewayImpl implements ExternalUserGateway {
    
    private final UserServiceClient userServiceClient;
    private final UserConverter userConverter;
    
    @Override
    public User getById(Long userId) {
        log.info("调用外部用户服务获取用户信息 - userId: {}", userId);
        
        try {
            ApiResponse<UserDTO> response = userServiceClient.getUserById(userId);
            
            if (response.isSuccess() && response.getData() != null) {
                User user = userConverter.toDomain(response.getData());
                log.info("成功获取用户信息 - userId: {}, name: {}", userId, user.getName());
                return user;
            } else {
                log.warn("获取用户信息失败 - userId: {}, errorCode: {}, message: {}", 
                    userId, response.getCode(), response.getMessage());
                return null;
            }
            
        } catch (Exception e) {
            log.error("调用外部用户服务异常 - userId: {}", userId, e);
            throw new RuntimeException("获取用户信息失败：" + e.getMessage(), e);
        }
    }
    
    @Override
    public void save(User user) {
        log.info("保存用户信息到外部系统 - userId: {}", user.getId());
        // 实现类似...
    }
}
```

---

### Step 7: 单元测试

#### 7.1 Feign Client 测试

```java
package com.harness.engineering.infrastructure.client;

import com.harness.engineering.infrastructure.client.dto.ApiResponse;
import com.harness.engineering.infrastructure.client.dto.CreateUserRequest;
import com.harness.engineering.infrastructure.client.dto.UserDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 用户服务 Feign Client 集成测试
 * 
 * <p>测试外部 API 调用是否正常</p>
 * 
 * @author Harness Engineering Team
 */
@SpringBootTest
class UserServiceClientTest {
    
    @Autowired
    private UserServiceClient userServiceClient;
    
    /**
     * 测试：根据 ID 获取用户
     */
    @Test
    void testGetUserById() {
        // Given
        Long userId = 1L;
        
        // When
        ApiResponse<UserDTO> response = userServiceClient.getUserById(userId);
        
        // Then
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertNotNull(response.getData());
        assertEquals(userId, response.getData().getId());
        
        System.out.println("用户姓名：" + response.getData().getName());
        System.out.println("用户邮箱：" + response.getData().getEmail());
    }
    
    /**
     * 测试：创建用户
     */
    @Test
    void testCreateUser() {
        // Given
        CreateUserRequest request = new CreateUserRequest();
        request.setName("张三");
        request.setEmail("zhangsan@example.com");
        request.setPhone("13800138000");
        
        // When
        ApiResponse<UserDTO> response = userServiceClient.createUser(request);
        
        // Then
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertNotNull(response.getData().getId());
        
        System.out.println("创建的用户 ID: " + response.getData().getId());
    }
    
    /**
     * 测试：分页查询用户列表
     */
    @Test
    void testListUsers() {
        // Given
        Integer page = 1;
        Integer size = 10;
        
        // When
        var response = userServiceClient.listUsers(page, size, null, null);
        
        // Then
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertNotNull(response.getData());
        assertTrue(response.getData().getItems().size() <= size);
        
        System.out.println("总记录数：" + response.getData().getTotal());
        System.out.println("当前页数据量：" + response.getData().getItems().size());
    }
    
    /**
     * 测试：获取不存在的用户（预期失败）
     */
    @Test
    void testGetNonExistentUser() {
        // Given
        Long userId = 999999L;
        
        // When
        ApiResponse<UserDTO> response = userServiceClient.getUserById(userId);
        
        // Then
        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(404, response.getCode());
    }
}
```

#### 7.2 Mock 测试（不依赖真实外部服务）

```java
package com.harness.engineering.infrastructure.gatewayimpl;

import com.harness.engineering.infrastructure.client.UserServiceClient;
import com.harness.engineering.infrastructure.client.dto.ApiResponse;
import com.harness.engineering.infrastructure.client.dto.UserDTO;
import com.harness.engineering.infrastructure.converter.UserConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ExternalUserGatewayImpl 单元测试（Mock 方式）
 */
@ExtendWith(MockitoExtension.class)
class ExternalUserGatewayImplTest {
    
    @Mock
    private UserServiceClient userServiceClient;
    
    @Mock
    private UserConverter userConverter;
    
    @InjectMocks
    private ExternalUserGatewayImpl externalUserGateway;
    
    @BeforeEach
    void setUp() {
        // 初始化
    }
    
    @Test
    void testGetById_Success() {
        // Given
        Long userId = 1L;
        UserDTO userDTO = new UserDTO();
        userDTO.setId(userId);
        userDTO.setName("张三");
        
        ApiResponse<UserDTO> apiResponse = new ApiResponse<>();
        apiResponse.setSuccess(true);
        apiResponse.setData(userDTO);
        
        com.harness.engineering.domain.model.User domainUser = 
            new com.harness.engineering.domain.model.User();
        domainUser.setId(userId);
        domainUser.setName("张三");
        
        when(userServiceClient.getUserById(userId)).thenReturn(apiResponse);
        when(userConverter.toDomain(userDTO)).thenReturn(domainUser);
        
        // When
        var result = externalUserGateway.getById(userId);
        
        // Then
        assertNotNull(result);
        assertEquals(userId, result.getId());
        assertEquals("张三", result.getName());
        verify(userServiceClient, times(1)).getUserById(userId);
        verify(userConverter, times(1)).toDomain(userDTO);
    }
    
    @Test
    void testGetById_Failure() {
        // Given
        Long userId = 999L;
        ApiResponse<UserDTO> apiResponse = new ApiResponse<>();
        apiResponse.setSuccess(false);
        apiResponse.setCode(404);
        apiResponse.setMessage("用户不存在");
        
        when(userServiceClient.getUserById(userId)).thenReturn(apiResponse);
        
        // When
        var result = externalUserGateway.getById(userId);
        
        // Then
        assertNull(result);
        verify(userServiceClient, times(1)).getUserById(userId);
    }
}
```

---

### Step 8: 启动类配置

```java
package com.harness.engineering;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 应用启动类
 * 
 * @author Harness Engineering Team
 */
@SpringBootApplication
@EnableFeignClients(basePackages = "com.harness.engineering.infrastructure.client")
public class HarnessStartApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(HarnessStartApplication.class, args);
        System.out.println("========================================");
        System.out.println("Harness Engineering Platform Started!");
        System.out.println("========================================");
    }
}
```

---

## 四、最佳实践总结

### ✅ 必须遵守的规范

1. **所有 Feign Client 放在 `harness-infrastructure/client/` 目录**
2. **必须在类注释上添加官方 API 文档链接**（如果有）
3. **必须编写单元测试**（集成测试 + Mock 测试）
4. **必须配置连接池参数**（maxTotal, maxPerRoute）
5. **必须设置超时时间**（connectTimeout, readTimeout）
6. **推荐使用熔断降级**（fallbackFactory）

### ✅ 推荐做法

1. **统一响应包装类**（ApiResponse<T>）
2. **统一的请求拦截器**（添加认证头、追踪 ID）
3. **详细的 JavaDoc 注释**（包括 API 端点、参数说明、错误码）
4. **DTO 验证注解**（@NotNull, @Email 等）
5. **日志记录**（记录请求参数、响应结果、异常信息）

### ❌ 禁止做法

1. ❌ 在 Domain 层直接调用 Feign Client
2. ❌ 在 App 层直接使用 Feign Client（应通过 Gateway）
3. ❌ 不配置超时时间（使用默认值）
4. ❌ 不使用连接池（性能差）
5. ❌ 不写单元测试

---

## 五、常见问题 FAQ

### Q1: Feign Client 报 "No qualifying bean of type" 错误？
A: 确保启动类添加了 `@EnableFeignClients` 注解，且 basePackages 路径正确。

### Q2: 如何查看 Feign 的详细日志？
A: 配置 `feign.client.config.default.loggerLevel=FULL`，并设置对应的 logger 级别为 DEBUG。

### Q3: 如何实现多个不同的认证方式？
A: 为不同的 Feign Client 创建不同的 RequestInterceptor，或在拦截器中根据 URL 判断。

### Q4: 连接池参数如何调优？
A: 根据并发量和外部服务能力调整：
- 小型系统：maxTotal=50, maxPerRoute=10
- 中型系统：maxTotal=200, maxPerRoute=20
- 大型系统：maxTotal=500, maxPerRoute=50

### Q5: 如何处理文件上传下载？
A: Feign 不支持文件上传下载，建议使用 RestTemplate 或 WebClient。

---

## 六、参考资料

- [Spring Cloud OpenFeign 官方文档](https://spring.io/projects/spring-cloud-openfeign)
- [OpenFeign GitHub](https://github.com/OpenFeign/feign)
- [Apache HttpClient 配置指南](https://hc.apache.org/httpcomponents-client-5.2.x/)
- [COLA-SKILL.md](../../../.agent/skills/architecture/ARCHITECTURE-SKILL)
