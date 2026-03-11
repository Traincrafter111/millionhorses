package com.tobyink.millionhorses.entity.client.animator;

import com.tobyink.millionhorses.entity.mobs.CynHorseEntity;
import mod.azure.azurelib.animation.AzAnimatorConfig;
import mod.azure.azurelib.animation.controller.AzAnimationController;
import mod.azure.azurelib.animation.controller.AzAnimationControllerContainer;
import mod.azure.azurelib.animation.impl.AzEntityAnimator;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class CynHorseAnimator extends AzEntityAnimator<CynHorseEntity> {

    private static final ResourceLocation ANIMATIONS = new ResourceLocation(
            "millionhorses", "animations/entity/cyn_horse.animation.json"
    );

    public CynHorseAnimator() {
        super(AzAnimatorConfig.defaultConfig());
    }

    @Override
    public void registerControllers(AzAnimationControllerContainer<CynHorseEntity> c) {
        c.add(AzAnimationController.builder(this, "base_controller").build());
        c.add(AzAnimationController.builder(this, "tail_controller").build());
        c.add(AzAnimationController.builder(this, "action_controller")
                .setTransitionLength(5)
                .build()
        );
        c.add(AzAnimationController.builder(this, "sleep_controller").build());
        c.add(AzAnimationController.builder(this, "baby_controller").build());
    }

    @Override
    public @NotNull ResourceLocation getAnimationLocation(CynHorseEntity animatable) {
        return ANIMATIONS;
    }
}