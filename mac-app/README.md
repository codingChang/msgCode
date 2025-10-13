# Mac端短信接收服务器

## 安装步骤

1. 确保已安装Python 3.7或更高版本：
```bash
python3 --version
```

2. 安装依赖：
```bash
pip3 install -r requirements.txt
```

## 使用方法

### 启动服务器
```bash
python3 server.py
```

### 访问Web界面
在浏览器中打开：
- 本地访问：http://localhost:5000
- 局域网访问：http://your-ip:5000

## 功能说明

- 接收来自安卓设备的短信推送
- Web界面实时显示短信内容
- 自动高亮显示验证码（4-8位数字）
- 保留最近50条消息记录
- 支持一键清空消息

## API接口

### POST /api/sms
接收短信数据

请求格式：
```json
{
  "sender": "发件人号码",
  "content": "短信内容",
  "timestamp": "时间戳"
}
```

### GET /api/messages
获取消息列表

### POST /api/clear
清空所有消息

## 配置说明

默认配置：
- 端口：5001（避免macOS AirPlay占用5000）
- 主机：0.0.0.0（监听所有网络接口）
- 最大消息数：50条

### 自定义端口

```bash
# 使用默认端口 5001
python3 server.py

# 使用自定义端口
python3 server.py 8080
```

## 安全建议

1. 仅在可信任的局域网中使用
2. 不要将服务暴露到公网
3. 如需公网访问，请添加认证机制
4. 定期清空消息记录

## 故障排查

### 端口被占用
```bash
# 查找占用5000端口的进程
lsof -i :5000

# 杀死该进程
kill -9 <PID>

# 或者修改server.py中的端口号
```

### 防火墙阻止
macOS可能会弹出防火墙提示，请点击"允许"。

或手动添加规则：
系统偏好设置 → 安全性与隐私 → 防火墙 → 防火墙选项 → 添加Python

## 开机自启动（可选）

### 方法1：使用launchd

创建文件 `~/Library/LaunchAgents/com.msgcode.smsserver.plist`：
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>Label</key>
    <string>com.msgcode.smsserver</string>
    <key>ProgramArguments</key>
    <array>
        <string>/usr/local/bin/python3</string>
        <string>/Users/你的用户名/IdeaProjects/msgCode/mac-app/server.py</string>
    </array>
    <key>RunAtLoad</key>
    <true/>
    <key>KeepAlive</key>
    <true/>
</dict>
</plist>
```

加载服务：
```bash
launchctl load ~/Library/LaunchAgents/com.msgcode.smsserver.plist
```

### 方法2：添加到登录项

系统偏好设置 → 用户与群组 → 登录项 → 添加启动脚本

