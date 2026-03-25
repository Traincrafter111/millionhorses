package com.tobyink.millionhorses.forge.event;

import com.tobyink.millionhorses.MillionHorsesMod;
import com.tobyink.millionhorses.config.ModConfig;
import com.tobyink.millionhorses.entity.mobs.CynDonkeyEntity;
import com.tobyink.millionhorses.entity.mobs.CynHorseEntity;
import com.tobyink.millionhorses.registry.EntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.horse.Donkey;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Forge: intercepta EntityJoinLevelEvent para reemplazar Horse/Donkey
 * vanilla con CynHorse/CynDonkey cuando allowVanillaHorseSpawns = false.
 *
 * EntityJoinLevelEvent existe en todas las versiones de Forge 1.20.x.
 * Se filtra por isPersistenceRequired() para no afectar spawns del jugador
 * (que fueron marcados por VanillaHorseSpawnHandler en el evento LIVING_CHECK_SPAWN).
 *
 * TODO: ZombieHorse, SkeletonHorse, Mule → añadir cuando se porten.
 */
@Mod.EventBusSubscriber(modid = MillionHorsesMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ForgeVanillaHorseReplacer {

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (ModConfig.allowVanillaHorseSpawns) return;
        if (event.getLevel().isClientSide()) return;

        var entity = event.getEntity();

        boolean isHorse  = entity instanceof Horse;
        boolean isDonkey = entity instanceof Donkey;
        if (!isHorse && !isDonkey) return;

        // Spawns del jugador (spawn egg, /summon) tienen persistencia — no tocar
        if (entity instanceof Mob mob && mob.isPersistenceRequired()) return;

        if (!(event.getLevel() instanceof net.minecraft.server.level.ServerLevel serverLevel)) return;

        double x = entity.getX(), y = entity.getY(), z = entity.getZ();
        BlockPos pos = entity.blockPosition();

        // Cancelar y programar reemplazo en el siguiente tick para evitar ConcurrentModificationException
        event.setCanceled(true);

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
    }
}