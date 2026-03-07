package com.mine.geometry_node.core.node;

import com.mine.geometry_node.core.node.nodes.BaseNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * [UI 层级定义] 菜单文件夹实体
 * 用于在注册阶段构建起一棵真实的目录树。
 */
public class NodeCategory {

    public final String translationKey;

    // 树枝&树叶
    private final List<NodeCategory> subCategories = new ArrayList<>();
    private final List<BaseNode> nodes = new ArrayList<>();

    public NodeCategory(String translationKey) {
        this.translationKey = translationKey;
    }

    // --- 链式构建方法 ---

    public NodeCategory addChild(NodeCategory child) {
        if (child != null && !this.subCategories.contains(child)) {
            this.subCategories.add(child);
        }
        return this;
    }

    public NodeCategory addNode(BaseNode node) {
        if (node != null && !this.nodes.contains(node)) {
            this.nodes.add(node);
        }
        return this;
    }

    // --- 只读获取方法 ---

    public List<NodeCategory> getSubCategories() {
        return Collections.unmodifiableList(subCategories);
    }

    public List<BaseNode> getNodes() {
        return Collections.unmodifiableList(nodes);
    }
}