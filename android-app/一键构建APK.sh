#!/bin/bash

echo "================================"
echo "  自动配置并构建 APK"
echo "================================"
echo ""

# 检查Java
if ! command -v java &> /dev/null; then
    echo "❌ 未安装 Java"
    echo "请先安装 JDK:"
    echo "  brew install openjdk@11"
    exit 1
fi

echo "✅ Java已安装"
echo ""

# 检查是否已有gradle wrapper jar
if [ ! -f "gradle/wrapper/gradle-wrapper.jar" ]; then
    echo "📦 检测到缺少Gradle Wrapper，正在下载..."
    echo ""
    
    # 创建目录
    mkdir -p gradle/wrapper
    
    # 下载gradle wrapper jar
    echo "⬇️  下载 gradle-wrapper.jar..."
    curl -L -o gradle/wrapper/gradle-wrapper.jar \
        https://raw.githubusercontent.com/gradle/gradle/master/gradle/wrapper/gradle-wrapper.jar
    
    if [ $? -ne 0 ]; then
        echo "❌ 下载失败"
        echo ""
        echo "💡 请手动下载或使用Android Studio打开项目"
        exit 1
    fi
    
    echo "✅ Gradle Wrapper 下载完成"
    echo ""
fi

# 设置执行权限
chmod +x gradlew

echo "🔨 开始构建 APK..."
echo "⏳ 首次构建需要下载依赖，可能需要5-10分钟..."
echo ""

# 构建
./gradlew assembleDebug

if [ $? -eq 0 ]; then
    echo ""
    echo "================================"
    echo "  ✅ 构建成功！"
    echo "================================"
    echo ""
    
    APK="app/build/outputs/apk/debug/app-debug.apk"
    
    if [ -f "$APK" ]; then
        echo "📱 APK文件: $(pwd)/$APK"
        echo "📦 文件大小: $(du -h "$APK" | cut -f1)"
        echo ""
        echo "================================"
        echo "  传输到手机的方法"
        echo "================================"
        echo ""
        echo "方法1: 启动HTTP服务器"
        echo "  cd app/build/outputs/apk/debug"
        echo "  python3 -m http.server 8000"
        echo "  手机浏览器访问: http://$(ipconfig getifaddr en0 2>/dev/null || echo "你的Mac的IP"):8000/app-debug.apk"
        echo ""
        echo "方法2: 用微信/QQ传文件"
        echo "  把 $APK 发给自己"
        echo ""
        echo "方法3: ADB安装（如果手机已连接）"
        echo "  adb install $APK"
        echo ""
        
        # 在Finder中显示
        open -R "$APK"
    fi
else
    echo ""
    echo "❌ 构建失败"
    echo ""
    echo "请尝试："
    echo "1. 安装 Android Studio"
    echo "2. 检查网络连接"
    echo "3. 查看上面的错误信息"
    exit 1
fi

