package com.tobyink.millionhorses.fabric;

import com.tobyink.millionhorses.MillionHorsesMod;
import com.tobyink.millionhorses.event.VanillaHorseSpawnHandler;
import net.fabricmc.api.ModInitializer;

public final class MillionHorsesModFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        MillionHorsesMod.init();
        VanillaHorseSpawnHandler.register();
    }
}