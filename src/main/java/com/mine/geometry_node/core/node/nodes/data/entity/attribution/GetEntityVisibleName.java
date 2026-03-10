package com.mine.geometry_node.core.node.nodes.data.entity.attribution;

import com.mine.geometry_node.core.execution.ExecutionContext;
import com.mine.geometry_node.core.node.nodes.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

import java.util.List;

public class GetEntityVisibleName extends BaseNode {

    public static final String TYPE_ID = "get_entity_visible_name";

    @Override
    public NodeDef getDefaultDefinition() {
        return NodeDef.builder(TYPE_ID, NodeType.DATA, Component.translatable("geometry_node.node.get_entity_visible_name"))
                .addRow(new PortRow(
                        StandardPorts.TARGET.toInput(),
                        PortDef.create("name", "geometry_node.port.name", PortType.STRING),
                        UIHint.DEFAULT, null, null
                ))
                .build();
    }

    @Override
    public Object compute(ExecutionContext context, String portName) {
        if (!"name".equals(portName)) return null;

        List<Entity> targets = getTargets(context, StandardPorts.TARGET.getId());
        if (targets.isEmpty()) return null;

        // 获取纯文本格式的实体显示名称
        return targets.getFirst().getName().getString();
    }
}