package com.tobyink.millionhorses.entity.client.renderer;

import com.google.common.collect.Maps;
import com.tobyink.millionhorses.MillionHorsesMod;
import com.tobyink.millionhorses.entity.client.animator.UnicornAnimator;
import com.tobyink.millionhorses.entity.client.renderer.layer.HorseCarpetLayer;
import com.tobyink.millionhorses.entity.client.renderer.layer.HorseEquipmentLayer;
import com.tobyink.millionhorses.entity.mobs.PegasusEntity;
import com.tobyink.millionhorses.entity.mobs.UnicornEntity;
import com.tobyink.millionhorses.entity.variant.UnicornVariant;
import mod.azure.azurelib.render.entity.AzEntityRenderer;
import mod.azure.azurelib.render.entity.AzEntityRendererConfig;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

public class UnicornRenderer extends AzEntityRenderer<UnicornEntity> {

    private static final ResourceLocation MODEL = MillionHorsesMod.modResource(
            "geo/entity/cyn_unicorn.geo.json");
    private static final ResourceLocation DEFAULT_TEXTURE =
            MillionHorsesMod.modResource("textures/entity/unicorn/unicorn1.png");

    public static final Map<UnicornVariant, ResourceLocation> LOCATION_BY_VARIANT =
            Util.make(Maps.newEnumMap(UnicornVariant.class), map -> {
                map.put(UnicornVariant.WHITE,        MillionHorsesMod.modResource("textures/entity/unicorn/unicorn1.png"));
                map.put(UnicornVariant.PURPLE,       MillionHorsesMod.modResource("textures/entity/unicorn/unicorn2.png"));
                map.put(UnicornVariant.RAINBOW,      MillionHorsesMod.modResource("textures/entity/unicorn/unicorn3.png"));
                map.put(UnicornVariant.DARK,         MillionHorsesMod.modResource("textures/entity/unicorn/unicorn4.png"));
                map.put(UnicornVariant.WHITE_BLUE,   MillionHorsesMod.modResource("textures/entity/unicorn/unicorn1_blue.png"));
                map.put(UnicornVariant.PURPLE_BLUE,  MillionHorsesMod.modResource("textures/entity/unicorn/unicorn2_blue.png"));
                map.put(UnicornVariant.RAINBOW_BLUE, MillionHorsesMod.modResource("textures/entity/unicorn/unicorn3_blue.png"));
                map.put(UnicornVariant.DARK_BLUE,    MillionHorsesMod.modResource("textures/entity/unicorn/unicorn4_blue.png"));
            });

    @SuppressWarnings("unchecked")
    public UnicornRenderer(EntityRendererProvider.Context context) {
        super(
                AzEntityRendererConfig.<UnicornEntity>builder(
                                $ -> MODEL,
                                entity -> LOCATION_BY_VARIANT.getOrDefault(
                                        entity.getUnicornVariant(), DEFAULT_TEXTURE))
                        .setAnimatorProvider(UnicornAnimator::new)
                        .addRenderLayer(new HorseEquipmentLayer<>())
                        .addRenderLayer(new HorseCarpetLayer<>())
                        .setShadowRadius(0.85F)
                        .setRenderType(entity -> RenderType.entityCutoutNoCull(
                                LOCATION_BY_VARIANT.getOrDefault(entity.getUnicornVariant(), DEFAULT_TEXTURE)))
                        .build(),
                context
        );
    }

    @Override
    public ResourceLocation getTextureLocation(UnicornEntity entity) {
        return LOCATION_BY_VARIANT.getOrDefault(entity.getUnicornVariant(), DEFAULT_TEXTURE);
    }
}