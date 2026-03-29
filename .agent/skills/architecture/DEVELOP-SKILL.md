# COLA 架构开发流程规范

> 📚 **这是 COLA 架构 Skill 体系的流程分册**，指导如何开展具体的开发任务。
> 
> **关联文档：**
> - [ARCHITECTURE-SKILL](./ARCHITECTURE-SKILL) - 架构规范（静态结构）
> - [OpenFeign-SKILL](./OpenFeign-SKILL.md) - 外部接口调用规范

---

## 一、元数据信息

### Name
`cola-development-workflow`

### Description
基于 COLA 架构的业务功能开发流程规范。覆盖新功能开发、已有功能迭代、Bug 修复等常见场景，提供标准化的开发步骤和质量检查点。

### 适用角色
- 💻 后端开发工程师
- 🔍 代码 Reviewer
- 📋 技术负责人/架构师

---

## 二、Overview（什么时候用）

### 适用场景

✅ **当你需要：**
- 开发全新的业务功能（如新增客户管理模块）
- 迭代已有的业务功能（如给客户模块增加新字段）
- 修复跨层的 Bug（如从 Controller 到数据库的全链路问题）
- 进行代码 Review（检查是否符合 COLA 规范）
- 新人入职培训（快速了解开发流程）

❌ **不适用场景：**
- 简单的配置修改（如改个文案、调个参数）
- 纯技术升级（如 Spring Boot 版本升级）
- 性能优化（不涉及业务逻辑变更）

### 在开发流程中的位置

```
需求分析 → 技术方案设计 → 【DEVELOP-SKILL】→ 编码实现 → 测试 → 上线
                              ↑
                    本 Skill 指导编码实现阶段
```

---

## 三、任务类型与流程映射

### 3.1 任务分类

根据业务需求的特点，将开发任务分为三类：

#### Type A: 🆕 全新功能开发

**特征：**
- 从零开始创建新的业务模块
- 涉及所有层次（Client → Adapter → App → Domain → Infra）
- 工作量较大（通常 > 5 人天）

**示例：**
- 新增"供应商管理"模块
- 新增"促销活动"功能
- 新增"积分系统"

**流程：** 详见 [第四章：新功能开发标准流程](#四新功能开发标准流程)

---

#### Type B: 🔄 已有功能迭代

**特征：**
- 在现有模块基础上增加/修改功能
- 可能涉及多个层次，但不用全部新建
- 工作量中等（通常 1-5 人天）

**示例：**
- 给客户模块增加"会员等级"字段
- 订单列表增加"导出 Excel"功能
- 支付接口增加"分期付款"选项

**流程：** 详见 [第五章：已有功能迭代标准流程](#五已有功能迭代标准流程)

---

#### Type C: 🐛 Bug 修复

**特征：**
- 修复现有功能的缺陷
- 通常只涉及局部层次
- 工作量较小（通常 < 1 人天）

**示例：**
- 修复客户名称无法更新的 Bug
- 修复订单金额计算错误
- 修复接口超时问题

**流程：** 详见 [第六章：Bug 修复标准流程](#六 bug 修复标准流程)

---

### 3.2 快速决策树

```
接到需求
    ↓
是全新业务模块吗？
    ├─ Yes → Type A: 新功能开发流程
    └─ No
        ↓
    是增加/修改功能吗？
        ├─ Yes → Type B: 已有功能迭代流程
        └─ No
            ↓
        是修复缺陷吗？
            ├─ Yes → Type C: Bug 修复流程
            └─ No → 其他类型（配置修改、技术升级等）
```

---

## 四、新功能开发标准流程（Type A）

### 4.1 流程图

```
需求分析 → 领域建模 → 定义 DTO → 定义接口 → 实现 Domain → 实现 Infra → 
实现 App → 实现 Adapter → 实现 Client → 单元测试 → 集成测试 → Code Review
```

### 4.2 详细步骤

#### Step 1: 需求分析与拆解（0.5-1 天）

**输入：** 产品需求文档（PRD）

**动作：**
1. 理解业务场景和用户价值
2. 识别涉及的领域对象（Entity）
3. 识别业务流程（Command/Query）
4. 识别外部依赖（第三方服务、其他模块）

**输出：**
- ✅ 领域模型草图
- ✅ 功能清单（Feature List）
- ✅ 技术难点和风险点

**示例（客户管理模块）：**
```
领域对象：Customer, CustomerAddress, CustomerLevel
业务流程：
  - Command: CreateCustomer, UpdateCustomer, DeleteCustomer
  - Query: GetCustomerById, ListCustomers
外部依赖：
  - 短信服务（发送验证码）
  - 用户中心（同步用户信息）
```

**检查点：**
- [ ] 领域对象是否识别完整？
- [ ] Command/Query 是否覆盖所有场景？
- [ ] 外部依赖是否明确？

---

#### Step 2: 领域建模（0.5-1 天）

**参考：** [ARCHITECTURE-SKILL §5.4](#) - Domain 模块命名规范

**动作：**
1. 创建领域实体（Entity）
2. 定义值对象（Value Object）
3. 定义聚合根（Aggregate Root）
4. 定义领域服务（Domain Service）

**目录结构：**
```
harness-domain/src/main/java/com/harness/engineering/domain/customer/
├── model/
│   ├── Customer.java              # 客户实体
│   ├── CustomerId.java            # 客户 ID（值对象）
│   ├── CustomerAddress.java       # 客户地址（值对象）
│   └── aggregate/
│       └── CustomerAggregate.java # 客户聚合根
├── ability/
│   └── CustomerDomainService.java # 客户领域服务
└── gateway/
    └── CustomerGateway.java       # 客户网关接口
```

**代码示例：**
```java
// domain/customer/model/Customer.java
@Data
public class Customer {
    private CustomerId id;
    private String name;
    private String email;
    private CustomerLevel level;  // 枚举类型
    
    // 领域行为
    public void activate() {
        // 激活逻辑
    }
    
    public void updateEmail(String newEmail) {
        // 业务规则校验
        if (!isValidEmail(newEmail)) {
            throw new BusinessException("邮箱格式不正确");
        }
        this.email = newEmail;
    }
}
```

**检查点：**
- [ ] 实体是否包含业务行为（不是贫血模型）？
- [ ] 值对象是否不可变？
- [ ] 领域服务是否必要（避免滥用）？
- [ ] 网关接口是否定义清晰？

---

#### Step 3: 定义 DTO 和接口（0.5 天）

**参考：** [ARCHITECTURE-SKILL §5.1](#) - Client 模块命名规范

**动作：**
1. 定义 Command/Query 对象
2. 定义 Response DTO
3. 定义 Service 接口

**目录结构：**
```
harness-client/src/main/java/com/harness/engineering/client/customer/
├── api/
│   └── CustomerServiceI.java      # 客户服务接口
└── dto/
    ├── command/
    │   ├── CreateCustomerCmd.java
    │   ├── UpdateCustomerCmd.java
    │   └── DeleteCustomerCmd.java
    ├── query/
    │   ├── GetCustomerQry.java
    │   └── ListCustomersQry.java
    └── response/
        └── CustomerDTO.java
```

**代码示例：**
```java
// client/customer/api/CustomerServiceI.java
public interface CustomerServiceI {
    Response<CustomerDTO> createCustomer(CreateCustomerCmd cmd);
    Response<CustomerDTO> updateCustomer(UpdateCustomerCmd cmd);
    Response<Void> deleteCustomer(DeleteCustomerCmd cmd);
    Response<CustomerDTO> getCustomer(GetCustomerQry qry);
    MultiResponse<CustomerDTO> listCustomers(ListCustomersQry qry);
}

// client/customer/dto/command/CreateCustomerCmd.java
@Data
public class CreateCustomerCmd implements Command {
    @NotBlank(message = "客户名称不能为空")
    private String name;
    
    @Email(message = "邮箱格式不正确")
    private String email;
    
    private String phone;
    private CustomerLevel level;
}
```

**检查点：**
- [ ] Command/Query 是否分离？
- [ ] DTO 是否实现 Serializable？
- [ ] 是否有必要的校验注解？
- [ ] 接口命名是否规范（ServiceI）？

---

#### Step 4: 实现基础设施层（1-2 天）

**参考：** [ARCHITECTURE-SKILL §5.5](#) - Infrastructure 模块命名规范

**动作：**
1. 实现 Domain 层定义的网关接口
2. 创建 Mapper 接口和 DO 对象
3. 编写 MyBatis XML 映射文件
4. （可选）创建 Feign Client 调用外部服务

**目录结构：**
```
harness-infrastructure/src/main/java/com/harness/engineering/infrastructure/customer/
├── gatewayimpl/
│   └── CustomerGatewayImpl.java   # 网关实现
├── mapper/
│   ├── CustomerMapper.java        # Mapper 接口
│   └── dataobject/
│       └── CustomerDO.java        # 数据对象
└── config/
    └── CustomerConfig.java        # 配置类（可选）
```

**代码示例：**
```java
// infrastructure/customer/gatewayimpl/CustomerGatewayImpl.java
@Component
public class CustomerGatewayImpl implements CustomerGateway {
    
    @Resource
    private CustomerMapper customerMapper;
    
    @Override
    public Customer getById(CustomerId id) {
        CustomerDO customerDO = customerMapper.selectById(id.getValue());
        return CustomerConverter.INSTANCE.toEntity(customerDO);
    }
    
    @Override
    public void create(Customer customer) {
        CustomerDO customerDO = CustomerConverter.INSTANCE.toDO(customer);
        customerMapper.insert(customerDO);
    }
}

// infrastructure/customer/mapper/dataobject/CustomerDO.java
@Data
@TableName("customer")
public class CustomerDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String email;
    private String phone;
    private Integer level;  // 枚举转整数
    private Date createTime;
    private Date updateTime;
}

// infrastructure/customer/mapper/CustomerMapper.java
@Mapper
public interface CustomerMapper {
    CustomerDO selectById(Long id);
    List<CustomerDO> findByName(@Param("name") String name);
    int insert(CustomerDO customerDO);
    int update(CustomerDO customerDO);
    int deleteById(Long id);
}
```

**检查点：**
- [ ] 网关实现是否遵循 Domain 层接口？
- [ ] DO 与 Entity 的转换是否正确？
- [ ] MyBatis XML 是否编写完整？
- [ ] 数据库索引是否创建？

---

#### Step 5: 实现应用层（1-2 天）

**参考：** [ARCHITECTURE-SKILL §5.3](#) - App 模块命名规范

**动作：**
1. 实现 Command 执行器
2. 实现 Query 执行器
3. 实现 Service 接口

**目录结构：**
```
harness-app/src/main/java/com/harness/engineering/app/customer/
├── executor/
│   ├── CreateCustomerCmdExe.java
│   ├── UpdateCustomerCmdExe.java
│   ├── DeleteCustomerCmdExe.java
│   ├── GetCustomerQryExe.java
│   └── ListCustomersQryExe.java
└── service/
    └── CustomerServiceImpl.java
```

**代码示例：**
```java
// app/customer/executor/CreateCustomerCmdExe.java
@Component
public class CreateCustomerCmdExe {
    
    @Resource
    private CustomerGateway customerGateway;
    
    @Resource
    private SmsGateway smsGateway;  // 外部服务
    
    public Response<CustomerDTO> execute(CreateCustomerCmd cmd) {
        // 1. Command 转 Entity
        Customer customer = new Customer();
        customer.setName(cmd.getName());
        customer.setEmail(cmd.getEmail());
        
        // 2. 执行业务规则校验
        validateCustomer(customer);
        
        // 3. 保存客户
        customerGateway.create(customer);
        
        // 4. 发送欢迎短信（外部服务）
        smsGateway.sendWelcomeSms(customer.getPhone());
        
        // 5. 返回结果
        return Response.success(convertToDTO(customer));
    }
    
    private void validateCustomer(Customer customer) {
        // 业务规则校验
        if (customerGateway.existsByEmail(customer.getEmail())) {
            throw new BusinessException("邮箱已被使用");
        }
    }
}

// app/customer/service/CustomerServiceImpl.java
@Service
@CatchAndLog
public class CustomerServiceImpl implements CustomerServiceI {
    
    @Resource
    private CreateCustomerCmdExe createCustomerCmdExe;
    
    @Resource
    private GetCustomerQryExe getCustomerQryExe;
    
    @Override
    public Response<CustomerDTO> createCustomer(CreateCustomerCmd cmd) {
        return createCustomerCmdExe.execute(cmd);
    }
    
    @Override
    public Response<CustomerDTO> getCustomer(GetCustomerQry qry) {
        return getCustomerQryExe.execute(qry);
    }
}
```

**检查点：**
- [ ] Command/Query 是否分离处理？
- [ ] 是否使用了 `@CatchAndLog` 注解？
- [ ] 业务规则是否在 Executor 中校验？
- [ ] 是否避免了事务过大？

---

#### Step 6: 实现适配层（0.5-1 天）

**参考：** [ARCHITECTURE-SKILL §5.2](#) - Adapter 模块命名规范

**动作：**
1. 创建 Controller
2. 定义 Request/VO 对象
3. 参数校验
4. 异常处理

**目录结构：**
```
harness-adapter/src/main/java/com/harness/engineering/adapter/customer/web/
├── CustomerController.java
└── vo/
    └── CustomerVO.java
```

**代码示例：**
```java
// adapter/customer/web/CustomerController.java
@RestController
@RequestMapping("/api/customers")
public class CustomerController {
    
    @Autowired
    private CustomerServiceI customerService;
    
    /**
     * 创建客户
     * POST /api/customers
     */
    @PostMapping
    public Response<CustomerVO> createCustomer(
            @Validated @RequestBody CreateCustomerRequest request) {
        
        // 1. Request 转 Command
        CreateCustomerCmd cmd = new CreateCustomerCmd();
        BeanUtils.copyProperties(request, cmd);
        
        // 2. 调用 App 层
        Response<CustomerDTO> response = customerService.createCustomer(cmd);
        
        // 3. DTO 转 VO
        CustomerVO vo = convertToVO(response.getData());
        
        return Response.success(vo);
    }
    
    /**
     * 获取客户详情
     * GET /api/customers/{id}
     */
    @GetMapping("/{id}")
    public Response<CustomerVO> getCustomer(@PathVariable Long id) {
        GetCustomerQry qry = new GetCustomerQry();
        qry.setId(id);
        
        Response<CustomerDTO> response = customerService.getCustomer(qry);
        CustomerVO vo = convertToVO(response.getData());
        
        return Response.success(vo);
    }
}
```

**检查点：**
- [ ] Controller 是否足够薄（只做参数转换）？
- [ ] 是否有统一的异常处理？
- [ ] 接口文档（Swagger）是否自动生成？
- [ ] 日志记录是否完整？

---

#### Step 7: 单元测试（1-2 天）

**动作：**
1. Domain 层单元测试（重点）
2. App 层单元测试
3. Infra 层集成测试
4. Adapter 层接口测试

**测试覆盖：**
- [ ] Domain Entity 的领域行为
- [ ] Command/Query 执行器逻辑
- [ ] Gateway 实现（Mock 数据库）
- [ ] Controller 接口（MockMvc）

**示例：**
```java
// test/domain/customer/model/CustomerTest.java
class CustomerTest {
    
    @Test
    void testActivate() {
        Customer customer = new Customer();
        customer.activate();
        assertEquals(CustomerStatus.ACTIVE, customer.getStatus());
    }
    
    @Test
    void testUpdateEmail_InvalidFormat() {
        Customer customer = new Customer();
        assertThrows(BusinessException.class, () -> {
            customer.updateEmail("invalid-email");
        });
    }
}

// test/app/customer/executor/CreateCustomerCmdExeTest.java
@ExtendWith(MockitoExtension.class)
class CreateCustomerCmdExeTest {
    
    @Mock
    private CustomerGateway customerGateway;
    
    @InjectMocks
    private CreateCustomerCmdExe exe;
    
    @Test
    void testExecute_EmailExists() {
        when(customerGateway.existsByEmail(any())).thenReturn(true);
        
        CreateCustomerCmd cmd = new CreateCustomerCmd();
        cmd.setEmail("test@example.com");
        
        assertThrows(BusinessException.class, () -> {
            exe.execute(cmd);
        });
    }
}
```

---

#### Step 8: 集成测试与 Code Review（0.5-1 天）

**集成测试：**
- [ ] 端到端流程测试
- [ ] 数据库事务测试
- [ ] 外部服务 Mock 测试
- [ ] 异常场景测试

**Code Review 清单：**
- [ ] 是否符合 COLA 分层规范？
- [ ] 命名是否规范？
- [ ] 注释是否完整？
- [ ] 单元测试覆盖率是否达标（>80%）？
- [ ] 是否有性能隐患？
- [ ] 是否有安全隐患？

---

#### Step 9: 部署上线

**前置检查：**
- [ ] SQL 脚本是否准备？
- [ ] 配置文件是否更新？
- [ ] 接口文档是否发布？
- [ ] 监控告警是否配置？

**上线后验证：**
- [ ] 核心接口是否正常？
- [ ] 日志是否正常输出？
- [ ] 监控指标是否正常？

---

## 五、已有功能迭代标准流程（Type B）

### 5.1 流程图

```
需求分析 → 影响范围评估 → 修改 Domain（如需要）→ 修改 Infra（如需要）→ 
修改 App（如需要）→ 修改 Adapter（如需要）→ 回归测试 → Code Review
```

### 5.2 与 Type A 的差异

| 步骤 | Type A（新功能） | Type B（迭代） | 说明 |
|------|-----------------|---------------|------|
| 领域建模 | ✅ 必须 | ⚠️ 按需 | 只有领域模型变化时才需要 |
| 定义 DTO | ✅ 必须 | ⚠️ 按需 | 只有接口变化时才需要 |
| 实现各层 | ✅ 全部 | ⚠️ 按需 | 哪层变改哪层 |
| 单元测试 | ✅ 全覆盖 | ⚠️ 增量 + 回归 | 新增功能单测 + 已有功能回归 |
| 工作量 | 5-10 天 | 1-5 天 | 迭代更快 |

### 5.3 典型场景

#### 场景一：增加字段

**示例：** 给客户增加"会员等级"字段

**影响范围：**
```
Domain: Customer 实体增加 level 字段 ✅
Infra: CustomerDO 增加 level 字段 ✅
     : CustomerMapper XML 增加 level 列 ✅
App:    无变化（自动映射）⚠️
Adapter: 无变化（自动映射）⚠️
Client: DTO 增加 level 字段 ✅
```

**步骤：**
1. 修改 `domain/customer/model/Customer.java`
2. 修改 `infrastructure/customer/mapper/dataobject/CustomerDO.java`
3. 修改 `infrastructure/customer/mapper/CustomerMapper.xml`
4. 修改 `client/customer/dto/CustomerDTO.java`
5. 数据库加字段：`ALTER TABLE customer ADD COLUMN level INT`
6. 回归测试（确保不影响现有功能）

---

#### 场景二：增加接口

**示例：** 给客户模块增加"批量导入"接口

**影响范围：**
```
Client: 增加 BatchImportCustomersCmd ✅
      : CustomerServiceI 增加 batchImport 方法 ✅
App:    增加 BatchImportCustomersCmdExe ✅
      : CustomerServiceImpl 实现 batchImport ✅
Adapter: 增加 CustomerController.batchImport() ✅
Domain: 无变化 ⚠️
Infra:  无变化 ⚠️
```

**步骤：**
1. 定义 Command: `BatchImportCustomersCmd`
2. 扩展接口：`CustomerServiceI.batchImport()`
3. 实现 Executor: `BatchImportCustomersCmdExe`
4. 实现 Controller: `CustomerController.batchImport()`
5. 单元测试（新增功能）
6. 回归测试（确保不影响其他接口）

---

## 六、Bug 修复标准流程（Type C）

### 6.1 流程图

```
Bug 分析 → 定位问题层次 → 修复代码 → 单元测试 → 回归测试 → Code Review
```

### 6.2 典型场景

#### 场景一：Domain 层逻辑错误

**示例：** 客户邮箱校验逻辑有误，导致某些合法邮箱被拒绝

**步骤：**
1. 定位问题：`domain/customer/model/Customer.java` 的 `updateEmail()` 方法
2. 修复正则表达式
3. 增加单元测试（覆盖边界情况）
4. 回归测试（确保不影响其他功能）

---

#### 场景二：Infra 层 SQL 错误

**示例：** 客户列表查询缺少条件，导致查询出已删除的客户

**步骤：**
1. 定位问题：`infrastructure/customer/mapper/CustomerMapper.xml`
2. 修复 SQL：增加 `AND deleted = 0` 条件
3. 增加集成测试
4. 回归测试

---

#### 场景三：App 层事务问题

**示例：** 创建客户失败后，短信仍然发送（事务未回滚）

**步骤：**
1. 定位问题：`app/customer/executor/CreateCustomerCmdExe`
2. 调整顺序：先保存客户，再发送短信
3. 增加 `@Transactional` 注解
4. 增加异常场景测试

---

## 七、质量检查点（Checklist）

### 7.1 通用检查点（所有类型任务）

#### 代码规范 ✅
- [ ] 包名是否符合 [ARCHITECTURE-SKILL §5](#)？
- [ ] 类名是否符合命名规范？
- [ ] 方法命名是否清晰？
- [ ] 代码缩进、格式化是否一致？

#### 注释文档 ✅
- [ ] 类是否有 JavaDoc？
- [ ] 公共方法是否有 JavaDoc？
- [ ] 复杂逻辑是否有注释？
- [ ] Swagger 接口文档是否生成？

#### 单元测试 ✅
- [ ] 核心业务逻辑是否有单测？
- [ ] 单元测试覆盖率是否 >80%？
- [ ] 异常场景是否测试？
- [ ] 边界条件是否测试？

#### 异常处理 ✅
- [ ] 是否使用统一异常类型（BusinessException）？
- [ ] 异常消息是否友好？
- [ ] 是否有全局异常处理？
- [ ] 日志是否记录完整堆栈？

#### 安全性 ✅
- [ ] 参数是否校验（@Validated）？
- [ ] SQL 注入风险？
- [ ] XSS 攻击风险？
- [ ] 敏感信息是否加密？

---

### 7.2 Type A 专用检查点（新功能）

- [ ] 领域模型是否合理？
- [ ] Command/Query 是否分离？
- [ ] 各层实现是否完整？
- [ ] 数据库表结构是否合理？
- [ ] 索引是否创建？
- [ ] 接口文档是否完整？

---

### 7.3 Type B 专用检查点（迭代）

- [ ] 影响范围是否评估完整？
- [ ] 是否需要数据库变更？
- [ ] 是否需要数据迁移？
- [ ] 回归测试是否充分？
- [ ] 向后兼容性？

---

### 7.4 Type C 专用检查点（Bug 修复）

- [ ] 根本原因是否找到？
- [ ] 是否只是治标不治本？
- [ ] 是否有类似 Bug？
- [ ] 修复是否引入新问题？
- [ ] 监控告警是否能提前发现？

---

## 八、常见问题 FAQ

### Q1: 如何判断一个需求属于哪种类型？

**A:** 看是否涉及新的业务领域：
- 全新业务领域 → Type A
- 已有业务领域增加功能 → Type B
- 修复缺陷 → Type C

**示例：**
- "新增供应商管理" → Type A（全新领域）
- "客户管理增加会员等级" → Type B（已有领域）
- "客户邮箱校验 Bug" → Type C（缺陷）

---

### Q2: 如果需求介于 Type A 和 Type B 之间怎么办？

**A:** 按 Type A 处理，因为：
- Type A 流程更完整，不容易遗漏
- 前期多花时间设计，后期少走弯路

---

### Q3: 小团队（3-5 人）是否需要严格遵守这些流程？

**A:** 建议遵守，但可以简化：
- 领域建模可以简化（但要思考）
- 文档可以减少（但要有）
- Code Review 必须坚持

---

### Q4: 如何保证团队成员都遵守这些流程？

**A:** 
1. **培训**：新人入职时学习本 Skill
2. **模板**：提供代码模板和 Checklist
3. **Review**：Code Review 时对照检查
4. **工具**：使用 IDE 插件自动检查规范

---

### Q5: 流程这么复杂，会不会影响开发效率？

**A:** 
- **短期**：可能稍微慢一点（20%）
- **长期**：显著提升效率（50%+）
  - 代码易维护
  - Bug 率降低
  - 新人上手快
  - 减少返工

---

## 九、最佳实践总结

### ✅ 必须遵守的原则

1. **领域驱动**：先建模，再编码
2. **分层清晰**：不跨层调用
3. **测试先行**：先写测试，再写实现
4. **代码审查**：必须经过 Review 才能合并
5. **文档同步**：代码变了，文档也要变

### 🎯 高质量代码特征

- 清晰的层次结构
- 统一的命名规范
- 完善的注释文档
- 充足的单元测试
- 合理的异常处理
- 完整的日志记录

### 📈 持续改进

- 每个 Sprint 结束后复盘
- 收集流程中的痛点
- 持续优化流程和模板
- 定期组织技术分享

---

## 十、参考资料

### 📚 相关 Skill
- [ARCHITECTURE-SKILL](./ARCHITECTURE-SKILL) - COLA 架构规范
- [OpenFeign-SKILL](./OpenFeign-SKILL.md) - 外部接口调用规范

### 🔗 外部资源
- [COLA GitHub](https://github.com/alibaba/COLA)
- [阿里巴巴开发手册](https://github.com/alibaba/p3c)
- [Clean Architecture](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)

### 🛠️ 工具推荐
- IDEA Live Templates（代码模板）
- CheckStyle（代码规范检查）
- SonarQube（代码质量分析）
- Jacoco（测试覆盖率）

---

## 附录：模板下载

### 代码模板
- [Entity 模板](#)
- [Command/Query 模板](#)
- [Gateway 模板](#)
- [Mapper 模板](#)
- [Executor 模板](#)
- [Controller 模板](#)

### 文档模板
- [领域建模模板](#)
- [技术方案模板](#)
- [Code Review Checklist](#)

---

**最后提醒：流程是手段，不是目的。** 要根据实际情况灵活运用，不要为了流程而流程！🎯
