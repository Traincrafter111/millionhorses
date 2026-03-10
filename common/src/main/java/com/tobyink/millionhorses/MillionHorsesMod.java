package com.tobyink.millionhorses;

import net.minecraft.resources.ResourceLocation;
import com.tobyink.millionhorses.registry.EntityRegistry;
import com.tobyink.millionhorses.registry.ItemRegistry;
import com.tobyink.millionhorses.registry.SoundRegistry;
import com.tobyink.millionhorses.registry.TabRegistry;
import com.tobyink.millionhorses.entity.client.renderer.PegasusRenderer;
import com.tobyink.millionhorses.entity.client.screen.mHorsesScreen;
import com.tobyink.millionhorses.entity.mobs.PegasusEntity;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.Blocks;

public final class MillionHorsesMod {
    public static final String MOD_ID = "millionhorses";

    public static void init() {
        EntityRegistry.init();
        ItemRegistry.init();
        SoundRegistry.init();
        TabRegistry.init();
        com.tobyink.millionhorses.registry.MenuRegistry.init();
        registerPegasusSpawner();
    }

    // Cada cuantos ticks intentar spawn (200 = 10 segundos)
    private static final int SPAWN_INTERVAL = 200;
    // Probabilidad de spawn por hay block encontrado (1 en N)
    private static final int SPAWN_CHANCE = 20;

    private static void registerPegasusSpawner() {
        TickEvent.SERVER_LEVEL_POST.register(level -> {
            if (level.getGameTime() % SPAWN_INTERVAL != 0) return;

            // Buscar jugadores y intentar spawn cerca de ellos
            level.players().forEach(player -> {
                BlockPos playerPos = player.blockPosition();

                int range = 48;
                for (int attempts = 0; attempts < 5; attempts++) {
                    int dx = level.random.nextInt(range * 2) - range;
                    int dz = level.random.nextInt(range * 2) - range;
                    for (int y = 175; y <= 320; y++) {
                        BlockPos pos = new BlockPos(playerPos.getX() + dx, y, playerPos.getZ() + dz);
                        if (level.getBlockState(pos).is(Blocks.HAY_BLOCK)) {
                            if (!level.getBlockState(pos.above()).isAir()) continue;
                            if (!level.getBlockState(pos.above(2)).isAir()) continue;
                            if (level.random.nextInt(SPAWN_CHANCE) != 0) continue;

                            long nearbyPegasus = level.getEntitiesOfClass(PegasusEntity.class, player.getBoundingBox().inflate(64)).size();
                            if (nearbyPegasus >= 3) continue;

                            PegasusEntity pegasus = EntityRegistry.PEGASUS.get().create(level);
                            if (pegasus == null) continue;
                            pegasus.moveTo(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5,
                                    level.random.nextFloat() * 360, 0);
                            pegasus.finalizeSpawn(level, level.getCurrentDifficultyAt(pos),
                                    MobSpawnType.NATURAL, null, null);
                            level.addFreshEntity(pegasus);
                            break;
                        }
                    }
                }
            });
        });
    }

    public static void initClient() {
        // EntityRenderer: en Forge lo registra MillionHorsesModForgeClient.registerRenderers
        // En Fabric lo registra MillionHorsesModFabricClient via EntityRendererRegistry
        MenuRegistry.registerScreenFactory(
                com.tobyink.millionhorses.registry.MenuRegistry.PEGASUS_MENU.get(),
                mHorsesScreen::new);
        dev.architectury.registry.client.rendering.ColorHandlerRegistry.registerItemColors(
                (stack, tintIndex) -> -1,
                ItemRegistry.PEGASUS_SPAWN_EGG
        );
    }

    public static ResourceLocation modResource(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}