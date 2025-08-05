#!/bin/bash

echo "=========================================="
echo "  Minecraft Queue Server - Mohist Build"
echo "=========================================="

echo
echo "正在清理旧的构建文件..."
rm -rf target/

echo
echo "正在编译Mohist队列插件..."
mvn clean package -q

if [ $? -eq 0 ]; then
    echo
    echo "✅ 编译成功！"
    echo
    echo "插件文件位置:"
    echo "  target/queue-mohist-1.0.0-SNAPSHOT.jar"
    echo
    echo "复制到releases目录..."
    mkdir -p releases/
    cp "target/queue-mohist-1.0.0-SNAPSHOT.jar" "releases/queue-mohist-standalone-1.0.0.jar"
    echo "  releases/queue-mohist-standalone-1.0.0.jar"
    echo
    echo "🎉 构建完成！插件已准备就绪。"
else
    echo
    echo "❌ 编译失败！请检查错误信息。"
    exit 1
fi

echo
