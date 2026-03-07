package com.tobyink.millionhorses.fabric.client;

import com.tobyink.millionhorses.MillionHorsesMod;
import com.tobyink.millionhorses.entity.client.renderer.PegasusRenderer;
import com.tobyink.millionhorses.registry.EntityRegistry;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import net.fabricmc.api.ClientModInitializer;

public final class MillionHorsesModFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(EntityRegistry.PEGASUS, PegasusRenderer::new);
        MillionHorsesMod.initClient();
    }
}