package com.tobyink.millionhorses.fabric;

import com.tobyink.millionhorses.MillionHorsesMod;
import net.fabricmc.api.ModInitializer;

public final class MillionHorsesModFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        MillionHorsesMod.init();
    }
}