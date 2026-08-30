# 编码规范（Rules）

> **版本**：v0.2（2026-08-30）
> 基于 skill 默认模板 + 本项目新增规则
> 变更记录：v0.1 初始；v0.2 增加 UI 现代化原则、CI 编译原则、更新接口裁剪清单

## 默认规则（创建即生效）
- 程序设计要分层（如：接口层 / 业务层 / 数据访问层），避免把业务逻辑堆在入口文件。
- 配置信息要放在统一位置（如 `config` 模块或配置文件），禁止在代码中散落硬编码。
- 关键流程必须有明确错误处理，禁止静默吞错。
- 对外输入必须先校验（参数、文件、请求体），再进入业务逻辑。
- 函数保持单一职责，复杂函数应拆分为小函数。
- 变更前先确认范围，变更后必须有最小可复现验证。

## 命名规范
- 使用有语义的命名，禁止 `tmp`、`test1` 等无意义命名进入正式代码。
- Kotlin 类名：PascalCase（`NewApiService`）
- Kotlin 文件名：与主类同名（`NewApiService.kt`）
- 包名全小写，分层用 `.`（`com.example.newapi_mobile.process`）
- Android 资源：`snake_case`（`activity_main.xml`、`ic_launcher.xml`）
- 常量：`UPPER_SNAKE_CASE`（`const val DEFAULT_PORT = 13000`）

## 代码风格
- 保持一致的格式化风格；优先小函数、低耦合。
- Kotlin：使用官方 Kotlin 代码风格（`editorconfig` + `ktlint`）
- 行宽：120 字符
- 缩进：4 空格（不用 Tab）
- Compose 函数全部用 `PascalCase` + `@Composable` 注解
- 字符串模板优先，避免 + 拼接
- 集合操作优先用 `forEach/map/filter`，避免 Java 风格

## 错误处理
- 错误信息应可定位（包含上下文），并返回明确失败状态。
- 后端启动失败：必须捕获并显示具体原因（端口占用/数据库锁/权限错误），参考 `electron/main.js` 的 `analyzeError`
- API 调用失败：保留 HTTP code + 响应 body，前端展示用户友好消息
- 进程管理失败：必须记录完整异常栈，不允许 `catch (e: Exception) {}` 静默吞掉
- 协程异常：使用 `CoroutineExceptionHandler` 全局兜底

## 日志与注释
- 日志记录关键路径；注释写"为什么"，不重复"做了什么"。
- Logcat 标签：使用 `NewApi/` 前缀（`Log.d("NewApi/Process", "...")`）
- 用户敏感信息（PAT、密码）禁止写入日志
- 注释用 `//` 而非 `/* */`，中文 OK
- 公开函数必须有 KDoc

## 安全与隐私
- 禁止提交密钥、令牌、密码等敏感信息。
- PAT 存 EncryptedSharedPreferences（AES256_GCM）
- 后端 `SESSION_SECRET` 存 DataStore，启动时自动生成 32 字节随机值
- 清除缓存/数据时同步清理内存中的 PAT
- 二进制不打包敏感配置

## 测试与回归要求
- 至少覆盖主流程与一个失败分支。
- ProcessManager 单元测试：mock ProcessBuilder，验证环境变量和参数
- HealthChecker 单元测试：mock 各种 HTTP 状态
- 关键 UI 流程手动验收：启动→登录→看渠道→停止

## 禁止事项（反模式）
- 禁止跨层直接调用导致耦合失控。
- 禁止在主线程做文件 I/O、网络请求
- 禁止把 PAT 写到日志或 SharedPreferences（非加密版）
- 禁止在 Compose 中直接持有 Activity 引用（用 `LocalContext.current`）
- 禁止硬编码 IP/端口/密钥（必须用配置）
- 禁止不写 await 调挂起函数
- 禁止把二进制文件存到外部存储（用 `filesDir`）
- 禁止在 stop 后还发请求到后端

## 本项目新增规则（来自 v0.1 实施经验）

### 后端二进制管理
- 编译命令固定：`CGO_ENABLED=0 GOOS=linux GOARCH=arm64 go build -ldflags="-s -w" -o new-api-arm64`
- 禁止 `armv7`、`x86_64`（用户只做 arm64）
- 二进制首次启动从 `assets/` 拷到 `filesDir/data/bin/`，加版本号标记以支持 OTA 升级
- 数据库文件固定位置：`filesDir/data/new-api.db`

### APP 进程管理
- 启动后端必须用前台服务，**禁止**用普通 Service（会被秒杀）
- 通知 channel ID 固定 `newapi_service`
- 优雅关停：先 SIGTERM，等 3 秒，再 SIGKILL
- 健康检查超时 30 秒（不是 60 秒，节省电量）

### UI 现代化原则（**v0.2 更新**）
- 屏幕数不硬性限制，**按后端功能自然拆分**（用户决定）
- 每个屏幕核心操作 ≤ 3 个主按钮（次要操作放菜单/弹窗）
- 长按 / 滑动等隐藏交互**禁止**使用（用户非开发者）
- Material 3 + Jetpack Compose
- 强制要求：
  - 亮色 / 暗色主题自适应（跟随系统）
  - 关键过渡用 `AnimatedContent` / `Crossfade` / `AnimatedVisibility`
  - 列表必须有骨架屏（`shimmer`）或进度条
  - 关键操作有 Snackbar 反馈
  - 错误状态有友好提示 + 重试按钮
  - 空状态有插画 + CTA
  - 点击反馈用 `ripple` + 适度 `haptic`
- 字体：默认 Roboto / 系统字体，**禁止**用第三方字体（APK 体积）
- 图标：Material Icons Extended 库
- 色彩：Material 3 动态色（Android 12+） + 兼容回退色板

### CI 编译原则（**v0.2 新增**）
- **禁止**改任何代码绕过 CI 问题（必须从根上修）
- CI 失败 → 看日志 → 修代码 → 重 push，**循环直到通过**
- GitHub Actions workflow 文件必须可独立阅读（注释清晰）
- APK 签名用 GitHub Secrets 存 keystore
- Release 自动生成 changelog（从 commit message 提取）
- APK 体积超 100MB 必须压缩（UPX 或资源裁剪）

### 接口裁剪（**v0.2 更新**）
- 后端所有 `/api/*` 接口都可用
- **APP UI 优先暴露以下**（核心需求）：
  - 启停控制
  - 登录（POST /api/user/login, GET /api/user/token）
  - 用户自信息（GET /api/user/self）
  - 渠道（GET/POST/PUT /api/channel/*）
  - 令牌（GET/POST/DELETE /api/token/*）
  - 模型（GET /v1/models, GET /api/models）
  - 日志（GET /api/log/self）
- **APP UI 可选暴露**（用户用得上）：
  - 仪表盘数据（GET /api/data/self, GET /api/log/self/stat）
  - 兑换码（GET /api/redemption/*, 仅自用兑换）
  - 用户管理（GET/POST/PUT /api/user/*, 多账号场景）
  - 设置（GET /api/option/*, GET /api/user/setting）
- **APP UI 不暴露**（用户确认不要）：
  - 订阅（/api/subscription/*）
  - 支付（/api/user/pay, /api/stripe/*, /api/creem/*, /api/waffo/*）
  - 部署（/api/deployments/*）
  - Midjourney 任务（/api/mj/*）
  - 任务中心（/api/task/*）
  - 自定义 OAuth（/api/custom-oauth-provider/*）
  - 认证授权（/api/authz/*）
  - 性能监控（/api/performance/*, /api/system-info/*, /api/system-task/*）
  - 供应商（/api/vendors/*）
  - 预填分组（/api/prefill_group/*）
  - 倍率同步（/api/ratio_sync/*）
  - 插件市场（/api/plugin/*）
- 其它任何未列出的接口**只在 WebView 兜底页**使用