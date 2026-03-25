package com.tobyink.millionhorses.registry;

import dev.architectury.registry.level.biome.BiomeModifications;
import dev.architectury.registry.level.entity.EntityAttributeRegistry;
import dev.architectury.registry.level.entity.SpawnPlacementsRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.Heightmap;
import com.tobyink.millionhorses.MillionHorsesMod;
import com.tobyink.millionhorses.entity.mobs.AlicornEntity;
import com.tobyink.millionhorses.entity.mobs.CynDonkeyEntity;
import com.tobyink.millionhorses.entity.mobs.CynHorseEntity;
import com.tobyink.millionhorses.entity.mobs.PegasusEntity;
import com.tobyink.millionhorses.entity.mobs.UnicornEntity;

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

    public static final RegistrySupplier<EntityType<UnicornEntity>> UNICORN = ENTITIES.register("unicorn", () ->
            EntityType.Builder.of(UnicornEntity::new, MobCategory.CREATURE)
                    .sized(1.6f, 1.8f)
                    .build(new ResourceLocation(MillionHorsesMod.MOD_ID, "unicorn").toString()));

    public static final RegistrySupplier<EntityType<CynDonkeyEntity>> CYN_DONKEY = ENTITIES.register("cyn_donkey", () ->
            EntityType.Builder.of(CynDonkeyEntity::new, MobCategory.CREATURE)
                    .sized(1.5f, 1.6f)
                    .build(new ResourceLocation(MillionHorsesMod.MOD_ID, "cyn_donkey").toString()));

    private static void initAttributes() {
        EntityAttributeRegistry.register(PEGASUS, PegasusEntity::createAttributes);
        EntityAttributeRegistry.register(ALICORN, AlicornEntity::createAttributes);
        EntityAttributeRegistry.register(CYN_HORSE, CynHorseEntity::createAttributes);
        EntityAttributeRegistry.register(UNICORN,   UnicornEntity::createAttributes);
        EntityAttributeRegistry.register(CYN_DONKEY,    CynDonkeyEntity::createAttributes);
    }

    private static void initSpawns() {
        // ── CynHorse: plains + savanna ────────────────────────────────────────
        // SpawnPlacementsRegistry define DÓNDE puede spawnear (superficie, luz, bloque)
        // BiomeModifications define EN QUÉ BIOMAS aparece — incluye biomas de otros mods
        // que usen los mismos tags
        SpawnPlacementsRegistry.register(
                CYN_HORSE,
                SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                CynHorseEntity::checkCynHorseSpawnRules
        );
        BiomeModifications.addProperties(
                ctx -> ctx.hasTag(ModBiomeTags.IS_PLAINS) || ctx.hasTag(ModBiomeTags.IS_SAVANNA),
                (ctx, props) -> props.getSpawnProperties().addSpawn(
                        MobCategory.CREATURE,
                        new MobSpawnSettings.SpawnerData(CYN_HORSE.get(), 5, 2, 4)
                )
        );

        // ── CynDonkey: plains + meadow ────────────────────────────────────────
        SpawnPlacementsRegistry.register(
                CYN_DONKEY,
                SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                CynDonkeyEntity::checkCynDonkeySpawnRules
        );
        BiomeModifications.addProperties(
                ctx -> ctx.hasTag(ModBiomeTags.IS_PLAINS) || ctx.hasTag(ModBiomeTags.IS_MEADOW),
                (ctx, props) -> props.getSpawnProperties().addSpawn(
                        MobCategory.CREATURE,
                        new MobSpawnSettings.SpawnerData(CYN_DONKEY.get(), 3, 1, 3)
                )
        );

        // ── Unicorn: forest + mountain + cherry grove ─────────────────────────
        // Raro (weight 1), individual o en parejas
        SpawnPlacementsRegistry.register(
                UNICORN,
                SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                UnicornEntity::checkUnicornSpawnRules
        );
        BiomeModifications.addProperties(
                ctx -> ctx.hasTag(ModBiomeTags.IS_FOREST)
                        || ctx.hasTag(ModBiomeTags.IS_MOUNTAIN)
                        || ctx.hasTag(ModBiomeTags.IS_CHERRY_GROVE),
                (ctx, props) -> props.getSpawnProperties().addSpawn(
                        MobCategory.CREATURE,
                        new MobSpawnSettings.SpawnerData(UNICORN.get(), 1, 1, 1)
                )
        );
    }

    public static void init() {
        ENTITIES.register();
        initAttributes();
        initSpawns();
    }
}