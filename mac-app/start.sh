#!/bin/bash

# 短信接收服务器启动脚本

echo "================================"
echo "  短信验证码接收服务器"
echo "================================"

# 检查Python版本
if ! command -v python3 &> /dev/null; then
    echo "❌ 错误: 未找到 python3"
    echo "请先安装 Python 3.7 或更高版本"
    exit 1
fi

# 检查依赖
if ! python3 -c "import flask" &> /dev/null; then
    echo "📦 正在安装依赖..."
    pip3 install -r requirements.txt
    if [ $? -ne 0 ]; then
        echo "❌ 依赖安装失败"
        exit 1
    fi
fi

# 启动服务器
echo "🚀 启动服务器..."
python3 server.py

