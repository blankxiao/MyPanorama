# 客户端：模块说明与快速开始

本文档简要说明 Android 客户端 (`panoramaView` 工程) 的模块结构与运行方式。

## 1. 模块结构说明

本工程主要由以下两个核心模块组成：**`panorama`** (核心增强渲染库) 与 **`demo`** (核心业务与示例应用)。

### 1.1 `panorama` 核心渲染库

全景图渲染的核心 SDK 模块。封装了基于 OpenGL 的底层图形绘制、手势触摸交互、陀螺仪传感器定位响应以及视角的动态计算等功能。

```text
panorama/src/main/java/cn/szu/blankxiao/panorama/
├── cg/                 # 核心图形学(Computer Graphics)封装
│   ├── camera/         # 相机视锥体视角定义
│   ├── gl/             # EGL环境配置与 GL 渲染线程 (GLProducerThread)
│   ├── mesh/           # 网格定义 (PanoramaMesh)
│   └── render/         # 原生 Shader 编译与纹理纹理渲染缓冲
├── controller/         # 外部调度与内部生命周期控制器 (FOV、角度、触摸控制)
├── helper/             # 手势拦截、图片异步加载机制
├── orientation/        # 陀螺仪(Gyro)传感器事件解析与定向提供
└── renderer/           # 顶层渲染抽象与模型构建器
    └── mesher/         # 网格构建器 (包含球体 Sphere 与圆柱体 Cylinder 的生成与旋转计算)
```
**作用与特点**：
作为一个纯净的、低耦合的底层库运行。开发者只需关心传入全景图（圆柱体或球状）Bitmap 和 `PanoramaView` 控件的挂载即可，无需关心复杂的矩阵运算与 OpenGL 上下文分配。

### 1.2 `demo` 业务与宿主应用

主工程的示例与业务集成模块。包含了完整的应用级业务流程（如身份认证、文生全景图交、后端 API 交互），并完整演示如何接入 `panorama` 库。

```text
demo/src/main/java/cn/szu/blankxiao/panoramaview/
├── api/                # 后端 RESTful 接口定义层
│   ├── auth/           # 登录、注册、验证码相关 API 及 DTO 实体
│   └── panorama/       # 业务全景图任务创建、轮询、预签名上传等 API
├── data/               # 客户端本地持久化存储 (如 Token 管理、夜间模式配置)
├── network/            # 网络拦截器 (AuthInterceptor)、Retrofit 封装与 WebSocket 管理
├── ui/                 # 界面层 (Fragments & Activities)
│   ├── profile/        # 个人主页与登录鉴权界面
│   ├── resources/      # 全景图库展示、历史任务列表界面
│   └── task/           # 新任务创建与绘画输入界面
└── viewmodel/          # 基于 MVVM 架构的 ViewModel 层 (如 AuthViewModel, PanoramaViewModel)
```
**作用与特点**：
`demo` 模块直接面向最终用户，实现了标准的 App 导航栏（通过 Navigation Component 组织的 Bottom Nav），并采用 Kotlin 协程 + Retrofit 结合 ViewModel 完成网端数据通信。业务层在这里直接持载 `panorama` 视图，负责加载服务端下发的全景图并展示。

## 2. 快速开始

### 开发环境要求
- **IDE**: Android Studio (建议采用近年较新版本，如 Iguana 及以上)
- **JDK**: Java 17 或以上 (匹配项目使用的 Gradle 插件版本)
- **设备**: Android 真机或使用最新镜像的模拟器（全景渲染和陀螺仪强烈建议使用真机测试）

### 运行步骤

1. **导入工程**
   打开 Android Studio，选择 `File -> Open`，定位并选择 `panoramaView` 根目录进行加载。
2. **同步依赖**
   等待 Gradle 构建与同步。如果环境缺少所需版本的 SDK 工具或 NDK，Android Studio 控制台会弹出提示，请点击链接自动下载补全。
3. **环境与接口配置（可选）**
   如果需要测试带有完整网端交互（如上传生成全景图、拉取历史记录）的流程，请确保已经跑通并启动了对应的后端服务（网关、Auth与Business模块），并确保手机与服务端机器在同一局域网下，修改 `demo` 模块网络请求层中的 API `Base URL` 为服务端所在机器的 IP 地址。
4. **编译与运行**
   在 IDE 顶部的运行配置栏中，确保选中了 `demo` 模块，选择你连接的设备或模拟器，点击 `Run` (绿色运行小箭头) 一键编译、安装并启动。

## 3. 常见问题
- **签名配置异常**：若提示签名相关错误，请检查项目的打包脚本并确认根目录的存在配置（如 `keystore.base64`），日常开发可先将 `build.gradle` 切换为默认的 debug 签名。
- **渲染卡顿/黑屏**：请确保测试设备支持 OpenGL ES 3.0+，且部分高度定制 ROM 可能在渲染上存在差异，建议查看 `logcat` 中的 Crash 或 EGL 报错。