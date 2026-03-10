package com.mine.geometry_node.core.node.nodes.data.type;

import com.mine.geometry_node.core.execution.ExecutionContext;
import com.mine.geometry_node.core.node.RegistryDataManager;
import com.mine.geometry_node.core.node.nodes.*;
import net.minecraft.network.chat.Component;

import java.util.Map;

public class GetBlockType extends BaseNode {

    public static final String TYPE_ID = "get_block_type";

    @Override
    public NodeDef getDefaultDefinition() {
        return NodeDef.builder(TYPE_ID, NodeType.DATA, Component.translatable("geometry_node.node.get_block_type"))
                .addRow(new PortRow(
                        null,
                        StandardPorts.TYPE.toOutput(),
                        UIHint.SELECT,
                        null,
                        Map.of("options", RegistryDataManager.getAllBlocks())
                ))
                .build();
    }

    @Override
    public Object compute(ExecutionContext context, String portName) {
        if ("block_id".equals(portName)) {
            return getInput(context, "selected_type", String.class); //
        }
        return null;
    }
}