package com.mine.geometry_node.core.execution;

import java.util.List;

/**
 * [执行结果协议] 定义节点执行后的控制流向。
 */
public sealed interface ExecutionResult {

    /**
     * [流转] 指示引擎立即通过指定的输出端口跳转到下一个节点。
     * @param outputPortName 触发的输出端口名称 (例如 "flow", "true", "loop_body")
     */
    record Next(String outputPortName) implements ExecutionResult {}

    /**
     * [挂起] 指示引擎暂停当前流程，等待指定时间后再恢复跳跃。
     * @param ticks
     */
    record Wait(long ticks, String nextPortName) implements ExecutionResult {}

    /**
     * [结束] 指示当前流程自然结束（如到达 End 节点，或分支走到尽头）。
     */
    record Finish() implements ExecutionResult {}

    /**
     * [异常] 指示执行过程中发生了非致命错误，流程被迫终止。
     * @param errorMessage 错误描述，用于调试或日志
     */
    record Error(String errorMessage) implements ExecutionResult {}

    /**
     * 顺序调用
     * 指示引擎将多个后续分支按顺序压入执行栈。
     * 虚拟机将先执行 list 中的第一个端口对应的分支，待该分支执行完毕(Finish)后，
     * 自动弹出栈顶，继续执行下一个分支。
     *
     * @param outputPorts 需要按顺序执行的输出端口列表 (例如 ["true", "false"] 或 ["flow_1", "flow_2"])
     */
    record Call(List<String> outputPorts) implements ExecutionResult {}

    // --- 静态工厂方法 (Syntactic Sugar) ---

    static ExecutionResult next(String port) {
        return new Next(port);
    }
    static ExecutionResult delay(long ticks, String nextPort) { return new Wait(ticks, nextPort); }
    static ExecutionResult finish() {
        return new Finish();
    }
    static ExecutionResult error(String msg) {
        return new Error(msg);
    }

    static ExecutionResult call(List<String> ports) { return new Call(ports); }
    static ExecutionResult call(String... ports) { return new Call(List.of(ports)); }
}