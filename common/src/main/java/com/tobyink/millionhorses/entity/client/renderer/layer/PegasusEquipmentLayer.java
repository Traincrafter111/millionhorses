package com.tobyink.millionhorses.entity.client.renderer.layer;

import com.mojang.blaze3d.platform.NativeImage;
import com.tobyink.millionhorses.MillionHorsesMod;
import com.tobyink.millionhorses.entity.mobs.PegasusEntity;
import com.tobyink.millionhorses.entity.variant.PegasusVariant;
import mod.azure.azurelib.model.AzBakedModel;
import mod.azure.azurelib.model.AzBone;
import mod.azure.azurelib.render.AzRendererPipelineContext;
import mod.azure.azurelib.render.layer.AzRenderLayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.HorseArmorItem;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PegasusEquipmentLayer implements AzRenderLayer<UUID, PegasusEntity> {

    private static final Set<String> DECORATIVE_BONES = Set.of(
            "wingL","wingL2","wingL3","wingL4",
            "wingR","wingL6","wingL7","wingL8",
            "feathers","feathers2","feathers3",
            "feathers4","feathers5","feathers6",
            "TailA","Mane",
            "Saddle","HeadReins","HeadSaddle",
            "SaddleMouthLineR","SaddleMouthLine","SaddleMouthR","SaddleMouthL",
            "Bags","Bag1","Bag2",
            "capeedges","FL_cape","FR_cape","cape_sideL","cape_sideR","cape_back",
            "BL_cape","BR_cape",
            "eyes","eyelids","UMouth","Ear1","Ear2"
    );

    private static final Map<ResourceLocation, ResourceLocation> ARMOR_CACHE = new HashMap<>();
    private static final int[][] UV_REMAP = buildUvRemap();

    private static int[][] buildUvRemap() {
        java.util.List<int[]> ops = new java.util.ArrayList<>();
        addBox(ops, 0,32,  0,32,  10,10,22);
        addBox(ops, 0,35,  0,35,   4,12, 7);
        addBox(ops, 0,35,  0,109,  4,12, 7);
        addBox(ops, 0,13,  0,13,   6, 5, 7);
        addBox(ops, 0,13, 23,116,  6, 5, 7);
        int[] ttX={52,68,100,84}, btX={56,72,104,88};
        int[][] lX={{48,52,56,60},{64,68,72,76},{96,100,104,108},{80,84,88,92}};
        int[] sX={48,52,56,60};
        for (int i=0;i<4;i++) {
            ops.add(new int[]{ttX[i],21,52,21,4,4});
            ops.add(new int[]{btX[i],21,56,21,4,4});
            ops.add(new int[]{ttX[i],30,52,21,4,4});
            ops.add(new int[]{btX[i],30,56,21,4,4});
            for (int f=0;f<4;f++) {
                ops.add(new int[]{lX[i][f],25,sX[f],25,4,5});
                ops.add(new int[]{lX[i][f],30,sX[f],30,4,6});
            }
        }
        return ops.toArray(new int[0][]);
    }

    private static void addBox(java.util.List<int[]> ops,
                               int su,int sv,int du,int dv,int W,int H,int D) {
        ops.add(new int[]{du+D,     dv,   su+D,     sv,   W,D});
        ops.add(new int[]{du+D+W,   dv,   su+D+W,   sv,   W,D});
        ops.add(new int[]{du,       dv+D, su,       sv+D, D,H});
        ops.add(new int[]{du+D,     dv+D, su+D,     sv+D, W,H});
        ops.add(new int[]{du+D+W,   dv+D, su+D+W,   sv+D, D,H});
        ops.add(new int[]{du+D+W+D, dv+D, su+D+W+D, sv+D, W,H});
    }

    private static ResourceLocation remapArmorTexture(ResourceLocation vanillaLoc) {
        return ARMOR_CACHE.computeIfAbsent(vanillaLoc, loc -> {
            try (InputStream stream = Minecraft.getInstance().getResourceManager()
                    .getResource(loc).orElseThrow().open();
                 NativeImage src = NativeImage.read(stream)) {

                NativeImage dst = new NativeImage(NativeImage.Format.RGBA, 128, 128, true);
                for (int[] op : UV_REMAP) {
                    int dx=op[0],dy=op[1],sx=op[2],sy=op[3],w=op[4],h=op[5];
                    for (int py=0;py<h;py++)
                        for (int px=0;px<w;px++)
                            if (sx+px<src.getWidth() && sy+py<src.getHeight())
                                dst.setPixelRGBA(dx+px,dy+py,src.getPixelRGBA(sx+px,sy+py));
                }
                ResourceLocation id = new ResourceLocation("millionhorses",
                        "dynamic/pegasus_armor/"+loc.getNamespace()+"_"
                                +loc.getPath().replace('/','_').replace('.','_'));
                Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(dst));
                return id;
            } catch (IOException e) {
                System.out.println("[PegasusArmor] ERROR textura: " + loc + " -> " + e.getMessage());
                return loc;
            }
        });
    }

    @Override
    public void preRender(AzRendererPipelineContext<UUID, PegasusEntity> context) {
        PegasusEntity entity = context.animatable();
        AzBakedModel model = context.bakedModel();
        if (model == null) return;

        setBoneVisible(model, "Saddle",     entity.isSaddled());
        setBoneVisible(model, "HeadReins",  entity.isSaddled());
        setBoneVisible(model, "HeadSaddle", entity.isSaddled());
        setBoneVisible(model, "Bags",       entity.hasChest());
    }

    @Override
    public void render(AzRendererPipelineContext<UUID, PegasusEntity> context) {
        PegasusEntity entity = context.animatable();
        ItemStack stack = entity.getArmor();
        if (stack.isEmpty() || !(stack.getItem() instanceof HorseArmorItem armorItem)) return;

        AzBakedModel model = context.bakedModel();
        if (model == null) return;

        ResourceLocation armorTexture = remapArmorTexture(armorItem.getTexture());
        RenderType renderType = RenderType.entityCutoutNoCull(armorTexture);

        // Guardar estado de visibilidad de todos los bones decorativos
        Map<String, Boolean> savedVisibility = new HashMap<>();
        for (String boneName : DECORATIVE_BONES) {
            model.getBone(boneName).ifPresent(b -> savedVisibility.put(boneName, b.isHidden()));
        }

        // Ocultar todos los bones decorativos para el reRender de armadura
        for (String boneName : DECORATIVE_BONES) {
            model.getBone(boneName).ifPresent(b -> b.setHidden(true));
        }

        // Hacer reRender con textura de armadura
        var prevRenderType = context.renderType();
        var prevVertexConsumer = context.vertexConsumer();

        context.setRenderType(renderType);
        context.setVertexConsumer(context.multiBufferSource().getBuffer(renderType));
        context.rendererPipeline().reRender(context);

        // Restaurar estado
        context.setRenderType(prevRenderType);
        context.setVertexConsumer(prevVertexConsumer);

        // Restaurar visibilidad original de todos los bones decorativos
        for (String boneName : DECORATIVE_BONES) {
            model.getBone(boneName).ifPresent(b -> {
                Boolean wasHidden = savedVisibility.get(boneName);
                if (wasHidden != null) b.setHidden(wasHidden);
            });
        }
    }

    @Override
    public void renderForBone(AzRendererPipelineContext<UUID, PegasusEntity> context, AzBone bone) {}

    private void setBoneVisible(AzBakedModel model, String name, boolean visible) {
        model.getBone(name).ifPresent(b -> b.setHidden(!visible));
    }
}