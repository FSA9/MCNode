package com.mine.geometry_node.client.ui.Viewport;

import com.mine.geometry_node.client.ui.UICommand.EditorContext;
import com.mine.geometry_node.core.node.NodeData;

public class GraphController {
    private final EditorContext mContext;

    public GraphController(EditorContext context) {
        this.mContext = context;
    }

    public void addNode(NodeData node) {
        mContext.getGraph().addNode(node);
        mContext.notifyNodeAdded(node);
    }

    public void removeNode(String nodeId) {
        mContext.getGraph().removeNode(nodeId);
        mContext.notifyNodeRemoved(nodeId);
    }

    public float[] getNodePosition(String nodeId) {
        NodeData node = mContext.getGraph().getNode(nodeId);
        return node != null ? node.uiPos : null;
    }

    // 设置节点位置并通知 UI
    public void setNodePosition(String nodeId, float x, float y) {
        NodeData node = mContext.getGraph().getNode(nodeId);
        if (node != null) {
            node.setPosition(x, y);
            mContext.notifyNodeMoved(nodeId, x, y); // 需要在 EditorContext 补充此事件
        }
    }

    // 数据层添加连线，并触发 UI 刷新
    public void addConnection(String outNodeId, String outPortId, String inNodeId, String inPortId) {
        NodeData outNode = mContext.getGraph().getNode(outNodeId);
        if (outNode != null) {
            outNode.addDataConnection(outPortId, inNodeId, inPortId);
            mContext.notifyConnectionAdded(outNodeId, outPortId, inNodeId, inPortId);
        }
    }

    // 数据层移除连线，并触发 UI 刷新
    public void removeConnection(String outNodeId, String outPortId, String inNodeId, String inPortId) {
//        NodeData outNode = mContext.getGraph().getNode(outNodeId);
//        if (outNode != null) {
//            java.util.List<java.util.List<String>> list = outNode.outputConnections.get(outPortId);
//            if (list != null) {
//                list.removeIf(link -> link.get(0).equals(inNodeId) && link.get(1).equals(inPortId));
//                if (list.isEmpty()) outNode.outputConnections.remove(outPortId);
//            }
//            mContext.notifyConnectionRemoved(outNodeId, outPortId, inNodeId, inPortId);
//        }
    }
}