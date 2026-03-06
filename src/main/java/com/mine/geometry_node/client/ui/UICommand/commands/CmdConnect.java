package com.mine.geometry_node.client.ui.UICommand.commands;

import com.mine.geometry_node.client.ui.UICommand.ICommand;
import com.mine.geometry_node.client.ui.Viewport.GraphController;

public class CmdConnect implements ICommand {
    private final GraphController mController;
    private final String outNodeId, outPortId;
    private final String inNodeId, inPortId;

    public CmdConnect(GraphController controller, String outNodeId, String outPortId, String inNodeId, String inPortId) {
        this.mController = controller;
        this.outNodeId = outNodeId; this.outPortId = outPortId;
        this.inNodeId = inNodeId; this.inPortId = inPortId;
    }

    @Override
    public void execute() {
        mController.addConnection(outNodeId, outPortId, inNodeId, inPortId);
    }

    @Override
    public void undo() {
        mController.removeConnection(outNodeId, outPortId, inNodeId, inPortId);
    }
}