# 实施计划（Plan）— v0.2，待用户最终确认

> **版本**：v0.2（在 v0.1 基础上回填用户决策：CI 编译 / UI 不限屏数 / 现代化设计）
> ⚠️ **本文件为草案**，按 skill 要求，L3 任务必须等用户确认后才能进入实施阶段。

## 里程碑

### M1：环境与原 New API 调研（已完成，参照物）
1. ✅ Clone `QuantumNous/new-api` v1.0.0-rc.28 到 `newapi-src/`
2. ✅ 阅读核心源码：router/middleware/service/controller/electron/dockerfile
3. ✅ 整理 7 份分析文档到 `newapi-src/docs/openapi/`（接口清单、鉴权、部署、APP 设计）
4. ✅ 验证 OpenAPI（136 路径）与路由代码（161 直接注册）一致性
5. ✅ 记入记忆库

### M0：最小技术验证（★ v0.3 新增，用户已确认走这条路）

**目的**：先证明「安卓 APP 能跑起 New API 二进制」这件事本身可行，再投入界面开发。

**为什么必须先做**：联网校准发现 Android 10+ 有 W^X 限制，`filesDir` 里的文件不可执行，原方案会 100% 失败。必须改用「二进制伪装成 .so 放 jniLibs」方案，此方案需要实机验证。

**技术方案（校准后）**：
1. New API 编译产物改名为 `libnewapi.so`（必须 lib 前缀 + .so 后缀，否则 release 包不打进去）
2. 放到 `app/src/main/jniLibs/arm64-v8a/libnewapi.so`
3. `AndroidManifest.xml` 设 `android:extractNativeLibs="true"`
4. `build.gradle` 设 `packaging { jniLibs { useLegacyPackaging = true } }`
5. 运行时路径：`applicationInfo.nativeLibraryDir + "/libnewapi.so"`（此目录系统授予执行权限）

**交付物**：一个极简 APK（1 个按钮 + 1 行状态文字，不追求美观）

**验收（AN0）**：
- 用户下载 APK 装机 → 点「启动」→ 状态显示「运行中 · 端口 13000」
- 手机浏览器访问 `http://127.0.0.1:13000/api/status` 返回 JSON
- 点「停止」→ 状态变「已停止」，浏览器访问失败

**若失败的备选路线**：gomobile 编译成 AAR 走 JNI 调用 / Termux 方案 / proot 方案

---

### M2：剥离前端，编译纯后端 ARM64 二进制（下一轮目标）
- 目标：产出 `new-api-arm64` 单文件（无前端，~20-30 MB），可独立运行
- 步骤：
  - 1. 修改 `main.go` 去掉 `//go:embed web/dist` 和 `webRouter` 引用（**这是仅有的源码改动**）
  - 2. 保留业务逻辑 0 改动（路由/控制器/服务/模型全部不动）
  - 3. 编译：`CGO_ENABLED=0 GOOS=linux GOARCH=arm64 go build -ldflags="-s -w" -o new-api-arm64`
  - 4. 验证：把二进制放到任意 Linux arm64 设备/模拟器运行，确认 `/api/setup` 等核心接口正常
- 验收：curl `http://127.0.0.1:13000/api/status` 返回 200，SQLite 初始化成功

### M3：APP 工程脚手架
- 目标：建立可被 GitHub Actions 编译的 Android 项目结构
- 步骤：
  1. **手写**所有 Gradle/Manifest/源码文件（**不依赖 Android Studio**）
  2. 包名：`com.example.newapi_mobile`（待定）
  3. minSdk = 26，targetSdk = 34，ABI = arm64-v8a
  4. 添加依赖：Retrofit、OkHttp、Coroutines、EncryptedSharedPreferences、Material3
  5. 目录结构按 `structure.md` 规划
- 验收：push 后 CI 跑通，产出空白 APK

### M4：APP 核心 - 后端进程管理
- 目标：APP 启动后能释放二进制、启动后端、健康检查、关停
- 模块：
  - `BinaryManager`：把 `assets/new-api-arm64` 拷到 `filesDir/data/bin/`，chmod 755
  - `ProcessManager`：用 `ProcessBuilder` 启动，设置环境变量（PORT/SQLITE_PATH/SESSION_SECRET/GIN_MODE）
  - `HealthChecker`：每 1 秒轮询 `/api/status`，最多 30 秒
  - `LogCapture`：实时读 stdout/stderr，给 UI 调试面板用
  - `LifecycleController`：启动 → 前台服务；停止 → 销毁服务
- 验收：在 APP 中点"启动"，3 秒内后端健康检查通过，APP 显示"运行中"

### M5：APP 核心 - 现代化 UI
- 目标：手机端 UI **现代化 + 美观 + 流畅**，按后端功能自然拆分屏幕（不硬性限制数量）
- 设计原则：
  - Material 3 + Jetpack Compose
  - 完整动画过渡、ripple、shared element
  - 暗色 / 亮色主题自适应
  - 下拉刷新、Skeleton 加载、空状态插画
  - 关键操作有撤销/二次确认
- 屏幕清单（**按需扩展**）：
  - 启动器（首次安装引导、初始化设置）
  - 主页（启动/停止 + 状态卡片 + 端口/IP 提示 + 快速入口）
  - 登录（用户名密码 + PAT + 2FA 输入）
  - 仪表盘（用户信息、余额、近期用量图表）
  - 渠道管理（列表 / 详情 / 编辑 / 测试 / 余额同步）
  - 令牌管理（列表 / 创建 / 编辑 / 删除 / 复制 sk-）
  - 模型市场（按类型筛选 / 搜索 / 复制 endpoint）
  - 日志（实时 / 历史 / 筛选 / 导出）
  - 设置（主题 / 语言 / 网络 / 数据备份 / 关于）
  - 兑换码（可选用，但 UI 友好）
  - 用户管理（可选用）
  - WebView 兜底页（高级功能）
- 仍不做的 UI 入口（用户用不上 + 后端剥除）：订阅、支付、deployments、mj、custom-oauth、ratio_sync、performance、system-info、authz、vendors、prefill_group、system-task
- 验收：APP 中能完成"登录 → 看到渠道列表 → 启用某渠道 → 生成新 token → 实时看日志"完整流程；UI 流畅度手动评估 90+

### M6：保活与资源释放
- 启动后：前台服务 + 通知 + START_STICKY + 忽略电池优化 + 自启动
- 停止后：服务 onDestroy 调 stopSelf；通知清掉；进程不重启
- 验收：
  - 启动后息屏 30 分钟，后端仍然存活（用外部 ping 验证）
  - 停止后关闭 APP，系统无残留进程（用 `ps -A` 验证）

### M7：GitHub Actions 云编译 + 发布
- 目标：代码 push 后自动出 APK，用户下载安装
- 步骤：
  - 1. 建立 `.github/workflows/build.yml`（Linux runner + JDK 17 + Android SDK + Go 1.25）
  - 2. 步骤 1：在 CI 里编译 new-api-arm64 二进制（M2 改 main.go 的代码）
  - 3. 步骤 2：把二进制塞进 `app/src/main/assets/`
  - 4. 步骤 3： `./gradlew assembleRelease` 出 APK
  - 5. 步骤 4：用 release-android build-tools 签名（用 GitHub Secrets 存 keystore）
  - 6. 步骤 5：自动创建 GitHub Release，附 APK + 校验和
  - 7. **CI 红 → 看日志 → 修代码 → 重 push**，循环直到通过
- 验收：每次 push main，CI 在 10-20 分钟内出 release APK，可直接在 GitHub Release 页面下载

## 任务拆解

| 任务 | 目标 | 输入 | 输出 | 状态 |
|---|---|---|---|---|
| 调研 New API 源码 | 摸清所有后端接口 | GitHub repo | 7 份分析文档 | ✅ 已完成 |
| M2 编译 ARM64 二进制 | 产出剥离前端的 New API | `newapi-src/` 源码 | `new-api-arm64` | 🔲 待开始 |
| M3 搭 APP 脚手架 | 可编译 APK | Gradle 配置 | 空 Compose 工程 | 🔲 待开始 |
| M4 进程管理 | 释放并启动后端 | 二进制 + 资源 | 进程管理模块 | 🔲 待开始 |
| M5 现代化 UI | 多屏 Compose UI | API 客户端 + Compose | 完整 APP | 🔲 待开始 |
| M5 鉴权 + 凭证 | 登录 + PAT 存储 | Retrofit + EncryptedSP | 鉴权模块 | 🔲 待开始 |
| M6 保活与生命周期 | 启动保活 / 停止释放 | Service API | 稳定运行 | 🔲 待开始 |
| M7 GitHub Actions CI | 自动化编译+发布 | `.github/workflows/` | Release APK | 🔲 待开始 |

## 验收节点
- **AN1（M2 完成）**：剥离前端的 New API 二进制在 Linux arm64 设备上能独立运行，curl `/api/status` 返回 200
- **AN2（M4 完成）**：本地模拟器上 APP 点"启动"后，后端进程存在且监听端口（CI 环境无安卓设备，靠单元测试 + 静态检查验证）
- **AN3（M5 完成）**：CI 编译成功，APK 产出；UI 截图在 CI artifact 中可看
- **AN4（M6 完成）**：单元测试覆盖保活 / 释放逻辑
- **AN5（M7 完成）**：CI 自动发版，Release 页面有 APK + 校验和
- **AN6（用户验收）**：用户下载 APK 装到真机，外部 agent 通过 `http://&lt;手机 IP&gt;:13000/v1/chat/completions` 调通

## 风险与回滚

| 风险 | 等级 | 应对 | 回滚 |
|---|---|---|---|
| New API 去掉 embed 前端后编译报错 | 中 | 把 `main.go` 的 `//go:embed web/dist` 改成一个最小占位（空 `index.html`），其他代码不动 | 用 `git checkout` 恢复源码 |
| Go 二进制在 Android Bionic libc 上跑不起来 | 中 | 编译时严格 `CGO_ENABLED=0`；准备 `ca-certificates.crt` | 退而求其次跑 Termux 方案 |
| 进程被安卓杀后台 | 高 | 前台服务 + 通知 + 忽略电池优化 + 自启动 | 提供"一键唤醒"按钮 |
| 端口被其他 APP 占用 | 低 | 启动前检查 13000-13500 区间，冲突则顺延 | 提示用户在 APP 内手动指定 |
| SQLite 数据库损坏 | 低 | 启动前做完整性检查；定期自动备份 | UI 提供"恢复出厂"按钮 |
| APK 体积过大（>100 MB） | 低 | 二进制用 UPX 压缩；web/dist 占位用空文件 | 仅打包 arm64-v8a |
| AGPLv3 法律风险 | 低 | APP 自身是 UI，源码公开；New API 后端按协议 | 不重新分发二进制，仅自用 |
| 用户用不上外部 agent 调用 | 中 | 文档明确"端口 + IP 提示"；日志面板显示当前 URL | 加 QR 码让用户扫码看 IP |
| APP 内 UI 误剥离导致找不到必要功能 | 中 | 与用户先 review 6 个屏幕清单；实现每个屏幕前先 demo | 后续小版本追加 |
| 后端 API 鉴权过严（如 cookie 依赖） | 中 | 用 PAT（永久 token）登录绕过 cookie | 改用 WebView 嵌入 New API 完整 Web |

## 关键决策（已替你拍板，供参考）
- **包名**：`com.example.newapi_mobile`（临时）
- **端口范围**：`13000-13500`
- **UI 框架**：Jetpack Compose + Material 3（现代化 + 流畅）
- **屏幕数**：不限，按需扩展
- **不做的 UI 入口**（用户确认）：订阅/支付/deployments/mj/custom-oauth/ratio_sync/performance/system-info/authz/vendors/prefill_group/system-task
- **CI 编译**：✅ GitHub Actions 云编译 + 自动 Release
- **二进制打包到 APK assets**：首次启动拷到 filesDir
- **M2 改 main.go 去掉 embed 前端**：仅改 1 处 embed，业务逻辑 0 改动

## 仓库规划
- **GitHub 仓库名**（建议）：`newapi-mobile` 或你喜欢的名字
- **仓库结构**：
  - `main` 分支：源代码
  - `releases`：自动从 main 出 APK
  - 标签：`v0.1.0`、`v0.2.0` ...

## 下一步等你确认
请 review 上面所有调整（特别是 M5 屏幕清单、M7 CI 步骤、关键决策），确认无误后我开干：
1. 计划 OK 吗？
2. GitHub 仓库名字想用啥？（默认 `newapi-mobile`，可改）
3. 接下来顺序是：先 M2（编译二进制）+ M3（搭脚手架）+ M7（CI 流水线）先跑通空壳，还是先 M2 → M3 → M4 → M5 → M6 → M7 串行？
4. 首次推到 GitHub 的仓库：是你已有空仓库，还是我帮你建？

确认后我开干。