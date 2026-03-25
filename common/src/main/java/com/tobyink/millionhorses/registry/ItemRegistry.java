package com.tobyink.millionhorses.registry;

import com.tobyink.millionhorses.MillionHorsesMod;
import com.tobyink.millionhorses.item.*;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;

public class ItemRegistry {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(MillionHorsesMod.MOD_ID, Registries.ITEM);

    public static final RegistrySupplier<Item> PEGASUS_SPAWN_EGG =
            ITEMS.register("pegasus_spawn_egg", PegasusSpawnEggItem::new);

    public static final RegistrySupplier<Item> ALICORN_SPAWN_EGG =
            ITEMS.register("alicorn_spawn_egg", AlicornSpawnEggItem::new);

    public static final RegistrySupplier<Item> UNICORN_SPAWN_EGG =
            ITEMS.register("unicorn_spawn_egg", UnicornSpawnEggItem::new);

    public static final RegistrySupplier<Item> CYN_HORSE_SPAWN_EGG =
            ITEMS.register("cyn_horse_spawn_egg", CynHorseSpawnEggItem::new);

    public static final RegistrySupplier<Item> CYN_DONKEY_SPAWN_EGG =
            ITEMS.register("cyn_donkey_spawn_egg", CynDonkeySpawnEggItem::new);

    public static final RegistrySupplier<Item> HORSE_WHISTLE =
            ITEMS.register("horse_whistle", HorseWhistleItem::new);

    public static void init() {
        ITEMS.register();
    }
}