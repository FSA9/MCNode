package com.mine.geometry_node.core.execution.variables;

import net.minecraft.nbt.Tag;

/**
 * 复杂变量类型的序列化说明书。
 * @param <T> 该序列化器负责处理的 Java 目标类
 */
public interface VariableSerializer<T> {

    /**
     * @return 存入 NBT 的唯一字符串标识 (例如 "vec3", "block_pos")
     */
    String getTypeId();

    /**
     * @return 负责处理的 Java 类型
     */
    Class<T> getTargetClass();

    /**
     * 打包：将 Java 对象转换为原版 NBT 标签
     */
    Tag serialize(T value);

    /**
     * 拆包：将 NBT 标签还原为 Java 对象
     */
    T deserialize(Tag tag);
}