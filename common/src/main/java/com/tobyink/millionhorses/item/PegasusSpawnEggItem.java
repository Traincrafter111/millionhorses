package com.tobyink.millionhorses.item;

import com.tobyink.millionhorses.registry.EntityRegistry;
import net.minecraft.world.item.SpawnEggItem;

public class PegasusSpawnEggItem extends SpawnEggItem {

    public PegasusSpawnEggItem() {
        super(EntityRegistry.PEGASUS.get(), 0xFFFFFF, 0xFFFFFF,
                new Properties().stacksTo(64));
    }
}