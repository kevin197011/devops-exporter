# DevOps Exporter - 域名和SSL证书过期监控工具

一个基于 Spring Boot 的模块化 Prometheus Exporter，用于监控域名 WHOIS 注册过期时间和 SSL 证书过期时间。

## 功能特性

- 🔍 **双重监控**: 同时监控域名 WHOIS 注册过期时间和 SSL 证书过期时间
- 📊 **Prometheus 集成**: 导出标准的 Prometheus 指标
- ⚙️ **配置驱动**: 通过 YAML 文件灵活配置监控参数
- 🏗️ **模块化设计**: 清晰的模块结构，域名和SSL监控完全分离
- 🚀 **异步处理**: 并发检查多个域名和证书，提高效率
- 🎯 **独立配置**: 域名和SSL监控可以独立启用/禁用
- 🔧 **REST API**: 提供手动触发和状态查询接口

## 快速开始

### 1. 配置监控

编辑 `src/main/resources/application.yml` 文件：

```yaml
# 域名 WHOIS 注册监控
domain:
  monitor:
    enabled: true
    check-interval: 3600  # 检查间隔（秒）
    warning-days: 30      # 过期预警天数
    domains:
      - example.com
      - your-domain.com
    connection-timeout: 5000
    read-timeout: 10000

# SSL 证书监控
ssl:
  monitor:
    enabled: true
    check-interval: 3600  # 检查间隔（秒）
    warning-days: 30      # 过期预警天数
    domains:
      - example.com
      - your-domain.com
      - api.example.com
    connection-timeout: 5000
    read-timeout: 10000
```

### 2. 运行应用

```bash
./gradlew bootRun
```

### 3. 查看指标

访问 Prometheus 指标端点：
```
http://localhost:8080/actuator/prometheus
```

## Prometheus 指标

| 指标名称 | 类型 | 描述 | 标签 |
|---------|------|------|------|
| `domain_expiration_days` | Gauge | 域名注册过期剩余天数 (-999=查询失败) | domain |
| `domain_status` | Gauge | 域名状态 (0=正常, 1=警告, 2=过期, 3=错误) | domain |
| `domain_expired` | Gauge | 域名是否过期 (0=未过期, 1=已过期) | domain |
| `domain_warning` | Gauge | 域名是否在警告期 (0=正常, 1=警告) | domain |
| `domain_last_checked_timestamp` | Gauge | 最后检查时间戳 | domain |

## API 端点

### 监控相关
- `GET /api/domain/status` - 获取所有域名状态
- `GET /api/domain/status/{domain}` - 获取指定域名状态
- `POST /api/domain/check` - 手动触发域名检查

### 系统相关
- `GET /actuator/prometheus` - Prometheus 指标
- `GET /actuator/metrics` - 应用指标

## 项目结构

```
src/main/java/io/github/devops/exporter/
├── config/                    # 配置相关
│   └── DomainMonitorProperties.java
├── controller/                # REST API 控制器
│   └── DomainMonitorController.java
├── domain/                    # 域名监控核心逻辑
│   ├── DomainInfo.java
│   └── DomainCheckService.java

├── metrics/                   # Prometheus 指标
│   └── DomainMetricsService.java
├── scheduler/                 # 定时任务
│   └── DomainMonitorScheduler.java
└── DevopsExporterApplication.java
```

## 配置说明

### 域名监控配置

```yaml
domain:
  monitor:
    enabled: true                    # 是否启用域名监控
    check-interval: 3600            # 检查间隔（秒）
    warning-days: 30                # 过期预警天数
    connection-timeout: 5000        # 连接超时（毫秒）
    read-timeout: 10000            # 读取超时（毫秒）
    domains:                        # 监控的域名列表
      - example.com
      - another-domain.com
      - third-domain.com
```

## Prometheus 告警规则示例

```yaml
groups:
  - name: domain_expiration
    rules:
      - alert: DomainRegistrationExpiringSoon
        expr: domain_expiration_days < 30 and domain_expiration_days >= 0
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "域名 {{ $labels.domain }} 注册即将过期"
          description: "域名 {{ $labels.domain }} 的注册将在 {{ $value }} 天后过期"

      - alert: DomainRegistrationExpired
        expr: domain_expired == 1
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "域名 {{ $labels.domain }} 注册已过期"
          description: "域名 {{ $labels.domain }} 的注册已经过期"

      - alert: DomainQueryFailed
        expr: domain_expiration_days == -999
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "域名 {{ $labels.domain }} WHOIS 查询失败"
          description: "无法获取域名 {{ $labels.domain }} 的注册信息，请检查域名是否有效或 WHOIS 服务是否可用"
```

## 扩展功能

该项目采用模块化设计，可以轻松扩展其他监控功能：

1. 在相应的包下创建新的服务类
2. 实现相应的指标收集逻辑
3. 在配置文件中添加相关配置
4. 创建对应的定时任务

## 开发和测试

```bash
# 编译
./gradlew build

# 运行测试
./gradlew test

# 生成 JAR 包
./gradlew bootJar
```

## Docker 部署

```dockerfile
FROM openjdk:17-jre-slim
COPY build/libs/devops-exporter-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

## 许可证

MIT License