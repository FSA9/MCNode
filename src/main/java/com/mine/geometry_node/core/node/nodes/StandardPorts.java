package com.mine.geometry_node.core.node.nodes;

/**
 * [标准端口字典]
 * 统一定义全系统共用的端口 ID、类型和翻译键。
 */
public enum StandardPorts {
    // Flow
    FLOW_IN("flow_in", PortType.EXECUTION),
    FLOW_OUT("flow_out", PortType.EXECUTION),

    // Bool
    CASE("case", PortType.BOOLEAN),

    // Float
    VALUE("value", PortType.FLOAT),
    TIME("time", PortType.FLOAT),

    // String
    MESSAGE("message", PortType.STRING),
    DIMENSION("dimension", PortType.STRING),
    DAMAGE_TYPE("damage_type", PortType.STRING),

    // Entity
    ENTITY("entity", PortType.ENTITY),
    TARGET("target", PortType.ENTITY),
    ATTACK_SOURCE("attack_source", PortType.ENTITY),
    DIRECT_SOURCE("direct_source", PortType.ENTITY),

    // Block
    BLOCK_STATE("block_state", PortType.BLOCK),

    // XYZ
    XYZ("xyz", PortType.XYZ);

    private final String id;
    private final PortType type;

    StandardPorts(String id, PortType type) {
        this.id = id;
        this.type = type;
    }

    public String getId() { return id; }
    public PortType getType() { return type; }
    public String getTranslationKey() { return "geometry_node.port." + id; }

    // --- 快捷构建工厂 (基础) ---

    public PortDef toInput() {
        return PortDef.create(id, getTranslationKey(), type);
    }

    public PortDef toInput(Object defaultValueOverride) {
        return PortDef.create(id, getTranslationKey(), type, defaultValueOverride);
    }

    public PortDef toOutput() {
        return PortDef.create(id, getTranslationKey(), type);
    }

    public PortDef toExec() {
        if (type != PortType.EXECUTION) throw new IllegalStateException("Not exec port: " + id);
        return PortDef.exec(id, getTranslationKey());
    }

    // --- 增强构建工厂 (带后缀支持) ---

    /**
     * 生成带序号后缀的输入端口。
     * 例如 VALUE.toInputWithIndex(1) -> id="value_1", name="...value_1"
     */
    public PortDef toInputWithIndex(int index) {
        String newId = id + "_" + index;
        return PortDef.create(newId, getTranslationKey(), type);
    }

    /**
     * 生成带序号后缀的输入端口 (带默认值覆盖)。
     */
    public PortDef toInputWithIndex(int index, Object defaultValueOverride) {
        String newId = id + "_" + index;
        return PortDef.create(newId, getTranslationKey(), type, defaultValueOverride);
    }

    /**
     * 生成带序号后缀的输出端口。
     */
    public PortDef toOutputWithIndex(int index) {
        String newId = id + "_" + index;
        return PortDef.create(newId, getTranslationKey(), type);
    }

    /**
     * 生成带序号后缀的执行流端口。
     */
    public PortDef toExecWithIndex(int index) {
        if (type != PortType.EXECUTION) throw new IllegalStateException("Not exec port: " + id);
        String newId = id + "_" + index;
        return PortDef.exec(newId, getTranslationKey());
    }
}