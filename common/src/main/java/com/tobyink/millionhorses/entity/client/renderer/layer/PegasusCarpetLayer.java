package com.tobyink.millionhorses.entity.client.renderer.layer;

import com.tobyink.millionhorses.MillionHorsesMod;
import com.tobyink.millionhorses.entity.mobs.PegasusEntity;
import mod.azure.azurelib.model.AzBakedModel;
import mod.azure.azurelib.model.AzBone;
import mod.azure.azurelib.render.AzRendererPipelineContext;
import mod.azure.azurelib.render.layer.AzRenderLayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PegasusCarpetLayer implements AzRenderLayer<UUID, PegasusEntity> {

    private static final Map<Item, ResourceLocation> CARPET_TEXTURES = Map.ofEntries(
            Map.entry(Items.BLACK_CARPET,      MillionHorsesMod.modResource("textures/entity/carpets/carpet_black.png")),
            Map.entry(Items.BLUE_CARPET,       MillionHorsesMod.modResource("textures/entity/carpets/carpet_blue.png")),
            Map.entry(Items.BROWN_CARPET,      MillionHorsesMod.modResource("textures/entity/carpets/carpet_brown.png")),
            Map.entry(Items.CYAN_CARPET,       MillionHorsesMod.modResource("textures/entity/carpets/carpet_cyan.png")),
            Map.entry(Items.GRAY_CARPET,       MillionHorsesMod.modResource("textures/entity/carpets/carpet_gray.png")),
            Map.entry(Items.GREEN_CARPET,      MillionHorsesMod.modResource("textures/entity/carpets/carpet_green.png")),
            Map.entry(Items.LIGHT_BLUE_CARPET, MillionHorsesMod.modResource("textures/entity/carpets/carpet_l_blue.png")),
            Map.entry(Items.LIGHT_GRAY_CARPET, MillionHorsesMod.modResource("textures/entity/carpets/carpet_l_gray.png")),
            Map.entry(Items.LIME_CARPET,       MillionHorsesMod.modResource("textures/entity/carpets/carpet_lime.png")),
            Map.entry(Items.MAGENTA_CARPET,    MillionHorsesMod.modResource("textures/entity/carpets/carpet_mag.png")),
            Map.entry(Items.MOSS_CARPET,       MillionHorsesMod.modResource("textures/entity/carpets/carpet_moss.png")),
            Map.entry(Items.ORANGE_CARPET,     MillionHorsesMod.modResource("textures/entity/carpets/carpet_orange.png")),
            Map.entry(Items.PINK_CARPET,       MillionHorsesMod.modResource("textures/entity/carpets/carpet_pink.png")),
            Map.entry(Items.PURPLE_CARPET,     MillionHorsesMod.modResource("textures/entity/carpets/carpet_purple.png")),
            Map.entry(Items.RED_CARPET,        MillionHorsesMod.modResource("textures/entity/carpets/carpet_red.png")),
            Map.entry(Items.WHITE_CARPET,      MillionHorsesMod.modResource("textures/entity/carpets/carpet_white.png")),
            Map.entry(Items.YELLOW_CARPET,     MillionHorsesMod.modResource("textures/entity/carpets/carpet_yellow.png"))
            // carpet_moss_pale: se añadirá cuando exista el item correspondiente
    );

    public static boolean isCarpet(ItemStack stack) {
        return CARPET_TEXTURES.containsKey(stack.getItem());
    }

    @Override
    public void preRender(AzRendererPipelineContext<UUID, PegasusEntity> context) {}

    @Override
    public void render(AzRendererPipelineContext<UUID, PegasusEntity> context) {
        PegasusEntity entity = context.animatable();
        ItemStack carpetStack = entity.getCarpetItem();
        if (carpetStack.isEmpty()) return;

        ResourceLocation texture = CARPET_TEXTURES.get(carpetStack.getItem());
        if (texture == null) return;

        AzBakedModel model = context.bakedModel();
        if (model == null) return;

        RenderType renderType = RenderType.entityCutoutNoCull(texture);


        // reRender con textura de alfombra
        var prevRenderType    = context.renderType();
        var prevVertexConsumer = context.vertexConsumer();

        context.setRenderType(renderType);
        context.setVertexConsumer(context.multiBufferSource().getBuffer(renderType));
        context.rendererPipeline().reRender(context);

        // Restaurar
        context.setRenderType(prevRenderType);
        context.setVertexConsumer(prevVertexConsumer);

    }

    @Override
    public void renderForBone(AzRendererPipelineContext<UUID, PegasusEntity> context, AzBone bone) {}
}