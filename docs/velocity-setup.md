# Velocity代理端配置指南

## 安装步骤

### 1. 下载Velocity
```bash
# 下载最新版本的Velocity
wget https://api.papermc.io/v2/projects/velocity/versions/3.2.0/builds/308/downloads/velocity-3.2.0-308.jar
```

### 2. 创建配置文件
创建 `velocity.toml` 配置文件：

```toml
# Velocity配置文件
config-version = "2.6"

# 服务器绑定设置
bind = "0.0.0.0:25577"

# 服务器列表信息
motd = "&3您的Minecraft服务器 &8| &a队列系统"
show-max-players = 500
online-mode = false  # 支持非正版玩家
prevent-client-proxy-connections = false

# 服务器定义
[servers]
# 队列服务器 (Mohist)
queue = "127.0.0.1:25565"
# 主游戏服务器 (Forge)
survival = "127.0.0.1:25566"
creative = "127.0.0.1:25567"

# 默认服务器设置
try = [
    "queue"
]

# Forge支持设置
[advanced]
compression-threshold = 256
compression-level = -1
login-ratelimit = 3000
connection-timeout = 5000
read-timeout = 30000
haproxy-protocol = false

# 转发设置 (重要: 支持Forge模组数据)
[player-info-forwarding]
forwarding-secret-file = "forwarding.secret"
player-info-forwarding-mode = "modern"

# 查询设置
[query]
enabled = true
port = 25577
map = "Minecraft"
show-plugins = false
```

### 3. Forge模组支持配置

#### 在队列服务器 (Mohist) 的 config.yml 中:
```yaml
# 队列服务器配置
proxy:
  enabled: true
  velocity-secret: "your-secret-here"
  
# 目标服务器 (Forge服务器)
target-servers:
  survival:
    host: "127.0.0.1"
    port: 25566
    max-players: 100
    forge-support: true
  creative:
    host: "127.0.0.1"  
    port: 25567
    max-players: 50
    forge-support: true
```

### 4. 启动脚本

#### Velocity启动脚本 (`start-velocity.bat`):
```batch
@echo off
title Velocity Proxy Server
java -Xms512M -Xmx1G -XX:+UseG1GC -XX:G1HeapRegionSize=4M -XX:+UnlockExperimentalVMOptions -XX:+UseJVMCICompiler -jar velocity-3.2.0-308.jar
pause
```

#### 队列服务器启动脚本 (`start-queue.bat`):
```batch
@echo off
title Queue Server - Mohist
java -Xms2G -Xmx4G -XX:+UseG1GC -jar mohist-1.19.2-xxx-server.jar
pause
```

## 网络架构

```
玩家客户端 (Forge)
    ↓
Velocity代理端 (:25577)
    ↓
├─ 队列服务器 (:25565) - Mohist + 队列插件
└─ 游戏服务器 (:25566) - Forge服务器
```

## 插件集成

### 队列插件需要的Velocity支持:
1. **玩家转发**: 支持Forge玩家数据转发
2. **服务器切换**: 自动将排队完成的玩家转发到目标服务器
3. **状态同步**: 实时同步各服务器的在线状态

### 在队列插件中添加Velocity支持代码:
```java
// 检测Velocity环境
if (getServer().getVersion().contains("Velocity")) {
    // 启用Velocity支持模式
    enableVelocityMode();
}

// 发送玩家到目标服务器
public void sendPlayerToServer(Player player, String serverName) {
    if (isVelocityMode()) {
        // 使用Velocity API发送玩家
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF(serverName);
        player.sendPluginMessage(this, "velocity:main", out.toByteArray());
    }
}
```

## 推荐的服务器组合

### 方案一: 标准Forge服务器
- **代理端**: Velocity
- **队列服务器**: Mohist 1.19.2 + 队列插件
- **游戏服务器**: Forge 1.19.2 + 模组包

### 方案二: 混合模组服务器  
- **代理端**: Velocity
- **队列服务器**: Mohist 1.19.2 + 队列插件
- **游戏服务器**: Mohist 1.19.2 + Forge模组 + Bukkit插件

## 性能优化建议

1. **内存分配**: 
   - Velocity: 1GB
   - 队列服务器: 2-4GB
   - 游戏服务器: 6-8GB+

2. **网络优化**:
   - 使用同一台机器或内网连接
   - 调整compression-threshold
   - 启用haproxy-protocol (如果需要)

3. **模组兼容性**:
   - 确保所有服务器使用相同的Forge版本
   - 模组列表保持一致
   - 配置文件同步

## 故障排除

### 常见问题:
1. **模组不同步**: 确保所有服务器的模组版本一致
2. **玩家数据丢失**: 检查player-info-forwarding配置
3. **连接超时**: 调整connection-timeout和read-timeout
4. **非正版支持**: 设置online-mode=false

### 调试命令:
```bash
# 查看Velocity连接状态
/velocity info

# 查看服务器列表
/velocity server list

# 重载配置
/velocity reload
```
