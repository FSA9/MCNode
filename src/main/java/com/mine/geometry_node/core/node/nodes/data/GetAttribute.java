package com.mine.geometry_node.core.node.nodes.data;

import com.mine.geometry_node.core.execution.ExecutionContext;
import com.mine.geometry_node.core.node.nodes.*;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class GetAttribute extends BaseNode {

    public static final String TYPE_ID = "get_attribute";

    @Override
    public NodeDef getDefaultDefinition() {
        return NodeDef.builder(TYPE_ID, NodeType.DATA, Component.translatable("geometry_node.node.get_attribute"))
                .addRow(new PortRow(StandardPorts.TARGET.toInput(), StandardPorts.ANY_VALUE.toInput(), UIHint.DEFAULT, null, null))
                .addRow(new PortRow(StandardPorts.NAME.toInput(), null, UIHint.INPUT, null, null))
                .build();
    }

    @Override
    @Nullable
    public Object compute(ExecutionContext context, String portName) {
        Object target = getInput(context, StandardPorts.TARGET.getId(), Object.class);
        String attrName = getInput(context, StandardPorts.NAME.getId(), String.class);

        if (attrName == null || attrName.trim().isEmpty()) {
            return null;
        }

        // 如果连进来的是一个列表，防御性处理：默认取第一个元素的值
        if (target instanceof List<?> list && !list.isEmpty()) {
            target = list.get(0);
        }

        return context.getPersistentAttribute(target, attrName);
    }
}