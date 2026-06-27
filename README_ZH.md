# Phonograph

[![License: GPL v3](https://img.shields.io/badge/License-GPL%20v3-blue.svg)](LICENSE.txt)

[English](README.md) | 简体中文

Phonograph 是一个 Material 风格的 Android 音乐播放器，基于原始项目
[kabouzeid/Phonograph](https://github.com/kabouzeid/Phonograph) 继续开发。
这个分支保留 Java 项目结构和现有应用架构，同时升级了构建栈、Android 平台适配、
播放链路和媒体库来源。

![Screenshots](./art/art.jpg?raw=true)

## 主要特性

- 适配 Android 16 / API 36：`compileSdk 36`、`targetSdk 36`、`minSdk 23`、
  Java 17、AGP 9.2.1、Gradle 9.4.1。
- 本地音乐库：支持歌曲、专辑、艺术家、流派、播放列表、文件夹、播放队列、
  桌面小组件、媒体通知控制和锁屏控制。
- Subsonic/OpenSubsonic/Navidrome V1 支持：可以添加和管理服务器，从侧边栏在
  本地媒体库和服务器之间切换，将远端元数据同步到本地 SQLite 缓存，浏览远端歌曲、
  专辑、艺术家、流派、播放列表，并串流播放音乐。
- 远端播放质量控制：默认播放原文件，也可以按设置向服务器请求转码，支持配置转码
  格式和码率。
- 播放兜底：优先使用 Android 原生播放；遇到 ALAC 等系统不支持的格式时，在
  Media3 可以解析容器的前提下使用 FFmpeg 解码播放作为兜底。
- Subsonic 专辑图缓存：远端封面会按需缓存到应用 cache 目录，服务器管理页也提供
  手动预缓存和清理缓存入口。
- 歌词：优先使用内嵌歌词和同目录 `.lrc`/`.txt`；找不到本地歌词时可自动网络匹配，
  也可以手动搜索选择结果。当前接入 LRCLIB、网易云音乐、酷狗和实验性的 QQ 音乐源。
  Apple Music 官方 API 不返回歌词文本，所以不作为歌词源。
- 只保留 Flat 正在播放界面。旧外观选项、Billing、Pro 解锁和捐赠流程已移除；
  原 Pro 功能默认开放。

## 当前范围

Subsonic 支持目前专注于浏览和串流播放。离线下载、远端播放列表编辑、收藏/评分/
scrobble，以及向服务器同步“正在播放”状态不属于 V1 范围。

应用不会缓存音乐文件。当前只会在应用私有目录中缓存 Subsonic 元数据、远端专辑图
以及已选择或自动匹配到的歌词。

## 构建

需要：

- JDK 17
- Android SDK Platform 36
- 支持 AGP 9.2.1 的 Android Studio，或使用项目内置 Gradle wrapper

Debug 构建：

```sh
./gradlew :app:assembleDebug
```

Lint：

```sh
./gradlew :app:lintDebug
```

Release 构建使用项目里的 release 签名配置。运行前请提供对应签名 properties，
或者按自己的本地环境调整 `app/build.gradle`：

```sh
./gradlew :app:assembleRelease
```

## 权限与隐私

- 本地媒体库在新版 Android 上需要媒体读取权限。
- 通知权限会独立申请；拒绝通知权限不应该阻塞播放。
- 远端媒体库不依赖本地媒体权限，即使拒绝本地音乐权限也可以浏览和播放服务器音乐。
- 播放 Subsonic/OpenSubsonic/Navidrome 音乐会向你配置的服务器发送请求。应用允许
  HTTP 自建服务器，但更推荐使用 HTTPS。
- 网络歌词匹配会把歌曲标题、艺术家、专辑、时长等元数据发送给第三方歌词源。

## 许可证

本项目使用 GNU General Public License v3.0 授权，详见 [LICENSE.txt](LICENSE.txt)。
