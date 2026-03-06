package com.mine.geometry_node.core.execution;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * [执行上下文接口]
 * <p>
 * 定义了节点在执行期间可以访问的“环境能力”。
 * 这是一个 {@code Facade} (外观模式) 接口，用于向节点隐藏 {@link GraphProcess} 的底层复杂性（如指针操作、序列化逻辑）。
 * 节点只能通过此接口与世界交互或读写变量。
 */
public interface ExecutionContext {

    /**
     * 获取当前图所属的世界。
     */
    ServerLevel getLevel();

    /**
     * 获取绑定该图的实体（如果有）。
     * @return 实体对象，如果图是依附于非实体对象（如全局事件）运行，可能为 null。
     */
    @Nullable
    Entity getEntity();

    /**
     * [变量读取] 获取局部变量。
     * <p>
     * 这里的变量系统应设计为强类型安全，建议后续配合 VarType 使用。
     * @param name 变量名
     * @return 变量值，若不存在则返回 null
     */
    @Nullable
    Object getVariable(String name);

    /**
     * [变量写入] 设置局部变量。
     * <p>
     * 实现类需在此处进行类型白名单检查 (Int/Float/String/UUID/BlockPos)，
     * 拒绝不支持序列化的复杂对象。
     * @param name 变量名
     * @param value 变量值
     */
    void setVariable(String name, Object value);

    /**
     * 获取当前运行的图 ID（用于调试或跨图调用）。
     */
    String getGraphId();

    /**
     * [数据拉取] 获取连接到指定输入端口的上游数据。
     * <p>
     * 该方法会根据 {@link RuntimeGraphIndex} 自动查找是谁连到了当前节点的 portName 端口，
     * 并递归触发上游节点的求值逻辑。
     *
     * @param portName 当前节点的输入端口名
     * @return 上游节点返回的数据，若未连接或执行异常则返回 null
     */
    @Nullable
    Object getInputValue(String portName);

    /**
     * [新增] 获取节点端口的静态默认值。
     * 对应 JSON 中的 "inputs" 字段。通常用于当端口未连线时的回落值。
     */
    @Nullable
    Object getStaticInput(String portName);

    /**
     * [配置获取] 获取当前执行节点的静态配置属性。
     * 这些属性来自于 JSON 中的 "properties" 字段，在 RuntimeGraphIndex 构建时预加载。
     * @param key 属性名
     * @return 属性值 (原始类型: String, Number, Boolean, List 等)，若不存在返回 null。
     */
    @Nullable
    Object getNodeProperty(String key);

    /**
     * [类型安全配置获取]
     * 尝试获取配置并转换为指定类型，若失败或不存在则返回默认值。
     */
    default <T> T getConfig(String key, Class<T> type, T defaultValue) {
        Object val = getNodeProperty(key);
        if (val == null) return defaultValue;

        // 简单的类型匹配
        if (type.isInstance(val)) {
            return type.cast(val);
        }

        // 数值类型宽容转换 (Integer -> Double 等)
        if (val instanceof Number num) {
            if (type == Integer.class) return type.cast(num.intValue());
            if (type == Float.class) return type.cast(num.floatValue());
            if (type == Double.class) return type.cast(num.doubleValue());
            if (type == Long.class) return type.cast(num.longValue());
        }

        return defaultValue;
    }

    /**
     * [事件参数读取] 获取系统注入的底层物理事件参数（如方块坐标、伤害来源）。
     * 与普通的局部变量隔离，防止被同名变量覆盖。
     * @param key 参数名称 (例如 "evt_pos")
     * @return 参数值
     */
    @Nullable
    Object getEventData(String key);

    /**
     * [事件参数写入] 仅供引擎在分发事件时调用，向运行时注入瞬时环境数据。
     */
    void setEventData(String key, Object value);

    /**
     * 检查当前执行节点是否定义了某个特定的输入端口。
     * 用于 Flow_Switch 等动态端口节点进行循环探测。
     */
    boolean hasPort(String portName);
}