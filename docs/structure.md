# 项目架构（Structure）

> **版本**：v0.2（2026-08-30）
> 本文件描述"多 API 中转站管理 APP"项目的目录结构、模块划分与关键入口
> 变更记录：v0.1 初始；v0.2 扩展 UI 屏幕清单、新增 .github/workflows、assets/ 二进制位置、components 通用组件

## 目录结构

```
/data/data/com.ai.assistance.operit/files/workspace/newapi/
├── README.md                          # 项目入口
├── docs/                              # ★ 项目级核心文档（skill 要求）
│   ├── goal.md                        # ★ 总目标（已确认）
│   ├── plan.md                        # ★ 实施计划（草案 v0.1，待确认）
│   ├── rules.md                       # ★ 编码规范（含项目新增规则）
│   └── structure.md                   # ★ 本文件
│
├── newapi-src/                        # 第三方 New API 源码（参考资料，不修改）
│   ├── main.go                        # 含 //go:embed web/dist
│   ├── router/                        # 路由定义
│   ├── controller/                    # 控制器
│   ├── service/                       # 业务层
│   ├── middleware/                    # 中间件（鉴权等）
│   ├── docs/openapi/                  # 调研产出的 7 份接口文档
│   │   ├── README.md
│   │   ├── 01_auth.md
│   │   ├── 02_admin_api.md
│   │   ├── 02_admin_api_extra.md
│   │   ├── 03_relay_api.md
│   │   ├── 04_deploy_embedded.md
│   │   ├── 05_app_design.md
│   │   ├── API_SUMMARY.txt
│   │   ├── api.json                   # 官方 OpenAPI 规范（136 路径）
│   │   └── relay.json                 # 官方 OpenAPI 规范（35 路径）
│   └── ...                            # 其他 New API 源码
│
├── app/                               # ★ APP 源码（M3 开始创建）
│   ├── build.gradle.kts               # 模块级 Gradle 配置
│   ├── src/main/
│   │   ├── AndroidManifest.xml        # 权限声明
│   │   ├── java/com/example/newapi_mobile/
│   │   │   ├── MainActivity.kt        # 唯一 Activity（Compose 容器）
│   │   │   ├── NewApiApplication.kt   # Application 类
│   │   │   ├── process/               # 进程管理子包
│   │   │   │   ├── BinaryManager.kt
│   │   │   │   ├── ProcessManager.kt
│   │   │   │   ├── HealthChecker.kt
│   │   │   │   ├── LogCapture.kt
│   │   │   │   └── LifecycleController.kt
│   │   │   ├── service/               # 前台服务
│   │   │   │   └── NewApiService.kt
│   │   │   ├── api/                   # 后端 API 客户端
│   │   │   │   ├── NewApiClient.kt
│   │   │   │   ├── interceptor/
│   │   │   │   │   └── AuthInterceptor.kt
│   │   │   │   ├── model/             # 请求/响应 DTO（按需扩展）
│   │   │   │   │   ├── LoginRequest.kt
│   │   │   │   │   ├── User.kt
│   │   │   │   │   ├── Channel.kt
│   │   │   │   │   ├── Token.kt
│   │   │   │   │   ├── Model.kt
│   │   │   │   │   ├── Log.kt
│   │   │   │   │   ├── Dashboard.kt
│   │   │   │   │   └── Redemption.kt
│   │   │   │   └── service/           # API 接口定义（按需扩展）
│   │   │   │       ├── AuthApi.kt
│   │   │   │       ├── ChannelApi.kt
│   │   │   │       ├── TokenApi.kt
│   │   │   │       ├── ModelApi.kt
│   │   │   │       ├── LogApi.kt
│   │   │   │       ├── DashboardApi.kt
│   │   │   │       ├── RedemptionApi.kt
│   │   │   │       └── UserApi.kt
│   │   │   ├── auth/                  # 凭证管理
│   │   │   │   ├── SessionManager.kt  # PAT 存储 + 自动注入
│   │   │   │   └── AuthRepository.kt
│   │   │   ├── ui/                    # Compose UI（**按需扩展屏幕**）
│   │   │   │   ├── theme/             # Material 3 主题
│   │   │   │   │   ├── Color.kt
│   │   │   │   │   ├── Theme.kt       # 支持亮/暗/动态色
│   │   │   │   │   └── Type.kt
│   │   │   │   ├── components/        # 通用组件
│   │   │   │   │   ├── StatusCard.kt
│   │   │   │   │   ├── SkeletonList.kt
│   │   │   │   │   ├── EmptyState.kt
│   │   │   │   │   ├── ErrorState.kt
│   │   │   │   │   └── BottomBar.kt
│   │   │   │   ├── nav/               # 导航
│   │   │   │   │   └── NavGraph.kt    # Navigation Compose
│   │   │   │   ├── onboarding/        # 启动引导
│   │   │   │   │   └── OnboardingScreen.kt
│   │   │   │   ├── home/              # 主页（启停）
│   │   │   │   │   └── HomeScreen.kt
│   │   │   │   ├── login/             # 登录
│   │   │   │   │   └── LoginScreen.kt
│   │   │   │   ├── dashboard/         # 仪表盘
│   │   │   │   │   └── DashboardScreen.kt
│   │   │   │   ├── channels/          # 渠道管理
│   │   │   │   │   ├── ChannelsScreen.kt
│   │   │   │   │   ├── ChannelDetailScreen.kt
│   │   │   │   │   └── ChannelEditDialog.kt
│   │   │   │   ├── tokens/            # Token 管理
│   │   │   │   │   ├── TokensScreen.kt
│   │   │   │   │   └── TokenCreateDialog.kt
│   │   │   │   ├── models/            # 模型市场
│   │   │   │   │   └── ModelsScreen.kt
│   │   │   │   ├── logs/              # 日志
│   │   │   │   │   └── LogsScreen.kt
│   │   │   │   ├── users/             # 用户管理（多账号场景）
│   │   │   │   │   └── UsersScreen.kt
│   │   │   │   ├── redemption/        # 兑换码（自用）
│   │   │   │   │   └── RedemptionScreen.kt
│   │   │   │   └── settings/          # 设置
│   │   │   │       ├── SettingsScreen.kt
│   │   │   │       ├── ThemeSettingsScreen.kt
│   │   │   │       └── BackupSettingsScreen.kt
│   │   │   ├── webview/               # WebView 兜底
│   │   │   │   └── WebViewActivity.kt
│   │   │   ├── config/                # 全局配置
│   │   │   │   └── AppConfig.kt
│   │   │   └── util/                  # 工具类
│   │   │       ├── NetworkUtil.kt
│   │   │       ├── IpAddressUtil.kt
│   │   │       └── ErrorAnalyzer.kt
│   │   ├── assets/
│   │   │   ├── new-api-arm64          # ★ New API 后端二进制（CI 编译注入）
│   │   │   └── ca-certificates.crt    # CA 证书（HTTPS 上游调用）
│   │   └── res/
│   │       ├── values/
│   │       │   ├── strings.xml
│   │       │   ├── colors.xml
│   │       │   └── themes.xml
│   │       ├── drawable/              # 空状态插画等
│   │       ├── anim/                  # 过渡动画
│   │       └── mipmap-*/              # APP 图标
│   ├── src/test/                      # 单元测试
│   │   └── java/com/example/newapi_mobile/
│   │       ├── process/ProcessManagerTest.kt
│   │       ├── process/HealthCheckerTest.kt
│   │       └── api/AuthInterceptorTest.kt
│   └── src/androidTest/               # 仪器测试（CI 跑 UI 截图）
│
├── .github/
│   └── workflows/
│       ├── build.yml                  # ★ 主流程：编译二进制 + APK + 发 Release
│       └── lint.yml                   # 可选：ktlint + detekt
│
├── gradle/                            # Gradle Wrapper
│   └── wrapper/
├── build.gradle.kts                   # 项目级 Gradle
├── settings.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
├── local.properties                   # SDK 路径（gitignore）
│
└── scripts/                           # 辅助脚本（开发时用，CI 不依赖）
    ├── build-backend.sh               # 编译 ARM64 二进制
    └── copy-binary-to-app.sh          # 把二进制拷到 app/src/main/assets/
```

## 模块划分

### 1. process/ — 后端进程管理（最关键）

**职责**：负责把 New API 二进制释放、启动、监控、停止

```
BinaryManager
├─ 职责：assets/new-api-arm64 → filesDir/data/bin/new-api-arm64 + chmod 755
├─ 依赖：仅 Context
└─ 状态：单例

ProcessManager
├─ 职责：用 ProcessBuilder 启动二进制，注入环境变量
├─ 依赖：BinaryManager、AppConfig
├─ 状态：保存当前 Process 句柄
└─ 关键 API：start() / stop() / isRunning()

HealthChecker
├─ 职责：每 1 秒轮询 http://127.0.0.1:&lt;port&gt;/api/status
├─ 超时：30 秒
├─ 输出：Flow&lt;HealthState&gt;
└─ 依赖：OkHttp

LogCapture
├─ 职责：读 process.stdout/stderr 实时推送给 UI
├─ 输出：Flow&lt;String&gt;
├─ 容量：保留最近 1000 行
└─ 依赖：ProcessManager

LifecycleController
├─ 职责：统一管理"启动"和"停止"的完整流程
├─ 串联：BinaryManager + ProcessManager + HealthChecker + 通知
└─ 依赖：上述所有
```

### 2. service/ — 安卓前台服务

**职责**：保持 New API 进程在后台运行

```
NewApiService : Service
├─ 类型：前台服务
├─ onStartCommand：START_STICKY（系统会重启）
├─ 通知：channel = "newapi_service"，内容显示"New API 正在运行 (端口 13000)"
├─ 绑定：通过 bindService 暴露 IPC
└─ 关键：禁止在 onDestroy 时杀进程（除非用户主动 stop）
```

### 3. api/ — 后端 API 客户端

**职责**：调 New API 后端的 170+ 个 HTTP 接口

```
NewApiClient (OkHttp + Retrofit)
├─ baseUrl：http://127.0.0.1:&lt;port&gt;
├─ 拦截器：AuthInterceptor（自动注入 PAT）
├─ 超时：connect 10s / read 60s / call 0（无超时，让 SSE 流式工作）
└─ 复用：单例

服务接口（按业务分文件）：
- AuthApi：login / generatePat / getSelf
- ChannelApi：list / get / create / update / delete / test
- TokenApi：list / create / delete / search
- ModelApi：list / listEnabled
- LogApi：self / selfStat / search
```

### 4. auth/ — 凭证管理

**职责**：登录获取 PAT、存 PAT、自动注入

```
SessionManager（基于 EncryptedSharedPreferences）
├─ save(pat: String, baseUrl: String)
├─ load(): Session?
├─ clear()
└─ 字段：pat, baseUrl, userId, username

AuthRepository
├─ login(username, password): Session
├─ generatePat(): String
└─ 依赖：AuthApi + SessionManager
```

### 5. ui/ — Compose UI

**职责**：6 个屏幕（按 plan.md §M5）

```
NavGraph（导航图）
├─ home（主页：启停 + 状态）
├─ logs（日志：实时流）
├─ login（登录：账号密码或 PAT）
├─ channels（渠道：列表 + 编辑）
├─ tokens（令牌：列表 + 创建）
└─ models（模型：列表 + 复制 endpoint）
```

## 数据流/调用关系

### 启动后端流程

```
用户点 "启动" 按钮 (HomeScreen)
  → HomeViewModel.onStartClicked()
    → LifecycleController.start()
      → BinaryManager.ensureBinary()  // 检查/释放二进制
      → ProcessManager.start(env)      // spawn 进程
      → HealthChecker.start()           // 开始轮询
        → emit HealthState.Starting → Running
      → NewApiService.startForeground() // 启动前台服务
      → LogCapture.start()              // 捕获日志
```

### 登录流程

```
用户输入用户名密码 (LoginScreen)
  → LoginViewModel.login()
    → AuthRepository.login(user, pass)
      → AuthApi.login()
        → POST /api/user/login {username, password}
        → 返回 {access_token, user}
      → AuthRepository.generatePat()
        → AuthApi.generatePat() // GET /api/user/token (用 access_token)
        → 返回永久 PAT
      → SessionManager.save(pat, baseUrl, user)
```

### API 调用流程

```
UI 调用 (e.g. ViewModel.loadChannels())
  → ChannelsRepository.list()
    → ChannelApi.list() // Retrofit 自动加 Authorization: Bearer &lt;PAT&gt;
      → AuthInterceptor: SessionManager.load()?.pat ?: throw
    → GET /api/channel/
    → 反序列化 Channel[]
```

## 依赖与外部接口

### 安卓内部依赖

| 库 | 用途 | 版本 |
|---|---|---|
| Jetpack Compose | UI 框架 | BOM 2024.10.00 |
| Material 3 | 设计系统 | (随 Compose BOM) |
| Navigation Compose | 屏幕导航 | 2.8.x |
| Lifecycle ViewModel | UI 状态管理 | 2.8.x |
| Coroutines | 异步 | 1.8.x |
| Retrofit | HTTP 客户端 | 2.11 |
| OkHttp | 网络底层 | 4.12 |
| Moshi | JSON 解析 | 1.15 |
| EncryptedSharedPreferences | 凭证加密 | androidx.security:1.1.0-alpha06 |
| DataStore | 配置存储 | 1.1.x |

### 外部接口

| 接口 | 用途 |
|---|---|
| New API 后端（HTTP） | 全部业务功能（详见 `newapi-src/docs/openapi/`） |
| 安卓系统：Service / Activity / PackageManager | 进程管理、权限 |

## 关键入口文件

| 文件 | 作用 | 何时被调用 |
|---|---|---|
| `MainActivity.kt` | 唯一 Activity | APP 启动 |
| `NewApiApplication.kt` | 全局初始化 | APP 启动 |
| `NewApiService.kt` | 前台服务 | 用户点"启动" |
| `LifecycleController.kt` | 启停统一入口 | UI/服务调用 |
| `ProcessManager.kt` | spawn 进程 | 启动时 |
| `HealthChecker.kt` | 轮询 /api/status | 启动后持续 |
| `SessionManager.kt` | PAT 加密存取 | 登录/每次 API 调用 |
| `HomeScreen.kt` | 主屏入口 | 始终在导航栈 |
| `build-backend.sh` | 编译后端 | 开发时执行 |

## 高风险模块

按风险等级排序：

### 🔴 高风险
- **ProcessManager**：spawn 进程涉及 ANR、僵尸进程、文件描述符泄露
- **NewApiService**：保活失败直接导致后端死亡
- **BinaryManager**：二进制写入失败 → APP 完全不可用

### 🟡 中风险
- **HealthChecker**：超时设置不当影响启动体验
- **SessionManager**：加密失败 → 无法登录
- **AuthInterceptor**：PAT 缺失处理不当 → 全 APP 崩溃

### 🟢 低风险
- **UI 各屏**：纯 Compose，可视化问题容易修
- **API 客户端**：Retrofit 自动处理大部分异常
- **WebView 兜底**：纯展示