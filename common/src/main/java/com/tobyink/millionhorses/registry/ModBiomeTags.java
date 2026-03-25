package com.tobyink.millionhorses.registry;

import dev.architectury.platform.Platform;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

/**
 * Tags de bioma compatibles entre plataformas.
 * Forge usa el namespace "forge:", Fabric usa "c:" (convention tags).
 *
 * Cualquier mod que marque sus biomas con estos tags tendrá spawns automáticamente.
 */
public class ModBiomeTags {

    // Plains: forge:is_plains / c:plains
    public static final TagKey<Biome> IS_PLAINS = biomeTag(
            "forge", "is_plains",
            "c",     "plains"
    );

    // Savanna: forge:is_savanna / c:savanna
    public static final TagKey<Biome> IS_SAVANNA = biomeTag(
            "forge", "is_savanna",
            "c",     "savanna"
    );

    // Forest (incluye flower_forest): forge:is_forest / c:forest
    public static final TagKey<Biome> IS_FOREST = biomeTag(
            "forge", "is_forest",
            "c",     "forest"
    );

    // Mountain: forge:is_mountain / c:mountain
    public static final TagKey<Biome> IS_MOUNTAIN = biomeTag(
            "forge", "is_mountain",
            "c",     "mountain"
    );

    // Cherry grove: forge:is_cherry_grove / c:cherry_grove
    // (Forge puede no tener este tag — el filtro extra lo hace checkSpawnRules)
    public static final TagKey<Biome> IS_CHERRY_GROVE = biomeTag(
            "forge", "is_cherry_grove",
            "c",     "cherry_grove"
    );

    // Meadow: forge:is_meadow / c:meadow
    public static final TagKey<Biome> IS_MEADOW = biomeTag(
            "forge", "is_meadow",
            "c",     "meadow"
    );

    // ── Helper ────────────────────────────────────────────────────────────────
    private static TagKey<Biome> biomeTag(
            String forgeNs, String forgePath,
            String fabricNs, String fabricPath) {
        String ns   = Platform.isForge() ? forgeNs   : fabricNs;
        String path = Platform.isForge() ? forgePath : fabricPath;
        return TagKey.create(Registries.BIOME, new ResourceLocation(ns, path));
    }
}