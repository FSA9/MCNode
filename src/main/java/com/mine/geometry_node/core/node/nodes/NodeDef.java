package com.mine.geometry_node.core.node.nodes;

import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;

/**
 * [元数据] 节点全量描述 (Schema)
 * 移除 menuPath，排版由 NodeRegistry 实体树接管
 */
public record NodeDef(
        String typeId,
        NodeType category,
        Component displayName,
        List<PortRow> rows // 删除了 menuPath
) {
    public static Builder builder(String typeId, NodeType category, Component displayName) {
        return new Builder(typeId, category, displayName);
    }

    public static class Builder {
        private final String typeId;
        private final NodeType category;
        private final Component displayName;

        // 删除了 menuPath 变量和 setMenuPath 方法

        private final List<PortRow> rows = new ArrayList<>();

        private Builder(String typeId, NodeType category, Component displayName) {
            this.typeId = typeId;
            this.category = category;
            this.displayName = displayName;
        }

        /**
         * add PortRow
         */
        public Builder addRow(PortRow row) {
            if (row != null) {
                this.rows.add(row);
            }
            return this;
        }

        public NodeDef build() {
            // 构建时不再传入 menuPath
            return new NodeDef(typeId, category, displayName, List.copyOf(rows));
        }
    }
}