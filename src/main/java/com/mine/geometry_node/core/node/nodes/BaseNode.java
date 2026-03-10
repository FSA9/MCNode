package com.mine.geometry_node.core.node.nodes;

import com.mine.geometry_node.core.execution.ExecutionContext;
import com.mine.geometry_node.core.execution.ExecutionResult;
import com.mine.geometry_node.core.node.NodeData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

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

        // 完全匹配
        if (type.isInstance(val)) {
            return type.cast(val);
        }

        // 基础数值互转
        if (val instanceof Number n) {
            if (type == Integer.class) return type.cast(n.intValue());
            if (type == Float.class) return type.cast(n.floatValue());
            if (type == Double.class) return type.cast(n.doubleValue());
            if (type == Boolean.class) return type.cast(n.floatValue() > 0);
        }

        // bool (true -> 1, false -> 0)
        if (val instanceof Boolean b) {
            if (type == Integer.class) return type.cast(b ? 1 : 0);
            if (type == Float.class) return type.cast(b ? 1.0f : 0.0f);
            if (type == Double.class) return type.cast(b ? 1.0 : 0.0);
        }

        // to string
        if (type == String.class) {
            if (val instanceof Entity e) return type.cast(e.getStringUUID());
            if (val instanceof Vec3 v) return type.cast(String.format("[%.2f, %.2f, %.2f]", v.x, v.y, v.z));
            if (val instanceof BlockPos p) return type.cast(String.format("[%d, %d, %d]", p.getX(), p.getY(), p.getZ()));

            // 方块/物品转 Registry ID (字符串)
            if (val instanceof BlockState bs) {
                return type.cast(BuiltInRegistries.BLOCK.getKey(bs.getBlock()).toString());
            }
            if (val instanceof ItemStack is) {
                return type.cast(BuiltInRegistries.ITEM.getKey(is.getItem()).toString());
            }
            if (val instanceof Item item) {
                return type.cast(BuiltInRegistries.ITEM.getKey(item).toString());
            }

            return type.cast(String.valueOf(val));
        }

        // List -> Vec3
        if (type == Vec3.class && val instanceof List<?> list) {
            if (list.size() >= 3 && list.get(0) instanceof Number) {
                double x = ((Number) list.get(0)).doubleValue();
                double y = ((Number) list.get(1)).doubleValue();
                double z = ((Number) list.get(2)).doubleValue();
                return type.cast(new Vec3(x, y, z));
            }
        }

        // 字符串反解析
        if (val instanceof String s) {
            // 解析 Entity (UUID)
            if (type == Entity.class) {
                try {
                    UUID uuid = UUID.fromString(s);
                    if (ctx.getLevel() != null) return type.cast(ctx.getLevel().getEntity(uuid));
                } catch (Exception ignored) {}
            }

            // 解析 BlockState
            if (type == BlockState.class) {
                try {
                    ResourceLocation resLoc = ResourceLocation.tryParse(s);
                    if (resLoc != null) {
                        Block block = BuiltInRegistries.BLOCK.get(resLoc);
                        if (block != null) return type.cast(block.defaultBlockState());
                    }
                } catch (Exception ignored) {}
            }

            // 解析 Item
            if (type == Item.class) {
                try {
                    ResourceLocation resLoc = ResourceLocation.tryParse(s);
                    if (resLoc != null) {
                        return type.cast(BuiltInRegistries.ITEM.get(resLoc));
                    }
                } catch (Exception ignored) {}
            }

//            // 解析 Vec3
//            if (type == Vec3.class) {
//                try {
//                    String clean = s.replaceAll("[\\[\\]\\s]", "");
//                    String[] parts = clean.split(",");
//                    if (parts.length >= 3) {
//                        return type.cast(new Vec3(
//                                Double.parseDouble(parts[0]),
//                                Double.parseDouble(parts[1]),
//                                Double.parseDouble(parts[2])
//                        ));
//                    }
//                } catch (Exception ignored) {}
//            }

            // 解析 Boolean
            if (type == Boolean.class) {
                if ("true".equalsIgnoreCase(s)) return type.cast(true);
                if ("false".equalsIgnoreCase(s)) return type.cast(false);
            }
        }

        System.err.println("getInput Error!!!");
        return null;
    }

    /**
     * [目标解析] 获取受影响实体。支持静默失败 (Action Fall)。
     */
    protected List<Entity> getTargets(ExecutionContext ctx, String portName) {
        Object val = getInput(ctx, portName, Object.class);

        // 情况 1: 没有任何输入 (没连线且没配置) -> 静默失败，返回空列表
        if (val == null) {
            return List.of();
        }

        // 情况 2: 单体
        if (val instanceof Entity entity) {
            return List.of(entity);
        }

        // 情况 3: 列表聚合
        if (val instanceof List<?> list) {
            List<Entity> entities = new ArrayList<>();
            for (Object obj : list) {
                if (obj instanceof Entity e) entities.add(e);
                else if (obj instanceof List<?> nested) { // 扁平化一层
                    for (Object n : nested) if (n instanceof Entity e) entities.add(e);
                }
            }
            return entities;
        }

        // 情况 4: 字符串 UUID 解析
        if (val instanceof String s) {
            try {
                UUID uuid = UUID.fromString(s);
                if (ctx.getLevel() != null) {
                    Entity e = ctx.getLevel().getEntity(uuid);
                    if (e != null) return List.of(e);
                }
            } catch (Exception ignored) {}
        }



        return List.of();
    }

    protected ExecutionResult finish() { return ExecutionResult.finish(); }
    protected ExecutionResult next(String portName) { return ExecutionResult.next(portName); }
}