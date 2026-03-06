package com.mine.geometry_node.core.node.nodes;

import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;

/**
 * [元数据] 节点全量描述 (Schema)
 */
public record NodeDef(
        String typeId,
        NodeType category,
        Component displayName,
        String[] menuPath,
        List<PortRow> rows
) {
    public static Builder builder(String typeId, NodeType category, Component displayName) {
        return new Builder(typeId, category, displayName);
    }

    public static class Builder {
        private final String typeId;
        private final NodeType category;
        private final Component displayName;
        private String[] menuPath = new String[]{"geometry_node.menu.uncategorized"};

        private final List<PortRow> rows = new ArrayList<>();

        private Builder(String typeId, NodeType category, Component displayName) {
            this.typeId = typeId;
            this.category = category;
            this.displayName = displayName;
        }

        public Builder setMenuPath(String... pathKeys) {
            this.menuPath = pathKeys;
            return this;
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
            return new NodeDef(typeId, category, displayName, menuPath, List.copyOf(rows));
        }
    }
}