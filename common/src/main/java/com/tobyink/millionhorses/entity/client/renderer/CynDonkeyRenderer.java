package com.tobyink.millionhorses.entity.client.renderer;

import com.google.common.collect.Maps;
import com.tobyink.millionhorses.MillionHorsesMod;

import com.tobyink.millionhorses.entity.client.animator.CynDonkeyAnimator;
import com.tobyink.millionhorses.entity.client.renderer.layer.HorseCarpetLayer;
import com.tobyink.millionhorses.entity.client.renderer.layer.HorseEquipmentLayer;;
import com.tobyink.millionhorses.entity.mobs.CynDonkeyEntity;
import com.tobyink.millionhorses.entity.variant.CynDonkeyVariant;
import mod.azure.azurelib.render.entity.AzEntityRenderer;
import mod.azure.azurelib.render.entity.AzEntityRendererConfig;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

public class CynDonkeyRenderer extends AzEntityRenderer<CynDonkeyEntity> {


    private static final ResourceLocation MODEL = MillionHorsesMod.modResource(
            "geo/entity/cyn_donkey.geo.json");
    private static final ResourceLocation DEFAULT_TEXTURE =
            MillionHorsesMod.modResource("textures/entity/donkey/base_0.png");

    public static final Map<CynDonkeyVariant, ResourceLocation> LOCATION_BY_VARIANT =
            Util.make(Maps.newEnumMap(CynDonkeyVariant.class), map -> {
                map.put(CynDonkeyVariant.DEFAULT,        MillionHorsesMod.modResource("textures/entity/donkey/base_0.png"));
                map.put(CynDonkeyVariant.GRAY,       MillionHorsesMod.modResource("textures/entity/donkey/base_1.png"));
                map.put(CynDonkeyVariant.BROWN,      MillionHorsesMod.modResource("textures/entity/donkey/base_2.png"));
            });

    @SuppressWarnings("unchecked")
    public CynDonkeyRenderer(EntityRendererProvider.Context context) {
        super(
                AzEntityRendererConfig.<CynDonkeyEntity>builder(
                                $ -> MODEL,
                                entity -> LOCATION_BY_VARIANT.getOrDefault(
                                        entity.getCynDonkeyVariant(), DEFAULT_TEXTURE))
                        .setAnimatorProvider(CynDonkeyAnimator::new)
                        .addRenderLayer(new HorseEquipmentLayer<CynDonkeyEntity>())
                        .addRenderLayer(new HorseCarpetLayer<CynDonkeyEntity>())
                        .setShadowRadius(0.85F)
                        .setRenderType(entity -> RenderType.entityCutoutNoCull(
                                LOCATION_BY_VARIANT.getOrDefault(entity.getCynDonkeyVariant(), DEFAULT_TEXTURE)))
                        .build(),
                context
        );
    }

    @Override
    public ResourceLocation getTextureLocation(CynDonkeyEntity entity) {
        return LOCATION_BY_VARIANT.getOrDefault(entity.getCynDonkeyVariant(), DEFAULT_TEXTURE);
    }
}