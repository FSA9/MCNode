package com.mine.geometry_node.client.ui.Viewport.Interaction;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mine.geometry_node.core.node.NodeData;
import icyllis.modernui.view.KeyEvent;

public class KeyManager {

    private final InteractionContext mContext;
    private final Gson mGson; // 用于验证 JSON 输出

    public KeyManager(InteractionContext context) {
        this.mContext = context;
        // 配置 GSON: 美化输出，忽略 null 值
        this.mGson = new GsonBuilder().setPrettyPrinting().create();
    }

    public boolean onKeyDown(KeyEvent event) {
        boolean isCtrl = event.isCtrlPressed();

        switch (event.getKeyCode()) {
            case KeyEvent.KEY_Z:
                if (isCtrl) {
                    mContext.getEditorContext().getCommandManager().undo();
                    return true;
                }
                break;
            case KeyEvent.KEY_Y:
                if (isCtrl) {
                    mContext.getEditorContext().getCommandManager().redo();
                    return true;
                }
                break;
            case KeyEvent.KEY_S:
                if (isCtrl) {
                    performSaveJSON();
                    return true;
                }
                break;
        }
        return false;
    }

    /** [核心验证] 模拟 Ctrl+S 保存 JSON */
    private void performSaveJSON() {
        System.out.println("====== 正在保存 Graph JSON ======");

        JsonObject root = new JsonObject();
        root.addProperty("graph_name", mContext.getEditorContext().getGraph().graphName);
        root.addProperty("version", "0.0.1");

//        // 完美构建你要求的 nodes 对象
//        JsonObject nodesObj = new JsonObject();
//        for (NodeData node : mContext.getEditorContext().getGraph().nodes) {
//            // JsonTree 会自动过滤掉值为 null 的字段 (例如 bar)
//            nodesObj.add(node.id, mGson.toJsonTree(node));
//        }
//        root.add("nodes", nodesObj);

        // 输出到控制台验证
        String jsonOutput = mGson.toJson(root);
        System.out.println(jsonOutput);
        System.out.println("=================================");
    }
}