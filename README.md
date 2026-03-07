# MyPanorama

用于展示和生成全景图的app,支持陀螺仪/手势切换视角,球体/圆柱体浏览等功能  

---

## 演示

![全景图展示](./docs/demo.gif)

---

## 技术栈

| 模块 | 技术 |
|------|------|
| 全景库 (panorama) | Kotlin、OpenGL ES |
| 示例 App (demo) | Fragment + Navigation + ViewPager2、ViewModel + StateFlow/SharedFlow、Retrofit + Moshi、OkHttp WebSocket、DataStore |

---

## 快速开始

1. 克隆仓库：
   ```bash
   git clone https://github.com/blankxiao/MyPanorama.git
   cd MyPanorama
   ```
2. 用 **Android Studio** 打开项目（选择仓库根目录）。
3. 连接设备或启动模拟器，运行 **demo** 模块。

> 需 Android SDK 24+，JDK 11+。

---

## 项目结构

```
panoramaView/
├── panorama/     # 全景渲染库
└── demo/         # 示例应用
```

---
