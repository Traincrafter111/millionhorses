package com.tobyink.millionhorses.forge.client;

import com.tobyink.millionhorses.MillionHorsesMod;
import com.tobyink.millionhorses.entity.client.renderer.*;
import com.tobyink.millionhorses.registry.EntityRegistry;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = MillionHorsesMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class MillionHorsesModForgeClient {

    @SubscribeEvent
    public static void onClientSetup(final FMLClientSetupEvent event) {
        MillionHorsesMod.initClient();
    }

    @SubscribeEvent
    public static void registerRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EntityRegistry.PEGASUS.get(), PegasusRenderer::new);
        event.registerEntityRenderer(EntityRegistry.ALICORN.get(), AlicornRenderer::new);
        event.registerEntityRenderer(EntityRegistry.UNICORN.get(), UnicornRenderer::new);
        event.registerEntityRenderer(EntityRegistry.CYN_HORSE.get(), CynHorseRenderer::new);
        event.registerEntityRenderer(EntityRegistry.CYN_DONKEY.get(), CynDonkeyRenderer::new);
    }
}