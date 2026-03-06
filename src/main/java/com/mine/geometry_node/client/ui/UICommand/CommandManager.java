package com.mine.geometry_node.client.ui.UICommand;

import java.util.Stack;

/**
 * 历史记录管理器
 * 负责维护撤销(Undo)和重做(Redo)的命令栈
 */
public class CommandManager {

    private final Stack<ICommand> mUndoStack = new Stack<>();
    private final Stack<ICommand> mRedoStack = new Stack<>();

    /**
     * 提交并执行一个新命令
     */
    public void execute(ICommand command) {
        command.execute();
        mUndoStack.push(command);
        // 一旦执行了新操作，之前的重做历史就失效了
        mRedoStack.clear();
    }

    /**
     * 撤销上一步操作 (Ctrl + Z)
     */
    public void undo() {
        if (!mUndoStack.isEmpty()) {
            ICommand cmd = mUndoStack.pop();
            cmd.undo();
            mRedoStack.push(cmd);
        }
    }

    /**
     * 重做上一步撤销的操作 (Ctrl + Y)
     */
    public void redo() {
        if (!mRedoStack.isEmpty()) {
            ICommand cmd = mRedoStack.pop();
            cmd.execute();
            mUndoStack.push(cmd);
        }
    }
}