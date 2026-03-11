package com.tobyink.millionhorses.entity.client.renderer;

import com.google.common.collect.Maps;
import com.tobyink.millionhorses.MillionHorsesMod;
import com.tobyink.millionhorses.entity.client.animator.AlicornAnimator;


import com.tobyink.millionhorses.entity.mobs.AlicornEntity;
import com.tobyink.millionhorses.entity.variant.AlicornVariant;
import com.tobyink.millionhorses.entity.client.renderer.layer.HorseEquipmentLayer;
import com.tobyink.millionhorses.entity.client.renderer.layer.HorseCarpetLayer;
import mod.azure.azurelib.render.entity.AzEntityRenderer;
import com.tobyink.millionhorses.entity.client.renderer.layer.HorseEquipmentLayer;
import com.tobyink.millionhorses.entity.client.renderer.layer.HorseCarpetLayer;
import mod.azure.azurelib.render.entity.AzEntityRendererConfig;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

public class AlicornRenderer extends AzEntityRenderer<AlicornEntity> {


    private static final ResourceLocation MODEL = MillionHorsesMod.modResource(
            "geo/entity/alicorn.geo.json");
    private static final ResourceLocation DEFAULT_TEXTURE =
            MillionHorsesMod.modResource("textures/entity/alicorn/alicorn1.png");

    public static final Map<AlicornVariant, ResourceLocation> LOCATION_BY_VARIANT =
            Util.make(Maps.newEnumMap(AlicornVariant.class), map -> {
                map.put(AlicornVariant.WHITE,        MillionHorsesMod.modResource("textures/entity/alicorn/alicorn1.png"));
                map.put(AlicornVariant.PURPLE,       MillionHorsesMod.modResource("textures/entity/alicorn/alicorn2.png"));
                map.put(AlicornVariant.RAINBOW,      MillionHorsesMod.modResource("textures/entity/alicorn/alicorn3.png"));
                map.put(AlicornVariant.DARK,         MillionHorsesMod.modResource("textures/entity/alicorn/alicorn4.png"));
                map.put(AlicornVariant.WHITE_BLUE,   MillionHorsesMod.modResource("textures/entity/alicorn/alicorn1_blue.png"));
                map.put(AlicornVariant.PURPLE_BLUE,  MillionHorsesMod.modResource("textures/entity/alicorn/alicorn2_blue.png"));
                map.put(AlicornVariant.RAINBOW_BLUE, MillionHorsesMod.modResource("textures/entity/alicorn/alicorn3_blue.png"));
                map.put(AlicornVariant.DARK_BLUE,    MillionHorsesMod.modResource("textures/entity/alicorn/alicorn4_blue.png"));
            });

    @SuppressWarnings("unchecked")
    public AlicornRenderer(EntityRendererProvider.Context context) {
        super(
                AzEntityRendererConfig.<AlicornEntity>builder(
                                $ -> MODEL,
                                entity -> LOCATION_BY_VARIANT.getOrDefault(
                                        entity.getAlicornVariant(), DEFAULT_TEXTURE))
                        .setAnimatorProvider(AlicornAnimator::new)
                        .addRenderLayer(new HorseEquipmentLayer<AlicornEntity>())
                        .addRenderLayer(new HorseCarpetLayer<AlicornEntity>())
                        .setShadowRadius(0.85F)
                        .setRenderType(entity -> RenderType.entityCutoutNoCull(
                                LOCATION_BY_VARIANT.getOrDefault(entity.getAlicornVariant(), DEFAULT_TEXTURE)))
                        .build(),
                context
        );
    }

    @Override
    public ResourceLocation getTextureLocation(AlicornEntity entity) {
        return LOCATION_BY_VARIANT.getOrDefault(entity.getAlicornVariant(), DEFAULT_TEXTURE);
    }
}