# JSON白名单格式支持

队列插件现在支持读取主服务器的JSON格式白名单文件。以下是支持的格式：

## 标准Minecraft白名单格式

```json
[
  {
    "uuid": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
    "name": "PlayerName1"
  },
  {
    "uuid": "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
    "name": "PlayerName2"
  }
]
```

## 仅用户名格式（适用于非正版服务器）

```json
[
  {
    "name": "Player1"
  },
  {
    "name": "Player2"
  },
  {
    "name": "Player3"
  }
]
```

## 仅UUID格式

```json
[
  {
    "uuid": "f47ac10b-58cc-4372-a567-0e02b2c3d479"
  },
  {
    "uuid": "6ba7b810-9dad-11d1-80b4-00c04fd430c8"
  }
]
```

## 配置设置

在 `config.yml` 中添加以下配置：

```yaml
whitelist:
  enabled: true                                    # 启用白名单功能
  file-path: "../game-server/whitelist.json"      # 白名单文件路径
  auto-reload: true                                # 自动重载白名单
  reload-interval-seconds: 30                      # 重载间隔（秒）
  kick-non-whitelisted: true                       # 踢出非白名单玩家
  kick-message: "&c你不在服务器白名单中!"            # 踢出消息
```

## 白名单管理命令

### 管理员命令（需要 `queueserver.whitelist` 权限）

- `/whitelist add <玩家名>` - 添加玩家到白名单
- `/whitelist remove <玩家名>` - 从白名单移除玩家
- `/whitelist check <玩家名>` - 检查玩家是否在白名单中
- `/whitelist list` - 显示白名单（开发中）
- `/whitelist reload` - 重新加载白名单
- `/whitelist stats` - 显示白名单统计信息
- `/whitelist info` - 显示白名单配置信息

## 功能特性

1. **自动文件监控**: 自动检测白名单文件的修改并重新加载
2. **多格式支持**: 支持标准Minecraft格式和自定义格式
3. **非正版兼容**: 支持非正版服务器的用户名验证
4. **实时管理**: 通过命令实时添加/删除白名单条目
5. **统计信息**: 提供详细的白名单统计和状态信息

## 使用流程

1. 在配置文件中启用白名单功能
2. 设置正确的白名单文件路径
3. 确保白名单文件格式正确
4. 使用命令管理白名单条目
5. 查看统计信息确认功能正常

## 注意事项

- 白名单文件路径是相对于队列服务器工作目录的
- 文件修改会被自动检测并重新加载
- 支持热重载，无需重启服务器
- 非正版服务器会使用用户名生成确定性UUID
