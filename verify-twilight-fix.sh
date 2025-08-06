#!/bin/bash

# Twilight Forest 崩溃修复验证脚本
# 用于测试暮色森林模组兼容性修复

echo "=== Minecraft Queue Server - Twilight Forest 崩溃修复验证 ==="
echo ""

# 检查Java环境
echo "1. 检查Java环境..."
if command -v java &> /dev/null; then
    echo "   ✓ Java 已安装: $(java -version 2>&1 | head -n 1)"
else
    echo "   ✗ Java 未安装"
    exit 1
fi

# 检查项目结构
echo ""
echo "2. 检查项目结构..."

if [ -f "src/main/java/com/github/queueserver/mohist/compatibility/ModCompatibilityHandler.java" ]; then
    echo "   ✓ ModCompatibilityHandler.java 存在"
else
    echo "   ✗ ModCompatibilityHandler.java 缺失"
fi

if [ -f "src/main/java/com/github/queueserver/mohist/compatibility/TwilightForestCrashHandler.java" ]; then
    echo "   ✓ TwilightForestCrashHandler.java 存在"
else
    echo "   ✗ TwilightForestCrashHandler.java 缺失"
fi

if [ -f "src/main/resources/mod-compatibility.yml" ]; then
    echo "   ✓ mod-compatibility.yml 配置文件存在"
else
    echo "   ✗ mod-compatibility.yml 配置文件缺失"
fi

# 检查Maven配置
echo ""
echo "3. 检查Maven配置..."
if [ -f "pom.xml" ]; then
    echo "   ✓ pom.xml 存在"
    if grep -q "spigot-api" pom.xml; then
        echo "   ✓ Spigot API 依赖已配置"
    else
        echo "   ⚠ Spigot API 依赖可能未配置"
    fi
else
    echo "   ✗ pom.xml 缺失"
fi

# 尝试编译
echo ""
echo "4. 尝试编译项目..."
if command -v mvn &> /dev/null; then
    echo "   正在编译..."
    if mvn clean compile > /dev/null 2>&1; then
        echo "   ✓ 编译成功"
    else
        echo "   ✗ 编译失败，请检查代码"
        echo "   运行 'mvn compile' 查看详细错误信息"
    fi
else
    echo "   ⚠ Maven 未安装，跳过编译测试"
fi

# 检查关键修复点
echo ""
echo "5. 检查关键修复点..."

# 检查忽略错误列表
if grep -q "twilightforest:restrictions" src/main/java/com/github/queueserver/mohist/compatibility/ModCompatibilityHandler.java; then
    echo "   ✓ Twilight Forest 错误忽略已配置"
else
    echo "   ✗ Twilight Forest 错误忽略未配置"
fi

# 检查系统属性设置
if grep -q "twilightforest.disable_registry_checks" src/main/java/com/github/queueserver/mohist/compatibility/ModCompatibilityHandler.java; then
    echo "   ✓ Twilight Forest 系统属性已设置"
else
    echo "   ✗ Twilight Forest 系统属性未设置"
fi

# 检查崩溃检测
if grep -q "handlePossibleCrash" src/main/java/com/github/queueserver/mohist/compatibility/TwilightForestCrashHandler.java; then
    echo "   ✓ 崩溃检测机制已实现"
else
    echo "   ✗ 崩溃检测机制未实现"
fi

echo ""
echo "=== 验证完成 ==="
echo ""
echo "修复说明："
echo "1. 增强了 ModCompatibilityHandler，添加了更多 Twilight Forest 错误忽略"
echo "2. 添加了 TwilightForestCrashHandler 来检测和处理客户端崩溃"
echo "3. 增加了系统属性设置来防止注册表错误"
echo "4. 添加了玩家恢复机制和管理员通知"
echo "5. 提供了 /qmod 命令来监控和管理模组兼容性"
echo ""
echo "使用方法："
echo "1. 编译插件: mvn clean package"
echo "2. 将生成的jar文件放入服务器plugins目录"
echo "3. 重启服务器"
echo "4. 使用 /qmod status 检查模组兼容性状态"
echo "5. 使用 /qmod crashes <玩家> 查看玩家崩溃统计"
echo ""
