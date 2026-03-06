package com.mine.geometry_node.core.execution.variables;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 变量序列化注册表。
 * 负责将 Java 对象和 NBT Tag 进行双向转换。
 */
public class VariableRegistry {

    private static final Map<Class<?>, VariableSerializer<?>> CLASS_TO_SERIALIZER = new HashMap<>();
    private static final Map<String, VariableSerializer<?>> ID_TO_SERIALIZER = new HashMap<>();

    // 包装盒的标准字段名
    private static final String TYPE_KEY = "_gn_type";
    private static final String DATA_KEY = "data";

    /**
     * 注册一个新的序列化器 (支持开闭原则)
     */
    public static <T> void register(VariableSerializer<T> serializer) {
        CLASS_TO_SERIALIZER.put(serializer.getTargetClass(), serializer);
        ID_TO_SERIALIZER.put(serializer.getTypeId(), serializer);
    }

    // --- 静态初始化块：注册内置的复杂类型 (慢车道) ---
    static {
        // 1. UUID
        register(new VariableSerializer<UUID>() {
            @Override public String getTypeId() { return "uuid"; }
            @Override public Class<UUID> getTargetClass() { return UUID.class; }
            @Override public Tag serialize(UUID value) { return NbtUtils.createUUID(value); }
            @Override public UUID deserialize(Tag tag) { return NbtUtils.loadUUID(tag); }
        });

        // 2. BlockPos (采用 Long 压缩存储以节省空间)
        register(new VariableSerializer<BlockPos>() {
            @Override public String getTypeId() { return "block_pos"; }
            @Override public Class<BlockPos> getTargetClass() { return BlockPos.class; }
            @Override public Tag serialize(BlockPos value) { return LongTag.valueOf(value.asLong()); }
            @Override public BlockPos deserialize(Tag tag) { return BlockPos.of(((LongTag) tag).getAsLong()); }
        });

        // 3. Vec3
        register(new VariableSerializer<Vec3>() {
            @Override public String getTypeId() { return "vec3"; }
            @Override public Class<Vec3> getTargetClass() { return Vec3.class; }
            @Override public Tag serialize(Vec3 value) {
                ListTag list = new ListTag();
                list.add(DoubleTag.valueOf(value.x));
                list.add(DoubleTag.valueOf(value.y));
                list.add(DoubleTag.valueOf(value.z));
                return list;
            }
            @Override public Vec3 deserialize(Tag tag) {
                ListTag list = (ListTag) tag;
                return new Vec3(list.getDouble(0), list.getDouble(1), list.getDouble(2));
            }
        });

        // 4. BlockState (使用原版 NbtUtils)
        register(new VariableSerializer<BlockState>() {
            @Override public String getTypeId() { return "block_state"; }
            @Override public Class<BlockState> getTargetClass() { return BlockState.class; }
            @Override public Tag serialize(BlockState value) { return NbtUtils.writeBlockState(value); }
            @Override public BlockState deserialize(Tag tag) {
                return NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), (CompoundTag) tag);
            }
        });
    }

    /**
     * [存盘] 将 Java 对象转换为 NBT
     */
    @Nullable
    public static Tag toTag(Object value) {
        if (value == null) return null;

        // 特殊处理：如果是实体，强制转换为 UUID 存储
        if (value instanceof Entity entity) {
            value = entity.getUUID();
        }

        // --- 快车道：基础类型直接转换 ---
        if (value instanceof Integer i) return IntTag.valueOf(i);
        if (value instanceof Float f) return FloatTag.valueOf(f);
        if (value instanceof String s) return StringTag.valueOf(s);
        if (value instanceof Boolean b) return ByteTag.valueOf(b);

        // --- 慢车道：复杂类型查找注册表包装 ---
        VariableSerializer<Object> serializer = (VariableSerializer<Object>) CLASS_TO_SERIALIZER.get(value.getClass());
        if (serializer != null) {
            CompoundTag wrapper = new CompoundTag();
            wrapper.putString(TYPE_KEY, serializer.getTypeId());
            wrapper.put(DATA_KEY, serializer.serialize(value));
            return wrapper;
        }

        System.err.println("[GeometryNode] Unsupported variable type for saving: " + value.getClass().getSimpleName());
        return null;
    }

    /**
     * [读盘] 将 NBT 还原为 Java 对象
     */
    @Nullable
    public static Object fromTag(Tag tag) {
        if (tag == null) return null;

        // --- 快车道：根据 NBT 基础 ID 直接还原 ---
        if (tag instanceof IntTag i) return i.getAsInt();
        if (tag instanceof FloatTag f) return f.getAsFloat();
        if (tag instanceof StringTag s) return s.getAsString();
        if (tag instanceof ByteTag b) return b.getAsByte() != 0;

        // --- 慢车道：拆包还原 ---
        if (tag instanceof CompoundTag compound && compound.contains(TYPE_KEY, Tag.TAG_STRING)) {
            String typeId = compound.getString(TYPE_KEY);
            VariableSerializer<?> serializer = ID_TO_SERIALIZER.get(typeId);

            if (serializer != null && compound.contains(DATA_KEY)) {
                return serializer.deserialize(compound.get(DATA_KEY));
            } else {
                System.err.println("[GeometryNode] Missing serializer for type: " + typeId);
            }
        }

        return null;
    }

    /**
     * 判断一个对象是否可以被序列化 (用于提前拦截)
     */
    public static boolean isSupported(Object value) {
        if (value == null) return false;
        if (value instanceof Entity || value instanceof Integer || value instanceof Float ||
                value instanceof String || value instanceof Boolean) return true;
        return CLASS_TO_SERIALIZER.containsKey(value.getClass());
    }
}