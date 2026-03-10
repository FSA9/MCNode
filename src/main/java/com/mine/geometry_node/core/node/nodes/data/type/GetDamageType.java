package com.mine.geometry_node.core.node.nodes.data.type;

import com.mine.geometry_node.core.execution.ExecutionContext;
import com.mine.geometry_node.core.node.nodes.*;
import net.minecraft.network.chat.Component;

import java.util.Map;

public class GetDamageType extends BaseNode {

    public static final String TYPE_ID = "get_damage_type";

    @Override
    public NodeDef getDefaultDefinition() {
        return NodeDef.builder(TYPE_ID, NodeType.DATA, Component.translatable("geometry_node.node.get_damage_type"))
                .addRow(new PortRow(
                        null,
                        StandardPorts.DAMAGE_TYPE.toOutput(),

                        // --- 下面是给前端 UI 层的契约指令 ---

                        // 告诉 UI：不要用普通的输入框，给我上自定义组件！
                        UIHint.CUSTOM,

                        // 告诉 UI：去找那个叫 "dynamic_registry_select" 的下拉框控件
                        "dynamic_registry_select",

                        // 告诉 UI：调用 RegistryDataManager 时，去查 "minecraft:damage_type" 这个表
                        Map.of("registry", "minecraft:damage_type")
                ))
                .build();
    }

    @Override
    public Object compute(ExecutionContext context, String portName) {
        if (StandardPorts.DAMAGE_TYPE.getId().equals(portName)) {
            // 直接读取玩家在 UI 下拉框里选中的那个字符串，原封不动地交出去
            return getInput(context, "selected_type", String.class);
        }
        return null;
    }
}