package com.tobyink.millionhorses.entity.client.renderer;

import com.google.common.collect.Maps;
import com.tobyink.millionhorses.MillionHorsesMod;
import com.tobyink.millionhorses.entity.client.animator.PegasusAnimator;
import com.tobyink.millionhorses.entity.client.renderer.layer.PegasusEquipmentLayer;
import com.tobyink.millionhorses.entity.mobs.PegasusEntity;
import com.tobyink.millionhorses.entity.variant.PegasusVariant;
import mod.azure.azurelib.render.entity.AzEntityRenderer;
import mod.azure.azurelib.render.entity.AzEntityRendererConfig;
import net.minecraft.Util;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

public class PegasusRenderer extends AzEntityRenderer<PegasusEntity> {

    private static final ResourceLocation MODEL = MillionHorsesMod.modResource(
            "geo/entity/cyn_pegasus.geo.json");
    private static final ResourceLocation DEFAULT_TEXTURE =
            MillionHorsesMod.modResource("textures/entity/pegasus/pegasus1.png");

    public static final Map<PegasusVariant, ResourceLocation> LOCATION_BY_VARIANT =
            Util.make(Maps.newEnumMap(PegasusVariant.class), map -> {
                map.put(PegasusVariant.WHITE,        MillionHorsesMod.modResource("textures/entity/pegasus/pegasus1.png"));
                map.put(PegasusVariant.PURPLE,       MillionHorsesMod.modResource("textures/entity/pegasus/pegasus2.png"));
                map.put(PegasusVariant.RAINBOW,      MillionHorsesMod.modResource("textures/entity/pegasus/pegasus3.png"));
                map.put(PegasusVariant.DARK,         MillionHorsesMod.modResource("textures/entity/pegasus/pegasus4.png"));
                map.put(PegasusVariant.WHITE_BLUE,   MillionHorsesMod.modResource("textures/entity/pegasus/pegasus1_blue.png"));
                map.put(PegasusVariant.PURPLE_BLUE,  MillionHorsesMod.modResource("textures/entity/pegasus/pegasus2_blue.png"));
                map.put(PegasusVariant.RAINBOW_BLUE, MillionHorsesMod.modResource("textures/entity/pegasus/pegasus3_blue.png"));
                map.put(PegasusVariant.DARK_BLUE,    MillionHorsesMod.modResource("textures/entity/pegasus/pegasus4_blue.png"));
            });

    @SuppressWarnings("unchecked")
    public PegasusRenderer(EntityRendererProvider.Context context) {
        super(
                AzEntityRendererConfig.<PegasusEntity>builder(
                                $ -> MODEL,  // Model location provider
                                entity -> LOCATION_BY_VARIANT.getOrDefault(
                                        entity.getPegasusVariant(), DEFAULT_TEXTURE)  // Texture location provider
                        )
                        .setAnimatorProvider(PegasusAnimator::new)
                        .addRenderLayer(new PegasusEquipmentLayer())
                        .setShadowRadius(0.85F)
                        .build(),
                context
        );
    }

    @Override
    public ResourceLocation getTextureLocation(PegasusEntity entity) {
        return LOCATION_BY_VARIANT.getOrDefault(entity.getPegasusVariant(), DEFAULT_TEXTURE);
    }
}