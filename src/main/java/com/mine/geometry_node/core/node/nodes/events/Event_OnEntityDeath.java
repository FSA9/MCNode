package com.mine.geometry_node.core.node.nodes.events;

import com.mine.geometry_node.core.node.nodes.*;
import net.minecraft.network.chat.Component;

public class Event_OnEntityDeath extends BaseEventNode { // <- 1. 继承 BaseEventNode

    public static final String TYPE_ID = "event_on_entity_death";

    @Override
    public NodeDef getDefaultDefinition() {
        return NodeDef.builder(TYPE_ID, NodeType.EVENT, Component.translatable("geometry_node.node.event_on_entity_death"))
                .addRow(new PortRow(null, StandardPorts.FLOW_OUT.toExec(), UIHint.DEFAULT, null, null))
                .addRow(new PortRow(null, StandardPorts.ENTITY.toOutput(), UIHint.DEFAULT, null, null))
                .addRow(new PortRow(null, StandardPorts.DAMAGE_TYPE.toOutput(), UIHint.DEFAULT, null, null))
                .addRow(new PortRow(null, StandardPorts.ATTACK_SOURCE.toOutput(), UIHint.DEFAULT, null, null))
                .addRow(new PortRow(null, StandardPorts.DIRECT_SOURCE.toOutput(), UIHint.DEFAULT, null, null))
                .build();
    }
}