package com.mine.geometry_node.core.node.nodes.attributes;

import com.mine.geometry_node.core.execution.ExecutionContext;
import com.mine.geometry_node.core.node.nodes.*;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class GetAttribute extends BaseNode {

    public static final String TYPE_ID = "attribute_get";

    @Override
    public NodeDef getDefaultDefinition() {
        return NodeDef.builder(TYPE_ID, NodeType.DATA, Component.translatable("geometry_node.node.attribute_get"))
                .addRow(new PortRow(
                        PortDef.create("target", "geometry_node.port.target", PortType.ANY),
                        PortDef.create("attr_value", "geometry_node.port.attr_value", PortType.ANY),
                        UIHint.DEFAULT, null, null))
                .addRow(new PortRow(
                        PortDef.create("attr_name", "geometry_node.port.attr_name", PortType.STRING, ""),
                        null, UIHint.INPUT, null, null))
                .build();
    }

    @Override
    @Nullable
    public Object compute(ExecutionContext context, String portName) {
        if (!"attr_value".equals(portName)) return null;

        Object target = getInput(context, "target", Object.class);
        String attrName = getInput(context, "attr_name", String.class);

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