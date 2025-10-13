# 🚀 快速测试Mac服务器

不用等Android应用，现在就能测试Mac端！

## 第1步：启动服务器（30秒）

```bash
cd /Users/apple/IdeaProjects/msgCode/mac-app
python3 server.py
```

## 第2步：打开网页界面（10秒）

在浏览器打开：http://localhost:5001

你会看到漂亮的验证码接收界面！

## 第3步：用手机测试发送（2分钟）

### 方法1：用手机浏览器测试

1. 查看Mac服务器显示的IP（比如 192.168.31.124）

2. 在手机浏览器打开开发者工具，或者用这个在线工具：
   https://reqbin.com/

3. 发送测试请求：
   ```
   URL: http://192.168.31.124:5001/api/sms
   Method: POST
   Content-Type: application/json
   
   Body:
   {
     "sender": "10086",
     "content": "您的验证码是123456，请勿泄露",
     "timestamp": "1697000000000"
   }
   ```

4. 刷新Mac的浏览器，就能看到消息了！

### 方法2：用curl命令测试（在Mac上）

```bash
curl -X POST http://localhost:5001/api/sms \
  -H "Content-Type: application/json" \
  -d '{
    "sender": "10086",
    "content": "您的验证码是123456，有效期5分钟",
    "timestamp": "1697000000000"
  }'
```

然后刷新浏览器，验证码会自动高亮显示！

---

## Android应用的解决方案

### 最简单：我帮你配置GitHub Actions自动构建

我可以帮你设置GitHub Actions，以后只要把代码推送到GitHub，就会自动构建APK，你直接下载就行！

需要的话我马上帮你配置。

### 最快：下载Android Studio

如果你以后可能会做Android开发，一次性安装Android Studio是最好的选择：

1. 下载：https://developer.android.com/studio
2. 安装（大约10分钟）
3. 打开项目自动构建（第一次5-10分钟）
4. Build → Build APK

### 最灵活：使用在线编译服务

有一些在线Android编译服务，不需要本地配置：
- Appetize.io
- CircleCI
- Travis CI

---

## 🎯 你现在可以这样做

1. **先测试Mac端**（确保功能正常）
   ```bash
   cd /Users/apple/IdeaProjects/msgCode/mac-app
   python3 server.py
   ```

2. **之后选择一个Android方案**：
   - 让我配置GitHub Actions自动构建
   - 或者安装Android Studio自己构建
   - 或者找个已有Android Studio的朋友帮忙

你想先测试Mac端，还是直接处理Android应用的问题？

