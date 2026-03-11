package com.tobyink.millionhorses.event;

import com.tobyink.millionhorses.config.ModConfig;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.level.ServerLevelAccessor;

public class VanillaHorseSpawnHandler {

    private static final int CHECK_INTERVAL = 40;

    public static void register() {

        // 1. En el momento del spawn: marcar con persistencia los caballos
        //    spawneados por el jugador (spawn egg, comando) para protegerlos.
        EntityEvent.LIVING_CHECK_SPAWN.register((entity, level, x, y, z, spawnType, spawner) -> {
            if (!(entity instanceof Horse horse)) return EventResult.pass();
            if (spawnType == MobSpawnType.SPAWN_EGG
                    || spawnType == MobSpawnType.COMMAND
                    || spawnType == MobSpawnType.BUCKET
                    || spawnType == MobSpawnType.MOB_SUMMONED) {
                horse.setPersistenceRequired();
            }
            return EventResult.pass();
        });

        // 2. Cada 2 segundos: descartar caballos vanilla naturales si config=false
        TickEvent.SERVER_LEVEL_POST.register(level -> {
            if (ModConfig.allowVanillaHorseSpawns) return;
            if (level.getGameTime() % CHECK_INTERVAL != 0) return;

            level.players().forEach(player ->
                    level.getEntitiesOfClass(Horse.class,
                                    player.getBoundingBox().inflate(128))
                            .forEach(horse -> {
                                if (!horse.isPersistenceRequired()) {
                                    horse.discard();
                                }
                            })
            );
        });
    }
}