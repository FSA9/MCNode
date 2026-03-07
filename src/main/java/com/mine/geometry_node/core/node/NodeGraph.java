package com.mine.geometry_node.core.node;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * [容器层] 蓝图对象。
 * 代表一整张逻辑图，包含所有节点数据。
 */
public class NodeGraph {
    @SerializedName("graph_name")
    public String graphName;        // 图名称
    @SerializedName("version")
    public String version;          // 版本
    // public String author;        // 作者信息
    // public long lastModified;    // 最后修改时间

    // 节点列表
    @SerializedName("nodes")
    public Map<String, NodeData> nodes = new HashMap<>();

    public NodeGraph() {
//        this.lastModified = System.currentTimeMillis();
    }

    public NodeGraph(String graphName) {
        this();
        this.graphName = graphName;
    }

    /**
     * 辅助方法：根据 UUID 查找节点数据
     */
    public NodeData getNode(String id) {
        return nodes.get(id); // Map 查找比 List 流查找快得多 (O(1) vs O(n))
    }

    public void addNode(NodeData node) {
        if (node.id == null) {
            throw new IllegalArgumentException("Node ID cannot be null when adding to graph");
        }
        this.nodes.put(node.id, node);
    }

    public void removeNode(String id) {
        this.nodes.remove(id);
    }
}