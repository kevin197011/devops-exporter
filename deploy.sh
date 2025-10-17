#!/bin/bash

# DevOps Exporter 部署脚本

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查 Docker 和 Docker Compose
check_dependencies() {
    log_info "检查依赖..."
    
    if ! command -v docker &> /dev/null; then
        log_error "Docker 未安装，请先安装 Docker"
        exit 1
    fi
    
    if ! command -v docker-compose &> /dev/null; then
        log_error "Docker Compose 未安装，请先安装 Docker Compose"
        exit 1
    fi
    
    log_info "依赖检查通过"
}

# 创建必要的目录
create_directories() {
    log_info "创建必要的目录..."
    
    mkdir -p logs
    mkdir -p config
    mkdir -p prometheus
    mkdir -p grafana/provisioning/dashboards
    mkdir -p grafana/provisioning/datasources
    mkdir -p grafana/dashboards
    
    log_info "目录创建完成"
}

# 构建镜像
build_image() {
    log_info "构建 Docker 镜像..."
    
    docker build -t devops-exporter:latest .
    
    if [ $? -eq 0 ]; then
        log_info "镜像构建成功"
    else
        log_error "镜像构建失败"
        exit 1
    fi
}

# 部署服务
deploy_services() {
    local env=${1:-dev}
    
    log_info "部署服务 (环境: $env)..."
    
    if [ "$env" = "prod" ]; then
        docker-compose -f docker-compose.prod.yml up -d
    else
        docker-compose up -d
    fi
    
    if [ $? -eq 0 ]; then
        log_info "服务部署成功"
    else
        log_error "服务部署失败"
        exit 1
    fi
}

# 检查服务状态
check_services() {
    log_info "检查服务状态..."
    
    sleep 10
    
    # 检查 devops-exporter
    if curl -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
        log_info "DevOps Exporter 服务正常"
    else
        log_warn "DevOps Exporter 服务可能未就绪，请稍后检查"
    fi
    
    # 检查 Prometheus (如果启用)
    if docker-compose ps | grep -q prometheus; then
        if curl -f http://localhost:9090/-/healthy > /dev/null 2>&1; then
            log_info "Prometheus 服务正常"
        else
            log_warn "Prometheus 服务可能未就绪"
        fi
    fi
    
    # 检查 Grafana (如果启用)
    if docker-compose ps | grep -q grafana; then
        if curl -f http://localhost:3000/api/health > /dev/null 2>&1; then
            log_info "Grafana 服务正常"
        else
            log_warn "Grafana 服务可能未就绪"
        fi
    fi
}

# 显示访问信息
show_access_info() {
    log_info "服务访问信息:"
    echo "  DevOps Exporter: http://localhost:8080"
    echo "  Prometheus 指标: http://localhost:8080/actuator/prometheus"
    echo "  API 文档: http://localhost:8080/api/domain/status"
    
    if docker-compose ps | grep -q prometheus; then
        echo "  Prometheus: http://localhost:9090"
    fi
    
    if docker-compose ps | grep -q grafana; then
        echo "  Grafana: http://localhost:3000 (admin/admin123)"
    fi
}

# 主函数
main() {
    local command=${1:-deploy}
    local env=${2:-dev}
    
    case $command in
        "build")
            check_dependencies
            create_directories
            build_image
            ;;
        "deploy")
            check_dependencies
            create_directories
            build_image
            deploy_services $env
            check_services
            show_access_info
            ;;
        "start")
            deploy_services $env
            check_services
            show_access_info
            ;;
        "stop")
            log_info "停止服务..."
            if [ "$env" = "prod" ]; then
                docker-compose -f docker-compose.prod.yml down
            else
                docker-compose down
            fi
            ;;
        "restart")
            log_info "重启服务..."
            if [ "$env" = "prod" ]; then
                docker-compose -f docker-compose.prod.yml restart
            else
                docker-compose restart
            fi
            check_services
            ;;
        "logs")
            if [ "$env" = "prod" ]; then
                docker-compose -f docker-compose.prod.yml logs -f
            else
                docker-compose logs -f
            fi
            ;;
        *)
            echo "用法: $0 {build|deploy|start|stop|restart|logs} [dev|prod]"
            echo "  build   - 构建镜像"
            echo "  deploy  - 完整部署 (构建+启动)"
            echo "  start   - 启动服务"
            echo "  stop    - 停止服务"
            echo "  restart - 重启服务"
            echo "  logs    - 查看日志"
            exit 1
            ;;
    esac
}

# 执行主函数
main "$@"