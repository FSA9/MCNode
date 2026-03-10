package com.mine.geometry_node.core.node.nodes.data.entity.attribution;

import com.mine.geometry_node.core.execution.ExecutionContext;
import com.mine.geometry_node.core.node.nodes.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

import java.util.List;

public class GetGameMode extends BaseNode {

    public static final String TYPE_ID = "get_game_mode";

    @Override
    public NodeDef getDefaultDefinition() {
        return NodeDef.builder(TYPE_ID, NodeType.DATA, Component.translatable("geometry_node.node.get_game_mode"))
                .addRow(new PortRow(
                        StandardPorts.TARGET.toInput(),
                        PortDef.create("gamemode", "geometry_node.port.gamemode", PortType.STRING),
                        UIHint.DEFAULT, null, null
                ))
                .build();
    }

    @Override
    public Object compute(ExecutionContext context, String portName) {
        if (!"gamemode".equals(portName)) return null;

        List<Entity> targets = getTargets(context, StandardPorts.TARGET.getId());
        if (targets.isEmpty()) return null;

        // 强转为服务端玩家，才能精准获取真实的游戏模式
        if (targets.getFirst() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            return serverPlayer.gameMode.getGameModeForPlayer().getName(); // 返回 "survival" 等字符串
        }

        return null;
    }
}