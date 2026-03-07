# MyPanorama

面向 360° 全景图展示与云端任务管理的 Android 应用。包含自研全景渲染库与示例 App，支持触摸/陀螺仪浏览、任务创建与列表、后端推送与登录等。

---

## 演示

![全景图展示](./docs/demo.gif)

---

## 技术栈

| 模块 | 技术 |
|------|------|
| 全景库 (panorama) | Kotlin、OpenGL ES 2.0、自定义 View（PanoramaView）、独立 GL 线程、EGL、Shader/纹理 |
| 示例 App (demo) | Fragment + Navigation + ViewPager2、ViewModel + StateFlow/SharedFlow、Retrofit + Moshi、OkHttp WebSocket（指数退避重连）、DataStore、Firebase Crashlytics |

---

## 项目结构

```
panoramaView/
├── panorama/     # 全景渲染库（可独立依赖）
└── demo/         # 示例应用
```

---
