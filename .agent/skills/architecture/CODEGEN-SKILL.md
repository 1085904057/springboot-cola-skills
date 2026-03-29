# Lombok + MapStruct 代码简化规范

> 📚 **这是 COLA 架构 Skill 体系的代码生成规范分册**，指导如何使用 Lombok 和 MapStruct 减少样板代码，提升开发效率。
> 
> **关联文档：**
> - [ARCHITECTURE-SKILL](./ARCHITECTURE-SKILL) - 架构规范
> - [BUILD-SKILL](./BUILD-SKILL.md) - 项目构建规范
> - [DEVELOP-SKILL](./DEVELOP-SKILL.md) - 开发流程规范

---

## 一、元数据信息

### Name
`lombok-mapstruct-codegen`

### Description
基于 COLA 架构的 Lombok 和 MapStruct 使用规范。覆盖 POJO 简化、对象转换、Converter 编写等场景，在保持代码清晰的同时大幅减少样板代码。

### 适用角色
- 💻 后端开发工程师
- 🔍 代码 Reviewer
- 📋 技术负责人

---

## 二、Overview（什么时候用）

### 适用场景

✅ **当你需要：**
- 简化 POJO 类的 getter/setter/toString 等样板代码
- 在不同层次的对象之间进行转换（DTO ↔ Entity ↔ DO）
- 减少手写转换代码的错误和维护成本
- 保持代码简洁性和可读性

❌ **不适用场景：**
- 需要自定义复杂转换逻辑（建议手动编写）
- 性能极其敏感的场景（MapStruct 有轻微开销）
- 简单的只有 2-3 个字段的对象（可能不需要）

### 在 COLA 架构中的位置

```
┌─────────────────────────────────────┐
│  Client 层                            │
│   - DTO（使用 Lombok）               │
│   - Command/Query（使用 Lombok）     │
├─────────────────────────────────────┤
│  Adapter 层                           │
│   - VO/Request（使用 Lombok）        │
├─────────────────────────────────────┤
│  App 层                               │
│   - Converter（使用 MapStruct）⭐    │
├─────────────────────────────────────┤
│  Domain 层                            │
│   - Entity（使用 Lombok）            │
│   - Value Object（使用 Lombok）      │
├─────────────────────────────────────┤
│  Infrastructure 层                    │
│   - DO（使用 Lombok）                │
│   - Converter（使用 MapStruct）⭐    │
└─────────────────────────────────────┘
```

---

## 三、核心概念

### 3.1 Lombok 是什么？

Lombok 是一个 Java 库，通过注解自动生成样板代码：

**传统方式（50+ 行）：**
```java
public class Customer {
    private Long id;
    private String name;
    private String email;
    
    public Customer() {}
    
    public Customer(Long id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    @Override
    public String toString() {
        return "Customer{id=" + id + ", name='" + name + "', email='" + email + "'}";
    }
    
    @Override
    public boolean equals(Object o) {
        // ... 30+ 行代码
    }
    
    @Override
    public int hashCode() {
        // ... 10+ 行代码
    }
}
```

**使用 Lombok（5 行）：**
```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {
    private Long id;
    private String name;
    private String email;
}
```

**编译后效果相同！** ✅

---

### 3.2 MapStruct 是什么？

MapStruct 是一个代码生成器，自动生成对象转换代码：

**传统方式（手写转换）：**
```java
public class CustomerConverter {
    
    public static CustomerDTO toDTO(Customer customer) {
        if (customer == null) {
            return null;
        }
        
        CustomerDTO dto = new CustomerDTO();
        dto.setId(customer.getId());
        dto.setName(customer.getName());
        dto.setEmail(customer.getEmail());
        dto.setPhone(customer.getPhone());
        // ... 20+ 字段
        
        return dto;
    }
    
    public static Customer toEntity(CustomerDTO dto) {
        // ... 同样 20+ 行代码
    }
}
```

**使用 MapStruct（声明式）：**
```java
@Mapper
public interface CustomerConverter {
    
    CustomerConverter INSTANCE = Mappers.getFactory(CustomerConverter.class).getMapper();
    
    CustomerDTO toDTO(Customer customer);
    Customer toEntity(CustomerDTO dto);
}
```

**编译时自动生成实现类！** ✅

---

### 3.3 为什么需要两者配合？

在 COLA 架构中，不同层次有不同的 POJO：

```
Client 层：CustomerDTO
           ↓ ↑
        转换（MapStruct）
           ↓ ↑
Domain 层：Customer（Entity）
           ↓ ↑
        转换（MapStruct）
           ↓ ↑
Infra 层：CustomerDO（Data Object）
```

**Lombok 负责：** 简化每个 POJO 的内部代码  
**MapStruct 负责：** 简化 POJO 之间的转换代码

---

## 四、Lombok 在 COLA 架构中的使用

### 4.1 Client 层（DTO/Command/Query）

#### DTO 对象

**文件：** `{project}-client/src/main/java/com/{company}/{project}/client/customer/dto/CustomerDTO.java`

```java
package com.{company}.{project}.client.customer.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.io.Serializable;
import java.util.Date;

/**
 * 客户 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long id;
    private String name;
    private String email;
    private String phone;
    private Integer level;
    private Date createTime;
    private Date updateTime;
}
```

**关键注解说明：**
- `@Data` - 生成 getter/setter/toString/equals/hashCode
- `@Builder` - 支持链式创建：`CustomerDTO.builder().id(1L).name("张三").build()`
- `@NoArgsConstructor` - 无参构造函数（框架需要）
- `@AllArgsConstructor` - 全参构造函数（测试需要）

---

#### Command 对象

**文件：** `{project}-client/src/main/java/com/{company}/{project}/client/customer/dto/command/CreateCustomerCmd.java`

```java
package com.{company}.{project}.client.customer.dto.command;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;

/**
 * 创建客户命令
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCustomerCmd implements Command {
    
    @NotBlank(message = "客户名称不能为空")
    private String name;
    
    @Email(message = "邮箱格式不正确")
    private String email;
    
    private String phone;
    private Integer level;
}
```

---

#### Query 对象

**文件：** `{project}-client/src/main/java/com/{company}/{project}/client/customer/dto/query/CustomerListQry.java`

```java
package com.{company}.{project}.client.customer.dto.query;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 客户列表查询
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerListQry implements Query {
    
    private String nameKeyword;
    private Integer pageNo = 1;
    private Integer pageSize = 10;
}
```

---

### 4.2 Domain 层（Entity/Value Object）

#### Entity 实体

**文件：** `{project}-domain/src/main/java/com/{company}/{project}/domain/customer/model/Customer.java`

```java
package com.{company}.{project}.domain.customer.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.Objects;

/**
 * 客户实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer {
    
    private Long id;
    private String name;
    private String email;
    private String phone;
    private CustomerLevel level;
    
    /**
     * 领域行为：激活客户
     */
    public void activate() {
        // 业务逻辑
    }
    
    /**
     * 领域行为：更新邮箱（带业务规则）
     */
    public void updateEmail(String newEmail) {
        if (!isValidEmail(newEmail)) {
            throw new BusinessException("邮箱格式不正确");
        }
        this.email = newEmail;
    }
    
    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }
}
```

**⚠️ 注意事项：**
- Entity 包含业务行为，不是贫血模型
- Lombok 只用于生成基础方法，业务逻辑要手写
- 不要使用 `@EqualsAndHashCode` 自动比较所有字段（应该比较 ID）

**正确的 equals/hashCode：**
```java
@Override
public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Customer customer = (Customer) o;
    return Objects.equals(id, customer.id);  // 只比较 ID
}

@Override
public int hashCode() {
    return Objects.hash(id);
}
```

---

#### Value Object 值对象

**文件：** `{project}-domain/src/main/java/com/{company}/{project}/domain/customer/model/CustomerAddress.java`

```java
package com.{company}.{project}.domain.customer.model;

import lombok.Value;
import lombok.With;

/**
 * 客户地址（值对象）
 */
@Value  // 不可变对象（所有字段 final，只有 getter，没有 setter）
@With   // 支持 withXxx() 方法创建新对象
public class CustomerAddress {
    
    String province;
    String city;
    String district;
    String street;
    String zipCode;
    
    /**
     * 值对象应该是不可变的
     * 修改时会返回新对象，而不是修改当前对象
     */
    public CustomerAddress withFullAddress(String fullAddress) {
        // 解析完整地址
        // ...
        return new CustomerAddress(province, city, district, street, zipCode);
    }
}
```

**使用示例：**
```java
CustomerAddress address = new CustomerAddress("浙江省", "杭州市", "西湖区", "XX 路", "310000");

// 修改街道（返回新对象）
CustomerAddress newAddress = address.withStreet("YY 路");

// 原对象不变
assert address.getStreet().equals("XX 路");  // true
assert newAddress.getStreet().equals("YY 路");  // true
```

---

### 4.3 Infrastructure 层（DO - Data Object）

**文件：** `{project}-infrastructure/src/main/java/com/{company}/{project}/infrastructure/customer/mapper/dataobject/CustomerDO.java`

```java
package com.{company}.{project}.infrastructure.customer.mapper.dataobject;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.baomidou.mybatisplus.annotation.*;
import java.util.Date;

/**
 * 客户数据对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("customer")
public class CustomerDO {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String name;
    private String email;
    private String phone;
    private Integer level;
    
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
    
    @TableLogic
    private Integer deleted;
}
```

---

### 4.4 Adapter 层（VO/Request）

**文件：** `{project}-adapter/src/main/java/com/{company}/{project}/adapter/customer/web/vo/CustomerVO.java`

```java
package com.{company}.{project}.adapter.customer.web.vo;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;

/**
 * 客户视图对象（返回给前端）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerVO {
    
    private Long id;
    private String name;
    private String email;
    private String phone;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
}
```

---

## 五、MapStruct 在 COLA 架构中的使用

### 5.1 Converter 的位置

在 COLA 架构中，Converter 有两个位置：

```
App 层：
  - 负责 DTO ↔ Entity 的转换
  - 接口名：XXXConverter
  
Infrastructure 层：
  - 负责 Entity ↔ DO 的转换
  - 接口名：XXXConverter
```

---

### 5.2 App 层 Converter（DTO ↔ Entity）

**文件：** `{project}-app/src/main/java/com/{company}/{project}/app/customer/converter/CustomerConverter.java`

```java
package com.{company}.{project}.app.customer.converter;

import com.{company}.{project}.client.customer.dto.CustomerDTO;
import com.{company}.{project}.domain.customer.model.Customer;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import java.util.List;

/**
 * 客户转换器
 */
@Mapper
public interface CustomerConverter {
    
    CustomerConverter INSTANCE = Mappers.getFactory(CustomerConverter.class).getMapper();
    
    /**
     * Entity 转 DTO
     */
    CustomerDTO toDTO(Customer customer);
    
    /**
     * DTO 转 Entity
     */
    Customer toEntity(CustomerDTO dto);
    
    /**
     * 批量转换
     */
    List<CustomerDTO> toDTOList(List<Customer> customers);
    
    List<Customer> toEntityList(List<CustomerDTO> dtos);
}
```

**编译后生成的实现类：**
```java
// target/generated-sources/annotations/.../CustomerConverterImpl.java
@Override
public CustomerDTO toDTO(Customer customer) {
    if (customer == null) {
        return null;
    }
    
    CustomerDTO dto = new CustomerDTO();
    dto.setId(customer.getId());
    dto.setName(customer.getName());
    dto.setEmail(customer.getEmail());
    dto.setPhone(customer.getPhone());
    dto.setLevel(customer.getLevel());
    
    return dto;
}
```

**完全自动生成，无需手写！** ✅

---

### 5.3 Infrastructure 层 Converter（Entity ↔ DO）

**文件：** `{project}-infrastructure/src/main/java/com/{company}/{project}/infrastructure/customer/converter/CustomerConverter.java`

```java
package com.{company}.{project}.infrastructure.customer.converter;

import com.{company}.{project}.domain.customer.model.Customer;
import com.{company}.{project}.infrastructure.customer.mapper.dataobject.CustomerDO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import java.util.List;

/**
 * 客户转换器
 */
@Mapper
public interface CustomerConverter {
    
    CustomerConverter INSTANCE = Mappers.getFactory(CustomerConverter.class).getMapper();
    
    /**
     * DO 转 Entity
     */
    Customer toEntity(CustomerDO customerDO);
    
    /**
     * Entity 转 DO
     */
    CustomerDO toDO(Customer customer);
    
    /**
     * 批量转换
     */
    List<Customer> toEntities(List<CustomerDO> customerDOs);
    
    List<CustomerDO> toDOs(List<Customer> customers);
}
```

---

### 5.4 在 Gateway 实现中使用

**文件：** `{project}-infrastructure/src/main/java/com/{company}/{project}/infrastructure/customer/gatewayimpl/CustomerGatewayImpl.java`

```java
package com.{company}.{project}.infrastructure.customer.gatewayimpl;

import com.{company}.{project}.domain.customer.gateway.CustomerGateway;
import com.{company}.{project}.domain.customer.model.Customer;
import com.{company}.{project}.infrastructure.customer.mapper.CustomerMapper;
import com.{company}.{project}.infrastructure.customer.mapper.dataobject.CustomerDO;
import com.{company}.{project}.infrastructure.customer.converter.CustomerConverter;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;
import java.util.List;

@Component
public class CustomerGatewayImpl implements CustomerGateway {
    
    @Resource
    private CustomerMapper customerMapper;
    
    @Override
    public Customer getById(Long id) {
        CustomerDO customerDO = customerMapper.selectById(id);
        
        // ✅ 使用 MapStruct 转换
        return CustomerConverter.INSTANCE.toEntity(customerDO);
    }
    
    @Override
    public void create(Customer customer) {
        // ✅ 使用 MapStruct 转换
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
    public List<Customer> listByIds(List<Long> ids) {
        List<CustomerDO> customerDOs = customerMapper.selectBatchIds(ids);
        return CustomerConverter.INSTANCE.toEntities(customerDOs);
    }
}
```

---

### 5.5 在 App 层 Service 中使用

**文件：** `{project}-app/src/main/java/com/{company}/{project}/app/customer/service/CustomerServiceImpl.java`

```java
package com.{company}.{project}.app.customer.service;

import com.{company}.{project}.app.customer.executor.CreateCustomerCmdExe;
import com.{company}.{project}.app.customer.executor.GetCustomerQryExe;
import com.{company}.{project}.client.customer.api.CustomerServiceI;
import com.{company}.{project}.client.customer.dto.CustomerDTO;
import com.{company}.{project}.client.customer.dto.command.CreateCustomerCmd;
import com.{company}.{project}.client.customer.dto.query.GetCustomerQry;
import com.{company}.{project}.client.dto.Response;
import com.{company}.{project}.app.customer.converter.CustomerConverter;
import com.{company}.{project}.domain.customer.model.Customer;
import com.{company}.{project}.domain.customer.gateway.CustomerGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;

@Service
@Slf4j
public class CustomerServiceImpl implements CustomerServiceI {
    
    @Resource
    private CustomerGateway customerGateway;
    
    @Override
    public Response<CustomerDTO> createCustomer(CreateCustomerCmd cmd) {
        log.info("Creating customer: {}", cmd.getName());
        
        // 1. Command 转 Entity
        Customer customer = CustomerConverter.INSTANCE.toEntityFromCmd(cmd);
        
        // 2. 调用 Domain 层
        customerGateway.create(customer);
        
        // 3. Entity 转 DTO
        CustomerDTO dto = CustomerConverter.INSTANCE.toDTO(customer);
        
        return Response.success(dto);
    }
    
    @Override
    public Response<CustomerDTO> getCustomer(GetCustomerQry qry) {
        log.info("Getting customer: {}", qry.getId());
        
        // 1. 从 Domain 层获取 Entity
        Customer customer = customerGateway.getById(qry.getId());
        
        // 2. Entity 转 DTO
        CustomerDTO dto = CustomerConverter.INSTANCE.toDTO(customer);
        
        return Response.success(dto);
    }
}
```

---

## 六、高级用法

### 6.1 自定义字段映射

**场景：** 字段名不一致

```java
@Mapper
public interface CustomerConverter {
    
    CustomerConverter INSTANCE = Mappers.getFactory(CustomerConverter.class).getMapper();
    
    /**
     * 字段名不一致时使用 @Mapping
     */
    @Mapping(source = "userName", target = "name")  // userName → name
    @Mapping(source = "emailAddress", target = "email")  // emailAddress → email
    CustomerDTO toDTO(Customer customer);
    
    /**
     * 嵌套对象映射
     */
    @Mapping(source = "address", target = "address")
    CustomerDTO toDTO(Customer customer);
}
```

---

### 6.2 表达式和常量

```java
@Mapper
public interface CustomerConverter {
    
    /**
     * 使用表达式
     */
    @Mapping(target = "status", expression = "java(customer.isActive() ? 1 : 0)")
    CustomerDTO toDTO(Customer customer);
    
    /**
     * 使用常量
     */
    @Mapping(target = "version", constant = "1")
    CustomerDTO toDTO(Customer customer);
    
    /**
     * 使用默认值
     */
    @Mapping(target = "level", defaultValue = "1")
    CustomerDTO toDTO(Customer customer);
}
```

---

### 6.3 日期格式化

```java
@Mapper
public interface CustomerConverter {
    
    /**
     * 日期格式化
     */
    @Mapping(target = "createTimeStr", 
             dateFormat = "yyyy-MM-dd HH:mm:ss",
             source = "createTime")
    CustomerDTO toDTO(Customer customer);
}
```

---

### 6.4 忽略字段

```java
@Mapper
public interface CustomerConverter {
    
    /**
     * 忽略某些字段不转换
     */
    @Mapping(target = "password", ignore = true)  // 不转换密码
    @Mapping(target = "secretKey", ignore = true)  // 不转换密钥
    CustomerDTO toDTO(Customer customer);
}
```

---

### 6.5 多参数映射

```java
@Mapper
public interface CustomerConverter {
    
    /**
     * 多个参数合并到一个对象
     */
    Customer createCustomer(@MappingTarget Customer customer,
                           @MappingSource CreateCustomerCmd cmd,
                           @MappingSource UpdateCustomerCmd updateCmd);
}
```

---

### 6.6 Spring 集成

如果使用 Spring，可以让 Spring 管理 Converter：

```java
@Mapper(componentModel = "spring")  // ⭐ 声明为 Spring Bean
public interface CustomerConverter {
    
    CustomerDTO toDTO(Customer customer);
    Customer toEntity(CustomerDTO dto);
}
```

**使用方式：**
```java
@Service
public class CustomerServiceImpl implements CustomerServiceI {
    
    @Autowired  // ⭐ 直接注入，不需要 INSTANCE
    private CustomerConverter converter;
    
    @Override
    public Response<CustomerDTO> getCustomer(GetCustomerQry qry) {
        Customer customer = customerGateway.getById(qry.getId());
        CustomerDTO dto = converter.toDTO(customer);  // 直接使用
        return Response.success(dto);
    }
}
```

---

## 七、最佳实践总结

### ✅ 必须遵守的规则

1. **Lombok 注解统一风格** - 所有 POJO 都使用相同的注解组合
2. **Entity 要有业务行为** - 不要只是 getter/setter
3. **Value Object 要不可变** - 使用 `@Value` 而不是 `@Data`
4. **Converter 放在正确的层** - App 层和 Infra 层分别管理
5. **不要混用转换方向** - toDTO 和 toEntity 分开定义

---

### 🎯 推荐的注解组合

#### DTO/Command/Query/DO/VO
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
```

#### Entity（有业务逻辑）
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
// 手写 equals/hashCode（只比较 ID）
```

#### Value Object（不可变）
```java
@Value
@With  // 可选，支持 withXxx() 方法
```

#### Converter
```java
@Mapper
// 或
@Mapper(componentModel = "spring")  // Spring 集成
```

---

### 📊 不同层的 POJO 对比

| 层 | 类名后缀 | Lombok 注解 | 是否可序列化 | 是否有业务逻辑 |
|----|---------|-----------|------------|--------------|
| **Client** | DTO | `@Data` + `@Builder` | ✅ 是 | ❌ 否 |
| **Client** | Command | `@Data` + `@Builder` | ✅ 是 | ❌ 否 |
| **Client** | Query | `@Data` + `@Builder` | ✅ 是 | ❌ 否 |
| **Domain** | Entity | `@Data` + `@Builder` | ❌ 否 | ✅ 是 |
| **Domain** | Value Object | `@Value` | ❌ 否 | ✅ 是 |
| **Infra** | DO | `@Data` + `@Builder` | ❌ 否 | ❌ 否 |
| **Adapter** | VO | `@Data` + `@Builder` | ✅ 是 | ❌ 否 |

---

### 🔧 IDE 配置

#### IDEA 配置 Lombok

1. **安装插件：** Settings → Plugins → 搜索 "Lombok" → Install
2. **启用注解处理：** Build → Compiler → Annotation Processors → Enable
3. **查看生成的代码：** 右键类 → Show Generated Code

#### IDEA 配置 MapStruct

1. **安装插件：** Settings → Plugins → 搜索 "MapStruct Support" → Install
2. **配置生成目录：** Settings → Build → Generated Sources → Add
   - 路径：`target/generated-sources/annotations`

---

### ⚠️ 常见错误

#### 错误一：找不到生成的类

**现象：** `cannot find symbol: class XXXConverterImpl`

**原因：**
- 没有编译项目
- IDE 没有识别生成目录

**解决：**
```bash
mvn clean compile  # 重新编译
```

IDEA 中：File → Invalidate Caches / Restart

---

#### 错误二：字段名不匹配

**现象：** 编译警告 "Unmapped target properties"

**解决：**
```java
@Mapping(source = "userName", target = "name")
CustomerDTO toDTO(Customer customer);
```

---

#### 错误三：循环依赖

**现象：** StackOverflowError

**原因：** 两个对象互相引用

**解决：**
```java
@Mapping(target = "orders", ignore = true)  // 忽略循环引用字段
CustomerDTO toDTO(Customer customer);
```

---

#### 错误四：Lombok 和 MapStruct 冲突

**现象：** 编译错误 "Cannot find getter/setter"

**原因：** 注解处理器顺序问题

**解决：** 在父 POM 中调整顺序
```xml
<annotationProcessorPaths>
    <path>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <version>${lombok.version}</version>
    </path>
    <path>
        <groupId>org.mapstruct</groupId>
        <artifactId>mapstruct-processor</artifactId>
        <version>${mapstruct.version}</version>
    </path>
</annotationProcessorPaths>
```

---

## 八、常见问题 FAQ

### Q1: Lombok 会不会影响代码可读性？

**A:** 不会，反而提升可读性！

**对比：**
```java
// ❌ 50 行 getter/setter，阅读时要跳过
public class Customer {
    private Long id;
    // ... 20 个字段
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    // ... 40 行代码
}

// ✅ 5 行，清晰明了
@Data
public class Customer {
    private Long id;
    private String name;
    // ... 一眼看清所有字段
}
```

---

### Q2: MapStruct 和 BeanUtils 哪个更好？

**A:** MapStruct 更好！

| 对比项 | MapStruct | BeanUtils |
|--------|-----------|-----------|
| **性能** | ⭐⭐⭐⭐⭐ 编译期生成代码 | ⭐⭐ 运行时反射 |
| **类型安全** | ⭐⭐⭐⭐⭐ 编译期检查 | ⭐⭐ 运行时才发现错误 |
| **可维护性** | ⭐⭐⭐⭐⭐ 接口清晰 | ⭐⭐⭐ 字符串字段名 |
| **灵活性** | ⭐⭐⭐⭐⭐ 支持自定义映射 | ⭐⭐ 只能同名映射 |

**示例：**
```java
// ❌ BeanUtils：运行时反射，慢且不安全
BeanUtils.copyProperties(source, target);

// ✅ MapStruct：编译期生成代码，快且安全
CustomerDTO dto = converter.toDTO(customer);
```

---

### Q3: 是否需要为每个模块都写 Converter？

**A:** 根据复杂度决定

**简单场景（< 5 个字段）：** 可以直接手写
```java
CustomerDTO dto = new CustomerDTO();
dto.setId(customer.getId());
dto.setName(customer.getName());
return dto;
```

**复杂场景（> 5 个字段）：** 使用 MapStruct
```java
@Mapper
public interface CustomerConverter {
    CustomerDTO toDTO(Customer customer);
}
```

---

### Q4: 如何处理嵌套对象的转换？

**A:** MapStruct 自动支持

```java
// Entity
@Data
public class Order {
    private Long id;
    private Customer customer;  // 嵌套对象
    private List<OrderItem> items;  // 集合
}

// DTO
@Data
public class OrderDTO {
    private Long id;
    private CustomerDTO customer;
    private List<OrderItemDTO> items;
}

// Converter
@Mapper
public interface OrderConverter {
    OrderDTO toDTO(Order order);  // 自动递归转换嵌套对象
}
```

---

### Q5: 如何调试生成的代码？

**A:** 三种方式

**方式一：查看生成的源代码**
```bash
# 编译后查看
ls target/generated-sources/annotations/
cat target/generated/.../XXXConverterImpl.java
```

**方式二：IDEA 插件**
- 安装 "MapStruct Support" 插件
- 会自动提示映射关系

**方式三：Debug 模式**
```java
// 在转换方法打断点
@Mapping(target = "name", source = "userName")
CustomerDTO toDTO(Customer customer);  // ← 在这里打断点
```

---

## 九、完整示例

### 9.1 客户管理模块完整代码

#### Client 层 - DTO

```java
// {project}-client/src/main/java/com/{company}/{project}/client/customer/dto/CustomerDTO.java
package com.{company}.{project}.client.customer.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long id;
    private String name;
    private String email;
    private String phone;
    private Integer level;
}
```

---

#### Client 层 - Command

```java
// {project}-client/src/main/java/com/{company}/{project}/client/customer/dto/command/CreateCustomerCmd.java
package com.{company}.{project}.client.customer.dto.command;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCustomerCmd implements Command {
    
    @NotBlank(message = "客户名称不能为空")
    private String name;
    
    @Email(message = "邮箱格式不正确")
    private String email;
    
    private String phone;
    private Integer level;
}
```

---

#### Domain 层 - Entity

```java
// {project}-domain/src/main/java/com/{company}/{project}/domain/customer/model/Customer.java
package com.{company}.{project}.domain.customer.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.Objects;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer {
    
    private Long id;
    private String name;
    private String email;
    private String phone;
    private CustomerLevel level;
    
    // 领域行为
    public void activate() {
        // 激活逻辑
    }
    
    public void updateEmail(String newEmail) {
        if (!isValidEmail(newEmail)) {
            throw new BusinessException("邮箱格式不正确");
        }
        this.email = newEmail;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Customer customer = (Customer) o;
        return Objects.equals(id, customer.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
```

---

#### Infrastructure 层 - DO

```java
// {project}-infrastructure/src/main/java/com/{company}/{project}/infrastructure/customer/mapper/dataobject/CustomerDO.java
package com.{company}.{project}.infrastructure.customer.mapper.dataobject;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.baomidou.mybatisplus.annotation.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("customer")
public class CustomerDO {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String name;
    private String email;
    private String phone;
    private Integer level;
    
    @TableLogic
    private Integer deleted;
}
```

---

#### App 层 - Converter

```java
// {project}-app/src/main/java/com/{company}/{project}/app/customer/converter/CustomerConverter.java
package com.{company}.{project}.app.customer.converter;

import com.{company}.{project}.client.customer.dto.CustomerDTO;
import com.{company}.{project}.client.customer.dto.command.CreateCustomerCmd;
import com.{company}.{project}.domain.customer.model.Customer;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface CustomerConverter {
    
    CustomerConverter INSTANCE = Mappers.getFactory(CustomerConverter.class).getMapper();
    
    // DTO ↔ Entity
    CustomerDTO toDTO(Customer customer);
    Customer toEntity(CustomerDTO dto);
    
    // Command → Entity
    Customer toEntityFromCmd(CreateCustomerCmd cmd);
}
```

---

#### Infrastructure 层 - Converter

```java
// {project}-infrastructure/src/main/java/com/{company}/{project}/infrastructure/customer/converter/CustomerConverter.java
package com.{company}.{project}.infrastructure.customer.converter;

import com.{company}.{project}.domain.customer.model.Customer;
import com.{company}.{project}.infrastructure.customer.mapper.dataobject.CustomerDO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface CustomerConverter {
    
    CustomerConverter INSTANCE = Mappers.getFactory(CustomerConverter.class).getMapper();
    
    // Entity ↔ DO
    Customer toEntity(CustomerDO customerDO);
    CustomerDO toDO(Customer customer);
}
```

---

## 十、参考资料

### 📚 相关 Skill
- [ARCHITECTURE-SKILL](./ARCHITECTURE-SKILL) - COLA 架构规范
- [BUILD-SKILL](./BUILD-SKILL.md) - Maven 构建规范
- [CACHE-SKILL](./CACHE-SKILL.md) - JetCache 缓存设计
- [DEVELOP-SKILL](./DEVELOP-SKILL.md) - 开发流程规范

### 🔗 外部资源
- [Lombok 官方文档](https://projectlombok.org/)
- [MapStruct 官方文档](https://mapstruct.org/)
- [Lombok GitHub](https://github.com/projectlombok/lombok)
- [MapStruct GitHub](https://github.com/mapstruct/mapstruct)

### 🛠️ 工具推荐
- **Lombok IntelliJ Plugin** - IDEA Lombok 插件
- **MapStruct Support** - IDEA MapStruct 插件
- **Lombok Delombok** - 查看生成的代码工具

---

**最后提醒：工具是手段，不是目的。** 合理使用 Lombok 和 MapStruct，在简洁性和可读性之间找到平衡！🎯
