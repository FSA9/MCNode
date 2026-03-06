package com.mine.geometry_node.core.node;

import com.google.gson.annotations.SerializedName;

/**
 * [数据层] 描述单条数据连线
 * 表示：从当前节点的某个输出端口 -> 连接到 -> 目标节点的某个输入端口
 */
public record Connection(
        @SerializedName("target_node") String targetNodeId,
        @SerializedName("target_port") String targetPortName
) {
    public boolean isValid() {
        return targetNodeId != null && !targetNodeId.isEmpty() &&
                targetPortName != null && !targetPortName.isEmpty();
    }
}