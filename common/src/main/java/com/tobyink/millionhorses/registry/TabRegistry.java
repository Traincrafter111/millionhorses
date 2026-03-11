package com.tobyink.millionhorses.registry;

import com.tobyink.millionhorses.MillionHorsesMod;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class TabRegistry {

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(MillionHorsesMod.MOD_ID, Registries.CREATIVE_MODE_TAB);

    public static final RegistrySupplier<CreativeModeTab> MILLION_HORSES_TAB =
            TABS.register("million_horses_tab", () ->
                    CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                            .title(Component.translatable("itemGroup.millionhorses.million_horses_tab"))
                            .icon(() -> new ItemStack(ItemRegistry.PEGASUS_SPAWN_EGG.get()))
                            .displayItems((params, output) -> {
                                output.accept(ItemRegistry.PEGASUS_SPAWN_EGG.get());
                                output.accept(ItemRegistry.ALICORN_SPAWN_EGG.get());
                                output.accept(ItemRegistry.CYN_HORSE_SPAWN_EGG.get());
                                output.accept(ItemRegistry.HORSE_WHISTLE.get());
                            })
                            .build()
            );

    public static void init() {
        TABS.register();
    }
}