package com.tobyink.millionhorses.item;

import com.tobyink.millionhorses.registry.EntityRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.SpawnEggItem;

public class UnicornSpawnEggItem extends SpawnEggItem {

    public UnicornSpawnEggItem() {
        super(EntityType.HORSE, 0xC8A2C8, 0xFFD700, new Properties().stacksTo(64));
    }

    @Override
    public EntityType<? extends Mob> getType(CompoundTag tag) {
        return EntityRegistry.UNICORN.get();
    }
}