package com.mine.geometry_node.core.execution;

import com.mine.geometry_node.core.execution.variables.VariableRegistry;
import com.mine.geometry_node.core.node.NodeRegistry;
import com.mine.geometry_node.core.node.nodes.BaseNode;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * [核心执行单元] 图的运行实例 (The "Process")。
 * <p>
 * 代表一次独立的蓝图执行流程，充当微型虚拟机 (VM) 的角色。
 * 负责维护执行指令栈、变量作用域、协程调度(延迟任务)以及瞬时环境上下文。
 */
public class GraphProcess {

    // ===================================================================================
    // 数据结构定义 (Data Structures)
    // ===================================================================================

    public enum State {
        RUNNING,    // 活跃状态：正在执行或准备执行
        WAITING,    // 挂起状态：等待协程任务(Delay)唤醒
        FINISHED    // 终止状态：流程彻底结束，等待引擎回收
    }

    /** 描述一个被挂起的延迟任务 */
    private record ScheduledTask(long wakeUpTick, String resumeNodeId) {}

    // ===================================================================================
    // 成员变量 (Fields)
    // ===================================================================================

    // 1. 基础配置 (Basic Configuration)
    private final String graphId;
    private final RuntimeGraphIndex index;
    private final InnerContext context; // 外观模式：暴露给节点使用的受限 API

    // 2. 运行时状态 (Runtime State)
    private State state = State.RUNNING;
    private String currentFlowId;       // 当前待执行的节点 ID
    private String activeNodeId;        // 当前正在计算/执行的节点 ID (用于溯源、报错提示)

    // 3. 任务调度 (Task Scheduling)
    private final List<ScheduledTask> sleepingTasks = new ArrayList<>(); // 等待唤醒的协程队列
    private boolean needsTimeRebase = false;                             // 读档标记：指示是否需要将相对时间转换为绝对世界时间

    // 4. 内存与作用域 (Memory & Scope)
    private final Deque<String> executionStack = new LinkedList<>();              // 指令执行栈 (LIFO)
    private final Deque<Map<String, Object>> variableStack = new LinkedList<>();  // 局部变量栈 (支持作用域嵌套)
    private final Map<String, Object> eventData = new HashMap<>();                // 事件瞬时数据沙箱 (如：破坏方块的坐标、攻击者)

    // 5. 帧级缓存 (Frame Cache - Transient)
    private final Map<String, Object> frameCache = new HashMap<>();               // 避免单次 Tick 内重复计算同一数据节点
    private final Set<String> recursionGuard = new HashSet<>();                   // 递归深度/环形依赖检测防护

    // 6. 外部环境 (Environment Context - Transient)
    private ServerLevel level;
    private Entity entity; // 挂载该图的宿主实体


    // ===================================================================================
    // 构造与初始化 (Constructors)
    // ===================================================================================

    /**
     * [全新启动] 创建并初始化一个新的执行进程。
     */
    public GraphProcess(String graphId, RuntimeGraphIndex index, String startNodeId) {
        this.graphId = graphId;
        this.index = index;
        this.currentFlowId = startNodeId;
        this.context = new InnerContext();

        // 初始化根作用域
        this.variableStack.push(new HashMap<>());
    }

    /**
     * [断点续传] 从 NBT 存档中反序列化恢复执行进程。
     */
    public GraphProcess(CompoundTag tag, RuntimeGraphIndex index) {
        this.index = index;
        this.context = new InnerContext();

        // 1. 恢复基础状态
        this.graphId = tag.getString("GraphId");
        this.currentFlowId = tag.contains("NodeId") ? tag.getString("NodeId") : null;
        this.state = State.valueOf(tag.getString("State"));

        // 2. 恢复睡眠任务队列
        this.sleepingTasks.clear();
        if (tag.contains("SleepingTasks", Tag.TAG_LIST)) {
            ListTag tasksTag = tag.getList("SleepingTasks", Tag.TAG_COMPOUND);
            for (int i = 0; i < tasksTag.size(); i++) {
                CompoundTag taskTag = tasksTag.getCompound(i);
                // 读档时由于尚未绑定 Level，wakeUpTick 暂存的是"剩余相对时间"
                this.sleepingTasks.add(new ScheduledTask(taskTag.getLong("WaitRemaining"), taskTag.getString("ResumeNodeId")));
            }
            this.needsTimeRebase = true; // 标记需要在首次 Tick 时校准时间轴
        }

        // 3. 恢复事件数据沙箱
        this.eventData.clear();
        if (tag.contains("EventData", Tag.TAG_COMPOUND)) {
            CompoundTag eventTag = tag.getCompound("EventData");
            for (String key : eventTag.getAllKeys()) {
                Object deserialized = VariableRegistry.fromTag(eventTag.get(key));
                if (deserialized != null) {
                    this.eventData.put(key, deserialized);
                }
            }
        }

        // 4. 恢复变量栈
        this.variableStack.clear();
        if (tag.contains("VariableStack", Tag.TAG_LIST)) {
            ListTag stackTag = tag.getList("VariableStack", Tag.TAG_COMPOUND);
            for (int i = 0; i < stackTag.size(); i++) {
                Map<String, Object> scope = new HashMap<>();
                loadVariables(stackTag.getCompound(i), scope);
                this.variableStack.addLast(scope);
            }
        } else {
            this.variableStack.push(new HashMap<>());
        }

        // 5. 恢复执行指令栈
        this.executionStack.clear();
        if (tag.contains("ExecutionStack", Tag.TAG_LIST)) {
            ListTag list = tag.getList("ExecutionStack", Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                this.executionStack.addLast(list.getString(i));
            }
        }
    }


    // ===================================================================================
    // 生命周期与驱动 API (Lifecycle & Public API)
    // ===================================================================================

    public void setEnvironment(ServerLevel level, @Nullable Entity entity) {
        this.level = level;
        this.entity = entity;
    }

    public void setEventData(String key, Object value) {
        this.eventData.put(key, value);
    }

    public boolean isFinished() {
        return state == State.FINISHED;
    }

    public String getGraphId() {
        return graphId;
    }

    /**
     * [核心驱动马达] 游戏主循环每 Tick 调用一次。
     * 负责处理时间轴对齐、唤醒到期协程，并驱动逻辑主循环。
     */
    public void tick(long currentWorldTick) {
        // 0. 前置合法性检查
        if (state == State.FINISHED || level == null) return;

        // 1. 清理瞬时缓存 (每 Tick 重置)
        frameCache.clear();
        recursionGuard.clear();

        // 2. 读档后首帧处理：时间轴校准 (相对时间 -> 绝对世界时间)
        if (this.needsTimeRebase) {
            for (int i = 0; i < sleepingTasks.size(); i++) {
                ScheduledTask oldTask = sleepingTasks.get(i);
                sleepingTasks.set(i, new ScheduledTask(currentWorldTick + oldTask.wakeUpTick(), oldTask.resumeNodeId()));
            }
            this.needsTimeRebase = false;
        }

        // 3. 任务调度：唤醒到期的任务并压入执行栈
        Iterator<ScheduledTask> it = sleepingTasks.iterator();
        while (it.hasNext()) {
            ScheduledTask task = it.next();
            if (currentWorldTick >= task.wakeUpTick()) {
                if (task.resumeNodeId() != null) {
                    this.executionStack.addLast(task.resumeNodeId());
                }
                it.remove(); // 任务出列
            }
        }

        // 4. 状态流转与主循环分发
        if (currentFlowId != null || !executionStack.isEmpty()) {
            state = State.RUNNING;
            runExecutionLoop();
        } else if (sleepingTasks.isEmpty()) {
            // 既无运行指令，也无挂起任务 -> 寿终正寝
            state = State.FINISHED;
        } else {
            // 无运行指令，但仍有任务沉睡 -> 保持挂起
            state = State.WAITING;
        }
    }


    // ===================================================================================
    // 核心执行引擎 (Core Execution Logic - Push Model)
    // ===================================================================================

    /**
     * 执行控制流逻辑 (Push Model)。
     * 只要指令栈有任务，将持续执行，直至挂起(Delay)或触及单帧执行上限。
     */
    private void runExecutionLoop() {
        int steps = 0;
        final int MAX_STEPS = 100; // 防御性编程：防止死循环/过于密集的逻辑卡死服务端

        while ((currentFlowId != null || !executionStack.isEmpty()) && state == State.RUNNING) {

            // 1. 指针转移：当前流程断链时，从栈顶弹出新任务
            if (currentFlowId == null) {
                currentFlowId = executionStack.pollFirst();
                if (currentFlowId == null) continue;
            }

            // 2. 帧限流防护
            if (steps++ > MAX_STEPS) {
                return; // 超过配额，主动让出执行权，下一 Tick 继续
            }

            // 3. 节点寻址与校验
            String nodeType = index.getNodeType(currentFlowId);
            if ("unknown".equals(nodeType)) {
                state = State.FINISHED;
                return;
            }

            BaseNode logic = NodeRegistry.INSTANCE.get(nodeType);
            if (logic == null) {
                System.err.println("GraphProcess: Unknown node type " + nodeType);
                state = State.FINISHED;
                return;
            }

            // 4. 执行节点与结果处理
            try {
                String previousActive = this.activeNodeId;
                this.activeNodeId = currentFlowId; // 记录现场

                ExecutionResult result = logic.execute(context);

                this.activeNodeId = previousActive; // 恢复现场

                handleExecutionResult(result);

            } catch (Exception e) {
                System.err.println("GraphProcess Error at node " + currentFlowId + ": " + e.getMessage());
                e.printStackTrace();
                state = State.FINISHED;
            }
        }

        // 5. 收尾判定
        if (currentFlowId == null && executionStack.isEmpty() && sleepingTasks.isEmpty()) {
            state = State.FINISHED;
        }
    }

    /**
     * 解析节点的执行结果，并操纵虚拟机状态机进行响应跳转。
     */
    private void handleExecutionResult(ExecutionResult result) {
        switch (result) {
            case ExecutionResult.Next next -> {
                // 单链跳转：更新当前执行指针
                this.currentFlowId = index.findFlowTarget(currentFlowId, next.outputPortName());
            }
            case ExecutionResult.Call call -> {
                // 分支调用：倒序压栈，确保声明在前的端口优先执行 (LIFO 特性)
                List<String> ports = call.outputPorts();
                for (int i = ports.size() - 1; i >= 0; i--) {
                    String portName = ports.get(i);
                    String targetId = index.findFlowTarget(currentFlowId, portName);
                    if (targetId != null) {
                        this.executionStack.addFirst(targetId);
                    }
                }
                this.currentFlowId = executionStack.pollFirst();
            }
            case ExecutionResult.Wait wait -> {
                // 协程挂起：将未来任务扔进调度器，交出当前执行权
                long wakeTime = level.getGameTime() + wait.ticks();
                String nextId = index.findFlowTarget(currentFlowId, wait.nextPortName());

                if (nextId != null) {
                    this.sleepingTasks.add(new ScheduledTask(wakeTime, nextId));
                }
                this.currentFlowId = null;
            }
            case ExecutionResult.Finish ignored -> {
                // 主动终止当前分支
                this.currentFlowId = null;
            }
            case ExecutionResult.Error err -> {
                // 抛出运行时异常，强制清栈退出
                System.err.println("Graph Error: " + err.errorMessage());
                this.executionStack.clear();
                this.currentFlowId = null;
                this.state = State.FINISHED;
            }
        }
    }


    // ===================================================================================
    // 数据拉取模型 (Data Flow Logic - Pull Model)
    // ===================================================================================

    /**
     * 递归向上游节点索要数据 (Pull Model)。
     * 附带了帧级结果缓存与成环依赖检测。
     */
    private Object executeDataNode(String nodeId, String portName) {
        String cacheKey = nodeId + "#" + portName;

        // 1. 命中缓存直接返回
        if (frameCache.containsKey(cacheKey)) {
            return frameCache.get(cacheKey);
        }

        // 2. 环形死锁检测
        if (recursionGuard.contains(nodeId)) {
            System.err.println("GraphProcess: Detected dependency cycle at node " + nodeId);
            return null;
        }

        recursionGuard.add(nodeId);
        String previousActiveNodeId = this.activeNodeId;

        try {
            String nodeType = index.getNodeType(nodeId);
            if ("unknown".equals(nodeType)) return null;

            BaseNode logic = NodeRegistry.INSTANCE.get(nodeType);
            if (logic == null) return null;

            this.activeNodeId = nodeId;

            // 触发运算
            Object result = logic.compute(context, portName);

            frameCache.put(cacheKey, result);
            return result;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            // 清理现场
            this.activeNodeId = previousActiveNodeId;
            recursionGuard.remove(nodeId);
        }
    }


    // ===================================================================================
    // 序列化与反序列化 (NBT Serialization)
    // ===================================================================================

    public CompoundTag save(CompoundTag tag) {
        // 1. 基础状态
        tag.putString("GraphId", graphId);
        tag.putString("State", state.name());
        if (currentFlowId != null) {
            tag.putString("NodeId", currentFlowId);
        }

        // 2. 协程队列 (转为剩余时间保存)
        if (!sleepingTasks.isEmpty()) {
            ListTag tasksTag = new ListTag();
            for (ScheduledTask task : sleepingTasks) {
                CompoundTag taskTag = new CompoundTag();
                long remaining = (level != null && !needsTimeRebase) ?
                        Math.max(0, task.wakeUpTick() - level.getGameTime()) :
                        task.wakeUpTick();

                taskTag.putLong("WaitRemaining", remaining);
                if (task.resumeNodeId() != null) {
                    taskTag.putString("ResumeNodeId", task.resumeNodeId());
                }
                tasksTag.add(taskTag);
            }
            tag.put("SleepingTasks", tasksTag);
        }

        // 3. 事件数据沙箱
        if (!eventData.isEmpty()) {
            CompoundTag eventTag = new CompoundTag();
            eventData.forEach((key, val) -> {
                Object toSave = (val instanceof Entity ent) ? ent.getUUID() : val;
                Tag serialized = VariableRegistry.toTag(toSave);
                if (serialized != null) {
                    eventTag.put(key, serialized);
                }
            });
            tag.put("EventData", eventTag);
        }

        // 4. 局部变量栈
        ListTag stackTag = new ListTag();
        Iterator<Map<String, Object>> it = variableStack.descendingIterator();
        while (it.hasNext()) {
            Map<String, Object> scope = it.next();
            CompoundTag scopeTag = new CompoundTag();
            saveVariables(scopeTag, scope);
            stackTag.add(scopeTag);
        }
        tag.put("VariableStack", stackTag);

        // 5. 执行指令栈
        if (!executionStack.isEmpty()) {
            ListTag list = new ListTag();
            for (String id : executionStack) {
                list.add(StringTag.valueOf(id));
            }
            tag.put("ExecutionStack", list);
        }

        return tag;
    }

    private boolean isValidType(Object v) {
        return VariableRegistry.isSupported(v);
    }

    private void saveVariables(CompoundTag tag, Map<String, Object> scope) {
        scope.forEach((key, val) -> {
            Tag serialized = VariableRegistry.toTag(val);
            if (serialized != null) {
                tag.put(key, serialized);
            }
        });
    }

    private void loadVariables(CompoundTag tag, Map<String, Object> scope) {
        for (String key : tag.getAllKeys()) {
            Object deserialized = VariableRegistry.fromTag(tag.get(key));
            if (deserialized != null) {
                scope.put(key, deserialized);
            }
        }
    }


    // ===================================================================================
    // 内部类：上下文实现 (Inner Class: Context Implementation)
    // ===================================================================================

    /**
     * 门面模式的内部实现，代理所有节点的对外读写请求。
     */
    private class InnerContext implements ExecutionContext {

        @Override
        public ServerLevel getLevel() { return GraphProcess.this.level; }

        @Override
        public Entity getEntity() { return GraphProcess.this.entity; }

        @Override
        public String getGraphId() { return GraphProcess.this.graphId; }

        @Override
        public Object getVariable(String name) {
            // 从栈顶往栈底查找作用域变量
            for (Map<String, Object> scope : variableStack) {
                if (scope.containsKey(name)) {
                    Object val = scope.get(name);
                    if (val instanceof UUID uuid) {
                        if (level == null) return null;
                        Entity resolvedEntity = level.getEntity(uuid);
                        if (resolvedEntity == null || resolvedEntity.isRemoved()) return null;
                        return resolvedEntity;
                    }
                    return val;
                }
            }
            return null;
        }

        @Override
        public void setVariable(String name, Object value) {
            Map<String, Object> currentScope = variableStack.peek();
            if (currentScope == null) return;

            if (value == null) {
                currentScope.remove(name);
                return;
            }

            if (isValidType(value)) {
                currentScope.put(name, (value instanceof Entity ent) ? ent.getUUID() : value);
            } else {
                System.err.println("GraphProcess: Unsupported variable type " + value.getClass().getSimpleName());
            }
        }

        @Override
        public Object getInputValue(String portName) {
            String currentNodeId = GraphProcess.this.activeNodeId;
            if (currentNodeId == null) return null;

            RuntimeGraphIndex.ConnectionSource source = index.findInputSource(currentNodeId, portName);
            if (source == null) return null;

            return executeDataNode(source.sourceNodeId(), source.sourcePortName());
        }

        @Override
        public Object getStaticInput(String portName) {
            String currentNodeId = GraphProcess.this.activeNodeId;
            return (currentNodeId != null) ? GraphProcess.this.index.getNodeStaticInput(currentNodeId, portName) : null;
        }

        @Override
        public Object getNodeProperty(String key) {
            String currentNodeId = GraphProcess.this.activeNodeId;
            return (currentNodeId != null) ? GraphProcess.this.index.getNodeProperty(currentNodeId, key) : null;
        }

        @Override
        public Object getEventData(String key) {
            Object val = GraphProcess.this.eventData.get(key);

            // 运行时自动解析 UUID -> Entity
            if (val instanceof UUID uuid) {
                if (level == null) return null;
                Entity resolvedEntity = level.getEntity(uuid);
                if (resolvedEntity == null || resolvedEntity.isRemoved()) return null;
                return resolvedEntity;
            }
            return val;
        }

        @Override
        public void setEventData(String key, Object value) {
            GraphProcess.this.eventData.put(key, value);
        }

        @Override
        public boolean hasPort(String portName) {
            String currentNodeId = GraphProcess.this.activeNodeId;
            return currentNodeId != null && GraphProcess.this.index.hasPort(currentNodeId, portName);
        }
    }
}