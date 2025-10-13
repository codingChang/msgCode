#!/usr/bin/env python3
"""
Mac端短信接收服务器
监听本地网络上的短信推送请求
"""

from flask import Flask, request, jsonify, render_template_string
from flask_cors import CORS
from datetime import datetime
import socket
import json

app = Flask(__name__)
CORS(app)

# 存储最近的短信记录
messages = []
MAX_MESSAGES = 50

def get_local_ip():
    """获取本机IP地址"""
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
        s.close()
        return ip
    except:
        return "127.0.0.1"

@app.route('/')
def index():
    """显示Web UI"""
    return render_template_string(HTML_TEMPLATE)

@app.route('/api/sms', methods=['POST'])
def receive_sms():
    """接收安卓端发送的短信"""
    try:
        data = request.get_json()
        
        message = {
            'sender': data.get('sender', '未知'),
            'content': data.get('content', ''),
            'timestamp': data.get('timestamp', datetime.now().isoformat()),
            'received_at': datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        }
        
        # 添加到消息列表
        messages.insert(0, message)
        
        # 保持最多50条消息
        if len(messages) > MAX_MESSAGES:
            messages.pop()
        
        print(f"[{message['received_at']}] 收到短信 - 发件人: {message['sender']}")
        print(f"内容: {message['content']}")
        
        return jsonify({
            'status': 'success',
            'message': '短信接收成功'
        }), 200
        
    except Exception as e:
        print(f"错误: {str(e)}")
        return jsonify({
            'status': 'error',
            'message': str(e)
        }), 400

@app.route('/api/messages', methods=['GET'])
def get_messages():
    """获取最近的短信列表"""
    return jsonify({
        'status': 'success',
        'messages': messages
    })

@app.route('/api/clear', methods=['POST'])
def clear_messages():
    """清空消息列表"""
    messages.clear()
    return jsonify({
        'status': 'success',
        'message': '消息已清空'
    })

# Web UI HTML模板
HTML_TEMPLATE = """
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>短信验证码接收器</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }
        
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB', sans-serif;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            min-height: 100vh;
            padding: 20px;
        }
        
        .container {
            max-width: 800px;
            margin: 0 auto;
        }
        
        .header {
            background: white;
            border-radius: 15px;
            padding: 30px;
            margin-bottom: 20px;
            box-shadow: 0 10px 30px rgba(0,0,0,0.2);
        }
        
        h1 {
            color: #667eea;
            font-size: 28px;
            margin-bottom: 10px;
        }
        
        .ip-info {
            background: #f0f4ff;
            padding: 15px;
            border-radius: 10px;
            margin-top: 15px;
        }
        
        .ip-info p {
            color: #555;
            margin: 5px 0;
        }
        
        .ip-address {
            font-family: 'Courier New', monospace;
            color: #667eea;
            font-weight: bold;
            font-size: 18px;
        }
        
        .controls {
            background: white;
            border-radius: 15px;
            padding: 20px;
            margin-bottom: 20px;
            box-shadow: 0 10px 30px rgba(0,0,0,0.2);
            display: flex;
            gap: 10px;
        }
        
        button {
            flex: 1;
            padding: 12px 24px;
            border: none;
            border-radius: 8px;
            font-size: 16px;
            cursor: pointer;
            transition: all 0.3s;
            font-weight: 500;
        }
        
        .btn-refresh {
            background: #667eea;
            color: white;
        }
        
        .btn-refresh:hover {
            background: #5568d3;
            transform: translateY(-2px);
            box-shadow: 0 5px 15px rgba(102, 126, 234, 0.4);
        }
        
        .btn-clear {
            background: #ef4444;
            color: white;
        }
        
        .btn-clear:hover {
            background: #dc2626;
            transform: translateY(-2px);
            box-shadow: 0 5px 15px rgba(239, 68, 68, 0.4);
        }
        
        .messages-container {
            background: white;
            border-radius: 15px;
            padding: 20px;
            box-shadow: 0 10px 30px rgba(0,0,0,0.2);
            min-height: 400px;
        }
        
        .message-card {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            border-radius: 12px;
            padding: 20px;
            margin-bottom: 15px;
            color: white;
            animation: slideIn 0.3s ease;
            position: relative;
            overflow: hidden;
        }
        
        @keyframes slideIn {
            from {
                opacity: 0;
                transform: translateY(-20px);
            }
            to {
                opacity: 1;
                transform: translateY(0);
            }
        }
        
        .message-card::before {
            content: '';
            position: absolute;
            top: -50%;
            right: -50%;
            width: 200%;
            height: 200%;
            background: radial-gradient(circle, rgba(255,255,255,0.1) 0%, transparent 70%);
        }
        
        .message-header {
            display: flex;
            justify-content: space-between;
            margin-bottom: 12px;
            font-size: 14px;
            opacity: 0.9;
        }
        
        .sender {
            font-weight: bold;
            font-size: 16px;
        }
        
        .message-content {
            background: rgba(255,255,255,0.2);
            padding: 15px;
            border-radius: 8px;
            font-size: 16px;
            line-height: 1.6;
            word-wrap: break-word;
            backdrop-filter: blur(10px);
        }
        
        .verification-code {
            background: rgba(255,255,255,0.3);
            padding: 8px 12px;
            border-radius: 6px;
            font-weight: bold;
            font-size: 20px;
            letter-spacing: 2px;
            display: inline-block;
            margin: 5px 0;
            font-family: 'Courier New', monospace;
        }
        
        .empty-state {
            text-align: center;
            padding: 60px 20px;
            color: #999;
        }
        
        .empty-state svg {
            width: 100px;
            height: 100px;
            margin-bottom: 20px;
            opacity: 0.3;
        }
        
        .status-indicator {
            display: inline-block;
            width: 10px;
            height: 10px;
            background: #10b981;
            border-radius: 50%;
            margin-right: 8px;
            animation: pulse 2s infinite;
        }
        
        @keyframes pulse {
            0%, 100% {
                opacity: 1;
            }
            50% {
                opacity: 0.5;
            }
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>📱 短信验证码接收器</h1>
            <p style="color: #666; margin-top: 10px;">
                <span class="status-indicator"></span>
                服务运行中
            </p>
            <div class="ip-info">
                <p><strong>本机IP地址：</strong><span class="ip-address" id="ipAddress">加载中...</span></p>
                <p><strong>端口：</strong><span class="ip-address" id="portNumber">5001</span></p></p>
                <p style="margin-top: 10px; font-size: 14px;">
                    ℹ️ 请在安卓应用中配置此IP地址
                </p>
            </div>
        </div>
        
        <div class="controls">
            <button class="btn-refresh" onclick="loadMessages()">🔄 刷新</button>
            <button class="btn-clear" onclick="clearMessages()">🗑️ 清空</button>
        </div>
        
        <div class="messages-container">
            <div id="messagesList">
                <div class="empty-state">
                    <svg viewBox="0 0 24 24" fill="currentColor">
                        <path d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H6l-2 2V4h16v12z"/>
                    </svg>
                    <p>暂无短信</p>
                    <p style="font-size: 14px; margin-top: 10px;">等待安卓设备发送短信...</p>
                </div>
            </div>
        </div>
    </div>
    
    <script>
        // 获取并显示IP地址和端口
        fetch('/api/messages')
            .then(response => response.json())
            .then(data => {
                // IP地址会在Python中获取，这里简化处理
                document.getElementById('ipAddress').textContent = window.location.hostname;
                document.getElementById('portNumber').textContent = window.location.port || '5001';
            });
        
        // 加载消息
        function loadMessages() {
            fetch('/api/messages')
                .then(response => response.json())
                .then(data => {
                    displayMessages(data.messages);
                })
                .catch(error => {
                    console.error('加载消息失败:', error);
                });
        }
        
        // 显示消息
        function displayMessages(messages) {
            const container = document.getElementById('messagesList');
            
            if (messages.length === 0) {
                container.innerHTML = `
                    <div class="empty-state">
                        <svg viewBox="0 0 24 24" fill="currentColor">
                            <path d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H6l-2 2V4h16v12z"/>
                        </svg>
                        <p>暂无短信</p>
                        <p style="font-size: 14px; margin-top: 10px;">等待安卓设备发送短信...</p>
                    </div>
                `;
                return;
            }
            
            container.innerHTML = messages.map(msg => {
                const content = highlightVerificationCode(msg.content);
                return `
                    <div class="message-card">
                        <div class="message-header">
                            <span class="sender">📱 ${msg.sender}</span>
                            <span>${msg.received_at}</span>
                        </div>
                        <div class="message-content">${content}</div>
                    </div>
                `;
            }).join('');
        }
        
        // 高亮验证码
        function highlightVerificationCode(content) {
            // 匹配常见的验证码模式（4-8位数字）
            return content.replace(/(\d{4,8})/g, '<span class="verification-code">$1</span>');
        }
        
        // 清空消息
        function clearMessages() {
            if (confirm('确定要清空所有消息吗？')) {
                fetch('/api/clear', { method: 'POST' })
                    .then(response => response.json())
                    .then(data => {
                        loadMessages();
                    })
                    .catch(error => {
                        console.error('清空消息失败:', error);
                    });
            }
        }
        
        // 自动刷新
        setInterval(loadMessages, 2000);
        
        // 初始加载
        loadMessages();
    </script>
</body>
</html>
"""

if __name__ == '__main__':
    import sys
    
    # 支持自定义端口，默认5001（避免macOS AirPlay占用5000）
    port = 5001
    if len(sys.argv) > 1:
        try:
            port = int(sys.argv[1])
        except ValueError:
            print("❌ 端口号必须是数字")
            sys.exit(1)
    
    local_ip = get_local_ip()
    print("=" * 50)
    print("📱 短信验证码接收服务器")
    print("=" * 50)
    print(f"本机IP地址: {local_ip}")
    print(f"访问地址: http://{local_ip}:{port}")
    print(f"本地访问: http://localhost:{port}")
    print("=" * 50)
    print("请在安卓应用中配置上述IP地址和端口")
    print("按 Ctrl+C 停止服务器")
    print("=" * 50)
    print(f"💡 提示：可以用 python3 server.py <端口号> 指定端口")
    print("=" * 50)
    
    try:
        app.run(host='0.0.0.0', port=port, debug=False)
    except OSError as e:
        if "Address already in use" in str(e):
            print(f"\n❌ 错误：端口 {port} 已被占用！")
            print(f"💡 解决方案：")
            print(f"   1. 使用其他端口: python3 server.py {port+1}")
            print(f"   2. 查看占用进程: lsof -i :{port}")
            if port == 5000:
                print(f"   3. macOS用户：关闭AirPlay Receiver")
                print(f"      系统设置 → 通用 → 隔空播放与接力 → 关闭隔空播放接收器")
        else:
            print(f"\n❌ 错误：{e}")
        sys.exit(1)

