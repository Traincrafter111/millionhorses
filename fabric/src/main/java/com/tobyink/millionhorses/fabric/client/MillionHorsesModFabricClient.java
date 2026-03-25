package com.tobyink.millionhorses.fabric.client;

import com.tobyink.millionhorses.MillionHorsesMod;
import com.tobyink.millionhorses.entity.client.renderer.*;
import com.tobyink.millionhorses.registry.EntityRegistry;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import net.fabricmc.api.ClientModInitializer;

public final class MillionHorsesModFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(EntityRegistry.PEGASUS, PegasusRenderer::new);
        EntityRendererRegistry.register(EntityRegistry.ALICORN, AlicornRenderer::new);
        EntityRendererRegistry.register(EntityRegistry.UNICORN, UnicornRenderer::new);
        EntityRendererRegistry.register(EntityRegistry.CYN_HORSE, CynHorseRenderer::new);
        EntityRendererRegistry.register(EntityRegistry.CYN_DONKEY, CynDonkeyRenderer::new);

        MillionHorsesMod.initClient();
    }
}