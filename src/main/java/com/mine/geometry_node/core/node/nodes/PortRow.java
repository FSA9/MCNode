package com.mine.geometry_node.core.node.nodes;

import org.jetbrains.annotations.Nullable;
import java.util.Map;

/**
 * [排版基石] 端口行定义
 * 描述节点 UI 中“一行”的绝对结构。
 * 强制显式传参：所有参数必须在构造时指明，null 代表该项不存在。
 */
public record PortRow(
        @Nullable PortDef leftPort,       // 左侧输入端口
        @Nullable PortDef rightPort,      // 右侧输出端口
        UIHint uiHint,                    // 内联控件类型
        @Nullable String customWidgetId,  // CUSTOM 组件 ID
        @Nullable Map<String, Object> hintParams // 额外静态参数
) { }