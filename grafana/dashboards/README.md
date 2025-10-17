# DevOps Exporter Grafana Dashboards

这个目录包含了 DevOps Exporter 的所有 Grafana Dashboard 配置文件。

## 📊 Dashboard 列表

### 1. **Overview Dashboard** (`overview-dashboard.json`)
- **UID**: `devops-overview`
- **功能**: 提供所有监控模块的总览
- **包含内容**:
  - 各模块的总数统计
  - 异常状态汇总
  - 最紧急的过期项目 Top 5
  - 快速导航链接到其他专门的 Dashboard

### 2. **Domain Registration Monitor** (`domain-monitor-dashboard.json`)
- **UID**: `domain-monitor`
- **功能**: 域名 WHOIS 注册监控
- **包含内容**:
  - 域名注册状态表格
  - 过期天数趋势图
  - 过期/警告域名统计
  - 状态分布饼图

### 3. **SSL Certificate Monitor** (`ssl-monitor-dashboard.json`)
- **UID**: `ssl-monitor`
- **功能**: SSL 证书过期监控
- **包含内容**:
  - SSL 证书状态表格
  - 证书过期天数趋势图
  - 过期/警告证书统计
  - 证书状态分布饼图

### 4. **Port Connectivity Monitor** (`port-monitor-dashboard.json`)
- **UID**: `port-monitor`
- **功能**: 端口连通性监控
- **包含内容**:
  - 端口连通性状态表格
  - 响应时间趋势图
  - 开放/关闭端口统计
  - 端口状态分布饼图

### 5. **HTTP Service Monitor** (`http-monitor-dashboard.json`)
- **UID**: `http-monitor`
- **功能**: HTTP 服务可用性监控
- **包含内容**:
  - HTTP 服务状态表格
  - 响应时间趋势图
  - 可用/不可用服务统计
  - 服务可用性分布饼图
  - HTTP 状态码趋势图

## 🚀 使用方法

### 自动导入 (推荐)
如果使用提供的 Docker Compose 配置，这些 Dashboard 会自动导入到 Grafana 中。

### 手动导入
1. 登录 Grafana (默认: http://localhost:3000, admin/admin123)
2. 点击左侧菜单的 "+" → "Import"
3. 选择 "Upload JSON file" 或直接粘贴 JSON 内容
4. 点击 "Load" 然后 "Import"

## 📈 Dashboard 特性

### 🎨 **视觉设计**
- 深色主题，适合监控环境
- 颜色编码：绿色(正常)、黄色(警告)、红色(异常)、紫色(错误)
- 响应式布局，适配不同屏幕尺寸

### ⏱️ **实时更新**
- 自动刷新间隔：30秒
- 默认时间范围：最近1小时
- 支持自定义时间范围

### 🔍 **交互功能**
- 表格支持排序和搜索
- 图表支持缩放和时间范围选择
- 工具提示显示详细信息
- 图例可点击隐藏/显示数据系列

### 🏷️ **标签和过滤**
- 每个 Dashboard 都有相应的标签
- 支持按域名、主机等维度过滤
- 模板变量支持动态过滤 (可扩展)

## 🎯 **监控指标说明**

### Domain Registration
- `domain_expiration_days`: 域名注册过期剩余天数 (-999=查询失败)
- `domain_status`: 域名状态 (0=正常, 1=警告, 2=过期, 3=错误)
- `domain_expired`: 域名是否过期 (0=未过期, 1=已过期)
- `domain_warning`: 域名是否在警告期 (0=正常, 1=警告)

### SSL Certificate
- `ssl_certificate_expiration_days`: SSL证书过期剩余天数 (-999=查询失败)
- `ssl_certificate_status`: SSL证书状态 (0=正常, 1=警告, 2=过期, 3=错误)
- `ssl_certificate_expired`: SSL证书是否过期 (0=未过期, 1=已过期)
- `ssl_certificate_warning`: SSL证书是否在警告期 (0=正常, 1=警告)

### Port Connectivity
- `port_open`: 端口是否开放 (1=开放, 0=关闭)
- `port_status`: 端口状态 (1=开放, 0=关闭, -1=错误)
- `port_response_time_ms`: 端口连接响应时间（毫秒）

### HTTP Service
- `http_available`: HTTP服务是否可用 (1=可用, 0=不可用)
- `http_status_code`: HTTP响应状态码
- `http_response_time_ms`: HTTP响应时间（毫秒）
- `http_status`: HTTP服务状态 (1=可用, 0=不可用, -1=错误)

## 🔧 **自定义配置**

### 修改阈值
在 Dashboard JSON 文件中找到 `thresholds` 部分，修改相应的阈值：

```json
"thresholds": {
  "steps": [
    {"color": "green", "value": null},
    {"color": "yellow", "value": 30},    // 警告阈值
    {"color": "red", "value": 7}         // 危险阈值
  ]
}
```

### 添加告警
1. 在 Dashboard 中选择面板
2. 点击面板标题 → "Edit"
3. 切换到 "Alert" 标签
4. 配置告警条件和通知渠道

### 添加模板变量
1. 在 Dashboard 设置中选择 "Variables"
2. 添加新变量，例如按域名过滤
3. 在查询中使用变量：`domain_expiration_days{domain=~"$domain"}`

## 📱 **移动端支持**

所有 Dashboard 都支持移动端访问，会自动调整布局以适应小屏幕设备。

## 🔗 **相关链接**

- [Grafana 官方文档](https://grafana.com/docs/)
- [Prometheus 查询语法](https://prometheus.io/docs/prometheus/latest/querying/)
- [DevOps Exporter API 文档](../../README.md#api-端点)