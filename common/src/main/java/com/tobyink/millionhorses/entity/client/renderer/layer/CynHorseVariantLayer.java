package com.tobyink.millionhorses.entity.client.renderer.layer;

import com.tobyink.millionhorses.MillionHorsesMod;
import com.tobyink.millionhorses.entity.mobs.CynHorseEntity;
import mod.azure.azurelib.model.AzBakedModel;
import mod.azure.azurelib.model.AzBone;
import mod.azure.azurelib.render.AzRendererPipelineContext;
import mod.azure.azurelib.render.layer.AzRenderLayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * Renderiza las 4 capas de variante del CynHorse sobre la textura base.
 *
 * Estructura de assets (128x128, negro puro = transparente):
 *   textures/entity/cyn_horse/base_coat/base_coat_0.png  … base_coat_21.png  (22)
 *   textures/entity/cyn_horse/pattern/pattern_0.png       … pattern_17.png   (18)
 *   textures/entity/cyn_horse/head/head_0.png             … head_16.png      (17)
 *   textures/entity/cyn_horse/sock/sock_0.png             … sock_13.png      (14)
 *   textures/entity/cyn_horse/mane/mane_0.png             … mane_13.png      (14)
 *
 * La textura base la sirve el renderer principal via getTextureLocation().
 * Este layer renderiza encima: pattern → head → sock → mane.
 */
public class CynHorseVariantLayer implements AzRenderLayer<UUID, CynHorseEntity> {

    // ── Rutas de textura ─────────────────────────────────────────────────────

    public static ResourceLocation getBaseTexture(int id) {
        return MillionHorsesMod.modResource(
                "textures/entity/cyn_horse/base_coats/base_coat_" + id + ".png");
    }

    public static ResourceLocation getPatternTexture(int id) {
        return MillionHorsesMod.modResource(
                "textures/entity/cyn_horse/patterns/pattern_" + id + ".png");
    }

    public static ResourceLocation getHeadTexture(int id) {
        return MillionHorsesMod.modResource(
                "textures/entity/cyn_horse/heads/head_" + id + ".png");
    }

    public static ResourceLocation getSockTexture(int id) {
        return MillionHorsesMod.modResource(
                "textures/entity/cyn_horse/socks/sock_" + id + ".png");
    }

    public static ResourceLocation getManeTexture(int id) {
        return MillionHorsesMod.modResource(
                "textures/entity/cyn_horse/manes/mane_" + id + ".png");
    }

    // ── AzRenderLayer ────────────────────────────────────────────────────────

    @Override
    public void preRender(AzRendererPipelineContext<UUID, CynHorseEntity> context) {}

    @Override
    public void render(AzRendererPipelineContext<UUID, CynHorseEntity> context) {
        CynHorseEntity entity = context.animatable();
        AzBakedModel model = context.bakedModel();
        if (model == null) return;

        renderLayer(context, getPatternTexture(entity.getPatternId()));
        renderLayer(context, getHeadTexture(entity.getHeadId()));
        renderLayer(context, getSockTexture(entity.getSockId()));
        renderLayer(context, getManeTexture(entity.getManeId()));
    }

    private void renderLayer(AzRendererPipelineContext<UUID, CynHorseEntity> context,
                             ResourceLocation texture) {
        RenderType renderType = RenderType.entityTranslucentCull(texture);

        var prevRenderType     = context.renderType();
        var prevVertexConsumer = context.vertexConsumer();

        context.setRenderType(renderType);
        context.setVertexConsumer(context.multiBufferSource().getBuffer(renderType));
        context.rendererPipeline().reRender(context);

        context.setRenderType(prevRenderType);
        context.setVertexConsumer(prevVertexConsumer);
    }

    @Override
    public void renderForBone(AzRendererPipelineContext<UUID, CynHorseEntity> context, AzBone bone) {}
}