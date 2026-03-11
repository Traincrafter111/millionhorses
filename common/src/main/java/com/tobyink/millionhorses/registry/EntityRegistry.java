package com.tobyink.millionhorses.registry;

import dev.architectury.registry.level.entity.EntityAttributeRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import com.tobyink.millionhorses.MillionHorsesMod;
import com.tobyink.millionhorses.entity.mobs.AlicornEntity;
import com.tobyink.millionhorses.entity.mobs.CynHorseEntity;
import com.tobyink.millionhorses.entity.mobs.PegasusEntity;

public class EntityRegistry {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(MillionHorsesMod.MOD_ID, Registries.ENTITY_TYPE);

    public static final RegistrySupplier<EntityType<PegasusEntity>> PEGASUS = ENTITIES.register("pegasus", () ->
            EntityType.Builder.of(PegasusEntity::new, MobCategory.CREATURE)
                    .sized(1.6f, 1.8f)
                    .build(new ResourceLocation(MillionHorsesMod.MOD_ID, "pegasus").toString()));

    public static final RegistrySupplier<EntityType<AlicornEntity>> ALICORN = ENTITIES.register("alicorn", () ->
            EntityType.Builder.of(AlicornEntity::new, MobCategory.CREATURE)
                    .sized(1.6f, 1.8f)
                    .build(new ResourceLocation(MillionHorsesMod.MOD_ID, "alicorn").toString()));

    public static final RegistrySupplier<EntityType<CynHorseEntity>> CYN_HORSE = ENTITIES.register("cyn_horse", () ->
            EntityType.Builder.of(CynHorseEntity::new, MobCategory.CREATURE)
                    .sized(1.6f, 1.8f)
                    .build(new ResourceLocation(MillionHorsesMod.MOD_ID, "cyn_horse").toString()));

    private static void initAttributes() {
        EntityAttributeRegistry.register(PEGASUS, PegasusEntity::createAttributes);
        EntityAttributeRegistry.register(ALICORN, AlicornEntity::createAttributes);
        EntityAttributeRegistry.register(CYN_HORSE, CynHorseEntity::createAttributes);
    }

    public static void init() {
        ENTITIES.register();
        initAttributes();
    }
}