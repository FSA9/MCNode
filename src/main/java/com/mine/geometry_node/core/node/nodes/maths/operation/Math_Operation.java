package com.mine.geometry_node.core.node.nodes.maths.operation;

import com.mine.geometry_node.core.execution.ExecutionContext;
import com.mine.geometry_node.core.node.NodeData;
import com.mine.geometry_node.core.node.nodes.*;
import net.minecraft.network.chat.Component;

import java.util.Map;

public class Math_Operation extends BaseNode {

    public static final String TYPE_ID = "math_operation";

    @Override
    public NodeDef getDefaultDefinition() {
        // 刚从菜单拖出来时，默认当作 "+" 处理
        return buildDef("+");
    }

    @Override
    public NodeDef getDefinition(NodeData instanceData) {
        String operator = (String) instanceData.properties.getOrDefault("operator", "+");
        return buildDef(operator);
    }

    private NodeDef buildDef(String operator) {
        NodeDef.Builder builder = NodeDef.builder(TYPE_ID, NodeType.MATH, Component.translatable("geometry_node.node.math_operation"));

        // 1: 右侧 VALUE 输出
        builder.addRow(new PortRow(
                null,
                StandardPorts.VALUE.toOutput(),
                UIHint.DEFAULT, null, null
        ));

        // 2: 下拉框
        builder.addRow(new PortRow(
                null, null, UIHint.SELECT, null,
                Map.of(
                        "property_key", "operator",
                        "options", new String[]{"+", "-", "sin", "cos"}
                )
        ));

        // 3: input 1
        builder.addRow(new PortRow(
                StandardPorts.VALUE.toInputWithIndex(1),
                null, UIHint.INPUT, null, null
        ));

        // 4: input 2
        if ("+".equals(operator) || "-".equals(operator)) {
            builder.addRow(new PortRow(
                    StandardPorts.VALUE.toInputWithIndex(2),
                    null, UIHint.INPUT, null, null
            ));
        }

        return builder.build();
    }

    @Override
    public Object compute(ExecutionContext context, String portName) {
        // 只有请求输出端口时才计算
        if (!StandardPorts.VALUE.getId().equals(portName)) return null;

        // 获取操作符
        String operator = (String) context.getNodeProperty("operator");
        if (operator == null) operator = "+";

        // 获取输入值
        Float v1 = getInput(context, "value_1", Float.class);
        if (v1 == null) v1 = 0.0f;

        switch (operator) {
            case "+":
                Float v2Add = getInput(context, "value_2", Float.class);
                return v1 + (v2Add != null ? v2Add : 0.0f);
            case "-":
                Float v2Sub = getInput(context, "value_2", Float.class);
                return v1 - (v2Sub != null ? v2Sub : 0.0f);
            case "sin":
                return (float) Math.sin(v1);
            case "cos":
                return (float) Math.cos(v1);
            default:
                return 0.0f;
        }
    }
}