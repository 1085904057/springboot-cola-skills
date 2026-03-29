# Adapter 层 REST API 开发规范

> 📚 **这是 COLA 架构 Skill 体系的适配器层开发分册**，指导如何在 Adapter 层使用 Swagger 生成接口文档、使用 Validation 自动校验请求参数，以及定义 RESTful API。
> 
> **关联文档：**
> - [ARCHITECTURE-SKILL](./ARCHITECTURE-SKILL) - 架构规范
> - [BUILD-SKILL](./BUILD-SKILL.md) - 项目构建规范
> - [DEVELOP-SKILL](./DEVELOP-SKILL.md) - 开发流程规范
> - [CODEGEN-SKILL](./CODEGEN-SKILL.md) - Lombok + MapStruct 代码生成

---

## 一、元数据信息

### Name
`adapter-rest-api-validation`

### Description
基于 COLA 架构的 Adapter 层 REST API 开发规范。覆盖 Swagger 接口文档生成、Validation 参数自动校验、RESTful API 设计、统一响应格式等场景，打造标准化、易维护的 Web 接口层。

### 适用角色
- 💻 后端开发工程师
- 🔍 代码 Reviewer
- 📋 技术负责人
- 🧪 测试工程师

---

## 二、Overview（什么时候用）

### 适用场景

✅ **当你需要：**
- 在 Adapter 层定义 REST API 接口
- 使用 Swagger 自动生成接口文档
- 自动校验请求参数（@Validated）
- 统一响应格式和错误处理
- 实现前后端分离的接口规范

❌ **不适用场景：**
- RPC 接口（应该使用 Dubbo/gRPC）
- 内部服务间调用（应该使用 Feign Client）
- 简单的 CRUD 应用（可能不需要这么复杂）

### 在 COLA 架构中的位置

```
┌─────────────────────────────────────┐
│         前端/客户端                  │
│    (Web/Mobile/Third-party)         │
└──────────────┬──────────────────────┘
               │ HTTP/REST
               ↓
┌─────────────────────────────────────┐
│  Adapter 层 ⭐                       │
│   - Controller（定义 API）          │
│   - Swagger 注解（生成文档）        │
│   - Validation（参数校验）          │
│   - Exception Handler（异常处理）   │
├─────────────────────────────────────┤
│  Client 层                            │
│   - DTO（数据传输对象）             │
│   - Command/Query（请求命令）       │
├─────────────────────────────────────┤
│  App 层                               │
│   - Service（业务协调）             │
├─────────────────────────────────────┤
│  Domain 层                            │
│   - Entity/Value Object             │
├─────────────────────────────────────┤
│  Infrastructure 层                    │
│   - Repository/Gateway 实现         │
└─────────────────────────────────────┘
```

---

## 三、核心概念

### 3.1 Adapter 层的职责

在 COLA 架构中，Adapter 层负责：

1. **请求接入** - 接收 HTTP 请求
2. **参数校验** - 验证请求参数合法性
3. **协议转换** - HTTP Request → Command/Query
4. **响应封装** - 统一返回格式
5. **异常处理** - 统一异常捕获和转换
6. **文档生成** - Swagger 自动生成 API 文档

**关键原则：**
- ✅ Adapter 层只负责协议适配，不包含业务逻辑
- ✅ 业务逻辑应该在 App 层和 Domain 层
- ✅ Adapter 层依赖 Client 层（DTO/Command/Query）

---

### 3.2 Swagger 是什么？

Swagger（OpenAPI）是一个 API 文档生成工具：

**传统方式（手写文档）：**
```markdown
## 创建客户接口

**URL:** POST /api/customers

**请求参数：**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | String | 是 | 客户名称 |
| email | String | 否 | 邮箱地址 |

**响应示例：**
{
  "code": "Success",
  "data": {
    "id": 1,
    "name": "张三"
  }
}
```

**问题：**
- ❌ 文档容易过时
- ❌ 需要手动维护
- ❌ 无法在线测试

**使用 Swagger（注解生成）：**
```java
@RestController
@RequestMapping("/api/customers")
@Tag(name = "客户管理")
public class CustomerController {
    
    @PostMapping
    @Operation(summary = "创建客户")
    public Response<CustomerDTO> create(
        @RequestBody @Validated CreateCustomerCmd cmd) {
        // ...
    }
}
```

**优势：**
- ✅ 代码即文档（永远同步）
- ✅ 支持在线测试
- ✅ 自动生成多种格式

---

### 3.3 Validation 是什么？

Validation 是 JSR-380 规范的参数校验框架：

**传统方式（手动校验）：**
```java
@PostMapping("/customers")
public Response<CustomerDTO> create(@RequestBody CreateCustomerCmd cmd) {
    
    // ❌ 冗长的手动校验
    if (cmd.getName() == null || cmd.getName().isEmpty()) {
        return Response.fail("客户名称不能为空");
    }
    if (cmd.getName().length() > 100) {
        return Response.fail("客户名称不能超过 100 个字符");
    }
    if (cmd.getEmail() != null && !isValidEmail(cmd.getEmail())) {
        return Response.fail("邮箱格式不正确");
    }
    if (cmd.getLevel() < 1 || cmd.getLevel() > 5) {
        return Response.fail("客户等级必须在 1-5 之间");
    }
    
    // 业务逻辑
    // ...
}
```

**使用 Validation（声明式）：**
```java
// Command 对象
@Data
public class CreateCustomerCmd implements Command {
    
    @NotBlank(message = "客户名称不能为空")
    @Size(max = 100, message = "客户名称不能超过 100 个字符")
    private String name;
    
    @Email(message = "邮箱格式不正确")
    private String email;
    
    @Min(value = 1, message = "客户等级最小为 1")
    @Max(value = 5, message = "客户等级最大为 5")
    private Integer level;
}

// Controller
@PostMapping("/customers")
public Response<CustomerDTO> create(
    @RequestBody @Validated CreateCustomerCmd cmd) {
    // ✅ 自动校验，失败抛出 MethodArgumentNotValidException
    return customerService.create(cmd);
}
```

**优势：**
- ✅ 声明式校验（代码简洁）
- ✅ 统一错误处理
- ✅ 可组合复杂规则
- ✅ 支持国际化

---

## 四、技术栈选型

### 4.1 Spring Boot 3.x 推荐方案

| 功能 | 技术选型 | Maven 坐标 |
|------|---------|-----------|
| **Web 框架** | Spring Web MVC | `spring-boot-starter-web` |
| **参数校验** | Hibernate Validator | `spring-boot-starter-validation` |
| **API 文档** | SpringDoc OpenAPI | `springdoc-openapi-starter-webmvc-ui` |
| **JSON 处理** | Jackson | `spring-boot-starter-json` |

**为什么选择 SpringDoc？**
- ✅ 官方推荐（替代 springfox）
- ✅ 支持 Spring Boot 3.x
- ✅ 支持 OpenAPI 3.0 规范
- ✅ 自动配置，开箱即用

---

## 五、完整开发流程

### 5.1 添加依赖

**文件：** `{project}-adapter/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>com.{company}.{project}</groupId>
        <artifactId>{project}-engineering</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    
    <artifactId>{project}-adapter</artifactId>
    <packaging>jar</packaging>
    <name>Adapter Layer</name>
    <description>适配器层 - REST API 接口定义</description>
    
    <dependencies>
        <!-- 依赖 Client 层（必须） -->
        <dependency>
            <groupId>com.{company}.{project}</groupId>
            <artifactId>{project}-client</artifactId>
        </dependency>
        
        <!-- Spring Web MVC -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        
        <!-- Validation（参数校验）⭐ -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        
        <!-- SpringDoc OpenAPI（Swagger UI）⭐ -->
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            <version>2.3.0</version>
        </dependency>
        
        <!-- Lombok（简化代码） -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>
    </dependencies>
</project>
```

---

### 5.2 配置文件

**文件：** `{project}-start/src/main/resources/application.yml`

```yaml
spring:
  application:
    name: {project}-service
  
server:
  port: 8080
  
# SpringDoc OpenAPI 配置 ⭐
springdoc:
  api-docs:
    enabled: true
    path: /v3/api-docs  # OpenAPI JSON 文档路径
  
  swagger-ui:
    enabled: true
    path: /swagger-ui.html  # Swagger UI 访问路径
    operations-sorter: method  # 按方法排序
    tags-sorter: alpha  # 按标签排序
    show-extensions: true
  
  packages-to-scan: com.{company}.{project}.adapter  # 扫描的包
  paths-to-match: /**  # 匹配的 URL 路径

# 全局 CORS 配置（跨域）
cors:
  allowed-origins: "*"
  allowed-methods: GET,POST,PUT,DELETE,OPTIONS
  allowed-headers: "*"
  max-age: 3600
```

---

### 5.3 Swagger 配置类

**文件：** `{project}-adapter/src/main/java/com/{company}/{project}/adapter/config/SwaggerConfig.java`

```java
package com.{company}.{project}.adapter.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger 配置
 */
@Configuration
public class SwaggerConfig {
    
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("{project} API Documentation")
                .version("1.0.0")
                .description("{project} REST API 接口文档")
                .contact(new Contact()
                    .name("Harness Engineering Team")
                    .email("team@harness.com")))
            .addSecurityItem(new SecurityRequirement()
                .addList("Bearer Authentication"))
            .schemaRequirement("Bearer Authentication", 
                new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT"));
    }
}
```

---

### 5.4 统一响应格式

**文件：** `{project}-client/src/main/java/com/{company}/{project}/client/dto/Response.java`

```java
package com.{company}.{project}.client.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.io.Serializable;

/**
 * 统一响应格式
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Response<T> implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private String code;
    private String message;
    private T data;
    private Long timestamp;
    
    /**
     * 成功响应
     */
    public static <T> Response<T> success(T data) {
        return Response.<T>builder()
            .code("Success")
            .message("操作成功")
            .data(data)
            .timestamp(System.currentTimeMillis())
            .build();
    }
    
    /**
     * 失败响应
     */
    public static <T> Response<T> fail(String message) {
        return Response.<T>builder()
            .code("Error")
            .message(message)
            .timestamp(System.currentTimeMillis())
            .build();
    }
    
    /**
     * 失败响应（带错误码）
     */
    public static <T> Response<T> fail(String code, String message) {
        return Response.<T>builder()
            .code(code)
            .message(message)
            .timestamp(System.currentTimeMillis())
            .build();
    }
}
```

---

### 5.5 定义 Request VO（可选）

虽然可以直接使用 Client 层的 Command/Query，但有时需要在 Adapter 层做一层转换：

**文件：** `{project}-adapter/src/main/java/com/{company}/{project}/adapter/customer/web/vo/CustomerCreateRequest.java`

```java
package com.{company}.{project}.adapter.customer.web.vo;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 客户创建请求 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "客户创建请求")
public class CustomerCreateRequest {
    
    @NotBlank(message = "客户名称不能为空")
    @Size(max = 100, message = "客户名称不能超过 100 个字符")
    @Schema(description = "客户名称", example = "张三", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;
    
    @Email(message = "邮箱格式不正确")
    @Schema(description = "邮箱地址", example = "zhangsan@example.com")
    private String email;
    
    @Size(max = 20, message = "手机号不能超过 20 个字符")
    @Schema(description = "手机号", example = "13800138000")
    private String phone;
    
    @Min(value = 1, message = "客户等级最小为 1")
    @Max(value = 5, message = "客户等级最大为 5")
    @Schema(description = "客户等级（1-5）", example = "1")
    private Integer level;
}
```

**转换为 Command：**
```java
// Adapter → Client
CreateCustomerCmd cmd = CreateCustomerCmd.builder()
    .name(request.getName())
    .email(request.getEmail())
    .phone(request.getPhone())
    .level(request.getLevel())
    .build();
```

---

### 5.6 Controller 定义 API

**文件：** `{project}-adapter/src/main/java/com/{company}/{project}/adapter/customer/web/CustomerController.java`

```java
package com.{company}.{project}.adapter.customer.web;

import com.{company}.{project}.adapter.customer.web.vo.CustomerCreateRequest;
import com.{company}.{project}.adapter.customer.web.vo.CustomerUpdateRequest;
import com.{company}.{project}.client.customer.api.CustomerServiceI;
import com.{company}.{project}.client.customer.dto.CustomerDTO;
import com.{company}.{project}.client.customer.dto.command.CreateCustomerCmd;
import com.{company}.{project}.client.customer.dto.command.UpdateCustomerCmd;
import com.{company}.{project}.client.customer.dto.query.GetCustomerQry;
import com.{company}.{project}.client.customer.dto.query.CustomerListQry;
import com.{company}.{project}.client.dto.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 客户管理 Controller
 */
@RestController
@RequestMapping("/api/v1/customers")
@Tag(name = "客户管理 API", description = "客户的增删改查操作")
@Slf4j
public class CustomerController {
    
    @Resource
    private CustomerServiceI customerService;
    
    /**
     * 创建客户
     */
    @PostMapping
    @Operation(summary = "创建客户", description = "创建一个新的客户记录")
    public Response<CustomerDTO> createCustomer(
            @RequestBody @Valid CustomerCreateRequest request) {
        
        log.info("Creating customer: {}", request.getName());
        
        // VO → Command
        CreateCustomerCmd cmd = CreateCustomerCmd.builder()
            .name(request.getName())
            .email(request.getEmail())
            .phone(request.getPhone())
            .level(request.getLevel())
            .build();
        
        // 调用 App 层
        return customerService.createCustomer(cmd);
    }
    
    /**
     * 更新客户
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新客户", description = "更新指定客户的信息")
    public Response<CustomerDTO> updateCustomer(
            @Parameter(description = "客户 ID", example = "1")
            @PathVariable("id") Long id,
            
            @RequestBody @Valid CustomerUpdateRequest request) {
        
        log.info("Updating customer: {}", id);
        
        // VO → Command
        UpdateCustomerCmd cmd = UpdateCustomerCmd.builder()
            .id(id)
            .name(request.getName())
            .email(request.getEmail())
            .phone(request.getPhone())
            .level(request.getLevel())
            .build();
        
        return customerService.updateCustomer(cmd);
    }
    
    /**
     * 删除客户
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除客户", description = "删除指定的客户记录")
    public Response<Void> deleteCustomer(
            @Parameter(description = "客户 ID", example = "1")
            @PathVariable("id") Long id) {
        
        log.info("Deleting customer: {}", id);
        return customerService.deleteCustomer(id);
    }
    
    /**
     * 查询单个客户
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询客户", description = "根据 ID 查询客户详情")
    public Response<CustomerDTO> getCustomer(
            @Parameter(description = "客户 ID", example = "1")
            @PathVariable("id") Long id) {
        
        log.info("Getting customer: {}", id);
        
        GetCustomerQry qry = GetCustomerQry.builder()
            .id(id)
            .build();
        
        return customerService.getCustomer(qry);
    }
    
    /**
     * 分页查询客户列表
     */
    @GetMapping
    @Operation(summary = "查询客户列表", description = "分页查询客户列表，支持条件过滤")
    public Response<Page<CustomerDTO>> listCustomers(
            @Parameter(description = "客户名称关键词")
            @RequestParam(required = false) String nameKeyword,
            
            @Parameter(description = "页码", example = "1")
            @RequestParam(defaultValue = "1") Integer pageNo,
            
            @Parameter(description = "每页大小", example = "10")
            @RequestParam(defaultValue = "10") Integer pageSize) {
        
        log.info("Listing customers: keyword={}, page={}, size={}", 
                 nameKeyword, pageNo, pageSize);
        
        CustomerListQry qry = CustomerListQry.builder()
            .nameKeyword(nameKeyword)
            .pageNo(pageNo)
            .pageSize(pageSize)
            .build();
        
        return customerService.listCustomers(qry);
    }
}
```

---

### 5.7 全局异常处理

**文件：** `{project}-adapter/src/main/java/com/{company}/{project}/adapter/exception/GlobalExceptionHandler.java`

```java
package com.{company}.{project}.adapter.exception;

import com.{company}.{project}.client.dto.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    /**
     * 处理参数校验异常（@RequestBody）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Response<Void> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e) {
        
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining("; "));
        
        log.warn("Validation error: {}", errorMessage);
        return Response.fail("VALIDATION_ERROR", errorMessage);
    }
    
    /**
     * 处理参数绑定异常（@RequestParam/@PathVariable）
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Response<Void> handleBindException(BindException e) {
        
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining("; "));
        
        log.warn("Binding error: {}", errorMessage);
        return Response.fail("BIND_ERROR", errorMessage);
    }
    
    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Response<Void> handleBusinessException(BusinessException e) {
        
        log.warn("Business error: {}", e.getMessage());
        return Response.fail("BUSINESS_ERROR", e.getMessage());
    }
    
    /**
     * 处理其他异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Response<Void> handleException(Exception e) {
        
        log.error("Unexpected error", e);
        return Response.fail("SYSTEM_ERROR", "系统繁忙，请稍后再试");
    }
}
```

---

### 5.8 启动类配置

**文件：** `{project}-start/src/main/java/com/{company}/{project}/StartApplication.java`

```java
package com.{company}.{project};

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * 启动类
 */
@SpringBootApplication
@ComponentScan(basePackages = "com.{company}.{project}")
public class StartApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(StartApplication.class, args);
    }
}
```

---

## 六、常用 Validation 注解

### 6.1 字符串校验

```java
@NotBlank(message = "不能为空")  // 不能为 null 或空字符串
@NotEmpty(message = "不能为空")  // 不能为 null 或空集合
@NotNull(message = "不能为 null")  // 不能为 null

@Size(min = 1, max = 100, message = "长度必须在 1-100 之间")
@Pattern(regexp = "^[a-zA-Z0-9]+$", message = "只能包含字母和数字")
```

---

### 6.2 数值校验

```java
@Min(value = 0, message = "最小值为 0")
@Max(value = 100, message = "最大值为 100")

@DecimalMin(value = "0.01", message = "最小金额为 0.01")
@DecimalMax(value = "10000.00", message = "最大金额为 10000")

@Positive(message = "必须是正数")
@PositiveOrZero(message = "必须是正数或零")
@Negative(message = "必须是负数")
```

---

### 6.3 日期校验

```java
@Past(message = "必须是过去的日期")
@Future(message = "必须是未来的日期")
@PastOrPresent(message = "必须是过去或现在的日期")
@FutureOrPresent(message = "必须是未来或现在的日期")
```

---

### 6.4 Email 和 URL 校验

```java
@Email(message = "邮箱格式不正确")
@URL(message = "URL 格式不正确")
```

---

### 6.5 自定义校验器

**步骤一：创建自定义注解**

```java
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PhoneValidator.class)
@Documented
public @interface Phone {
    
    String message() default "手机号格式不正确";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
}
```

**步骤二：实现校验逻辑**

```java
public class PhoneValidator implements ConstraintValidator<Phone, String> {
    
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return true;  // 允许为空，配合@NotNull 使用
        }
        
        // 中国大陆手机号正则
        String regex = "^1[3-9]\\d{9}$";
        return value.matches(regex);
    }
}
```

**步骤三：使用自定义注解**

```java
@Data
public class CreateCustomerCmd implements Command {
    
    @Phone(message = "手机号格式不正确")
    private String phone;
}
```

---

## 七、Swagger 注解详解

### 7.1 类级别注解

```java
@Tag(name = "客户管理 API", description = "客户的增删改查操作")
@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {
    // ...
}
```

---

### 7.2 方法级别注解

```java
@Operation(
    summary = "创建客户",
    description = "创建一个新的客户记录",
    tags = {"customer"}
)
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "操作成功"),
    @ApiResponse(responseCode = "400", description = "请求参数错误"),
    @ApiResponse(responseCode = "500", description = "服务器内部错误")
})
@PostMapping
public Response<CustomerDTO> createCustomer(@RequestBody CreateCustomerCmd cmd) {
    // ...
}
```

---

### 7.3 参数注解

```java
@Parameter(
    name = "id",
    description = "客户 ID",
    example = "1",
    required = true,
    schema = @Schema(type = "integer", format = "int64")
)
@PathVariable("id") Long id
```

---

### 7.4 请求体注解

```java
@io.swagger.v3.oas.annotations.parameters.RequestBody(
    description = "客户创建请求",
    required = true,
    content = @Content(
        mediaType = "application/json",
        schema = @Schema(implementation = CreateCustomerCmd.class)
    )
)
@PostMapping
public Response<CustomerDTO> create(@RequestBody CreateCustomerCmd cmd) {
    // ...
}
```

---

## 八、最佳实践总结

### ✅ 必须遵守的规则

1. **Adapter 层只负责协议适配** - 不包含业务逻辑
2. **所有接口必须使用 @Validated** - 自动校验参数
3. **统一响应格式** - 使用 Response<T> 包装
4. **统一异常处理** - 使用 @RestControllerAdvice
5. **所有接口必须有 Swagger 注解** - 生成完整文档
6. **遵循 RESTful 风格** - 使用合适的 HTTP 方法
7. **版本号放在 URL 中** - `/api/v1/resource`

---

### 🎯 推荐的目录结构

```
{project}-adapter/
└── src/main/java/com/{company}/{project}/adapter/
    ├── customer/                    # 按业务模块分包
    │   └── web/
    │       ├── vo/                 # Request/Response VO
    │       │   ├── CustomerCreateRequest.java
    │       │   └── CustomerUpdateRequest.java
    │       └── CustomerController.java
    ├── order/                      # 另一个业务模块
    │   └── web/
    │       ├── vo/
    │       └── OrderController.java
    ├── config/                     # 配置类
    │   └── SwaggerConfig.java
    ├── exception/                  # 异常处理
    │   └── GlobalExceptionHandler.java
    └── common/                     # 通用部分（可选）
        └── BaseController.java
```

---

### 📊 RESTful API 设计规范

| 操作 | HTTP 方法 | URL | 说明 |
|------|---------|-----|------|
| 创建 | POST | `/api/v1/customers` | 创建新资源 |
| 更新 | PUT | `/api/v1/customers/{id}` | 全量更新 |
| 部分更新 | PATCH | `/api/v1/customers/{id}` | 部分更新 |
| 删除 | DELETE | `/api/v1/customers/{id}` | 删除资源 |
| 查询单个 | GET | `/api/v1/customers/{id}` | 获取详情 |
| 查询列表 | GET | `/api/v1/customers` | 分页查询 |

---

### 🔧 常用技巧

#### 技巧一：直接使用 Client 层的 Command

如果不需要额外转换，可以直接使用 Client 层的 Command：

```java
@PostMapping
public Response<CustomerDTO> create(
        @RequestBody @Valid CreateCustomerCmd cmd) {
    return customerService.createCustomer(cmd);
}
```

**优点：** 减少 VO 转换代码  
**缺点：** Client 层耦合了 Web 层的校验注解

---

#### 技巧二：使用分组校验

不同场景使用不同的校验规则：

```java
// 定义校验组
public interface CreateGroup {}
public interface UpdateGroup {}

// Command
@Data
public class CreateCustomerCmd implements Command {
    
    @NotBlank(message = "客户名称不能为空", groups = CreateGroup.class)
    private String name;
    
    @NotNull(message = "ID 不能为空", groups = UpdateGroup.class)
    private Long id;
}

// Controller
@PostMapping
public Response<CustomerDTO> create(
        @RequestBody @Validated(CreateGroup.class) CreateCustomerCmd cmd) {
    // ...
}

@PutMapping("/{id}")
public Response<CustomerDTO> update(
        @RequestBody @Validated(UpdateGroup.class) CreateCustomerCmd cmd) {
    // ...
}
```

---

#### 技巧三：批量操作接口

```java
@PostMapping("/batch")
@Operation(summary = "批量创建客户")
public Response<List<CustomerDTO>> batchCreate(
        @RequestBody @Valid List<@Validated CreateCustomerCmd> cmds) {
    
    List<CustomerDTO> results = cmds.stream()
        .map(customerService::createCustomer)
        .map(Response::getData)
        .collect(Collectors.toList());
    
    return Response.success(results);
}
```

---

#### 技巧四：导出接口

```java
@GetMapping("/export")
@Operation(summary = "导出客户列表")
public void exportCustomers(
        HttpServletResponse response,
        CustomerListQry qry) throws IOException {
    
    response.setContentType("application/vnd.ms-excel");
    response.setCharacterEncoding("utf-8");
    response.setHeader("Content-Disposition", 
        "attachment; filename=customers.xlsx");
    
    List<CustomerDTO> list = customerService.listCustomers(qry).getData();
    
    // 使用 EasyExcel 导出
    EasyExcel.write(response.getOutputStream(), CustomerDTO.class)
        .sheet("客户列表")
        .doWrite(list);
}
```

---

## 九、常见问题 FAQ

### Q1: Validation 不生效怎么办？

**A:** 检查以下几点

1. **是否添加了 `@Valid` 或 `@Validated` 注解**
```java
// ❌ 错误：没有校验注解
@PostMapping
public Response create(@RequestBody CreateCustomerCmd cmd) {
    // ...
}

// ✅ 正确：添加@Valid
@PostMapping
public Response create(@RequestBody @Valid CreateCustomerCmd cmd) {
    // ...
}
```

2. **是否在 Spring 管理的 Bean 中**
```java
// ❌ 错误：手动 new 的对象
new CustomerController().create(cmd);  // Validation 不生效

// ✅ 正确：Spring 注入
@Resource
private CustomerController controller;
controller.create(cmd);  // Validation 生效
```

3. **是否缺少依赖**
```xml
<!-- 必须包含 spring-boot-starter-validation -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

---

### Q2: Swagger UI 无法访问怎么办？

**A:** 检查配置

1. **确认依赖已添加**
```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.3.0</version>
</dependency>
```

2. **确认配置正确**
```yaml
springdoc:
  api-docs:
    enabled: true
  swagger-ui:
    enabled: true
    path: /swagger-ui.html
```

3. **访问正确的 URL**
```
http://localhost:8080/swagger-ui.html
```

4. **查看控制台是否有错误日志**

---

### Q3: 如何处理复杂的校验逻辑？

**A:** 使用自定义校验器

```java
// 场景：开始时间必须早于结束时间
@Data
public class MeetingCreateCmd {
    
    @Future(message = "开始时间必须是未来")
    private LocalDateTime startTime;
    
    @Future(message = "结束时间必须是未来")
    private LocalDateTime endTime;
    
    // 自定义校验方法
    @AssertTrue(message = "结束时间必须晚于开始时间")
    public boolean isValidEndTime() {
        if (startTime == null || endTime == null) {
            return true;
        }
        return endTime.isAfter(startTime);
    }
}
```

---

### Q4: 如何隐藏某些接口？

**A:** 使用 `@Hidden` 注解

```java
@Hidden  // ⭐ 不在 Swagger 文档中显示
@GetMapping("/internal")
public Response internalApi() {
    // ...
}
```

或者通过包扫描排除：
```yaml
springdoc:
  packages-to-scan: com.{company}.{project}.adapter.web  # 只扫描 web 包
```

---

### Q5: 如何实现接口的权限控制？

**A:** 结合 Spring Security

```java
@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {
    
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER_VIEW')")  // ⭐ 权限校验
    public Response<CustomerDTO> getCustomer(@PathVariable Long id) {
        // ...
    }
    
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER_CREATE')")
    public Response<CustomerDTO> createCustomer(@RequestBody CreateCustomerCmd cmd) {
        // ...
    }
}
```

---

### Q6: 如何处理文件上传？

**A:** 使用 MultipartFile

```java
@PostMapping("/upload")
@Operation(summary = "上传客户头像")
public Response<String> uploadAvatar(
        @Parameter(description = "头像文件")
        @RequestParam("file") @NotNull MultipartFile file) {
    
    if (file.isEmpty()) {
        return Response.fail("文件不能为空");
    }
    
    // 保存到 OSS/本地
    String url = fileStorageService.upload(file);
    
    return Response.success(url);
}
```

**Swagger 配置：**
```java
@Bean
public OpenAPI customOpenAPI() {
    return new OpenAPI()
        .schemaRequirement("multipart/form-data", 
            new io.swagger.v3.oas.models.media.MediaType()
                .schema(new Schema<>().type("object")));
}
```

---

## 十、完整示例

### 10.1 客户管理模块完整代码

#### Request VO

```java
// {project}-adapter/src/main/java/com/{company}/{project}/adapter/customer/web/vo/CustomerCreateRequest.java
package com.{company}.{project}.adapter.customer.web.vo;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "客户创建请求")
public class CustomerCreateRequest {
    
    @NotBlank(message = "客户名称不能为空")
    @Size(max = 100, message = "客户名称不能超过 100 个字符")
    @Schema(description = "客户名称", example = "张三", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;
    
    @Email(message = "邮箱格式不正确")
    @Schema(description = "邮箱地址", example = "zhangsan@example.com")
    private String email;
    
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "手机号", example = "13800138000")
    private String phone;
    
    @Min(value = 1, message = "客户等级最小为 1")
    @Max(value = 5, message = "客户等级最大为 5")
    @Schema(description = "客户等级（1-5）", example = "1")
    private Integer level;
}
```

---

#### Controller

```java
// {project}-adapter/src/main/java/com/{company}/{project}/adapter/customer/web/CustomerController.java
package com.{company}.{project}.adapter.customer.web;

import com.{company}.{project}.adapter.customer.web.vo.CustomerCreateRequest;
import com.{company}.{project}.adapter.customer.web.vo.CustomerUpdateRequest;
import com.{company}.{project}.client.customer.api.CustomerServiceI;
import com.{company}.{project}.client.customer.dto.CustomerDTO;
import com.{company}.{project}.client.customer.dto.command.CreateCustomerCmd;
import com.{company}.{project}.client.customer.dto.command.UpdateCustomerCmd;
import com.{company}.{project}.client.customer.dto.query.GetCustomerQry;
import com.{company}.{project}.client.customer.dto.query.CustomerListQry;
import com.{company}.{project}.client.dto.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@RequestMapping("/api/v1/customers")
@Tag(name = "客户管理 API", description = "客户的增删改查操作")
@Slf4j
public class CustomerController {
    
    @Resource
    private CustomerServiceI customerService;
    
    @PostMapping
    @Operation(summary = "创建客户", description = "创建一个新的客户记录")
    public Response<CustomerDTO> createCustomer(
            @RequestBody @Valid CustomerCreateRequest request) {
        
        log.info("Creating customer: {}", request.getName());
        
        CreateCustomerCmd cmd = CreateCustomerCmd.builder()
            .name(request.getName())
            .email(request.getEmail())
            .phone(request.getPhone())
            .level(request.getLevel())
            .build();
        
        return customerService.createCustomer(cmd);
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "更新客户", description = "更新指定客户的信息")
    public Response<CustomerDTO> updateCustomer(
            @Parameter(description = "客户 ID", example = "1")
            @PathVariable("id") Long id,
            
            @RequestBody @Valid CustomerUpdateRequest request) {
        
        log.info("Updating customer: {}", id);
        
        UpdateCustomerCmd cmd = UpdateCustomerCmd.builder()
            .id(id)
            .name(request.getName())
            .email(request.getEmail())
            .phone(request.getPhone())
            .level(request.getLevel())
            .build();
        
        return customerService.updateCustomer(cmd);
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "删除客户", description = "删除指定的客户记录")
    public Response<Void> deleteCustomer(
            @Parameter(description = "客户 ID", example = "1")
            @PathVariable("id") Long id) {
        
        log.info("Deleting customer: {}", id);
        return customerService.deleteCustomer(id);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "查询客户", description = "根据 ID 查询客户详情")
    public Response<CustomerDTO> getCustomer(
            @Parameter(description = "客户 ID", example = "1")
            @PathVariable("id") Long id) {
        
        log.info("Getting customer: {}", id);
        
        GetCustomerQry qry = GetCustomerQry.builder()
            .id(id)
            .build();
        
        return customerService.getCustomer(qry);
    }
    
    @GetMapping
    @Operation(summary = "查询客户列表", description = "分页查询客户列表，支持条件过滤")
    public Response<Page<CustomerDTO>> listCustomers(
            @Parameter(description = "客户名称关键词")
            @RequestParam(required = false) String nameKeyword,
            
            @Parameter(description = "页码", example = "1")
            @RequestParam(defaultValue = "1") Integer pageNo,
            
            @Parameter(description = "每页大小", example = "10")
            @RequestParam(defaultValue = "10") Integer pageSize) {
        
        log.info("Listing customers: keyword={}, page={}, size={}", 
                 nameKeyword, pageNo, pageSize);
        
        CustomerListQry qry = CustomerListQry.builder()
            .nameKeyword(nameKeyword)
            .pageNo(pageNo)
            .pageSize(pageSize)
            .build();
        
        return customerService.listCustomers(qry);
    }
}
```

---

## 十一、参考资料

### 📚 相关 Skill
- [ARCHITECTURE-SKILL](./ARCHITECTURE-SKILL) - COLA 架构规范
- [BUILD-SKILL](./BUILD-SKILL.md) - Maven 构建规范
- [CODEGEN-SKILL](./CODEGEN-SKILL.md) - Lombok + MapStruct 代码生成
- [DEVELOP-SKILL](./DEVELOP-SKILL.md) - 开发流程规范

### 🔗 外部资源
- [SpringDoc 官方文档](https://springdoc.org/)
- [Hibernate Validator 官方文档](https://hibernate.org/validator/)
- [OpenAPI Specification](https://swagger.io/specification/)
- [RESTful API 最佳实践](https://restfulapi.net/)

### 🛠️ 工具推荐
- **Swagger UI** - API 文档可视化
- **Postman** - API 测试工具
- **Apifox** - 国产 API 协作平台

---

**最后提醒：Adapter 层只是协议适配器，真正的业务逻辑应该在 App 层和 Domain 层！** 🎯
