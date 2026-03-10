package com.mine.geometry_node.core.node.nodes.data;

import com.mine.geometry_node.core.execution.ExecutionContext;
import com.mine.geometry_node.core.execution.ExecutionResult;
import com.mine.geometry_node.core.node.nodes.*;
import net.minecraft.network.chat.Component;

import java.util.List;

public class SetAttribute extends BaseNode {

    public static final String TYPE_ID = "set_attribute";

    @Override
    public NodeDef getDefaultDefinition() {
        return NodeDef.builder(TYPE_ID, NodeType.DATA, Component.translatable("geometry_node.node.set_attribute"))
                .addRow(new PortRow(StandardPorts.FLOW_IN.toExec(), StandardPorts.FLOW_OUT.toExec(), UIHint.DEFAULT, null, null))
                .addRow(new PortRow(StandardPorts.TARGET.toInput(), null, UIHint.DEFAULT, null, null))
                .addRow(new PortRow(StandardPorts.NAME.toInput(), null, UIHint.INPUT, null, null))
                .addRow(new PortRow(StandardPorts.ANY_VALUE.toInput(), null, UIHint.INPUT, null, null))
                .build();
    }

    @Override
    public ExecutionResult execute(ExecutionContext context) {
        Object target = getInput(context, StandardPorts.TARGET.getId(), Object.class);
        String attrName = getInput(context, StandardPorts.NAME.getId(), String.class);
        Object attrValue = getInput(context, StandardPorts.ANY_VALUE.getId(), Object.class);

        if (attrName != null && !attrName.trim().isEmpty()) {
            // 如果传入的是列表，自动拆解给每一个实体赋值
            if (target instanceof List<?> list) {
                for (Object item : list) {
                    context.setPersistentAttribute(item, attrName, attrValue);
                }
            } else {
                // 单个实体 或 null(全局)
                context.setPersistentAttribute(target, attrName, attrValue);
            }
        }

        return next(StandardPorts.FLOW_OUT.getId());
    }
}