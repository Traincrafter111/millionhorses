package com.tobyink.millionhorses.forge;

import com.tobyink.millionhorses.MillionHorsesMod;
import com.tobyink.millionhorses.event.VanillaHorseSpawnHandler;
import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(MillionHorsesMod.MOD_ID)
public class MillionHorsesModForge {
    public MillionHorsesModForge() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        EventBuses.registerModEventBus(MillionHorsesMod.MOD_ID, modEventBus);
        MillionHorsesMod.init();
        VanillaHorseSpawnHandler.register();
        // initClient() lo llama MillionHorsesModForgeClient via @EventBusSubscriber
        // ForgeVanillaHorseReplacer se auto-registra via @EventBusSubscriber(bus=FORGE)
    }
}