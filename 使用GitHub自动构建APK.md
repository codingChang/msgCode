# 📦 使用 GitHub Actions 自动构建 APK

完全不需要在Mac上配置Android环境！代码推送到GitHub后自动构建APK。

## 🎯 步骤

### 1️⃣ 创建 GitHub 仓库

```bash
cd /Users/apple/IdeaProjects/msgCode

# 初始化 Git（如果还没有的话）
git init

# 添加所有文件
git add .

# 提交
git commit -m "初始提交：短信验证码转发器"
```

### 2️⃣ 推送到 GitHub

在 GitHub 网站上：
1. 登录 github.com
2. 点击右上角 + 号 → New repository
3. 仓库名：`msgCode` 或任意名字
4. 点击 Create repository

然后在Mac终端：
```bash
# 添加远程仓库（替换成你的GitHub用户名）
git remote add origin https://github.com/你的用户名/msgCode.git

# 推送代码
git branch -M main
git push -u origin main
```

### 3️⃣ GitHub 自动开始构建

推送完成后：
1. 打开你的GitHub仓库页面
2. 点击顶部的 "Actions" 标签
3. 会看到正在运行的构建任务
4. 等待3-5分钟

### 4️⃣ 下载构建好的 APK

构建完成后：
1. 在 Actions 页面点击最新的构建任务
2. 滚动到底部，找到 "Artifacts" 区域
3. 点击 `app-debug` 下载 APK
4. 解压下载的zip文件
5. 得到 `app-debug.apk`

### 5️⃣ 安装到手机

把 APK 传到手机：
- 用微信/QQ发给自己
- 用隔空投送（如果手机支持）
- 通过数据线复制

在手机上：
1. 找到APK文件
2. 点击安装
3. 可能需要允许"安装未知来源应用"

---

## 🔄 以后修改代码

修改代码后，重新推送：
```bash
git add .
git commit -m "更新说明"
git push
```

GitHub会自动重新构建APK！

---

## 💡 更简单的方法：手动触发构建

不想每次push都构建？

1. 去GitHub仓库的 Actions 页面
2. 左侧选择 "构建 Android APK"
3. 右侧点击 "Run workflow"
4. 点击绿色的 "Run workflow" 按钮

---

## ⚡ 现在就试试

我已经帮你创建好了构建配置文件：
`.github/workflows/build-apk.yml`

你现在只需要：

1. **初始化Git并推送**
   ```bash
   cd /Users/apple/IdeaProjects/msgCode
   git init
   git add .
   git commit -m "短信验证码转发器 v1.0"
   ```

2. **在GitHub创建仓库**（网页操作）

3. **推送代码**
   ```bash
   git remote add origin https://github.com/你的用户名/仓库名.git
   git push -u origin main
   ```

4. **等待自动构建，然后下载APK！**

---

需要我详细指导哪一步吗？😊

