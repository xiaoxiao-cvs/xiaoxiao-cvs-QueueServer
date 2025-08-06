# Queue Forge Plugin v2.0.0

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)
[![Java Version](https://img.shields.io/badge/Java-17%2B-orange.svg)](https://www.oracle.com/java/technologies/javase-jdk17-downloads.html)
[![Minecraft Version](https://img.shields.io/badge/Minecraft-1.20.1-green.svg)](https://www.minecraft.net/)
[![Server Type](https://img.shields.io/badge/Server-Forge%2FMohist%2FArclight-red.svg)](https://files.minecraftforge.net/)

一个为 Minecraft 1.20.1 Forge 环境设计的现代化队列管理插件，使用 HTTP 协议与代理服务器通信。

## 🚀 特性

### 核心功能
- **HTTP 通信**: 使用现代的 HTTP 协议替代传统的 BungeeCord 消息通道
- **Forge 兼容**: 完全支持 Minecraft 1.20.1 + Forge 47.3.22
- **混合服务器支持**: 兼容 Mohist、Arclight 等混合服务器
- **异步处理**: 所有网络操作都是异步执行，不会阻塞主线程

### 队列系统
- **智能队列**: VIP 优先级队列 + 普通队列
- **批量传送**: 支持批量传送多个玩家以提高效率
- **实时状态**: 实时队列位置和等待时间显示
- **离线清理**: 自动清理离线玩家

### VIP 系统
- **权限整合**: 支持 LuckPerms、PermissionsEx 等权限插件
- **缓存优化**: 使用 Caffeine 缓存提高性能
- **优先级倍数**: 可配置的 VIP 优先级计算

### 安全功能
- **IP 白名单**: 支持 CIDR 表示法的 IP 过滤
- **反破解**: 检测并阻止盗版客户端
- **连接限制**: 防止频繁连接攻击
- **安全认证**: HTTP Bearer Token 认证

### 数据存储
- **多数据库支持**: SQLite (默认) 和 MySQL
- **连接池**: HikariCP 高性能连接池
- **数据持久化**: 队列历史、VIP 记录、统计信息

## 📋 系统要求

### 服务器要求
- **Minecraft**: 1.20.1
- **Forge**: 47.3.22 或更高版本
- **Java**: 17 或更高版本
- **内存**: 最低 2GB RAM

### 支持的服务器类型
- ✅ **Mohist** (推荐)
- ✅ **Arclight** 
- ✅ **Paper** (基础功能)
- ✅ **Spigot** (基础功能)
- ⚠️ **Bukkit** (有限支持)

### 依赖插件 (可选)
- **LuckPerms** - VIP 权限管理
- **PermissionsEx** - 替代权限系统
- **Vault** - 经济系统整合
- **PlaceholderAPI** - 变量支持

## �️ 安装步骤

### 1. 编译插件
```bash
cd Server
mvn clean package
```

### 2. 安装插件
将生成的 `queue-forge-plugin-2.0.0-SNAPSHOT.jar` 复制到服务器的 `plugins` 目录。

### 3. 配置设置
首次运行后，编辑 `plugins/QueueForgePlugin/config.yml`:

```yaml
# HTTP代理服务器配置
proxy:
  server-url: "http://your-proxy-server:8080"
  token: "your-secret-token"
  heartbeat-interval: 30

# 队列配置
queue:
  enabled: true
  max-size: 100
  process-interval: 5
  transfer-batch-size: 3

# VIP配置
vip:
  enabled: true
  permission: "queue.vip"
  priority-multiplier: 2.0
```

### 4. 数据库配置 (MySQL 可选)
```yaml
database:
  type: "mysql"
  url: "jdbc:mysql://localhost:3306/queue"
  username: "queue_user"
  password: "your_password"
  max-pool-size: 10
```

## 🎮 使用说明

### 玩家命令
| 命令 | 描述 | 权限 |
|------|------|------|
| `/queue` | 加入队列或查看状态 | `queue.use` |
| `/queueinfo` | 查看队列信息 | `queue.info` |
| `/leave` | 离开队列 | `queue.leave` |

### 管理员命令
| 命令 | 描述 | 权限 |
|------|------|------|
| `/queueadmin clear` | 清空所有队列 | `queue.admin` |
| `/queueadmin reload` | 重载配置 | `queue.admin` |
| `/queueadmin remove <玩家>` | 移除指定玩家 | `queue.admin` |
| `/queueadmin setvip <玩家> <true/false>` | 设置 VIP 状态 | `queue.admin` |
| `/qstats` | 查看详细统计 | `queue.stats` |
| `/qreload` | 重载配置 | `queue.reload` |

### 权限节点
| 权限 | 描述 | 默认 |
|------|------|------|
| `queue.*` | 所有权限 | OP |
| `queue.use` | 使用队列 | `true` |
| `queue.vip` | VIP 优先级 | `false` |
| `queue.admin` | 管理权限 | OP |

## 🔧 HTTP API

### 代理服务器端点
插件会向代理服务器发送以下 HTTP 请求：

#### 心跳
```http
POST /api/heartbeat
Authorization: Bearer your-secret-token
Content-Type: application/json

{
  "serverName": "queue-server",
  "onlinePlayers": 25,
  "maxPlayers": 100,
  "tps": 19.8,
  "timestamp": 1691234567890
}
```

#### 玩家传送
```http
POST /api/player/transfer
Authorization: Bearer your-secret-token
Content-Type: application/json

{
  "playerId": "uuid-here",
  "playerName": "PlayerName",
  "sourceServer": "queue-server",
  "targetServer": "game",
  "timestamp": 1691234567890
}
```

#### 服务器状态查询
```http
GET /api/server/status
Authorization: Bearer your-secret-token
```

## 📊 性能优化

### 内存优化
- **缓存系统**: 使用 Caffeine 缓存减少数据库查询
- **对象池**: 重用数据库连接和 HTTP 连接
- **异步处理**: 避免阻塞主线程

### 网络优化
- **连接复用**: HTTP/1.1 Keep-Alive
- **压缩传输**: GZIP 压缩 (如果代理服务器支持)
- **超时控制**: 合理的连接和读取超时

### 数据库优化
- **连接池**: HikariCP 高性能连接池
- **索引优化**: 数据库表添加适当索引
- **批量操作**: 减少数据库往返次数

## 🐛 故障排除

### 常见问题

#### 1. 插件无法加载
**症状**: 插件在启动时被禁用
**解决方案**:
- 检查 Java 版本 (需要 17+)
- 确认服务器支持 Paper API
- 查看控制台错误日志

#### 2. HTTP 连接失败
**症状**: 无法连接到代理服务器
**解决方案**:
- 检查代理服务器地址和端口
- 确认认证令牌正确
- 检查防火墙设置

#### 3. VIP 检测失败
**症状**: VIP 玩家被当作普通玩家
**解决方案**:
- 检查权限插件配置
- 确认 VIP 权限节点设置
- 使用 `/queueadmin setvip` 手动设置

#### 4. 数据库连接错误
**症状**: 数据库相关功能异常
**解决方案**:
- 检查数据库配置
- 确认数据库服务运行正常
- 查看数据库连接权限

### 调试模式
启用调试模式以获取更详细的日志:
```yaml
debug: true
```

### 日志分析
重要日志文件位置:
- **插件日志**: `logs/latest.log`
- **队列数据**: `plugins/QueueForgePlugin/queue.db`
- **配置文件**: `plugins/QueueForgePlugin/config.yml`

## � 更新说明

### v2.0.0 的主要变更
1. **架构重写**: 从 BungeeCord 消息通道改为 HTTP 通信
2. **Forge 优化**: 专门针对 Forge 环境优化
3. **性能提升**: 使用现代化的缓存和异步处理
4. **安全增强**: 新增多层安全验证机制
5. **功能扩展**: 增加更多管理命令和统计功能

### 迁移指南
从 v1.x 升级到 v2.0.0:
1. **备份数据**: 备份旧版本的配置和数据
2. **更新配置**: 使用新的配置格式
3. **测试功能**: 在测试环境验证所有功能
4. **更新代理**: 确保代理服务器支持新的 HTTP API

## 📞 支持

### 获取帮助
- **GitHub Issues**: [提交问题](https://github.com/xiaoxiao-cvs/QueueServer/issues)
- **文档**: 查看在线文档
- **社区**: 加入开发者社区

### 贡献代码
欢迎提交 Pull Request 来改进这个插件:
1. Fork 这个仓库
2. 创建功能分支
3. 提交更改
4. 发起 Pull Request

### 许可证
本项目使用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

---

**开发者**: QueueServer Team  
**版本**: 2.0.0  
**支持**: Minecraft 1.20.1 + Forge 47.3.22  
**最后更新**: 2025-08-06
- ✅ 基础队列管理系统
- ✅ Mohist 混合服务器支持
- ✅ VIP 优先队列
- ✅ 服务器状态监控
- ✅ 数据库持久化
- ✅ 命令系统
- 🔄 代理网络集成
- 🔄 Web 管理界面

## 📄 许可证

本项目基于 [MIT License](LICENSE) 开源协议。

## 🔗 相关链接

- [Mohist 官网](https://mohistmc.com/)
- [Spigot API 文档](https://hub.spigotmc.org/javadocs/spigot/)
- [Velocity 文档](https://docs.papermc.io/velocity)
- [问题反馈](https://github.com/xiaoxiao-cvs/QueueServer/issues)

## 👥 开发团队

- **维护者**: QueueServer Team
- **贡献者**: 查看 [Contributors](https://github.com/xiaoxiao-cvs/QueueServer/graphs/contributors)

---

如果这个项目对您有帮助，请给我们一个 ⭐ Star！

有问题或建议？欢迎 [提交 Issue](https://github.com/xiaoxiao-cvs/QueueServer/issues/new) 或加入我们的社区讨论。