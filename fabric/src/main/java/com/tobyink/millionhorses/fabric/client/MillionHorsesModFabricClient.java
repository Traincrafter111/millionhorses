package com.tobyink.millionhorses.fabric.client;

import com.tobyink.millionhorses.MillionHorsesMod;
import net.fabricmc.api.ClientModInitializer;

public final class MillionHorsesModFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MillionHorsesMod.initClient();
    }
}