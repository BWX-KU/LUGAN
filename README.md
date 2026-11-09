# 路感 RoadSense · Android 安装包工程

把线上「路感车路协同平台」打包成真正的 Android `.apk`。工程是**纯 Android SDK 的 WebView 壳**，不依赖 AndroidX / 第三方库，云构建几分钟就能出包。

> 日常使用其实不需要 APK：手机浏览器打开安装中心，点「添加到主屏幕」在 Android 上就会生成一个可卸载的原生级应用（WebAPK）。APK 适合需要分发、侧载、应用商店上架或离线预装的场景。

## 目录结构

```
road-sense-android/
├── settings.gradle / build.gradle / gradle.properties
├── gradle/wrapper/gradle-wrapper.properties
├── .github/workflows/build-apk.yml     ← 云端自动出 APK
├── app/build.gradle                     ← 包名、版本、SDK 版本
└── app/src/main/
    ├── AndroidManifest.xml
    ├── java/cn/roadsense/app/MainActivity.java   ← 服务器地址在这
    └── res/ (layout / values / mipmap 图标)
```

## 方式 A：本机 Android Studio（有图形界面，最直观）

1. 安装 [Android Studio](https://developer.android.com/studio)，首次启动让它装好 Android SDK 34 + JDK 17。
2. `File → Open` 选择本目录（**road-sense-android 这一层**，不要选到上一层）。
3. 提示 "Gradle wrapper not found" 时选 **OK / Create wrapper**（IDE 会自动补齐 `gradle-wrapper.jar`）。
4. 等 Gradle Sync 完成 → 菜单 `Build → Build Bundle(s) / APK(s) → Build APK(s)`。
5. 产物在 `app/build/outputs/apk/debug/app-debug.apk`，IDE 右下角也会给「locate」链接。

## 方式 B：GitHub Actions 云构建（不用装 Android Studio，推荐）

1. 在 GitHub 新建一个空仓库（例如 `roadsense-android`）。
2. 把本目录**里面的全部文件**上传到仓库根目录（包含隐藏的 `.github/` 目录）。
3. 打开仓库 `Actions` 页，运行 `构建路感 APK`（也可以点 `Run workflow` 手动触发）。
4. 跑完进入该次运行记录，在 **Artifacts** 下载 `RoadSense-debug-apk`，解压得到 `app-debug.apk`。

工作流会自动装 JDK 17、Android SDK 34、Gradle 8.7，补齐 wrapper，然后 `assembleDebug`。Debug 包用自动生成的调试签名，可直接安装。

## 安装到手机

1. 把 `app-debug.apk` 传到手机（数据线 / 微信文件传输 / 网盘均可）。
2. 在手机上点开 APK，系统提示「未知来源」时允许本次安装。
3. 装好后桌面出现「路感」图标，打开即全屏工作台；首次按提示登录 `17503228587 / 123456`。
4. 长按图标可像普通 App 一样卸载。

## 常用修改

| 想改什么 | 改哪里 |
|---|---|
| 服务器地址 | `MainActivity.java` 的 `BASE_URL` 和 `HOST`；若分享码变了，同时改站点 `manifest.json` 的 `start_url` |
| 应用名称 | `res/values/strings.xml` 的 `app_name` |
| 图标 | 替换 `res/mipmap-*/ic_launcher.png`（48/72/96/144/192） |
| 包名 | `app/build.gradle` 的 `applicationId` 与 `namespace`，以及 Java 文件的 package / 目录 |
| 版本号 | `app/build.gradle` 的 `versionCode` / `versionName` |
| 最低系统 | `minSdk 21`（Android 5.0）；`targetSdk 34` |
| 屏幕方向 | 默认不锁定，手机竖屏、平板横屏都自适应；如需锁定在 Manifest 的 activity 加 `android:screenOrientation` |

## 正式发布（上架 / 分发）

Debug 包不适合对外分发，正式包要自己签名：

```bash
keytool -genkey -v -keystore roadsense.jks -keyalg RSA -keysize 2048 -validity 10000 -alias roadsense
```

在 `app/build.gradle` 的 `android {}` 里加：

```gradle
signingConfigs {
    release {
        storeFile file("../roadsense.jks")
        storePassword "你的库密码"
        keyAlias "roadsense"
        keyPassword "你的密钥密码"
    }
}
buildTypes {
    release { signingConfig signingConfigs.release }
}
```

然后 `./gradlew assembleRelease`，产物 `app-release.apk`。**keystore 与密码不要提交到公开仓库**。

## 常见问题

- **打开白屏 / 「链接不完整」**：`BASE_URL` 必须保留 `?sharecode=SOFDUZBXLY`。托管网关要求首访带这个参数下发会话 cookie，丢了就 403。
- **能打开但三端不同步**：确认登录的是同一个账号；后端 `/api/health` 要能访问（App 内 WebView 会带 cookie）。
- **证书错误**：工程对 SSL 错误一律拒绝，不会静默放行；请检查服务器证书是否过期。
- **返回键直接退出**：已实现「网页内回退 → 双击退出」，与原生 App 习惯一致。
- **离线**：壳与图标由 Service Worker 缓存，断网能打开页面；实时路况/限速/导航需要联网，恢复后自动重连。
- **iOS**：不能侧载 IPA，请用 Safari「添加到主屏幕」，见站点安装中心。

## 与 PWA 的关系

两者共用同一个线上地址与后端，账号、状态、事件流水完全一致。PWA 零编译、随时更新；APK 是固定壳、适合分发，但内容仍来自线上，所以**升级页面逻辑不需要重新发版**。
