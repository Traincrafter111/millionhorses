package com.tobyink.millionhorses.item;

import com.tobyink.millionhorses.registry.EntityRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.SpawnEggItem;

public class PegasusSpawnEggItem extends SpawnEggItem {

    public PegasusSpawnEggItem() {
        // Pasamos un EntityType placeholder para evitar NPE en el constructor.
        // getType() lo sobreescribimos para devolver el tipo real de forma lazy.
        super(EntityType.HORSE, 0xFFFFFF, 0xFFFFFF, new Properties().stacksTo(64));
    }

    @Override
    public EntityType<? extends Mob> getType(CompoundTag tag) {
        return EntityRegistry.PEGASUS.get();
    }
}