#!/bin/bash

echo "================================"
echo "  构建 Android APK"
echo "================================"

# 检查是否有Java环境
if ! command -v java &> /dev/null; then
    echo "❌ 错误: 未找到 Java"
    echo "请先安装 JDK 8 或更高版本"
    echo ""
    echo "安装方法："
    echo "1. 使用 Homebrew: brew install openjdk@11"
    echo "2. 或下载：https://www.oracle.com/java/technologies/downloads/"
    exit 1
fi

echo "✅ Java 版本: $(java -version 2>&1 | head -n 1)"
echo ""

# 设置执行权限
chmod +x gradlew

echo "🔨 开始构建 APK..."
echo "⏳ 首次构建可能需要几分钟下载依赖，请耐心等待..."
echo ""

# 构建 Debug APK
./gradlew assembleDebug

if [ $? -eq 0 ]; then
    echo ""
    echo "================================"
    echo "  ✅ 构建成功！"
    echo "================================"
    echo ""
    echo "📱 APK 文件位置："
    APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
    
    if [ -f "$APK_PATH" ]; then
        FULL_PATH="$(pwd)/$APK_PATH"
        FILE_SIZE=$(du -h "$APK_PATH" | cut -f1)
        
        echo "   $FULL_PATH"
        echo "   文件大小: $FILE_SIZE"
        echo ""
        echo "================================"
        echo "  📲 安装方法"
        echo "================================"
        echo ""
        echo "方法1: ADB安装（如果手机已连接Mac）"
        echo "   adb install $APK_PATH"
        echo ""
        echo "方法2: 文件传输"
        echo "   1. 将 APK 文件发送到手机"
        echo "      - 用隔空投送(AirDrop)"
        echo "      - 用微信/QQ发给自己"
        echo "      - 用数据线复制"
        echo "   2. 在手机上点击 APK 文件安装"
        echo ""
        echo "方法3: 用 Python 创建下载服务器"
        echo "   python3 -m http.server 8000"
        echo "   然后在手机浏览器访问："
        echo "   http://$(ipconfig getifaddr en0):8000/$APK_PATH"
        echo ""
        
        # 询问是否启动HTTP服务器
        echo "💡 是否启动HTTP服务器方便手机下载？(y/n)"
        read -r response
        if [[ "$response" =~ ^[Yy]$ ]]; then
            echo ""
            echo "🌐 启动下载服务器..."
            echo "📱 在手机浏览器中访问："
            echo "   http://$(ipconfig getifaddr en0):8000/$APK_PATH"
            echo ""
            echo "按 Ctrl+C 停止服务器"
            echo ""
            cd "$(dirname "$APK_PATH")" && python3 -m http.server 8000
        fi
    else
        echo "❌ 未找到 APK 文件"
    fi
else
    echo ""
    echo "❌ 构建失败"
    echo ""
    echo "可能的原因："
    echo "1. 未安装 Android SDK"
    echo "2. Java 版本不兼容"
    echo "3. 网络问题（无法下载依赖）"
    echo ""
    echo "💡 建议："
    echo "1. 安装 Android Studio（包含完整的 Android SDK）"
    echo "2. 或查看上方错误信息进行排查"
    exit 1
fi

