# New API Android 网关

在安卓手机上运行 [New API](https://github.com/QuantumNous/new-api) 后端，为本地 AI Agent 提供统一的 OpenAI 兼容 API 网关。

## 技术方案

- **Android 原生 APP**：Kotlin + Jetpack Compose
- **内嵌后端**：New API 剥离前端编译为 ARM64 静态二进制，伪装成 `libnewapi.so` 放入 jniLibs
- **W^X 限制规避**：Android 10+ 禁止执行 filesDir 文件，`.so` 方案经 `extractNativeLibs=true` 解压到 nativeLibraryDir 后执行
- **CI 构建**：GitHub Actions 云端编译，自动发布 APK 到 Release

## 目录结构

```
android/          # Android 工程
  .github/workflows/build.yml  # CI 构建脚本
  app/src/main/java/           # Kotlin 源码
  app/src/main/jniLibs/        # ARM64 二进制（CI 构建时生成）
newapi-src/       # New API 后端源码（submodule，仅构建用）
```

## 使用

1. 从 [Releases](../../releases) 下载 APK
2. 安装后打开 APP
3. 点击「启动后端」
4. 等待状态变为「运行中」
5. 在 Agent 中配置 `http://127.0.0.1:13000/v1`

默认账号：`root / 123456`（首次启动自动创建，请及时修改）

## 构建

```bash
# 在 GitHub Actions 中自动完成，无需本地环境
git push origin main
```

## License

AGPLv3（New API 采用 AGPLv3）