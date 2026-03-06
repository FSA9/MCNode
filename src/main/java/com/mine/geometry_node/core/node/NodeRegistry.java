package com.mine.geometry_node.core.node;

import com.mine.geometry_node.core.node.nodes.BaseNode;
import com.mine.geometry_node.core.node.nodes.NodeDef;
import com.mine.geometry_node.core.node.nodes.actions.Action_SendMessage;
import com.mine.geometry_node.core.node.nodes.events.Event_OnBlockBreak;
import com.mine.geometry_node.core.node.nodes.events.Event_OnBlockPlace;
import com.mine.geometry_node.core.node.nodes.events.Event_OnEntityDeath;
import com.mine.geometry_node.core.node.nodes.flows.Flow_Switch;
import com.mine.geometry_node.core.node.nodes.functions.communication.Communication_ReceiveBlueprint;
import com.mine.geometry_node.core.node.nodes.functions.communication.Communication_TriggerBlueprint;
import com.mine.geometry_node.core.node.nodes.functions.time.Function_Delay_s;
import com.mine.geometry_node.core.node.nodes.maths.operation.Math_Operation;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * [注册中心] 节点逻辑管理器。
 * <p>
 * 职责：
 * 1. 维护全服唯一的节点逻辑单例 (BaseNode)。
 * 2. 缓存所有节点的元数据 (NodeDef)，供前端 UI 高效读取。
 * 3. 作为 Mod 加载阶段的初始化入口，注册所有内置节点。
 */
public class NodeRegistry {
    // 全局单例
    public static final NodeRegistry INSTANCE = new NodeRegistry();

    // 核心存储: Key = Node Type ID
    private final Map<String, BaseNode> registry = new HashMap<>();

    // 元数据缓存: 仅缓存 getDefaultDefinition()，供右键菜单使用
    private final Map<String, NodeDef> defaultDefCache = new LinkedHashMap<>();

    private NodeRegistry() {}

    public void init() {
        System.out.println("[GeometryNode] Registering built-in nodes...");

        // --- 事件节点 (Events) ---
        register(new Event_OnBlockBreak());
        register(new Event_OnBlockPlace());
        register(new Event_OnEntityDeath());

        // --- 动作节点 (Actions) ---
        register(new Action_SendMessage());
//        register(new Action_UseItem());

        // --- 控制流节点 (Flow) ---
        register(new Flow_Switch());

        // --- 逻辑节点 (Logics) ---
//        register(new Logic_Gate());

        // --- 数学节点 (Math) ---
        register(new Math_Operation());

        // --- 功能节点 (Functions) ---
        register(new Function_Delay_s());
        register(new Communication_ReceiveBlueprint());
        register(new Communication_TriggerBlueprint());


        System.out.println("[GeometryNode] Successfully registered " + registry.size() + " nodes.");
    }

    public void register(BaseNode node) {
        if (node == null) throw new IllegalArgumentException("Cannot register null node");

        NodeDef def = node.getDefaultDefinition();
        String typeId = def.typeId();

        if (registry.containsKey(typeId)) {
            throw new IllegalStateException("Duplicate node type registered: " + typeId);
        }

        // 1. 存逻辑单例
        registry.put(typeId, node);
        // 2. 存元数据缓存
        defaultDefCache.put(typeId, def);
    }

    /**
     * [后端专用] 获取逻辑单例
     */
    @Nullable
    public BaseNode get(String typeId) {
        return registry.get(typeId);
    }

    /** [前端专用] 获取该节点的默认长相 (用于菜单) */
    @Nullable
    public NodeDef getDefaultDefinition(String typeId) {
        return defaultDefCache.get(typeId);
    }

    public boolean has(String typeId) {
        return registry.containsKey(typeId);
    }

    public Set<String> getAllTypeIds() {
        return Collections.unmodifiableSet(registry.keySet());
    }

    public Collection<NodeDef> getAllDefinitions() {
        return Collections.unmodifiableCollection(defaultDefCache.values());
    }
}