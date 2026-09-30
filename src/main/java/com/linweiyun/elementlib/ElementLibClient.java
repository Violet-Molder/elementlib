package com.linweiyun.elementlib;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = ElementLib.MOD_ID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = ElementLib.MOD_ID, value = Dist.CLIENT)
public class ElementLibClient {
    public ElementLibClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, (mc, parent) -> new ConfigurationScreen(container, parent));
    }
}
