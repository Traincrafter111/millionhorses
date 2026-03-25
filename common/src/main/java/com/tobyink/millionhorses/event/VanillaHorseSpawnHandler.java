package com.tobyink.millionhorses.event;

import com.tobyink.millionhorses.config.ModConfig;
import com.tobyink.millionhorses.entity.mobs.CynDonkeyEntity;
import com.tobyink.millionhorses.entity.mobs.CynHorseEntity;
import com.tobyink.millionhorses.registry.EntityRegistry;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.horse.Donkey;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.server.level.ServerLevel;

/**
 * Intercepts Horse/Donkey vanilla spawns using Architectury's EntityEvent.ADD
 * (same API used by AdorableHamsterPets — confirmed to work with Mojang mappings).
 *
 * Also handles LIVING_CHECK_SPAWN to mark player-spawned horses as persistent
 * so the replacer doesn't touch them.
 *
 * Works on both Forge and Fabric through Architectury.
 * Called from both platform inits.
 *
 * TODO: ZombieHorse, SkeletonHorse, Mule → add when ported.
 */
public class VanillaHorseSpawnHandler {

    public static void register() {

        // ── 1. Mark player-spawned horses/donkeys as persistent ──────────────
        // so EntityEvent.ADD won't replace them
        EntityEvent.LIVING_CHECK_SPAWN.register((entity, level, x, y, z, spawnType, spawner) -> {
            if (!(entity instanceof Horse) && !(entity instanceof Donkey))
                return EventResult.pass();

            if (spawnType == MobSpawnType.SPAWN_EGG
                    || spawnType == MobSpawnType.COMMAND
                    || spawnType == MobSpawnType.MOB_SUMMONED) {
                // setPersistenceRequired() is on Mob, not LivingEntity
                if (entity instanceof Mob mob) mob.setPersistenceRequired();
            }
            return EventResult.pass();
        });

        // ── 2. Replace natural Horse/Donkey spawns when they join the world ──
        EntityEvent.ADD.register((entity, level) -> {
            if (ModConfig.allowVanillaHorseSpawns) return EventResult.pass();
            if (level.isClientSide()) return EventResult.pass();

            boolean isHorse  = entity instanceof Horse;
            boolean isDonkey = entity instanceof Donkey;
            if (!isHorse && !isDonkey) return EventResult.pass();

            // Skip player-spawned entities (marked as persistent above)
            if (entity instanceof Mob mob && mob.isPersistenceRequired())
                return EventResult.pass();

            if (!(level instanceof ServerLevel serverLevel)) return EventResult.pass();

            double x = entity.getX(), y = entity.getY(), z = entity.getZ();
            BlockPos pos = entity.blockPosition();

            // Schedule replacement on next tick to avoid ConcurrentModificationException
            serverLevel.getServer().execute(() -> {
                if (isHorse) {
                    CynHorseEntity r = EntityRegistry.CYN_HORSE.get().create(serverLevel);
                    if (r == null) return;
                    r.moveTo(x, y, z, serverLevel.random.nextFloat() * 360f, 0f);
                    r.finalizeSpawn(serverLevel,
                            serverLevel.getCurrentDifficultyAt(pos),
                            MobSpawnType.NATURAL, null, null);
                    serverLevel.addFreshEntity(r);
                } else {
                    CynDonkeyEntity r = EntityRegistry.CYN_DONKEY.get().create(serverLevel);
                    if (r == null) return;
                    r.moveTo(x, y, z, serverLevel.random.nextFloat() * 360f, 0f);
                    r.finalizeSpawn(serverLevel,
                            serverLevel.getCurrentDifficultyAt(pos),
                            MobSpawnType.NATURAL, null, null);
                    serverLevel.addFreshEntity(r);
                }
            });

            // Interrupt the original entity from joining the level
            return EventResult.interruptFalse();
        });
    }
}