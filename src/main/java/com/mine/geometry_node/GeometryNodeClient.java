package com.mine.geometry_node;

import com.mine.geometry_node.client.key.KeyBindings;
import com.mine.geometry_node.client.ui.MainUI;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = GeometryNode.MODID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = GeometryNode.MODID, value = Dist.CLIENT)
public class GeometryNodeClient {
    public GeometryNodeClient(IEventBus modBus) {
        // 注册按键
        modBus.addListener(KeyBindings::register);

        // 监听按键
        NeoForge.EVENT_BUS.addListener(this::onClientTick);
    }

    private void onClientTick(ClientTickEvent.Post event) {
        while (KeyBindings.OPEN_EDITOR.consumeClick()) {
            icyllis.modernui.mc.MuiModApi.openScreen(new MainUI());
        }
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        // Some client setup code
        GeometryNode.LOGGER.info("HELLO FROM CLIENT SETUP");
        GeometryNode.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
    }
}
