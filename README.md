# openTCS Middleware

## 简介

这是一个 Java 中间件服务，用于为 openTCS 7.2.1 提供完整的 REST API 接口，支持创建运输订单、查看车辆状态等功能。

## 功能特性

- 🚗 **车辆状态查询** - 获取所有 AGV 车辆的实时状态
- 📦 **创建运输订单** - 通过 Web API 创建运输订单
- 📋 **订单管理** - 查看和取消订单
- 🌐 **多人协作** - 支持局域网内多用户访问

## 技术栈

- Java 17
- Javalin 6.5.0 (轻量级 Web 框架)
- Maven

## 快速开始

### 1. 编译项目

```bash
cd f:\TRAE\xiangmu\agv\opentcs-middleware
build.bat
```

### 2. 启动服务

```bash
run.bat
```

服务将在端口 8081 启动。

### 3. 启动前端

```bash
cd f:\TRAE\xiangmu\agv\web-frontend
python server.py
```

### 4. 访问前端

打开浏览器访问 http://localhost:8080

## API 端点

| 端点 | 方法 | 说明 |
|------|------|------|
| `/api/vehicles` | GET | 获取车辆列表 |
| `/api/orders` | GET | 获取订单列表 |
| `/api/orders` | POST | 创建运输订单 |
| `/api/orders/{name}` | DELETE | 取消订单 |
| `/api/locations` | GET | 获取位置点列表 |
| `/api/health` | GET | 健康检查 |

## 创建订单示例

```bash
curl -X POST http://localhost:8081/api/orders \
  -H "Content-Type: application/json" \
  -d '{"vehicleName": "Vehicle-01", "destination": "Point-0002"}'
```

## 文件结构

```
opentcs-middleware/
├── src/
│   └── main/
│       └── java/
│           └── com/
│               └── myagv/
│                   └── middleware/
│                       └── Main.java
├── pom.xml
├── build.bat
├── run.bat
└── README.md
```

## 注意事项

1. 确保 openTCS Kernel 已启动并运行
2. openTCS REST API 默认端口为 55200
3. 中间件服务端口为 8081
4. 前端服务端口为 8080

## 依赖

- Java 17 或更高版本
- Maven (用于编译)

## 许可证

MIT License