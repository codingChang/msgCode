# Android端短信转发应用

## 功能特点

- 📱 实时监听短信接收
- 🚀 自动转发到Mac服务器
- 🔔 前台服务保持运行
- ⚙️ 简单直观的配置界面
- 🔒 仅在本地局域网工作

## 构建步骤

### 前置要求
- Android Studio Arctic Fox (2020.3.1) 或更高版本
- JDK 8 或更高版本
- Android SDK (API 24+)

### 构建应用

1. 使用Android Studio打开此项目

2. 等待Gradle同步完成

3. 连接安卓设备或启动模拟器

4. 点击运行按钮，或使用命令行：
```bash
./gradlew installDebug
```

### 生成发布版本
```bash
./gradlew assembleRelease
```

APK文件将生成在：`app/build/outputs/apk/release/`

## 权限说明

应用需要以下权限：
- `RECEIVE_SMS` - 接收短信
- `READ_SMS` - 读取短信内容
- `INTERNET` - 网络连接
- `ACCESS_NETWORK_STATE` - 检查网络状态
- `FOREGROUND_SERVICE` - 前台服务
- `POST_NOTIFICATIONS` - 显示通知（Android 13+）

## 使用说明

1. **首次启动**：
   - 应用会请求必要的权限，请全部允许
   - 某些手机需要手动在设置中允许后台运行

2. **配置服务器**：
   - 输入Mac服务器的IP地址（查看Mac端显示的IP）
   - 端口默认为5000
   - 点击"测试连接"确认能否连接

3. **启动服务**：
   - 开启"启用短信转发"开关
   - 应用会显示前台通知表示服务运行中

4. **接收短信**：
   - 收到短信后会自动转发到Mac
   - 通知栏会显示转发状态

## 技术架构

### 核心组件

1. **MainActivity.kt**
   - 主界面Activity
   - 配置管理
   - 权限请求
   - 服务控制

2. **SmsReceiver.kt**
   - BroadcastReceiver监听系统短信
   - 提取短信内容和发件人
   - 触发转发服务

3. **SmsForwarderService.kt**
   - 前台Service保持运行
   - 执行网络转发任务
   - 更新通知状态

4. **NetworkHelper.kt**
   - 网络请求封装
   - 使用OkHttp发送HTTP POST
   - 错误处理

### 依赖库

- AndroidX Core KTX
- Material Design Components
- OkHttp - HTTP客户端
- Gson - JSON序列化

## 配置文件

- `AndroidManifest.xml` - 权限和组件声明
- `build.gradle` - 依赖和构建配置
- `proguard-rules.pro` - 代码混淆规则

## 故障排查

### 短信无法监听
1. 检查权限是否全部授予
2. 部分手机需要设置为默认短信应用（可选）
3. 检查系统是否禁止了后台运行

### 无法连接服务器
1. 确认手机和Mac在同一WiFi
2. 检查IP地址是否正确
3. 尝试ping Mac的IP：`ping 192.168.1.xxx`
4. 检查Mac防火墙设置

### 服务自动停止
1. 将应用加入电池优化白名单
2. 允许应用后台运行
3. 锁定应用在后台（部分手机）

## 不同手机品牌的特殊设置

### 小米/Redmi
- 设置 → 应用设置 → 应用管理 → 短信转发器
- 省电策略 → 无限制
- 自启动 → 开启

### 华为/荣耀
- 设置 → 应用 → 应用启动管理
- 找到短信转发器，设置为手动管理
- 允许自启动、允许关联启动、允许后台活动

### OPPO/一加
- 设置 → 电池 → 应用耗电管理
- 找到短信转发器，允许后台运行

### vivo
- 设置 → 电池 → 后台耗电管理
- 允许应用后台高耗电

### 三星
- 设置 → 电池 → 应用程序电源管理
- 将短信转发器加入未监视的应用

## 调试

### 查看日志
```bash
adb logcat | grep -E "SmsReceiver|SmsForwarder|NetworkHelper"
```

### 测试短信接收（需要root或模拟器）
```bash
adb emu sms send 10086 "测试验证码：123456"
```

### 查看应用信息
```bash
adb shell dumpsys package com.msgcode.smsforwarder
```

## 安全建议

- ⚠️ 短信包含敏感信息，请只在可信网络使用
- ⚠️ 不要连接公共WiFi时使用此功能
- ✅ 建议仅在家庭或办公室局域网使用
- ✅ 可以在不需要时关闭服务

## 未来改进

- [ ] 添加黑白名单过滤
- [ ] 支持关键词过滤
- [ ] 添加转发历史记录
- [ ] 支持多个服务器配置
- [ ] 添加统计信息
- [ ] 支持加密传输

## 开源协议

MIT License

