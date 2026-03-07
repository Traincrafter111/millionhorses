package com.tobyink.millionhorses;

import net.minecraft.resources.ResourceLocation;
import com.tobyink.millionhorses.registry.EntityRegistry;
import com.tobyink.millionhorses.registry.ItemRegistry;
import com.tobyink.millionhorses.registry.TabRegistry;
import com.tobyink.millionhorses.entity.client.renderer.PegasusRenderer;
import com.tobyink.millionhorses.entity.client.screen.mHorsesScreen;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import dev.architectury.registry.menu.MenuRegistry;

public final class MillionHorsesMod {
    public static final String MOD_ID = "millionhorses";

    public static void init() {
        EntityRegistry.init();
        ItemRegistry.init();
        TabRegistry.init();
        com.tobyink.millionhorses.registry.MenuRegistry.init();
    }

    public static void initClient() {
        // EntityRenderer: en Forge lo registra MillionHorsesModForgeClient.registerRenderers
        // En Fabric lo registra MillionHorsesModFabricClient via EntityRendererRegistry
        MenuRegistry.registerScreenFactory(
                com.tobyink.millionhorses.registry.MenuRegistry.PEGASUS_MENU.get(),
                mHorsesScreen::new);
        dev.architectury.registry.client.rendering.ColorHandlerRegistry.registerItemColors(
                (stack, tintIndex) -> -1,
                ItemRegistry.PEGASUS_SPAWN_EGG
        );
    }

    public static ResourceLocation modResource(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}