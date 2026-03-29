# Maven 项目构建规范

> 📚 **这是 COLA 架构 Skill 体系的工程构建分册**，指导如何用 Maven 组织和管理项目。
> 
> **关联文档：**
> - [ARCHITECTURE-SKILL](./ARCHITECTURE-SKILL) - 架构规范（方法论）
> - [DEVELOP-SKILL](./DEVELOP-SKILL.md) - 开发流程规范

---

## 一、元数据信息

### Name
`maven-project-build`

### Description
基于 COLA 架构的 Maven 项目构建规范。覆盖父子模块划分、依赖管理、版本控制、构建插件配置等工程化实践，将架构设计落地为可执行的项目结构。

### 适用角色
- 💻 后端开发工程师
- 🔧 技术负责人/架构师
- 📦 DevOps 工程师

---

## 二、Overview（什么时候用）

### 适用场景

✅ **当你需要：**
- 从零开始搭建 COLA 架构项目
- 重构现有项目的 Maven 结构
- 统一管理多模块依赖版本
- 优化构建速度和产物
- 配置多环境部署

❌ **不适用场景：**
- 单模块简单项目
- 非 Maven 项目（如 Gradle）
- 纯前端项目

### 在工程中的位置

```
需求分析 → 技术方案 → 【ARCHITECTURE-SKILL】→ 【BUILD-SKILL】→ 【DEVELOP-SKILL】→ 编码实现
                                    ↑              ↑              ↑
                                架构设计       工程构建       开发流程
```

---

## 三、核心原则

### 3.1 模块划分原则

#### 原则一：物理隔离

**目标：** 不同层的代码放在不同的 Maven 模块中

```
harness-engineering/
├── harness-client         # Client 层
├── harness-adapter        # Adapter 层
├── harness-app            # App 层
├── harness-domain         # Domain 层
├── harness-infrastructure # Infra 层
└── harness-start          # 启动模块
```

**好处：**
- ✅ 编译时检查依赖方向
- ✅ 清晰的模块边界
- ✅ 便于独立打包和发布

---

#### 原则二：依赖单向

**目标：** 严格遵循 COLA 依赖方向

```xml
<!-- ✅ 正确：上层依赖下层 -->
<dependency>
    <groupId>com.harness.engineering</groupId>
    <artifactId>harness-client</artifactId>
</dependency>

<!-- ❌ 错误：禁止反向依赖 -->
<!-- Domain 层不能依赖 App 层 -->
```

**依赖关系图：**
```
start → adapter → client → app → domain ← infrastructure
                                              ↑
                                          实现网关
```

---

#### 原则三：版本集中

**目标：** 所有依赖版本在父 POM 中统一管理

```xml
<!-- 父 pom.xml -->
<properties>
    <spring-boot.version>3.2.0</spring-boot.version>
    <mybatis-plus.version>3.5.5</mybatis-plus.version>
</properties>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-dependencies</artifactId>
            <version>${spring-boot.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

**好处：**
- ✅ 避免版本冲突
- ✅ 升级方便（只需改一处）
- ✅ 子模块无需声明版本

---

### 3.2 目录结构规范

#### 标准 Maven 目录

```
harness-{module}/
├── src/
│   ├── main/
│   │   ├── java/                    # Java 源代码
│   │   │   └── com/harness/engineering/{module}/
│   │   │       └── ...              # 按 ARCHITECTURE-SKILL §5 分包
│   │   └── resources/               # 资源文件
│   │       ├── mapper/              # MyBatis XML
│   │       ├── application.yml      # 配置文件
│   │       └── templates/           # 模板文件
│   └── test/
│       ├── java/                    # 测试源代码
│       └── resources/               # 测试资源
├── pom.xml                          # 模块 POM
└── README.md                        # 模块说明（可选）
```

---

## 四、父子模块设计

### 4.1 根 POM（Parent）

**文件：** `pom.xml`

**职责：**
- 定义模块列表（modules）
- 管理所有依赖版本（dependencyManagement）
- 配置构建插件（build plugins）
- 定义通用属性（properties）

**完整示例：**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    
    <modelVersion>4.0.0</modelVersion>
    
    <!-- 基础坐标 -->
    <groupId>com.harness.engineering</groupId>
    <artifactId>harness-engineering</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>
    <name>Harness Engineering Platform</name>
    <description>基于 COLA 架构的业务系统工程</description>
    
    <!-- 子模块列表 -->
    <modules>
        <module>harness-client</module>
        <module>harness-adapter</module>
        <module>harness-app</module>
        <module>harness-domain</module>
        <module>harness-infrastructure</module>
        <module>harness-start</module>
    </modules>
    
    <!-- 属性定义 -->
    <properties>
        <java.version>21</java.version>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        
        <!-- 框架版本 -->
        <spring-boot.version>3.2.0</spring-boot.version>
        <spring-cloud.version>2023.0.0</spring-cloud.version>
        
        <!-- 持久层版本 -->
        <mybatis-plus.version>3.5.5</mybatis-plus.version>
        <mysql.version>8.0.33</mysql.version>
        <druid.version>1.2.20</druid.version>
        
        <!-- 工具类版本 -->
        <lombok.version>1.18.30</lombok.version>
        <mapstruct.version>1.5.5.Final</mapstruct.version>
        <hutool.version>5.8.24</hutool.version>
        <fastjson2.version>2.0.42</fastjson2.version>
        <guava.version>32.1.3-jre</guava.version>
    </properties>
    
    <!-- 依赖管理 -->
    <dependencyManagement>
        <dependencies>
            <!-- Spring Boot BOM -->
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring-boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            
            <!-- Spring Cloud BOM -->
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            
            <!-- 内部模块依赖（版本管理） -->
            <dependency>
                <groupId>com.harness.engineering</groupId>
                <artifactId>harness-client</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.harness.engineering</groupId>
                <artifactId>harness-adapter</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.harness.engineering</groupId>
                <artifactId>harness-app</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.harness.engineering</groupId>
                <artifactId>harness-domain</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.harness.engineering</groupId>
                <artifactId>harness-infrastructure</artifactId>
                <version>${project.version}</version>
            </dependency>
            
            <!-- MyBatis Plus -->
            <dependency>
                <groupId>com.baomidou</groupId>
                <artifactId>mybatis-plus-boot-starter</artifactId>
                <version>${mybatis-plus.version}</version>
            </dependency>
            
            <!-- MySQL -->
            <dependency>
                <groupId>com.mysql</groupId>
                <artifactId>mysql-connector-j</artifactId>
                <version>${mysql.version}</version>
            </dependency>
            
            <!-- Druid 连接池 -->
            <dependency>
                <groupId>com.alibaba</groupId>
                <artifactId>druid-spring-boot-3-starter</artifactId>
                <version>${druid.version}</version>
            </dependency>
            
            <!-- Lombok -->
            <dependency>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
                <version>${lombok.version}</version>
            </dependency>
            
            <!-- MapStruct -->
            <dependency>
                <groupId>org.mapstruct</groupId>
                <artifactId>mapstruct</artifactId>
                <version>${mapstruct.version}</version>
            </dependency>
            <dependency>
                <groupId>org.mapstruct</groupId>
                <artifactId>mapstruct-processor</artifactId>
                <version>${mapstruct.version}</version>
            </dependency>
            
            <!-- Hutool -->
            <dependency>
                <groupId>cn.hutool</groupId>
                <artifactId>hutool-all</artifactId>
                <version>${hutool.version}</version>
            </dependency>
            
            <!-- FastJSON2 -->
            <dependency>
                <groupId>com.alibaba.fastjson2</groupId>
                <artifactId>fastjson2</artifactId>
                <version>${fastjson2.version}</version>
            </dependency>
            
            <!-- Guava -->
            <dependency>
                <groupId>com.google.guava</groupId>
                <artifactId>guava</artifactId>
                <version>${guava.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>
    
    <!-- 构建配置 -->
    <build>
        <plugins>
            <!-- 编译插件 -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
                <configuration>
                    <source>${java.version}</source>
                    <target>${java.version}</target>
                    <encoding>${project.build.sourceEncoding}</encoding>
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
                </configuration>
            </plugin>
            
            <!-- 源码插件 -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-source-plugin</artifactId>
                <version>3.3.0</version>
                <executions>
                    <execution>
                        <id>attach-sources</id>
                        <goals>
                            <goal>jar-no-fork</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
    
    <!-- Profile 配置（多环境） -->
    <profiles>
        <profile>
            <id>dev</id>
            <properties>
                <active.profile>dev</active.profile>
            </properties>
            <activation>
                <activeByDefault>true</activeByDefault>
            </activation>
        </profile>
        <profile>
            <id>test</id>
            <properties>
                <active.profile>test</active.profile>
            </properties>
        </profile>
        <profile>
            <id>prod</id>
            <properties>
                <active.profile>prod</active.profile>
            </properties>
        </profile>
    </profiles>
</project>
```

---

### 4.2 子模块 POM

#### 4.2.1 Client 模块

**文件：** `harness-client/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>com.harness.engineering</groupId>
        <artifactId>harness-engineering</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    
    <artifactId>harness-client</artifactId>
    <packaging>jar</packaging>
    <name>Harness Client SDK</name>
    <description>客户端接口层 - 服务接口和 DTO 定义</description>
    
    <dependencies>
        <!-- Jakarta Validation API（参数校验） -->
        <dependency>
            <groupId>jakarta.validation</groupId>
            <artifactId>jakarta.validation-api</artifactId>
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

**关键点：**
- ✅ Client 层是纯接口，不依赖 Spring
- ✅ 只依赖 Jakarta Validation API（用于参数校验注解）
- ✅ Lombok 使用 `provided` 作用域

---

#### 4.2.2 Domain 模块

**文件：** `harness-domain/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>com.harness.engineering</groupId>
        <artifactId>harness-engineering</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    
    <artifactId>harness-domain</artifactId>
    <packaging>jar</packaging>
    <name>Harness Domain Layer</name>
    <description>领域层 - 核心业务逻辑</description>
    
    <dependencies>
        <!-- 领域层应该是纯 POJO，不依赖 Spring 等框架 -->
        
        <!-- Lombok（仅编译时需要） -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>
        
        <!-- Hutool（工具类，可选） -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
        </dependency>
    </dependencies>
</project>
```

**关键点：**
- ✅ 领域层是纯 POJO，不依赖 Spring
- ✅ 可以使用 Hutool 等纯 Java 工具库
- ✅ Lombok 使用 `provided` 作用域

---

#### 4.2.3 Infrastructure 模块

**文件：** `harness-infrastructure/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>com.harness.engineering</groupId>
        <artifactId>harness-engineering</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    
    <artifactId>harness-infrastructure</artifactId>
    <packaging>jar</packaging>
    <name>Harness Infrastructure Layer</name>
    <description>基础设施层 - 技术实现</description>
    
    <dependencies>
        <!-- 依赖 Domain 层（实现网关接口） -->
        <dependency>
            <groupId>com.harness.engineering</groupId>
            <artifactId>harness-domain</artifactId>
        </dependency>
        
        <!-- Spring Boot Starter -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>
        
        <!-- MyBatis Plus -->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-boot-starter</artifactId>
        </dependency>
        
        <!-- MySQL -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>
        
        <!-- Druid 连接池 -->
        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>druid-spring-boot-3-starter</artifactId>
        </dependency>
        
        <!-- OpenFeign（外部服务调用） -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-openfeign</artifactId>
        </dependency>
        
        <!-- Feign Apache HttpClient -->
        <dependency>
            <groupId>io.github.openfeign</groupId>
            <artifactId>feign-httpclient</artifactId>
        </dependency>
        
        <!-- Apache HttpClient5 -->
        <dependency>
            <groupId>org.apache.httpcomponents.client5</groupId>
            <artifactId>httpclient5</artifactId>
        </dependency>
        
        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>
        
        <!-- MapStruct -->
        <dependency>
            <groupId>org.mapstruct</groupId>
            <artifactId>mapstruct</artifactId>
        </dependency>
        <dependency>
            <groupId>org.mapstruct</groupId>
            <artifactId>mapstruct-processor</artifactId>
            <scope>provided</scope>
        </dependency>
    </dependencies>
</project>
```

**关键点：**
- ✅ 必须依赖 Domain 层（实现网关接口）
- ✅ 包含所有技术组件（MyBatis、Feign 等）
- ✅ MapStruct 处理器使用 `provided` 作用域

---

#### 4.2.4 App 模块

**文件：** `harness-app/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>com.harness.engineering</groupId>
        <artifactId>harness-engineering</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    
    <artifactId>harness-app</artifactId>
    <packaging>jar</packaging>
    <name>Harness Application Layer</name>
    <description>应用层 - 业务协调</description>
    
    <dependencies>
        <!-- 依赖 Client 层（接口定义） -->
        <dependency>
            <groupId>com.harness.engineering</groupId>
            <artifactId>harness-client</artifactId>
        </dependency>
        
        <!-- 依赖 Domain 层（领域模型） -->
        <dependency>
            <groupId>com.harness.engineering</groupId>
            <artifactId>harness-domain</artifactId>
        </dependency>
        
        <!-- Spring Boot Starter -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>
        
        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>
        
        <!-- MapStruct -->
        <dependency>
            <groupId>org.mapstruct</groupId>
            <artifactId>mapstruct</artifactId>
        </dependency>
        <dependency>
            <groupId>org.mapstruct</groupId>
            <artifactId>mapstruct-processor</artifactId>
            <scope>provided</scope>
        </dependency>
    </dependencies>
</project>
```

**关键点：**
- ✅ 同时依赖 Client 层和 Domain 层
- ✅ 不直接依赖 Infra 层（通过网关解耦）

---

#### 4.2.5 Adapter 模块

**文件：** `harness-adapter/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>com.harness.engineering</groupId>
        <artifactId>harness-engineering</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    
    <artifactId>harness-adapter</artifactId>
    <packaging>jar</packaging>
    <name>Harness Adapter Layer</name>
    <description>适配层 - 请求接入</description>
    
    <dependencies>
        <!-- 依赖 Client 层（调用服务） -->
        <dependency>
            <groupId>com.harness.engineering</groupId>
            <artifactId>harness-client</artifactId>
        </dependency>
        
        <!-- Spring Web MVC -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        
        <!-- Validation（参数校验） -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        
        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>
    </dependencies>
</project>
```

**关键点：**
- ✅ 依赖 Client 层（调用服务接口）
- ✅ 包含 Spring Web 和 Validation

---

#### 4.2.6 Start 模块

**文件：** `harness-start/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>com.harness.engineering</groupId>
        <artifactId>harness-engineering</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    
    <artifactId>harness-start</artifactId>
    <packaging>jar</packaging>
    <name>Harness Start Application</name>
    <description>启动模块 - 应用入口</description>
    
    <dependencies>
        <!-- 依赖 Adapter 层 -->
        <dependency>
            <groupId>com.harness.engineering</groupId>
            <artifactId>harness-adapter</artifactId>
        </dependency>
        
        <!-- 依赖 App 层 -->
        <dependency>
            <groupId>com.harness.engineering</groupId>
            <artifactId>harness-app</artifactId>
        </dependency>
        
        <!-- 依赖 Infrastructure 层 -->
        <dependency>
            <groupId>com.harness.engineering</groupId>
            <artifactId>harness-infrastructure</artifactId>
        </dependency>
        
        <!-- Spring Boot Starter Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        
        <!-- Spring Boot Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
    
    <build>
        <plugins>
            <!-- Spring Boot Maven Plugin（打包可执行 JAR） -->
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <version>${spring-boot.version}</version>
                <executions>
                    <execution>
                        <goals>
                            <goal>repackage</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

**关键点：**
- ✅ 依赖所有其他模块（Adapter, App, Infra）
- ✅ 使用 Spring Boot Maven Plugin 打包可执行 JAR
- ✅ 包含测试依赖

---

## 五、依赖管理规范

### 5.1 依赖版本控制

#### ✅ 正确做法

```xml
<!-- 父 POM 统一管理版本 -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-boot-starter</artifactId>
            <version>3.5.5</version>  <!-- 版本在这里定义 -->
        </dependency>
    </dependencies>
</dependencyManagement>

<!-- 子 POM 直接使用，不写版本 -->
<dependencies>
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-boot-starter</artifactId>
        <!-- 不需要版本号 -->
    </dependency>
</dependencies>
```

#### ❌ 错误做法

```xml
<!-- 每个子模块都写版本号 -->
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-boot-starter</artifactId>
    <version>3.5.5</version>  <!-- ❌ 不应该在这里写 -->
</dependency>
```

---

### 5.2 依赖作用域

| 作用域 | 说明 | 使用场景 |
|--------|------|---------|
| `compile` | 编译、测试、运行都需要 | 默认，如 Spring Boot Starter |
| `provided` | 编译时需要，运行时由容器提供 | Lombok、MapStruct Processor |
| `runtime` | 运行时才需要 | MySQL 驱动、JDBC 实现 |
| `test` | 仅测试需要 | JUnit、Mockito |

**示例：**
```xml
<!-- Lombok：编译时需要，运行时不需要 -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <scope>provided</scope>
</dependency>

<!-- MySQL 驱动：运行时才需要 -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- JUnit：仅测试需要 -->
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
```

---

### 5.3 依赖排除

**场景：** 排除传递依赖（避免冲突）

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
    <exclusions>
        <!-- 排除旧版本 HttpClient -->
        <exclusion>
            <groupId>org.apache.httpcomponents</groupId>
            <artifactId>httpclient</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

---

## 六、构建配置

### 6.1 编译插件配置

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.11.0</version>
    <configuration>
        <source>21</source>
        <target>21</target>
        <encoding>UTF-8</encoding>
        
        <!-- 注解处理器路径 -->
        <annotationProcessorPaths>
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
                <version>1.18.30</version>
            </path>
            <path>
                <groupId>org.mapstruct</groupId>
                <artifactId>mapstruct-processor</artifactId>
                <version>1.5.5.Final</version>
            </path>
        </annotationProcessorPaths>
    </configuration>
</plugin>
```

---

### 6.2 源码插件配置

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-source-plugin</artifactId>
    <version>3.3.0</version>
    <executions>
        <execution>
            <id>attach-sources</id>
            <goals>
                <goal>jar-no-fork</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

---

### 6.3 Spring Boot 打包插件

```xml
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    <version>3.2.0</version>
    <executions>
        <execution>
            <goals>
                <goal>repackage</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

---

## 七、多环境配置

### 7.1 Profile 定义

```xml
<profiles>
    <profile>
        <id>dev</id>
        <properties>
            <active.profile>dev</active.profile>
        </properties>
        <activation>
            <activeByDefault>true</activeByDefault>
        </activation>
    </profile>
    <profile>
        <id>test</id>
        <properties>
            <active.profile>test</active.profile>
        </properties>
    </profile>
    <profile>
        <id>prod</id>
        <properties>
            <active.profile>prod</active.profile>
        </properties>
    </profile>
</profiles>
```

---

### 7.2 激活 Profile

**方式一：命令行激活**
```bash
# 开发环境
mvn clean package

# 测试环境
mvn clean package -Ptest

# 生产环境
mvn clean package -Pprod
```

**方式二：application.yml 引用**
```yaml
spring:
  profiles:
    active: @active.profile@  # Maven 占位符
```

---

## 八、常用命令

### 8.1 构建命令

```bash
# 清理 + 编译
mvn clean compile

# 清理 + 打包（跳过测试）
mvn clean package -DskipTests

# 清理 + 安装到本地仓库
mvn clean install -DskipTests

# 只编译指定模块
mvn clean package -pl harness-start -am -DskipTests
```

### 8.2 依赖命令

```bash
# 查看依赖树
mvn dependency:tree

# 查看指定模块的依赖树
mvn dependency:tree -pl harness-start

# 分析依赖冲突
mvn dependency:tree -Dverbose -Dincludes=commons-lang

# 清理未使用的依赖
mvn dependency:analyze
```

### 8.3 运行命令

```bash
# 运行 Spring Boot 应用
mvn spring-boot:run -pl harness-start

# 运行打包后的 JAR
java -jar harness-start/target/harness-start-1.0.0-SNAPSHOT.jar
```

---

## 九、常见问题 FAQ

### Q1: 模块间循环依赖怎么办？

**A:** 违反 COLA 架构原则，必须重构！

**错误示例：**
```
app 依赖 domain
domain 依赖 app  ← ❌ 循环依赖
```

**解决方案：**
1. 提取公共接口到 Client 层
2. 通过网关接口解耦
3. 调整依赖方向

---

### Q2: 如何升级某个依赖的版本？

**A:** 只需修改父 POM 的 `<properties>` 或 `<dependencyManagement>`

```xml
<!-- 父 POM -->
<properties>
    <mybatis-plus.version>3.5.5</mybatis-plus.version>  <!-- 改这里 -->
</properties>
```

所有子模块会自动使用新版本。

---

### Q3: 构建速度慢怎么办？

**A:** 优化建议：

1. **增量编译**
```bash
mvn clean install -T 1C  # 多线程编译
```

2. **跳过不必要的步骤**
```bash
mvn package -DskipTests -Dmaven.javadoc.skip=true
```

3. **使用 Maven Daemon**
```bash
mvnd package  # 比 mvn 快 3-5 倍
```

---

### Q4: 如何排查依赖冲突？

**A:** 使用依赖树命令

```bash
# 查看完整依赖树
mvn dependency:tree -Dverbose

# 查找特定类的来源
mvn dependency:tree -Dincludes=*:* -Dverbose | grep "StringUtils"
```

**解决方案：** 使用 `<exclusions>` 排除冲突版本

---

### Q5: 多个环境如何配置？

**A:** 使用 Maven Profile + Spring Profile

```yaml
# application-dev.yml
spring:
  datasource:
    url: jdbc:mysql://dev-db:3306/harness

# application-prod.yml
spring:
  datasource:
    url: jdbc:mysql://prod-db:3306/harness
```

```bash
# 打包时激活对应 Profile
mvn package -Pprod
```

---

## 十、最佳实践总结

### ✅ 必须遵守的规则

1. **父 POM 管理所有依赖版本** - 子模块不写版本号
2. **严格按 COLA 分层依赖** - 禁止跨层、反向依赖
3. **Domain 层保持纯 POJO** - 不依赖 Spring 等框架
4. **Start 模块负责打包** - 其他模块只负责业务
5. **多环境 Profile 隔离** - dev/test/prod 独立配置

### 🎯 高质量项目特征

- 清晰的模块划分
- 统一的依赖管理
- 快速的构建速度
- 灵活的环境配置
- 完善的插件配置

### 📈 持续优化

- 定期升级依赖版本（安全补丁）
- 清理未使用的依赖
- 优化构建插件配置
- 使用 Maven Wrapper（统一 Maven 版本）

---

## 十一、参考资料

### 📚 相关 Skill
- [ARCHITECTURE-SKILL](./ARCHITECTURE-SKILL) - COLA 架构规范
- [DEVELOP-SKILL](./DEVELOP-SKILL.md) - 开发流程规范
- [OpenFeign-SKILL](./OpenFeign-SKILL.md) - 外部接口调用规范

### 🔗 外部资源
- [Maven 官方文档](https://maven.apache.org/guides/)
- [Spring Boot Maven Plugin](https://docs.spring.io/spring-boot/docs/current/maven-plugin/reference/html/)
- [COLA GitHub](https://github.com/alibaba/COLA)

### 🛠️ 工具推荐
- [Maven Wrapper](https://github.com/takari/maven-wrapper) - 统一 Maven 版本
- [Maven Daemon (mvnd)](https://github.com/apache/maven-mvnd) - 加速构建
- [Dependency-Check](https://jeremylong.github.io/DependencyCheck/) - 安全漏洞扫描

---

**最后提醒：Maven 是工具，不是目的。** 合理的模块划分和依赖管理才能让项目可持续发展！🎯
