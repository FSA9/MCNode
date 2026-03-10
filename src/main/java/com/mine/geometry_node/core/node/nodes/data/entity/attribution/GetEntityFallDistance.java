package com.mine.geometry_node.core.node.nodes.data.entity.attribution;

import com.mine.geometry_node.core.execution.ExecutionContext;
import com.mine.geometry_node.core.node.nodes.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

import java.util.List;

public class GetEntityFallDistance extends BaseNode {

    public static final String TYPE_ID = "get_entity_fall_distance";

    @Override
    public NodeDef getDefaultDefinition() {
        return NodeDef.builder(TYPE_ID, NodeType.DATA, Component.translatable("geometry_node.node.get_entity_fall_distance"))
                .addRow(new PortRow(
                        StandardPorts.TARGET.toInput(),
                        StandardPorts.VALUE.toOutput(),
                        UIHint.DEFAULT, null, null
                ))
                .build();
    }

    @Override
    public Object compute(ExecutionContext context, String portName) {
        if (!StandardPorts.VALUE.getId().equals(portName)) return null;

        List<Entity> targets = getTargets(context, StandardPorts.TARGET.getId());
        if (targets.isEmpty()) return null;

        return targets.getFirst().fallDistance; // 返回 float
    }
}
