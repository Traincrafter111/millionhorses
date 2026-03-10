package com.tobyink.millionhorses.registry;

import com.tobyink.millionhorses.MillionHorsesMod;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public class SoundRegistry {

    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(MillionHorsesMod.MOD_ID, Registries.SOUND_EVENT);

    public static final RegistrySupplier<SoundEvent> WHISTLE_WANDER =
            register("whistle_wander");

    public static final RegistrySupplier<SoundEvent> WHISTLE_FOLLOW =
            register("whistle_follow");

    public static final RegistrySupplier<SoundEvent> WHISTLE_STAY =
            register("whistle_stay");

    private static RegistrySupplier<SoundEvent> register(String name) {
        ResourceLocation id = new ResourceLocation(MillionHorsesMod.MOD_ID, name);
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }

    public static void init() {
        SOUNDS.register();
    }
}