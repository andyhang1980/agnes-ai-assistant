# 阿福影音工具箱（Agnes AI 助手）

纯前端单文件 AI 影音工具应用：对话、生图、生视频、短剧、**一键成片**、**Edge TTS 语音朗读/配音**。
支持双服务商：**Agnes**（生图/生视频/全功能）与 **商汤 SenseNova**（文本模型，多 Key 并发）。

- 网页版：单文件 `index.html`，零依赖、可直接丢到任意静态主机
- 移动端：同一份 `index.html` 打包进 Android WebView APK（支持 Edge TTS 官方音色）

---

## 功能

| 模块 | 说明 |
| --- | --- |
| 对话 | 流式多轮对话、附件（图片/文档）、复制、朗读 |
| 文生图 / 图生图 | 多种风格、尺寸、画质，参考图上传 |
| 文生视频 / 单图生视频 / 多图生视频 / 首尾帧 | 按模型官方时长（5/10/12、5/10/15/18 秒） |
| 生成短剧 | 剧本 → 分镜 → 出图 → 合成，多线程批量 |
| 一键成片 | 自动分镜 + 素材(实拍/AI) + **Edge TTS 配音** + 字幕 + 视频导出，自动缓存历史成片 |
| 语音朗读 | 26 种 Edge TTS 音色、语速/音调、试听；浏览器不支持时自动降级系统语音 |
| 素材库 | Pexels / Pixabay 免费实拍素材检索 |
| API Key 池 | 多 Key 轮询、冷却、并发线程、失效自动探测与切换（Agnes 与商汤各自独立） |

---

## 技术栈与约束

- 纯 HTML/CSS/JavaScript，单文件内联，无构建步骤
- 配置与数据存浏览器 `localStorage`（**不需要数据库**）
- 所有请求由浏览器直连服务商 API
- **Edge TTS 朗读/配音依赖安全上下文（HTTPS 或 `file://`）**：HTTP 下 `crypto.subtle` 不可用，朗读会自动降级为系统语音，一键成片配音会提示失败

---

## 部署

### 方式一：Vercel（Git 集成自动部署，推荐）

1. 把仓库推送到 GitHub
2. 在 https://vercel.com 导入项目（Framework preset 选 **Other**）
3. 之后每次 `push master` 自动部署

CLI 方式：

```bash
npm i -g vercel
vercel --prod
```

### 方式二：任意静态主机（免费主机 / VPS / cPanel）

把 `index.html` 上传到网站根目录即可（**只上传这一个文件**）。

```bash
# FTP 上传示例（把主机/账号/密码换成你自己的）
curl -T index.html ftp://<ftp-host>:21/<web-root>/index.html --user <user>:<password>

# 本地验证
python -m http.server 8080     # 或任意静态服务器
```

常见网站根目录：`public_html`、`www`、`htdocs`、`web`。

Nginx 示例：

```nginx
server {
    listen 80;
    server_name example.com;
    root /var/www/html;      # index.html 所在目录
    index index.html;
    location / { try_files $uri $uri/ /index.html; }
}
```

Apache/cPanel：上传到 `public_html` 即可；如需伪静态可用 `.htaccess`：

```apache
DirectoryIndex index.html
```

> ⚠️ **务必开启 HTTPS**（控制面板「免费 SSL / Let's Encrypt」即可）。HTTP 下 Edge TTS 与复制按钮不可用。

### 不要上传 / 不要提交的内容

- `android-app/keystore/`、`android-app/keystore.properties`（签名密钥与密码）
- `android-app/local.properties`（本机 SDK 路径）
- `android-app/app/build/`、`.gradle/`、构建产物与 `*.apk`

---

## Android APK 构建与安装

### 环境

- JDK 17（示例：`D:\AI_Tools\jdk-17.0.19`）
- Android SDK（示例：`D:\Android\Sdk`），`compileSdk 34`、`minSdk 26`
- Gradle 8.7（`gradle wrapper` 或本机 gradle）

### 签名配置（不提交仓库）

在 `android-app/keystore.properties` 中填写（该文件已被 `.gitignore` 忽略）：

```properties
storeFile=../keystore/toolbox.keystore
storePassword=<你的签名密码>
keyAlias=toolbox
keyPassword=<你的签名密码>
```

也可用环境变量 `KS_STORE_PASSWORD` / `KS_KEY_PASSWORD`。

### 构建

```bash
# 1. 先把最新的网页文件同步进 App 资源
cp index.html android-app/app/src/main/assets/index.html

# 2. 构建正式签名包
cd android-app
gradle assembleRelease
# 产物：app/build/outputs/apk/release/app-release.apk
```

### 安装到手机

```bash
adb install -r app-release.apk
```

- 包名固定为 `com.freekit.toolbox`（改包名会导致无法覆盖安装）
- App 内通过 `file:///android_asset/index.html` 加载页面，并把 WebView UA 追加 `Edg/143.0.0.0`，**Edge TTS 官方音色在 App 内可用**

---

## 配置说明

### Agnes（默认）

- **服务商**：Agnes
- **API Key 池**：每行一个，支持导入 TXT、测试全部、清空
- **参数**：自动检测（探测空闲 Key）、检测间隔、冷却时间、并发线程（自动/手动）
- **Base URL**：国际版 `https://apihub.agnes-ai.com/v1`、国内版 `https://api.agnes-ai.cn/v1`

### 商汤 SenseNova

- **服务商**：商汤 SenseNova（仅对话 / 一键成片等文本功能；生图、生视频仍走 Agnes）
- **API Key 池**：`sk-` 开头的密钥，控制台创建：https://platform.sensenova.cn/console/keys
- **Base URL**：`https://token.sensenova.cn/v1`
- **可用模型**：`sensenova-6.8-flash-lite`、`sensenova-u1.5-lite`、`sensenova-u1.5-fast`、`deepseek-v4-flash`、`deepseek-flash`、`glm-5.2`、`kimi-k3`
- 多 Key 与并发/冷却/自动探测机制与 Agnes 相同，两套池互不影响

---

## 文件结构

```
.
├── index.html                 # 主应用（唯一需要部署的文件）
├── vercel.json                # Vercel 部署配置
├── README.md
└── android-app/               # Android WebView 壳工程
    ├── app/build.gradle       # 签名配置从 keystore.properties 读取
    ├── keystore.properties    # 本地签名信息（已 gitignore）
    └── app/src/main/
        ├── assets/index.html  # 与根目录 index.html 保持一致
        ├── java/com/freekit/toolbox/MainActivity.java
        └── res/values/strings.xml   # 应用名「阿福影音工具箱」
```

---

## 常见问题

| 现象 | 原因 / 处理 |
| --- | --- |
| 一键成片没有声音 | ① 确认设置里「配音」为开启；② 桌面 Chrome 无 `Edg/` UA 时 Edge TTS 不可用，改用 Edge 浏览器或手机 App；③ 若弹出解码失败提示，请反馈日志（App 内 `FreeKitJS` tag） |
| 朗读没变化 | 换音色后需重新点击；面板会提示当前引擎（Edge TTS / 系统语音）。Chrome 下建议用 Edge 浏览器获得 26 种音色 |
| 保存后 Key 丢失 | 确认在「设置 → 服务商」里填写对应 Key 池（商汤 Key 与 Agnes Key 分开保存） |
| 分镜解析失败 | 已兼容 `{"data":[...]}` 等包装格式；仍失败时查看提示中的模型返回开头 |
| 手机端点顶栏「语音」无反应 | 旧版本问题，升级到最新版（弹窗已改为挂载到 `body` 定位） |

---

## 安全提示

- API Key 仅保存在本机浏览器/设备，不上传服务器
- 仓库不包含任何密钥、签名文件或数据库口令；请勿把真实凭据写进代码或提交到 Git