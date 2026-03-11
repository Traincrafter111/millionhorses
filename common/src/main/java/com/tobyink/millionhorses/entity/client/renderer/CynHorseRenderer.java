package com.tobyink.millionhorses.entity.client.renderer;

import com.tobyink.millionhorses.MillionHorsesMod;
import com.tobyink.millionhorses.entity.client.animator.CynHorseAnimator;
import com.tobyink.millionhorses.entity.client.renderer.layer.CynHorseVariantLayer;
import com.tobyink.millionhorses.entity.client.renderer.layer.HorseCarpetLayer;
import com.tobyink.millionhorses.entity.client.renderer.layer.HorseEquipmentLayer;
import com.tobyink.millionhorses.entity.mobs.CynHorseEntity;
import mod.azure.azurelib.render.entity.AzEntityRenderer;
import mod.azure.azurelib.render.entity.AzEntityRendererConfig;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class CynHorseRenderer extends AzEntityRenderer<CynHorseEntity> {

    private static final ResourceLocation MODEL =
            MillionHorsesMod.modResource("geo/entity/cyn_horse.geo.json");

    @SuppressWarnings("unchecked")
    public CynHorseRenderer(EntityRendererProvider.Context context) {
        super(
                AzEntityRendererConfig.<CynHorseEntity>builder(
                                $ -> MODEL,
                                entity -> CynHorseVariantLayer.getBaseTexture(entity.getBaseId()))
                        .setAnimatorProvider(CynHorseAnimator::new)
                        .addRenderLayer(new CynHorseVariantLayer())
                        .addRenderLayer(new HorseEquipmentLayer<CynHorseEntity>())
                        .addRenderLayer(new HorseCarpetLayer<CynHorseEntity>())
                        .setShadowRadius(0.85F)
                        .setRenderType(entity -> RenderType.entityCutoutNoCull(
                                CynHorseVariantLayer.getBaseTexture(entity.getBaseId())))
                        .build(),
                context
        );
    }

    @Override
    public ResourceLocation getTextureLocation(CynHorseEntity entity) {
        return CynHorseVariantLayer.getBaseTexture(entity.getBaseId());
    }
}