package com.mine.geometry_node.core.node.nodes.actions;

import com.mine.geometry_node.core.execution.ExecutionContext;
import com.mine.geometry_node.core.execution.ExecutionResult;
import com.mine.geometry_node.core.node.nodes.BaseNode;
import com.mine.geometry_node.core.node.nodes.StandardPorts;
import org.jetbrains.annotations.Nullable;

/**
 * [动作节点基类]
 * 适用于绝大多数“线性执行、产生副作用、无数据输出”的指令节点。
 */
public abstract class BaseActionNode extends BaseNode {

    /**
     * 动作节点被引擎驱动时，执行具体动作，然后自动将控制流推给下一节点。
     */
    @Override
    public final ExecutionResult execute(ExecutionContext context) {
        performAction(context);
        return next(StandardPorts.FLOW_OUT.getId());
    }

    /**
     * 动作节点默认不产生纯数据流输出，直接锁死返回 null。
     */
    @Override
    @Nullable
    public final Object compute(ExecutionContext context, String portName) {
        return null;
    }

    /**
     * [强制实现] 执行逻辑。
     */
    protected abstract void performAction(ExecutionContext context);
}