package com.mine.geometry_node.core.execution;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.Nullable;

import java.io.Reader;
import java.util.*;

/**
 * [运行时图索引] (不可变 / 只读)
 * <p>
 * 直接从 JSON 结构构建。将 JSON 中的“连接关系”预编译为内存中的哈希索引表。
 * <p>
 * 变更日志 (Phase 1.1):
 * - 适配新 JSON 规范 (execution, outputs, inputs, properties)。
 * - 实现静态输入值 (inputs) 与 属性配置 (properties) 的分离索引。
 */
public class RuntimeGraphIndex {

    // ==================================================================================
    // 1. 核心索引字段 (Fields)
    // ==================================================================================

    // 原始 JSON 节点存储 (用于获取 raw data 或调试)
    private final Map<String, JsonObject> nodeDataLookup;

    // 属性索引: NodeID -> { PropertyKey -> Value } (对应 JSON "properties")
    private final Map<String, Map<String, Object>> propertyLookup;

    // 静态输入索引: NodeID -> { PortName -> Value } (对应 JSON "inputs")
    private final Map<String, Map<String, Object>> staticInputLookup;

    // 控制流正向索引: SourceID -> { PortName -> TargetID } (对应 JSON "execution")
    private final Map<String, Map<String, String>> flowOutputLookup;

    // 数据流反向索引: TargetID#TargetPort -> ConnectionSource (对应 JSON "outputs")
    private final Map<String, ConnectionSource> inputLookup;

    // 类型分类索引: NodeType -> List<NodeID>
    private final Map<String, List<String>> typeLookup;


    // ==================================================================================
    // 2. 构造器与工厂方法 (Constructors & Factory)
    // ==================================================================================

    private RuntimeGraphIndex(Map<String, JsonObject> nodes,
                              Map<String, Map<String, String>> flow,
                              Map<String, ConnectionSource> inputs,
                              Map<String, List<String>> types,
                              Map<String, Map<String, Object>> properties,
                              Map<String, Map<String, Object>> staticInputs) {
        this.nodeDataLookup = nodes;
        this.flowOutputLookup = flow;
        this.inputLookup = inputs;
        this.typeLookup = types;
        this.propertyLookup = properties;
        this.staticInputLookup = staticInputs;
    }

    /**
     * [核心构建] 从 Reader (JSON) 直接构建索引
     */
    public static RuntimeGraphIndex build(Reader jsonReader) {
        JsonObject root = JsonParser.parseReader(jsonReader).getAsJsonObject();
        JsonObject rootNodes = root.getAsJsonObject("nodes");

        // 1. 快速扫描是否包含节点组 (优化性能 & 方便调试)
        boolean hasGroups = false;
        for (String key : rootNodes.keySet()) {
            JsonObject node = rootNodes.getAsJsonObject(key);
            if (node.has("node_type") && "node_group".equals(node.get("node_type").getAsString())) {
                hasGroups = true;
                break;
            }
        }

        if (hasGroups) {
            System.out.println("[RuntimeGraphIndex] Detected node groups. Using GraphFlattener.");
        }

        // 2. 统一使用 GraphFlattener 构建
        // 即使没有节点组，复用 Flattener 的内部解析也能保证逻辑一致性，且 bridgeGroups 会自动空转，开销极小。
        GraphFlattener flattener = new GraphFlattener();
        flattener.flatten(rootNodes);

        // 3. 校验数据流循环依赖
        validateNoDataCycles(flattener.inputLookup);

        // 4. 封装为不可变索引返回
        return new RuntimeGraphIndex(
                Map.copyOf(flattener.nodeDataLookup),
                Map.copyOf(flattener.flowOutputLookup),
                Map.copyOf(flattener.inputLookup),
                Map.copyOf(flattener.typeLookup),
                Map.copyOf(flattener.propertyLookup),
                Map.copyOf(flattener.staticInputLookup)
        );
    }


    // ==================================================================================
    // 3. 公开查询 API (Public Query APIs) - O(1) 复杂度
    // ==================================================================================

    /**
     * 获取节点类型
     */
    public String getNodeType(String nodeId) {
        JsonObject node = nodeDataLookup.get(nodeId);
        return node != null && node.has("node_type") ? node.get("node_type").getAsString() : "unknown";
    }

    /**
     * 严格查询端口是否存在 (不关心值)
     */
    public boolean hasPort(String nodeId, String portName) {
        JsonObject node = nodeDataLookup.get(nodeId);
        if (node == null) return false;
        if (node.has("inputs") && node.getAsJsonObject("inputs").has(portName)) return true;
        if (node.has("outputs") && node.getAsJsonObject("outputs").has(portName)) return true;
        if (node.has("execution") && node.getAsJsonObject("execution").has(portName)) return true;
        return false;
    }

    /**
     * 获取节点配置属性 (Properties)
     * @return 属性值，若不存在返回 null
     */
    @Nullable
    public Object getNodeProperty(String nodeId, String key) {
        Map<String, Object> props = propertyLookup.get(nodeId);
        return (props != null) ? props.get(key) : null;
    }

    /**
     * 获取节点静态输入值 (Static Inputs)
     * 对应 UI 输入框中的默认值
     */
    @Nullable
    public Object getNodeStaticInput(String nodeId, String portName) {
        Map<String, Object> inputs = staticInputLookup.get(nodeId);
        return (inputs != null) ? inputs.get(portName) : null;
    }

    /**
     * 查找控制流的目标节点
     */
    @Nullable
    public String findFlowTarget(String currentNodeId, String outputPortName) {
        Map<String, String> outputs = flowOutputLookup.get(currentNodeId);
        return (outputs != null) ? outputs.get(outputPortName) : null;
    }

    /**
     * 查找数据流的输入源头
     */
    @Nullable
    public ConnectionSource findInputSource(String targetNodeId, String inputPortName) {
        return inputLookup.get(makeKey(targetNodeId, inputPortName));
    }

    /**
     * 根据节点类型批量获取节点 ID 列表
     */
    public List<String> findNodesByType(String nodeType) {
        return typeLookup.getOrDefault(nodeType, List.of());
    }


    // ==================================================================================
    // 4. 核心验证逻辑 (Validation Logic)
    // ==================================================================================

    private static void validateNoDataCycles(Map<String, ConnectionSource> inputLookup) {
        Map<String, Set<String>> dependencyGraph = new HashMap<>();
        for (String key : inputLookup.keySet()) {
            String targetNodeId = key.split("#")[0];
            String sourceNodeId = inputLookup.get(key).sourceNodeId();
            dependencyGraph.computeIfAbsent(targetNodeId, k -> new HashSet<>()).add(sourceNodeId);
        }

        Set<String> visited = new HashSet<>();
        Set<String> recStack = new HashSet<>();

        for (String nodeId : dependencyGraph.keySet()) {
            if (checkCycleDFS(nodeId, dependencyGraph, visited, recStack)) {
                throw new IllegalStateException("Data flow cycle detected! Graph compilation failed at node: " + nodeId);
            }
        }
    }

    private static boolean checkCycleDFS(String current, Map<String, Set<String>> adj,
                                         Set<String> visited, Set<String> recStack) {
        if (recStack.contains(current)) return true;
        if (visited.contains(current)) return false;

        recStack.add(current);
        visited.add(current);

        Set<String> dependencies = adj.get(current);
        if (dependencies != null) {
            for (String dep : dependencies) {
                if (checkCycleDFS(dep, adj, visited, recStack)) return true;
            }
        }

        recStack.remove(current);
        return false;
    }


    // ==================================================================================
    // 5. 内部辅助方法 (Internal Helpers) - 供包内其他类(如 Flattener)使用
    // ==================================================================================

    static Map<String, Object> parseValueMap(JsonObject obj) {
        Map<String, Object> map = new HashMap<>();
        for (String key : obj.keySet()) {
            JsonElement val = obj.get(key);
            map.put(key, unwrapJsonElement(val));
        }
        return Map.copyOf(map);
    }

    static Object unwrapJsonElement(JsonElement element) {
        if (element.isJsonPrimitive()) {
            var prim = element.getAsJsonPrimitive();
            if (prim.isBoolean()) return prim.getAsBoolean();
            if (prim.isNumber()) return prim.getAsNumber();
            if (prim.isString()) return prim.getAsString();
        }
        return null;
    }

    private static String makeKey(String nodeId, String portName) {
        return nodeId + "#" + portName;
    }


    // ==================================================================================
    // 6. 内部数据结构 (Nested Types)
    // ==================================================================================

    /**
     * 表示数据流连接的源头信息
     */
    public record ConnectionSource(String sourceNodeId, String sourcePortName) {}
}