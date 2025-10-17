# DevOps Exporter - 域名和SSL证书过期监控工具

一个基于 Spring Boot 的模块化 Prometheus Exporter，用于监控域名 WHOIS 注册过期时间和 SSL 证书过期时间。

## 功能特性

- 🔍 **四重监控**: 同时监控域名 WHOIS 注册过期时间、SSL 证书过期时间、端口连通性和 HTTP 服务可用性
- 📊 **Prometheus 集成**: 导出标准的 Prometheus 指标
- ⚙️ **配置驱动**: 通过 YAML 文件灵活配置监控参数
- 🏗️ **模块化设计**: 清晰的模块结构，域名、SSL、端口和HTTP监控完全分离
- 🚀 **异步处理**: 并发检查多个域名、证书、端口和HTTP服务，提高效率
- 🎯 **独立配置**: 域名、SSL、端口和HTTP监控可以独立启用/禁用
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

# 端口连通性监控
port:
  monitor:
    enabled: true
    check-interval: 300   # 检查间隔（秒）
    ports:
      - 1.1.1.1:80       # IP:端口格式
      - 8.8.8.8:53
      - example.com:443   # 域名:端口格式
      - your-api.com:8080
    connection-timeout: 5000

# HTTP 服务可用性监控
http:
  monitor:
    enabled: true
    check-interval: 300   # 检查间隔（秒）
    urls:
      - https://baidu.com
      - http://1.1.1.1:8998
      - https://your-api.com/health
      - http://localhost:8080/actuator/health
    connection-timeout: 10000
    read-timeout: 15000
    expected-status-codes:
      - 200
      - 201
      - 202
      - 204
    follow-redirects: true
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

### 域名 WHOIS 监控
- `POST /api/domain/check` - 触发域名 WHOIS 检查
- `GET /api/domain/status` - 获取所有域名状态
- `GET /api/domain/status/{domain}` - 获取指定域名状态

### SSL 证书监控
- `POST /api/ssl/check` - 触发 SSL 证书检查
- `GET /api/ssl/status` - 获取所有 SSL 证书状态
- `GET /api/ssl/status/{domain}` - 获取指定域名 SSL 证书状态

### 端口连通性监控
- `POST /api/port/check` - 触发端口连通性检查
- `GET /api/port/status` - 获取所有端口状态
- `GET /api/port/status/{target}` - 获取指定端口状态（注意：URL中的冒号需要编码为%3A）

### HTTP 服务监控
- `POST /api/http/check` - 触发 HTTP 服务可用性检查
- `GET /api/http/status` - 获取所有 HTTP 服务状态
- `GET /api/http/status/{urlHash}` - 获取指定 URL 的状态（使用 URL 的 hashCode）

### 统一监控管理
- `POST /api/monitor/check/all` - 触发所有类型的检查
- `GET /api/monitor/status/summary` - 获取监控状态汇总
- `GET /api/monitor/health` - 获取服务健康状态

### 系统相关
- `GET /actuator/prometheus` - Prometheus 指标
- `GET /actuator/metrics` - 应用指标
- `GET /actuator/health` - Spring Boot 健康检查

## 项目结构

```
src/main/java/io/github/devops/exporter/
├── config/                    # 配置相关
│   ├── DomainMonitorProperties.java    # 域名WHOIS监控配置
│   ├── SslMonitorProperties.java       # SSL证书监控配置
│   ├── PortMonitorProperties.java      # 端口监控配置
│   └── HttpMonitorProperties.java      # HTTP监控配置
├── controller/                # 统一控制器
│   └── MonitorController.java          # 统一监控管理API
├── domain/                    # 域名WHOIS监控模块
│   ├── DomainInfo.java
│   ├── DomainCheckService.java
│   ├── DomainMetricsService.java
│   ├── DomainMonitorScheduler.java
│   └── DomainController.java
├── ssl/                       # SSL证书监控模块
│   ├── SslCertificateInfo.java
│   ├── SslCheckService.java
│   ├── SslMetricsService.java
│   ├── SslMonitorScheduler.java
│   └── SslController.java
├── port/                      # 端口监控模块
│   ├── PortInfo.java
│   ├── PortCheckService.java
│   ├── PortMetricsService.java
│   ├── PortMonitorScheduler.java
│   └── PortController.java
├── http/                      # HTTP监控模块
│   ├── HttpInfo.java
│   ├── HttpCheckService.java
│   ├── HttpMetricsService.java
│   ├── HttpMonitorScheduler.java
│   └── HttpController.java

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