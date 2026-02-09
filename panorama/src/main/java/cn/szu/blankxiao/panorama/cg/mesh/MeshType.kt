package cn.szu.blankxiao.panorama.cg.mesh

/**
 * 全景图模型类型枚举
 * @author BlankXiao
 */
enum class MeshType {
    /**
     * 球体模型 - 完整的 360° x 180° 全景
     * 适用于：equirectangular 全景图
     */
    SPHERE,

    /**
     * 圆柱体模型 - 360° 水平全景，垂直方向线性
     * 适用于：cylindrical 全景图、街景等
     */
    CYLINDER
}

