package com.mine.geometry_node.core.node.nodes.data.entity.attribution;

import com.mine.geometry_node.core.execution.ExecutionContext;
import com.mine.geometry_node.core.node.nodes.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

import java.util.List;

public class GetEntityEyePosition extends BaseNode {

    public static final String TYPE_ID = "get_entity_eye_position";

    @Override
    public NodeDef getDefaultDefinition() {
        return NodeDef.builder(TYPE_ID, NodeType.DATA, Component.translatable("geometry_node.node.get_entity_eye_position"))
                .addRow(new PortRow(
                        StandardPorts.TARGET.toInput(),
                        StandardPorts.XYZ.toOutput(),
                        UIHint.DEFAULT, null, null
                ))
                .build();
    }

    @Override
    public Object compute(ExecutionContext context, String portName) {
        if (!StandardPorts.XYZ.getId().equals(portName)) return null;

        List<Entity> targets = getTargets(context, StandardPorts.TARGET.getId());
        if (targets.isEmpty()) return null;

        return targets.getFirst().getEyePosition(); // 返回 Vec3
    }
}