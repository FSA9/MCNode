package com.mine.geometry_node.core.node;

import com.mine.geometry_node.core.node.nodes.BaseNode;
import com.mine.geometry_node.core.node.nodes.NodeDef;
import com.mine.geometry_node.core.node.nodes.actions.Action_SendMessage;
import com.mine.geometry_node.core.node.nodes.attributes.GetAttribute;
import com.mine.geometry_node.core.node.nodes.attributes.SetAttribute;
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

public class NodeRegistry {
    public static final NodeRegistry INSTANCE = new NodeRegistry();
    // 后端核心存储
    private final Map<String, BaseNode> registry = new HashMap<>();
    private final Map<String, NodeDef> defaultDefCache = new LinkedHashMap<>();

    // 前端目录树
    public final NodeCategory ROOT = new NodeCategory("geometry_node.menu.root");

    // 一级目录
    public final NodeCategory ACTIONS = new NodeCategory("geometry_node.menu.actions");
    public final NodeCategory EVENTS = new NodeCategory("geometry_node.menu.events");
    public final NodeCategory FLOWS = new NodeCategory("geometry_node.menu.flow");
    public final NodeCategory FUNCTIONS = new NodeCategory("geometry_node.menu.functions");
    public final NodeCategory LOGICS = new NodeCategory("geometry_node.menu.logics");
    public final NodeCategory MATHS = new NodeCategory("geometry_node.menu.maths");
    public final NodeCategory ATTRIBUTES = new NodeCategory("geometry_node.menu.attributes");

    // 二级目录
    public final NodeCategory MATHS_OPERATION = new NodeCategory("geometry_node.menu.operation");
    public final NodeCategory COMM = new NodeCategory("geometry_node.menu.communication");
    public final NodeCategory TEXT = new NodeCategory("geometry_node.menu.text");
    public final NodeCategory TIME = new NodeCategory("geometry_node.menu.time");
    public final NodeCategory VECTOR = new NodeCategory("geometry_node.menu.vector");

    private NodeRegistry() {
        ROOT.addChild(EVENTS)
            .addChild(ACTIONS)
            .addChild(ATTRIBUTES)
            .addChild(FLOWS)
            .addChild(MATHS)
            .addChild(LOGICS)
            .addChild(FUNCTIONS);

        MATHS.addChild(MATHS_OPERATION);

        FUNCTIONS.addChild(COMM)
                .addChild(TEXT)
                .addChild(TIME)
                .addChild(VECTOR);
    }

    public void init() {
        System.out.println("[GeometryNode] Registering built-in nodes...");

        // Events
        register(EVENTS, new Event_OnBlockBreak());
        register(EVENTS, new Event_OnBlockPlace());
        register(EVENTS, new Event_OnEntityDeath());

        // Actions
        register(ACTIONS, new Action_SendMessage());

        // Attribute
        register(ATTRIBUTES, new SetAttribute());
        register(ATTRIBUTES, new GetAttribute());

        // Flows
        register(FLOWS, new Flow_Switch());

        // Maths
        register(MATHS_OPERATION, new Math_Operation());

        // Functions
        register(FUNCTIONS, new Function_Delay_s());
        register(COMM, new Communication_ReceiveBlueprint());
        register(COMM, new Communication_TriggerBlueprint());

        System.out.println("[GeometryNode] Successfully registered " + registry.size() + " nodes.");
    }

    public void register(NodeCategory category, BaseNode node) {
        if (node == null || category == null) {
            throw new IllegalArgumentException("Cannot register null node or null category");
        }

        NodeDef def = node.getDefaultDefinition();
        String typeId = def.typeId();

        if (registry.containsKey(typeId)) {
            throw new IllegalStateException("Duplicate node type registered: " + typeId);
        }

        registry.put(typeId, node);
        defaultDefCache.put(typeId, def);

        category.addNode(node);
    }

    @Nullable
    public BaseNode get(String typeId) {
        return registry.get(typeId);
    }

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