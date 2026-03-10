package com.tobyink.millionhorses.registry;

import com.tobyink.millionhorses.MillionHorsesMod;
import com.tobyink.millionhorses.item.HorseWhistleItem;
import com.tobyink.millionhorses.item.PegasusSpawnEggItem;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;

public class ItemRegistry {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(MillionHorsesMod.MOD_ID, Registries.ITEM);

    public static final RegistrySupplier<Item> PEGASUS_SPAWN_EGG =
            ITEMS.register("pegasus_spawn_egg", PegasusSpawnEggItem::new);

    public static final RegistrySupplier<Item> HORSE_WHISTLE =
            ITEMS.register("horse_whistle", HorseWhistleItem::new);

    public static void init() {
        ITEMS.register();
    }
}