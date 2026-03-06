package com.mine.geometry_node.client.key;

import com.mojang.blaze3d.platform.InputConstants;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class KeyBindings {
    public static final KeyMapping OPEN_EDITOR = new KeyMapping(
            "key.geometry_node.open_editor",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            "key.categories.geometry_node"
    );

    public static void register(RegisterKeyMappingsEvent event) {
        event.register(OPEN_EDITOR);
    }
}