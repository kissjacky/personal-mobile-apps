# Personal Mobile Apps

Personal Mobile Apps 是一个用 Web Coding / Code Agent 方式持续开发的安卓小应用集合。

这些应用的目标很朴素：完全开源、足够简单、没有广告、能直接装到自己的安卓手机里使用。你可以下载编译好的 APK，也可以阅读源码，或者用自己的 Code Agent 在这个项目上继续做二次开发。AI 正在把开发成本降到很低，很多过去“不值得专门开发”的个人需求，现在都可以被快速、认真地满足。

## Apps

| App | 路径 | 包名 | 说明 |
| --- | --- | --- | --- |
| 钢琴节拍器 | `apps/metronome` | `com.personalapps.metronome` | Android 原生节拍器，支持 BPM 输入、拍号选择和第一拍重音。 |

共享 Android 代码放在 `packages/` 下。

## 下载 APK

GitHub 是主仓库和主发布源，Release 以 GitHub 为准：

- GitHub Releases: <https://github.com/kissjacky/personal-mobile-apps/releases>

Gitee 只做后置同步镜像，方便中国用户下载，可能会比 GitHub 稍有延迟：

- Gitee Releases: <https://gitee.com/jackyyu/personal-mobile-apps/releases>

## 本地配置

个人路径、签名文件、Token 等不要写进 Gradle 或源码。复制 `.env.example` 到 `.env` 后再填写本机配置：

```bash
cp .env.example .env
```

常用项：

- `JAVA_HOME`: JDK 路径，建议 JDK 17。
- `ANDROID_HOME` / `ANDROID_SDK_ROOT`: Android SDK 路径。
- `ANDROID_COMPILE_SDK` / `ANDROID_MIN_SDK` / `ANDROID_TARGET_SDK`: Android SDK 版本。
- `METRONOME_VERSION_CODE` / `METRONOME_VERSION_NAME`: 节拍器版本。
- `ANDROID_SIGNING_*`: 发布 APK 签名配置，只放在 `.env` 或 CI secrets。
- `GITEE_ACCESS_TOKEN`: 后置同步 Gitee Release 时使用，只放在 `.env` 或 CI secrets。

`.env`、签名文件、构建产物和临时目录都已在 `.gitignore` 中排除。

## 构建

Debug 构建：

```bash
./scripts/build_app.sh metronome debug
```

其他 Gradle 命令建议通过包装脚本执行，这样会自动加载 `.env`：

```bash
./scripts/gradle.sh projects
```

输出：

```text
apps/metronome/build/outputs/apk/debug/*debug.apk
```

Release 构建需要先在 `.env` 中配置签名：

```bash
./scripts/build_release.sh metronome
```

签名后的 APK 会复制到：

```text
dist/releases/
```

## USB 安装

1. 在安卓手机上开启开发者选项。
2. 开启 USB 调试。
3. 连接手机并同意 RSA 授权。
4. 运行：

```bash
./scripts/install_metronome.sh
```

## 发布

推荐用 tag 触发 GitHub Actions 自动构建并创建 Release：

```bash
git tag -a metronome-v1.0.12 -m "metronome 1.0.12"
git push origin main --tags
```

也可以在本机创建 GitHub Release：

```bash
./scripts/create_github_release.sh metronome
```

Gitee 不作为主发布入口。GitHub Release 发布成功后，`Sync to Gitee` workflow 会把代码、tag 和 Release 资产同步过去。

更完整的发布和 CI secrets 配置见 [docs/RELEASE.md](docs/RELEASE.md)。

## 开源协议

本项目使用 MIT License，见 [LICENSE](LICENSE)。
