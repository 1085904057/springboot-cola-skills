# 快速开始指南

## 📝 项目构建完成

恭喜！基于 COLA 架构的业务系统框架已经搭建完成！

---

## 🎯 已完成的工作

### 1. ✅ Maven 多模块项目结构
- 父 POM 统一管理依赖版本
- 6 个标准 COLA 模块已创建并编译成功

### 2. ✅ 示例代码
每个模块都包含完整的示例代码：
- **Client**: `DemoServiceI`, `Response`, `Command`, `Query`
- **Adapter**: `DemoController`
- **App**: `DemoCmdExe`, `DemoServiceImpl`
- **Domain**: `DemoEntity`, `DemoGateway`
- **Infra**: `DemoGatewayImpl`
- **Start**: `HarnessStartApplication`, `application.yml`

### 3. ✅ 配置文件
- Spring Boot 配置（application.yml）
- MyBatis Plus 配置
- Druid 连接池配置
- 日志配置

### 4. ✅ 文档
- `README.md` - 项目完整说明
- `BUILD_SUCCESS.md` - 构建成功报告
- `QUICKSTART.md` - 本文档
- `.agent/skills/architecture/COLA-SKILL.md` - COLA 架构详细规范

---

## 🚀 立即运行

### 步骤 1: 启动应用

```bash
# 进入项目目录
cd D:\Code\Mine\harness-engineering\harness-start

# 方式一：使用 Maven 运行
mvn spring-boot:run

# 方式二：打包后运行
mvn clean package
java -jar target/harness-start-1.0-SNAPSHOT.jar
```

### 步骤 2: 测试接口

应用启动后，访问示例 API：

```bash
curl http://localhost:8080/api/demo/welcome
```

或在浏览器中打开：
```
http://localhost:8080/api/demo/welcome
```

预期响应：
```json
{
  "code": "SUCCESS",
  "message": "操作成功",
  "data": "Hello from COLA Architecture!",
  "success": true
}
```

---

## 📦 数据库配置（可选）

如果需要使用数据库，请执行以下步骤：

### 1. 创建数据库

```sql
CREATE DATABASE harness_engineering DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 2. 修改配置文件

编辑 `harness-start/src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/harness_engineering?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: 你的密码
```

### 3. 重启应用

---

## 💡 开发新功能的步骤

假设你要开发"用户管理"功能：

### Step 1: 定义 DTO (harness-client)

```java
// harness-client/dto/command/UserAddCmd.java
@Data
public class UserAddCmd extends Command {
    private String name;
    private String email;
}

// harness-client/dto/UserDTO.java
@Data
public class UserDTO implements Serializable {
    private Long id;
    private String name;
    private String email;
}
```

### Step 2: 定义接口 (harness-client)

```java
// harness-client/api/UserServiceI.java
public interface UserServiceI {
    Response<UserDTO> addUser(UserAddCmd cmd);
}
```

### Step 3: 实现领域模型 (harness-domain)

```java
// harness-domain/model/User.java
@Data
public class User {
    private Long id;
    private String name;
    private String email;
    
    public void activate() {
        // 领域行为
    }
}

// harness-domain/gateway/UserGateway.java
public interface UserGateway {
    User getById(Long id);
    void save(User user);
}
```

### Step 4: 实现 App 层 (harness-app)

```java
// harness-app/executor/UserAddCmdExe.java
@Component
public class UserAddCmdExe {
    @Resource
    private UserGateway userGateway;
    
    public Response<UserDTO> execute(UserAddCmd cmd) {
        User user = new User();
        user.setName(cmd.getName());
        user.setEmail(cmd.getEmail());
        
        userGateway.save(user);
        
        return Response.success(convertToDTO(user));
    }
}

// harness-app/service/UserServiceImpl.java
@Service
public class UserServiceImpl implements UserServiceI {
    @Resource
    private UserAddCmdExe userAddCmdExe;
    
    @Override
    public Response<UserDTO> addUser(UserAddCmd cmd) {
        return userAddCmdExe.execute(cmd);
    }
}
```

### Step 5: 实现网关 (harness-infrastructure)

```java
// harness-infrastructure/gatewayimpl/UserGatewayImpl.java
@Component
public class UserGatewayImpl implements UserGateway {
    @Resource
    private UserMapper userMapper;
    
    @Override
    public User getById(Long id) {
        UserDO userDO = userMapper.selectById(id);
        return convertToEntity(userDO);
    }
    
    @Override
    public void save(User user) {
        UserDO userDO = convertToDO(user);
        if (user.getId() == null) {
            userMapper.insert(userDO);
        } else {
            userMapper.update(userDO);
        }
    }
}
```

### Step 6: 实现 Controller (harness-adapter)

```java
// harness-adapter/web/UserController.java
@RestController
@RequestMapping("/api/users")
public class UserController {
    @Autowired
    private UserServiceI userService;
    
    @PostMapping
    public Response<UserDTO> createUser(@RequestBody UserAddCmd cmd) {
        return userService.addUser(cmd);
    }
}
```

---

## 🔧 常用 Maven 命令

```bash
# 清理并重新构建
mvn clean install

# 跳过测试构建
mvn clean install -DskipTests

# 只编译特定模块
mvn clean install -pl harness-app -am

# 查看依赖树
mvn dependency:tree

# 运行应用
cd harness-start
mvn spring-boot:run
```

---

## 📚 学习资源

### 项目文档
- [README.md](./README.md) - 项目介绍和架构说明
- [BUILD_SUCCESS.md](./BUILD_SUCCESS.md) - 构建报告和下一步工作
- [COLA-SKILL.md](./.agent/skills/architecture/ARCHITECTURE-SKILL) - COLA 架构详细规范

### 外部资源
- [COLA GitHub](https://github.com/alibaba/COLA)
- [COLA 架构博客](https://cloud.tencent.com/developer/article/1971122)

---

## ❓ 常见问题

### Q: 为什么要用 COLA 架构？
A: COLA 提供了清晰的分层架构，让代码更易维护，延缓代码腐烂速度。

### Q: 如何添加新的依赖？
A: 在父 `pom.xml` 的 `<dependencyManagement>` 中统一管理的版本，子模块不需要指定版本号。

### Q: Domain 层为什么不能有 Spring 注解？
A: Domain 层应该是纯 POJO，不依赖任何框架，这样才能保持领域模型的纯粹性。

### Q: 什么时候需要网关？
A: 当 Domain 层需要访问外部系统（数据库、第三方服务等）时，通过网关接口解耦。

---

## 🎉 开始编码吧！

现在你已经准备好了！按照 COLA 架构规范，开始开发你的业务功能吧！

记住：
- ✅ 先按业务分包，再按功能分包
- ✅ 遵循依赖方向：Adapter → Client → App → Domain ← Infra
- ✅ Domain 层通过网关与基础设施解耦
- ✅ 保持代码整洁，定期重构

**Happy Coding!** 🚀
