package com.mine.geometry_node.core.node.nodes.functions.communication;

import com.mine.geometry_node.core.execution.ExecutionContext;
import com.mine.geometry_node.core.execution.ExecutionResult;
import com.mine.geometry_node.core.node.nodes.*;
import net.minecraft.network.chat.Component;

public class Communication_ReceiveBlueprint extends BaseNode {

    public static final String TYPE_ID = "communication_receive_blueprint";

    @Override
    public NodeDef getDefaultDefinition() {
        return NodeDef.builder(TYPE_ID, NodeType.EVENT, Component.translatable("geometry_node.node.communication_receive"))
                // 第 1 行: 频率输入框 (纯静态设置，用于引擎前置匹配)
                .addRow(new PortRow(
                        PortDef.create("frequency", "geometry_node.port.frequency", PortType.STRING, ""),
                        null, UIHint.INPUT, null, null
                ))
                // 第 2 行: 只有执行流输出 (作为事件入口)
                .addRow(new PortRow(
                        null,
                        StandardPorts.FLOW_OUT.toExec(),
                        UIHint.DEFAULT, null, null
                ))
                .build();
    }

    @Override
    public ExecutionResult execute(ExecutionContext context) {
        // 作为事件入口，它的 execute 逻辑非常简单：直接启动流程往后走即可
        return next(StandardPorts.FLOW_OUT.getId());
    }
}