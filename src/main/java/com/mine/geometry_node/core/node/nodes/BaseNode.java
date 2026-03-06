package com.mine.geometry_node.core.node.nodes;

import com.mine.geometry_node.core.execution.ExecutionContext;
import com.mine.geometry_node.core.execution.ExecutionResult;
import com.mine.geometry_node.core.node.NodeData;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.ArrayList;

/**
 * [逻辑定义层] 节点行为基类
 */
public abstract class BaseNode {

    public abstract NodeDef getDefaultDefinition();

    /**
     * [动态元数据引擎] (核心修改)
     * 根据传入的具体实例数据 (NodeData)，动态推演当前节点应该长什么样。
     * 默认行为：不动态变形，直接返回默认长相。子类可以重写此方法实现变形。
     */
    public NodeDef getDefinition(NodeData instanceData) {
        return getDefaultDefinition();
    }

    public final String getTypeId() {
        return getDefaultDefinition().typeId();
    }

    public ExecutionResult execute(ExecutionContext context) {
        return ExecutionResult.finish();
    }

    @Nullable
    public Object compute(ExecutionContext context, String portName) {
        return null;
    }

    // --- 核心数据获取逻辑 (The Grand Unified Accessor) ---

    /**
     * [全能输入获取]
     * 1. 优先从上游连线获取数据 (Context.getInputValue)。
     * 2. 如果没连线，从 JSON 静态配置获取数据 (Context.getNodeConfig)。
     * 3. 自动处理基础类型转换。
     */
    @Nullable
    protected <T> T getInput(ExecutionContext ctx, String portName, Class<T> type) {
        // 1: 查连线
        Object val = ctx.getInputValue(portName);

        // 2: 查静态配置
        if (val == null) {
            val = ctx.getStaticInput(portName);
        }

        // 这种情况理论上不该发生，除非图坏了
        if (val == null) return null;

        // --- 类型转换逻辑 ---

        // 1. 完全匹配
        if (type.isInstance(val)) {
            return type.cast(val);
        }

        // 2. 基础数值互转
        if (type == Integer.class && val instanceof Number n) return type.cast(n.intValue());
        if (type == Float.class && val instanceof Number n) return type.cast(n.floatValue());
        if (type == Double.class && val instanceof Number n) return type.cast(n.doubleValue());
        if (type == Boolean.class && val instanceof Number n) return type.cast(n.floatValue() > 0);

        // 3. 强制转字符串
        if (type == String.class) {
            if (val instanceof net.minecraft.world.entity.Entity e) return type.cast(e.getStringUUID());
            if (val instanceof net.minecraft.world.phys.Vec3 v) return type.cast(String.format("[%.2f, %.2f, %.2f]", v.x, v.y, v.z));
            if (val instanceof net.minecraft.core.BlockPos p) return type.cast(String.format("[%d, %d, %d]", p.getX(), p.getY(), p.getZ()));
            return type.cast(String.valueOf(val));
        }

        // 4. 字符串反解析 (保持不变)
        if (val instanceof String s) {
            if (type == net.minecraft.world.entity.Entity.class) {
                try {
                    java.util.UUID uuid = java.util.UUID.fromString(s);
                    if (ctx.getLevel() != null) return type.cast(ctx.getLevel().getEntity(uuid));
                } catch (Exception ignored) {}
            }
            // ... BlockState 解析保持不变 ...
        }

        return null;
    }

    /**
     * [目标解析] 获取受影响实体。支持静默失败 (Action Fall)。
     */
    protected List<net.minecraft.world.entity.Entity> getTargets(ExecutionContext ctx, String portName) {
        Object val = getInput(ctx, portName, Object.class); // 使用统一入口拿数据

        // 情况 1: 没有任何输入 (没连线且没配置) -> 静默失败，返回空列表
        if (val == null) {
            return List.of();
        }

        // 情况 2: 单体
        if (val instanceof net.minecraft.world.entity.Entity entity) {
            return List.of(entity);
        }

        // 情况 3: 列表聚合
        if (val instanceof List<?> list) {
            List<net.minecraft.world.entity.Entity> entities = new ArrayList<>();
            for (Object obj : list) {
                if (obj instanceof net.minecraft.world.entity.Entity e) entities.add(e);
                else if (obj instanceof List<?> nested) { // 扁平化一层
                    for (Object n : nested) if (n instanceof net.minecraft.world.entity.Entity e) entities.add(e);
                }
            }
            return entities;
        }

        // 情况 4: 字符串 UUID 解析
        if (val instanceof String s) {
            try {
                java.util.UUID uuid = java.util.UUID.fromString(s);
                if (ctx.getLevel() != null) {
                    net.minecraft.world.entity.Entity e = ctx.getLevel().getEntity(uuid);
                    if (e != null) return List.of(e);
                }
            } catch (Exception ignored) {}
        }

        return List.of();
    }

    protected ExecutionResult finish() { return ExecutionResult.finish(); }
    protected ExecutionResult next(String portName) { return ExecutionResult.next(portName); }
}