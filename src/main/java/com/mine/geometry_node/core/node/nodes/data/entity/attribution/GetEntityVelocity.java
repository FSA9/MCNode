package com.mine.geometry_node.core.node.nodes.data.entity.attribution;

import com.mine.geometry_node.core.execution.ExecutionContext;
import com.mine.geometry_node.core.node.nodes.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

import java.util.List;

public class GetEntityVelocity extends BaseNode {

    public static final String TYPE_ID = "get_entity_velocity";

    @Override
    public NodeDef getDefaultDefinition() {
        return NodeDef.builder(TYPE_ID, NodeType.DATA, Component.translatable("geometry_node.node.get_entity_velocity"))
                .addRow(new PortRow(
                        StandardPorts.TARGET.toInput(),
                        PortDef.create("velocity", "geometry_node.port.velocity", PortType.XYZ),
                        UIHint.DEFAULT, null, null
                ))
                .build();
    }

    @Override
    public Object compute(ExecutionContext context, String portName) {
        if (!"velocity".equals(portName)) return null;

        List<Entity> targets = getTargets(context, StandardPorts.TARGET.getId());
        if (targets.isEmpty()) return null;

        return targets.getFirst().getDeltaMovement(); // 返回 Vec3
    }
}
