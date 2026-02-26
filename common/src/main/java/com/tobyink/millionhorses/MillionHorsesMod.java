package com.tobyink.millionhorses;

import net.minecraft.resources.ResourceLocation;
import com.tobyink.millionhorses.registry.EntityRegistry;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import com.tobyink.millionhorses.entity.client.renderer.PegasusRenderer;


public final class MillionHorsesMod {
    public static final String MOD_ID = "millionhorses";

    public static void init() {
        EntityRegistry.init();
    }

    public static void initClient() {
        EntityRendererRegistry.register(EntityRegistry.PEGASUS, PegasusRenderer::new);
    }

    public static ResourceLocation modResource(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}