package com.tobyink.millionhorses.registry;

import com.tobyink.millionhorses.MillionHorsesMod;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import com.tobyink.millionhorses.item.PegasusSpawnEggItem;
import net.minecraft.world.item.Item;

public class ItemRegistry {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(MillionHorsesMod.MOD_ID, Registries.ITEM);

    public static final RegistrySupplier<Item> PEGASUS_SPAWN_EGG =
            ITEMS.register("pegasus_spawn_egg", PegasusSpawnEggItem::new);

    public static void init() {
        ITEMS.register();
    }
}