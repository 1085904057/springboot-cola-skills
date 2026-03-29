# Harness Engineering Skills 索引

> 📚 **这是所有开发规范的导航页**，按层次组织。

---

## 🌳 Skill 树结构

```
📁 architecture/（架构规范）
│
├── 📄 ARCHITECTURE-SKILL（总纲）⭐ 根节点
│   │
│   ├── 📄 ADAPTER-SKILL（REST API 开发）✅ 已创建
│   ├── 📄 BUILD-SKILL（工程构建）✅ 已创建
│   ├── 📄 CACHE-SKILL（缓存设计）✅ 已创建
│   ├── 📄 CODEGEN-SKILL（代码生成）✅ 已创建
│   ├── 📄 DEVELOP-SKILL（开发流程）✅ 已创建
│   └── 📄 OpenFeign-SKILL（外部接口调用）✅ 已创建
│
└── 🔒 更多子 Skill 待创建...
│
📁 infrastructure/（基础设施使用规范）
│   ├── 🔒 MyBatis-SKILL.md（数据库访问） - 待创建
│   ├── 🔒 Redis-SKILL.md（缓存设计） - 待创建
│   └── 🔒 MQ-SKILL.md（消息队列） - 待创建
│
📁 best-practices/（最佳实践）
│   ├── 🔒 Exception-SKILL.md（异常处理） - 待创建
│   ├── 🔒 Extension-SKILL.md（扩展点设计） - 待创建
│   └── 🔒 Validation-SKILL.md（参数校验） - 待创建
│
📁 testing/（测试规范）
│   ├── 🔒 UnitTest-SKILL.md（单元测试） - 待创建
│   └── 🔒 IntegrationTest-SKILL.md（集成测试） - 待创建
```

---

## 📖 Skill 分类说明

### 🏗️ Architecture（架构规范）

从宏观角度介绍整体架构设计和分层原则。

| Skill 名称 | 描述 | 状态 |
|-----------|------|------|
| [ARCHITECTURE-SKILL](./architecture/ARCHITECTURE-SKILL) | COLA 架构总纲，介绍整体架构和分层原则 | ✅ 已完成 |
| [ADAPTER-SKILL](./architecture/ADAPTER-SKILL.md) | Adapter 层 REST API 开发规范，Swagger 文档、Validation 校验 | ✅ 已完成 ⭐ |
| [BUILD-SKILL](./architecture/BUILD-SKILL.md) | Maven 项目构建规范，模块划分、依赖管理、多环境配置 | ✅ 已完成 |
| [CACHE-SKILL](./architecture/CACHE-SKILL.md) | JetCache 缓存设计规范，声明式缓存、多级缓存、防穿透击穿 | ✅ 已完成 |
| [CODEGEN-SKILL](./architecture/CODEGEN-SKILL.md) | Lombok + MapStruct 使用规范，POJO 简化、对象转换、Converter 编写 | ✅ 已完成 ⭐ |
| [DEVELOP-SKILL](./architecture/DEVELOP-SKILL.md) | COLA 架构开发流程规范，覆盖新功能、迭代、Bug 修复 | ✅ 已完成 |
| [OpenFeign-SKILL](./architecture/RESTCALL-SKILL) | 使用 OpenFeign 进行外部 REST API 调用 | ✅ 已完成 |

### 🔧 Infrastructure（基础设施使用规范）

具体技术组件的使用规范。

| Skill 名称 | 描述 | 状态 |
|-----------|------|------|
| MyBatis-SKILL | MyBatis Plus 数据库访问规范 | 🔒 待创建 |
| Redis-SKILL | Redis 缓存设计和最佳实践 | 🔒 待创建 |
| MQ-SKILL | 消息队列使用规范 | 🔒 待创建 |

### 🎯 Best Practices（最佳实践）

通用开发最佳实践。

| Skill 名称 | 描述 | 状态 |
|-----------|------|------|
| Exception-SKILL | 统一异常处理和错误响应 | 🔒 待创建 |
| Extension-SKILL | COLA 扩展点设计和使用 | 🔒 待创建 |
| Validation-SKILL | 参数校验和注解使用 | 🔒 待创建 |

### 🧪 Testing（测试规范）

测试相关的规范和最佳实践。

| Skill 名称 | 描述 | 状态 |
|-----------|------|------|
| UnitTest-SKILL | 单元测试编写规范 | 🔒 待创建 |
| IntegrationTest-SKILL | 集成测试策略和实践 | 🔒 待创建 |

---

### 🚀 快速导航

### 新手入门路径

1. **第一步：** 阅读 [ARCHITECTURE-SKILL](./architecture/ARCHITECTURE-SKILL) - 了解整体架构
2. **第二步：** 查看 [BUILD-SKILL](./architecture/BUILD-SKILL.md) - 了解项目构建
3. **第三步：** 查看 [DEVELOP-SKILL](./architecture/DEVELOP-SKILL.md) - 了解开发流程
4. **第四步：** 查看项目 [README](../../README.md) - 了解项目结构
5. **第五步：** 参考 [QUICKSTART](../../QUICKSTART.md) - 快速开始开发
6. **第六步：** 根据需求查看对应的子 Skill：
   - 需要定义 REST API → [ADAPTER-SKILL](./architecture/ADAPTER-SKILL.md) ⭐
   - 需要使用缓存 → [CACHE-SKILL](./architecture/CACHE-SKILL.md)
   - 需要简化 POJO → [CODEGEN-SKILL](./architecture/CODEGEN-SKILL.md)
   - 需要调用外部 API → [OpenFeign-SKILL](./architecture/RESTCALL-SKILL)

### 常见场景导航

#### 场景一：需要调用外部 REST API
👉 查看 [OpenFeign-SKILL](./architecture/RESTCALL-SKILL)

#### 场景二：需要定义 REST API 接口
👉 查看 [ADAPTER-SKILL](./architecture/ADAPTER-SKILL.md)

#### 场景三：需要操作数据库
🔒 MyBatis-SKILL（待创建）

#### 场景四：需要使用缓存
👉 查看 [CACHE-SKILL](./architecture/CACHE-SKILL.md)

#### 场景四：需要简化 POJO 和对象转换
👉 查看 [CODEGEN-SKILL](./architecture/CODEGEN-SKILL.md)

#### 场景五：需要发送消息
🔒 MQ-SKILL（待创建）

#### 场景五：需要统一异常处理
🔒 Exception-SKILL（待创建）

---

## 📝 Skill 模板

每个 Skill 都包含以下标准结构：

```markdown
# {Skill 名称}

## 一、元数据信息
- Name: {skill-name}
- Description: {简短描述}

## 二、Overview（什么时候用）
- 适用场景
- 不适用场景
- 在架构中的位置

## 三、怎么用（完整开发流程）
1. 添加依赖
2. 配置文件
3. 代码示例
4. 单元测试

## 四、最佳实践总结
- ✅ 必须遵守的规范
- ✅ 推荐做法
- ❌ 禁止做法

## 五、常见问题 FAQ

## 六、参考资料
```

---

## 🔄 更新日志

| 日期 | 新增/更新内容 | 作者 |
|------|-------------|------|
| 2026-03-28 | 创建 ADAPTER-SKILL REST API 开发规范 | Harness Team |
| 2026-03-28 | 创建 ARCHITECTURE-SKILL 总纲 | Harness Team |
| 2026-03-28 | 创建 BUILD-SKILL 工程构建规范 | Harness Team |
| 2026-03-28 | 创建 CACHE-SKILL 缓存设计规范 | Harness Team |
| 2026-03-28 | 创建 CODEGEN-SKILL 代码生成规范（Lombok + MapStruct） | Harness Team |
| 2026-03-28 | 创建 DEVELOP-SKILL 开发流程规范 | Harness Team |
| 2026-03-28 | 创建 OpenFeign-SKILL | Harness Team |
| 2026-03-28 | 创建 Skills 索引页 | Harness Team |

---

## 💡 如何贡献

欢迎提交新的 Skill 或改进现有 Skill！

1. 在对应目录下创建 `{SkillName}-SKILL.md` 文件
2. 遵循标准的 Skill 模板格式
3. 在本索引文件中添加链接
4. 提交 PR 审核

---

## 📞 联系方式

如有疑问，请联系 Harness Engineering Team。
