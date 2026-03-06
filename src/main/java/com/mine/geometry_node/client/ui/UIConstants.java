package com.mine.geometry_node.client.ui;

/**
 * UI 常量配置中心
 * 包含基础调色板定义和各模块的具体参数配置
 */
public class UIConstants {

    // ==========================================
    // 基础调色板 (Color Palette - 集中定义所有原始颜色值)
    // ==========================================

    // --- 背景深色系 ---
    public static final int CLR_BG_DARK_1 = 0xFF181818;      // 视口背景 (最深)
    public static final int CLR_BG_DARK_2 = 0xFF1D1D1D;      // 根容器/菜单背景
    public static final int CLR_BG_DARK_3 = 0xFF252525;      // 面板背景
    public static final int CLR_BG_DARK_4 = 0xFF2D2D2D;      // 标题栏背景
    public static final int CLR_BG_DARK_5 = 0xFF303030;      // 属性栏背景
    public static final int CLR_BG_NODE_BODY = 0xE6303030;   // 节点主体背景 (略透)

    // --- 装饰与功能色 ---
    public static final int CLR_BLACK = 0xFF000000;          // 纯黑 (描边/轴线)
    public static final int CLR_WHITE = 0xFFFFFFFF;          // 纯白 (高亮/文字)
    public static final int CLR_GRAY_TEXT = 0xFFAAAAAA;      // 辅助文字灰色
    public static final int CLR_GRAY_LABEL = 0xFFCCCCCC;     // 标签文字灰色
    public static final int CLR_SEARCH_BG = 0xFF333333;      // 搜索框深灰色
    public static final int CLR_GRID_LINE = 0xFF282828;      // 网格线暗灰色
    public static final int CLR_HOVER_WHITE = 0x40FFFFFF;    // 悬停覆盖色 (半透白)

    // --- 框选与节点类型色 ---
    public static final int CLR_SELECT_FILL = 0x3342A5F5;    // 框选填充蓝 (半透)
    public static final int CLR_SELECT_STROKE = 0xFF42A5F5;  // 框选边框蓝
    public static final int CLR_NODE_GEOMETRY = 0xFF00D6A3;  // 几何类型色 (青色)
    public static final int CLR_NODE_MATH = 0xFF6363C7;      // 数学类型色 (紫色)
    public static final int CLR_NODE_VALUE = 0xFFA1A1A1;     // 数值端口色 (中灰)

    //
    public static final float mDensity = 2.0f;  // 屏幕密度

    // ==========================================
    // 模块特定配置 (Module Configurations)
    // ==========================================

    /**
     * MainUI 主界面布局配置
     */
    public static class MainUI {
        public static final int BG_ROOT = CLR_BG_DARK_2;         // 根布局背景
        public static final int BG_HEADER = CLR_BG_DARK_4;       // 顶部标题栏背景
        public static final int BG_OUTLINER = CLR_BG_DARK_5;     // 左侧面板背景
        public static final int BG_VIEWPORT = CLR_BG_DARK_1;     // 中间视口背景
        public static final int BG_PROPERTIES = CLR_BG_DARK_5;   // 右侧面板背景
        public static final int BG_TIMELINE = CLR_BG_DARK_3;     // 底部面板背景
        public static final int BG_SPLITTER = CLR_BLACK;         // 分割线颜色
        public static final int TEXT_COLOR = CLR_GRAY_TEXT;      // 默认文字颜色

        public static final int HEIGHT_HEADER = 30;              // 顶部栏高度 (dp)
        public static final int HEIGHT_BOTTOM_DEFAULT = 150;     // 底部栏默认高度 (dp)
        public static final int HEIGHT_BOTTOM_MIN = 50;          // 底部栏最小高度 (dp)
        public static final int SPLITTER_HITBOX_SIZE = 2;        // 分割线触发区大小 (dp)
        public static final int SPLITTER_VISUAL_SIZE = 2;        // 分割线视觉粗细 (dp)
        public static final int TEXT_SIZE = 14;                  // 全局字体大小 (sp)

        public static final float WEIGHT_LEFT = 0.2f;            // 左侧面板宽度权重
        public static final float WEIGHT_CENTER = 0.6f;          // 中间视口宽度权重
        public static final float WEIGHT_RIGHT = 0.2f;           // 右侧面板宽度权重
        public static final float WEIGHT_MIN = 0.05f;            // 最小面板权重
    }

    /**
     * ViewPort 视口与网格配置
     */
    public static class ViewPort {
        public static final float SCREEN_DENSITY = 2.0f;         // 固定屏幕缩放密度
        public static final int BG_COLOR = CLR_BG_DARK_1;        // 画布背景色
        public static final int COLOR_GRID_LINE = CLR_GRID_LINE; // 网格线颜色
        public static final int COLOR_GRID_AXIS = CLR_BLACK;     // 坐标轴颜色
        public static final int COLOR_SELECTION_FILL = CLR_SELECT_FILL;     // 框选填充色
        public static final int COLOR_SELECTION_STROKE = CLR_SELECT_STROKE; // 框选边框色

        public static final int GRID_SIZE = 20;                  // 单个网格大小 (px)
        public static final float LINE_WIDTH_NORMAL = 0.5f;      // 普通网格线宽
        public static final float LINE_WIDTH_AXIS = 1.0f;        // 坐标轴线宽

        public static final float ZOOM_MIN = 0.5f;               // 最小缩放倍率
        public static final float ZOOM_MAX = 10.0f;              // 最大缩放倍率
        public static final float ZOOM_SENSITIVITY = 0.1f;       // 缩放灵敏度

        /** 视口右键菜单配置 */
        public static class NodeMenu {
            public static final int BG_COLOR = CLR_BG_DARK_2;       // 菜单背景色
            public static final int SEARCH_BG_COLOR = CLR_SEARCH_BG; // 搜索框背景色
            public static final int TEXT_COLOR = CLR_GRAY_TEXT;     // 默认项文字颜色
            public static final int TEXT_COLOR_HOVER = CLR_WHITE;   // 悬停项文字颜色
            public static final int TEXT_COLOR_SEARCH = CLR_WHITE;  // 搜索框文字颜色
            public static final int HOVER_COLOR = CLR_HOVER_WHITE;  // 项悬停覆盖色

            public static final int HEIGHT_SEARCH_BOX = 36;         // 搜索框高度 (dp)
            public static final int ITEM_HEIGHT = 30;               // 菜单项高度 (dp)
            public static final int ITEM_WEIGHT = 180;              // 菜单项宽度 (dp)
            public static final int BORDER_RADIUS = 0;              // 菜单圆角半径 (dp)
            public static final double TEXT_SIZE = 0.5;             // 字体比例
        }
    }
}