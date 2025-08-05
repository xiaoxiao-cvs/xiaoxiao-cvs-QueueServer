# Mohist 版本排队系统部署指南

## 📋 概述

Mohist 是一个混合服务器核心，同时支持 Bukkit 插件和 Forge Mod。本指南将帮助您在 Mohist 环境下部署排队系统。

## 🔧 环境要求

### 必需组件
- **Java 17+** - Mohist 需要较新的Java版本
- **Mohist 1.20.1+** - 推荐使用最新稳定版本
- **MySQL 8.0+** 或 **SQLite** - 数据存储
- **Velocity 3.3.0+** - 代理服务器

### 推荐配置
- **内存**: 最少4GB，推荐8GB+
- **CPU**: 多核处理器
- **存储**: SSD硬盘
- **网络**: 稳定的网络连接

## 📦 下载和安装

### 1. 下载 Mohist 服务器

```powershell
# 创建服务器目录
mkdir "E:\MinecraftServer\Mohist"
cd "E:\MinecraftServer\Mohist"

# 下载 Mohist (请从官网获取最新版本)
# https://mohistmc.com/download
# 下载 mohist-1.20.1-xxx-server.jar 到此目录
```

### 2. 编译队列插件

在项目根目录执行：

```powershell
# 如果有Maven环境
mvn clean package

# 或者手动编译（需要配置Java classpath）
```

### 3. 复制插件文件

```powershell
# 创建插件目录
mkdir plugins

# 复制Mohist插件
copy "queue-mohist\target\queue-mohist-1.0.0-SNAPSHOT.jar" "plugins\"

# 复制配置文件
mkdir "plugins\MinecraftQueueServer"
copy "queue-mohist\src\main\resources\config.yml" "plugins\MinecraftQueueServer\"
```

## ⚙️ 配置文件

### 1. Mohist 服务器配置 (server.properties)

```properties
# 基本设置
server-port=25566
max-players=50
online-mode=false
difficulty=easy
gamemode=survival
level-name=queue-world

# Mohist特定设置
mohist.check-update=false
mohist.enable-forge-features=true

# 队列服务器设置
motd=§a排队服务器 §7- §e等待进入游戏服务器
```

### 2. 队列插件配置 (plugins/MinecraftQueueServer/config.yml)

```yaml
# 服务器设置
server:
  # 队列服务器模式
  queue-server: true
  
  # 目标游戏服务器
  target-server: "survival"
  target-server-host: "127.0.0.1"
  target-server-port: 25565
  
  # 最大玩家数（队列服务器）
  max-players: 50

# 队列设置
queue:
  enabled: true
  check-interval: 30  # 秒
  transfer-batch-size: 5
  
  # 队列消息
  messages:
    welcome: "§a欢迎来到排队服务器！"
    position: "§e您在队列中的位置: §6#{position} §7/ §6{total}"
    transfer: "§a正在传送到游戏服务器..."

# 服务器监控设置
monitor:
  # 是否启用服务器监控
  enabled: true
  
  # 监控检查间隔（秒）
  check-interval: 30
  
  # 状态广播间隔（秒）
  broadcast-interval: 300  # 5分钟
  
  # 连接超时时间（毫秒）
  connection-timeout: 5000
  
  # 是否在服务器状态变化时立即广播
  broadcast-on-change: true
  
  # 广播消息格式
  messages:
    # 独立模式广播消息
    standalone-format: "§6§l[服务器状态] §a当前在线: §e{current}§7/{max} §8| §a队列中: §e{queue}人"
    # 代理模式广播消息
    proxy-format: "§6§l[服务器状态] §a{target}服务器在线: §e{current}人 §8| §a队列服务器: §e{queue}人排队"
    # 服务器离线消息
    offline-format: "§c§l[服务器状态] §c{target}服务器暂时离线，请稍后再试"

# VIP设置
vip:
  enabled: true
  priority-levels:
    bronze: 1
    silver: 2
    gold: 3
    diamond: 4
    supreme: 5

# 白名单设置
whitelist:
  enabled: false
  message: "§c您不在服务器白名单中！"

# 数据库设置
database:
  type: "sqlite"  # 或 "mysql"
  
  # SQLite设置
  sqlite:
    file: "queue_data.db"
  
  # MySQL设置（如果使用MySQL）
  mysql:
    host: "localhost"
    port: 3306
    database: "minecraft_queue"
    username: "queue_user"
    password: "your_password"
    
# 安全设置
security:
  rate-limiting:
    enabled: true
    max-attempts: 3
    window-seconds: 60
  
  duplicate-connections:
    enabled: true
    max-connections-per-ip: 2
```

### 3. Mohist配置 (mohist.yml)

```yaml
# Mohist性能优化
settings:
  # 插件兼容性
  bukkit:
    enable-plugin-profiling: false
    
  # Forge兼容性
  forge:
    enable-mod-security-manager: true
    enable-network-compression: true
    
  # 性能优化
  optimization:
    enable-async-chunk-loading: true
    enable-async-pathfinding: true
    
  # 日志设置
  logging:
    log-player-commands: true
    log-world-changes: false
```

## 🚀 启动脚本

### Windows 启动脚本 (start-mohist.bat)

```batch
@echo off
title Minecraft Queue Server - Mohist

echo ====================================
echo   Minecraft Queue Server - Mohist
echo ====================================

:start
echo 正在启动服务器...

java -Xmx4G -Xms4G ^
  -XX:+UseG1GC ^
  -XX:G1HeapRegionSize=8M ^
  -XX:+UnlockExperimentalVMOptions ^
  -XX:+DisableExplicitGC ^
  -XX:+AlwaysPreTouch ^
  -XX:+ParallelRefProcEnabled ^
  -Dmohist.check-libraries=false ^
  -jar mohist-1.20.1-xxx-server.jar nogui

echo.
echo 服务器已关闭
echo.
set /p restart="是否重启服务器? (y/n): "
if /i "%restart%"=="y" goto start

pause
```

### Linux 启动脚本 (start-mohist.sh)

```bash
#!/bin/bash

echo "===================================="
echo "  Minecraft Queue Server - Mohist"
echo "===================================="

while true; do
    echo "正在启动服务器..."
    
    java -Xmx4G -Xms4G \
      -XX:+UseG1GC \
      -XX:G1HeapRegionSize=8M \
      -XX:+UnlockExperimentalVMOptions \
      -XX:+DisableExplicitGC \
      -XX:+AlwaysPreTouch \
      -XX:+ParallelRefProcEnabled \
      -Dmohist.check-libraries=false \
      -jar mohist-1.20.1-xxx-server.jar nogui
    
    echo ""
    echo "服务器已关闭"
    echo ""
    read -p "是否重启服务器? (y/n): " restart
    if [[ $restart != "y" ]]; then
        break
    fi
done
```

## 🌐 网络架构与代理服务器安装

### 完整部署架构

```
玩家客户端
    ↓
代理服务器 (Velocity/BungeeCord 端口25577)
    ↓
┌─────────────────┬─────────────────┐
│   队列服务器      │   游戏服务器      │
│  (Mohist)       │  (Paper/Mohist) │
│  端口: 25566    │  端口: 25565    │
│  队列插件        │  游戏世界        │
└─────────────────┴─────────────────┘
    ↓
MySQL/SQLite数据库
```

## 🔧 代理服务器安装

### 选择代理服务器

| 特性 | Velocity | BungeeCord |
|------|----------|------------|
| 性能 | 更高 | 较低 |
| 插件生态 | 较新 | 成熟 |
| 配置复杂度 | 简单 | 中等 |
| **推荐指数** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ |

### 方案一：BungeeCord 安装（经典方案）

#### 1. 下载 BungeeCord

```powershell
# 创建代理服务器目录
mkdir "E:\MinecraftServer\BungeeCord"
cd "E:\MinecraftServer\BungeeCord"

# 下载最新版本的 BungeeCord
# 访问: https://ci.md-5.net/job/BungeeCord/
# 下载 BungeeCord.jar
```

#### 2. BungeeCord 配置文件 (config.yml)

```yaml
# BungeeCord 主配置
player_limit: 100
permissions:
  default:
  - bungeecord.command.server
  - bungeecord.command.list
  admin:
  - bungeecord.command.alert
  - bungeecord.command.end
  - bungeecord.command.ip
  - bungeecord.command.reload

timeout: 30000
log_commands: false
log_pings: true
online_mode: false  # 支持非正版玩家
remote_ping_cache: -1

network_compression_threshold: 256
prevent_proxy_connections: false

# 服务器列表
servers:
  queue:
    motd: '&a排队服务器'
    address: 127.0.0.1:25566
    restricted: false
  survival:
    motd: '&e生存服务器'
    address: 127.0.0.1:25565
    restricted: false

# 监听设置
listeners:
- query_port: 25577
  motd: '&a混合服务器网络 &7- &eForge + Bukkit支持'
  tab_list: GLOBAL_PING
  query_enabled: false
  proxy_protocol: false
  forced_hosts:
    pvp.md-5.net: pvp
  ping_passthrough: false
  priorities:
  - queue  # 默认连接到队列服务器
  bind_local_address: true
  host: 0.0.0.0:25577
  max_players: 100
  tab_size: 60
  force_default_server: false

# IP转发（重要！）
ip_forward: true
forge_support: true  # 支持Forge客户端

# 其他设置
disabled_commands:
- disabledcommandhere
groups:
  md_5:
  - admin
connection_throttle: 4000
connection_throttle_limit: 3
stats: false
```

#### 3. BungeeCord 启动脚本

**Windows (start-bungee.bat):**
```batch
@echo off
title BungeeCord Proxy Server

echo ====================================
echo      BungeeCord Proxy Server
echo ====================================

:start
echo 正在启动代理服务器...

java -Xmx1G -Xms1G ^
  -XX:+UseG1GC ^
  -XX:G1HeapRegionSize=4M ^
  -XX:+UnlockExperimentalVMOptions ^
  -XX:+DisableExplicitGC ^
  -Dfile.encoding=UTF-8 ^
  -jar BungeeCord.jar

echo.
echo 代理服务器已关闭
echo.
set /p restart="是否重启代理服务器? (y/n): "
if /i "%restart%"=="y" goto start

pause
```

**Linux (start-bungee.sh):**
```bash
#!/bin/bash

echo "===================================="
echo "    BungeeCord Proxy Server"
echo "===================================="

while true; do
    echo "正在启动代理服务器..."
    
    java -Xmx1G -Xms1G \
      -XX:+UseG1GC \
      -XX:G1HeapRegionSize=4M \
      -XX:+UnlockExperimentalVMOptions \
      -XX:+DisableExplicitGC \
      -Dfile.encoding=UTF-8 \
      -jar BungeeCord.jar
    
    echo ""
    echo "代理服务器已关闭"
    echo ""
    read -p "是否重启代理服务器? (y/n): " restart
    if [[ $restart != "y" ]]; then
        break
    fi
done
```

#### 4. 后端服务器配置

**队列服务器 (server.properties):**
```properties
# 重要：必须设置为离线模式
online-mode=false
# 设置端口
server-port=25566
# 禁用Bungeecord支持（队列服务器不需要）
bungeecord=false
```

**游戏服务器 (server.properties):**
```properties
# 重要：必须设置为离线模式
online-mode=false
# 设置端口
server-port=25565
# 如果是Spigot/Paper服务器，启用Bungeecord支持
bungeecord=true
```

**游戏服务器 spigot.yml (如果使用Spigot/Paper):**
```yaml
settings:
  bungeecord: true
  # IP转发设置
  attribute:
    maxHealth:
      max: 2048.0
    movementSpeed:
      max: 2048.0
    attackDamage:
      max: 2048.0
```

### 方案二：Velocity 安装（推荐）

#### 1. 下载 Velocity

```powershell
# 创建代理服务器目录
mkdir "E:\MinecraftServer\Velocity"
cd "E:\MinecraftServer\Velocity"

# 下载最新版本的 Velocity
# 访问: https://papermc.io/downloads/velocity
# 下载 velocity-3.x.x-xxx.jar
```

#### 2. Velocity 配置文件 (velocity.toml)

```toml
# Velocity配置 - 推荐配置
config-version = "2.6"

# 代理服务器绑定地址和端口
bind = "0.0.0.0:25577"

# 服务器信息显示
motd = "§a混合服务器网络 §7- §eForge + Bukkit支持"
show-max-players = 100
online-mode = false  # 支持非正版玩家

# 服务器列表配置
[servers]
# 队列服务器（玩家首先连接）
queue = "127.0.0.1:25566"
# 游戏服务器（队列完成后传送到这里）  
survival = "127.0.0.1:25565"

# 默认连接服务器
try = ["queue"]

# 转发设置（重要！）
[player-info-forwarding]
mode = "modern"  # 或 "legacy" 用于兼容旧版本
secret = "your-secret-key-here"  # 生成一个安全的密钥

# 高级网络设置
[advanced]
compression-threshold = 256
compression-level = -1
login-ratelimit = 3000
connection-timeout = 5000
read-timeout = 30000
haproxy-protocol = false

# 查询设置
[query]
enabled = false
port = 25577

# Forge支持
[forced-hosts]
# "forge.example.com" = ["survival"]

# 其他设置
announce-forge = true  # 支持Forge客户端
kick-existing-players = false
```

#### 3. Velocity 启动脚本

**Windows (start-velocity.bat):**
```batch
@echo off
title Velocity Proxy Server

echo ====================================
echo      Velocity Proxy Server  
echo ====================================

:start
echo 正在启动代理服务器...

java -Xmx1G -Xms1G ^
  -XX:+UseG1GC ^
  -XX:G1HeapRegionSize=4M ^
  -XX:+UnlockExperimentalVMOptions ^
  -XX:+DisableExplicitGC ^
  -XX:+AlwaysPreTouch ^
  -Dfile.encoding=UTF-8 ^
  -jar velocity-3.3.0-xxx.jar

echo.
echo 代理服务器已关闭
echo.
set /p restart="是否重启代理服务器? (y/n): "
if /i "%restart%"=="y" goto start

pause
```

**Linux (start-velocity.sh):**
```bash
#!/bin/bash

echo "===================================="
echo "    Velocity Proxy Server"
echo "===================================="

while true; do
    echo "正在启动代理服务器..."
    
    java -Xmx1G -Xms1G \
      -XX:+UseG1GC \
      -XX:G1HeapRegionSize=4M \
      -XX:+UnlockExperimentalVMOptions \
      -XX:+DisableExplicitGC \
      -XX:+AlwaysPreTouch \
      -Dfile.encoding=UTF-8 \
      -jar velocity-3.3.0-xxx.jar
    
    echo ""
    echo "代理服务器已关闭"  
    echo ""
    read -p "是否重启代理服务器? (y/n): " restart
    if [[ $restart != "y" ]]; then
        break
    fi
done
```

#### 4. 后端服务器配置（Velocity版本）

**队列服务器 paper.yml (如果使用Paper):**
```yaml
settings:
  velocity-support:
    enabled: true
    online-mode: true  # Velocity模式下设为true
    secret: "your-secret-key-here"  # 与velocity.toml中相同
```

**游戏服务器 paper.yml:**
```yaml  
settings:
  velocity-support:
    enabled: true
    online-mode: true
    secret: "your-secret-key-here"  # 与velocity.toml中相同
```

## 🚀 代理服务器部署步骤

### 完整部署流程

1. **安装Java环境**
```powershell
# 确保安装Java 17+
java -version
```

2. **创建目录结构**
```powershell
# 创建完整的服务器目录结构
mkdir "E:\MinecraftServer"
mkdir "E:\MinecraftServer\Velocity"      # 或 BungeeCord
mkdir "E:\MinecraftServer\QueueServer"   # 队列服务器
mkdir "E:\MinecraftServer\GameServer"    # 游戏服务器
```

3. **配置启动顺序**
```powershell
# 建议的启动顺序：
# 1. 先启动游戏服务器
cd "E:\MinecraftServer\GameServer"
start cmd /k "start-game-server.bat"

# 2. 再启动队列服务器  
cd "E:\MinecraftServer\QueueServer"
start cmd /k "start-queue-server.bat"

# 3. 最后启动代理服务器
cd "E:\MinecraftServer\Velocity"
start cmd /k "start-velocity.bat"
```

### 防火墙配置

```powershell
# Windows防火墙规则
netsh advfirewall firewall add rule name="Minecraft-Proxy" dir=in action=allow protocol=TCP localport=25577
netsh advfirewall firewall add rule name="Minecraft-Queue" dir=in action=allow protocol=TCP localport=25566  
netsh advfirewall firewall add rule name="Minecraft-Game" dir=in action=allow protocol=TCP localport=25565
```

### 连接测试

1. **测试代理服务器**
   - 连接地址：`你的服务器IP:25577`
   - 应该自动连接到队列服务器

2. **测试队列功能**
   - 在队列服务器使用命令：`/queue`
   - 观察是否正确显示队列信息

3. **测试传送功能**  
   - 等待队列处理或使用管理员命令强制传送
   - 观察是否成功传送到游戏服务器

## 🔧 故障排除

### 常见问题

| 问题 | 原因 | 解决方案 |
|------|------|----------|
| 无法连接代理服务器 | 端口被占用或防火墙阻止 | 检查端口和防火墙设置 |
| 传送失败 | 插件消息通道未注册 | 检查BungeeCord消息通道 |
| UUID不一致 | IP转发配置错误 | 确保代理和后端服务器配置一致 |
| Forge客户端无法连接 | Forge支持未启用 | 在代理配置中启用Forge支持 |

### 推荐使用 Velocity 的原因

1. **更好的性能** - 现代异步架构
2. **更强的安全性** - 现代IP转发机制  
3. **更好的Forge支持** - 原生支持ModdedNetwork
4. **活跃维护** - 持续更新和bug修复
5. **简单配置** - TOML配置文件更直观

### Velocity配置 (velocity.toml)

```toml
# Velocity配置
config-version = "2.6"
bind = "0.0.0.0:25577"
motd = "§a混合服务器网络 §7- §eForge + Bukkit支持"
show-max-players = 100
online-mode = false

[servers]
queue = "127.0.0.1:25566"      # Mohist队列服务器
survival = "127.0.0.1:25565"   # 游戏服务器

try = ["queue"]

[advanced]
compression-threshold = 256
compression-level = -1
login-ratelimit = 3000
connection-timeout = 5000
read-timeout = 30000

[query]
enabled = false
```

## 🔧 权限配置

### LuckPerms权限示例

```bash
# VIP权限设置
/lp group bronze permission set queue.vip.bronze true
/lp group silver permission set queue.vip.silver true
/lp group gold permission set queue.vip.gold true
/lp group diamond permission set queue.vip.diamond true
/lp group supreme permission set queue.vip.supreme true

# 管理员权限
/lp group admin permission set queue.admin true
/lp group admin permission set queue.stats true
/lp group admin permission set queue.whitelist true

# 玩家权限
/lp group default permission set queue.use true
```

## 📊 监控和维护

### 1. 日志文件位置
- **Mohist日志**: `logs/latest.log`
- **插件日志**: `logs/plugins/MinecraftQueueServer/`
- **错误日志**: `crash-reports/`

### 2. 性能监控

```bash
# 查看服务器状态
/queue admin info

# 查看队列统计
/qstats

# 查看TPS
/tps

# 查看内存使用
/gc
```

### 3. 常见问题排查

| 问题 | 解决方案 |
|------|----------|
| 插件无法加载 | 检查Java版本和依赖 |
| 权限系统不工作 | 确保权限插件正确加载 |
| 队列传送失败 | 检查BungeeCord配置 |
| Forge Mod不兼容 | 更新Mohist版本 |

## 📝 命令大全

### 玩家命令
- `/queue` - 查看队列状态
- `/queueinfo` - 显示详细队列信息
- `/leave` - 离开队列

### 管理员命令
- `/queueadmin reload` - 重载配置
- `/queueadmin clear` - 清空队列
- `/queueadmin kick <玩家>` - 踢出队列
- `/queueadmin setvip <玩家> <等级>` - 设置VIP
- `/queueadmin info` - 系统详细信息
- `/queueadmin monitor check` - 立即检查服务器状态
- `/queueadmin monitor broadcast` - 立即广播服务器状态
- `/queueadmin monitor info` - 显示监控详细信息

### 统计命令
- `/qstats` - 队列统计
- `/whitelist <add|remove|list>` - 白名单管理

## 🖥️ 服务器监控功能

### 监控功能特性

1. **实时服务器状态监控**
   - 自动监控目标服务器在线人数
   - 检测服务器在线/离线状态
   - 支持独立模式和代理模式

2. **智能状态广播**
   - 定时向公屏广播服务器状态
   - 服务器状态变化时立即通知
   - 可自定义广播消息格式

3. **管理员监控工具**
   - 手动检查服务器状态
   - 强制广播当前状态
   - 详细监控信息查看

### 监控配置说明

```yaml
monitor:
  enabled: true                    # 启用监控功能
  check-interval: 30               # 每30秒检查一次
  broadcast-interval: 300          # 每5分钟广播一次
  connection-timeout: 5000         # 5秒连接超时
  broadcast-on-change: true        # 状态变化时立即广播
```

### 监控命令使用

```bash
# 立即检查目标服务器状态
/queueadmin monitor check

# 立即向所有玩家广播当前状态
/queueadmin monitor broadcast

# 查看详细的监控信息
/queueadmin monitor info
```

### 广播消息示例

**代理模式广播:**
```
[服务器状态] survival服务器在线: 45人 | 队列服务器: 12人排队
```

**独立模式广播:**
```
[服务器状态] 当前在线: 38/50 | 队列中: 8人
```

**服务器离线通知:**
```
[服务器状态] survival服务器暂时离线，请稍后再试
```

## 🔒 安全建议

### 1. 服务器安全
```yaml
# 在server.properties中设置
online-mode=false  # 支持非正版，但需要额外安全措施
white-list=true    # 启用白名单（可选）
spawn-protection=16
enable-command-block=false
```

### 2. 插件安全
- 定期更新Mohist版本
- 使用强密码保护数据库
- 限制管理员权限分配
- 定期备份配置文件

### 3. 网络安全
- 使用防火墙限制访问
- 配置DDoS保护
- 监控异常连接

## 🚀 优化建议

### JVM优化参数
```bash
# 生产环境推荐（8GB内存）
-Xmx8G -Xms8G
-XX:+UseG1GC
-XX:G1HeapRegionSize=16M
-XX:+UnlockExperimentalVMOptions
-XX:+DisableExplicitGC
-XX:+AlwaysPreTouch
-XX:+ParallelRefProcEnabled
-XX:MaxGCPauseMillis=200
-XX:+UnlockDiagnosticVMOptions
-XX:+LogGC
-XX:+LogGCDetails
-XX:+LogGCTimeStamps
-XX:+UseCompressedOops
-Dusing.aikars.flags=https://mcflags.emc.gs
```

### 插件性能优化
- 合理设置队列检查间隔
- 使用异步数据库操作
- 定期清理过期缓存
- 优化SQL查询

这份指南提供了完整的Mohist版本排队系统部署方案。如果您在部署过程中遇到任何问题，请随时询问！
