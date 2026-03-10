package com.mine.geometry_node.core.node.nodes.actions.entity;

import com.mine.geometry_node.core.execution.ExecutionContext;
import com.mine.geometry_node.core.node.nodes.*;
import com.mine.geometry_node.core.node.nodes.actions.BaseActionNode;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public class SendMessage extends BaseActionNode {

    public static final String TYPE_ID = "action_send_message";

    @Override
    public NodeDef getDefaultDefinition() {
        return NodeDef.builder(TYPE_ID, NodeType.ACTION, Component.translatable("geometry_node.node.action_send_message"))
                .addRow(new PortRow(StandardPorts.FLOW_IN.toExec(), StandardPorts.FLOW_OUT.toExec(), UIHint.DEFAULT, null, null))
                .addRow(new PortRow(StandardPorts.TARGET.toInput(), null, UIHint.DEFAULT, null, null))
                .addRow(new PortRow(StandardPorts.MESSAGE.toInput("Hello Geometry Node!"), null, UIHint.INPUT, null, null))
                .build();
    }

    @Override
    protected void performAction(ExecutionContext context) {
//        System.out.print(11111111);
        List<Entity> targets = getTargets(context, StandardPorts.TARGET.getId());
        if (targets.isEmpty()) return;

        String message = getInput(context, StandardPorts.MESSAGE.getId(), String.class);
        if (message == null) message = "";

        for (Entity target : targets) {
            if (target instanceof Player player) {
                player.sendSystemMessage(Component.literal(message));
            }
        }
    }
}