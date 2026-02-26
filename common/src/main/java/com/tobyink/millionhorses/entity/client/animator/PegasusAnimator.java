package com.tobyink.millionhorses.entity.client.animator;

import com.tobyink.millionhorses.MillionHorsesMod;
import com.tobyink.millionhorses.entity.mobs.PegasusEntity;
import mod.azure.azurelib.animation.AzAnimatorConfig;
import mod.azure.azurelib.animation.controller.AzAnimationController;
import mod.azure.azurelib.animation.controller.AzAnimationControllerContainer;
import mod.azure.azurelib.animation.impl.AzEntityAnimator;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class PegasusAnimator extends AzEntityAnimator<PegasusEntity> {

    private static final  ResourceLocation ANIMATIONS = MillionHorsesMod.modResource(
            "animations/entity/cyn_pegasus.animation.json"
    );


    public PegasusAnimator() {
        super(AzAnimatorConfig.defaultConfig());
    }


    @Override
    public void registerControllers(AzAnimationControllerContainer<PegasusEntity> animationControllerContainer) {
        animationControllerContainer.add(
                AzAnimationController.builder(this, "base_controller")
                        .build()
        );
        animationControllerContainer.add(
                AzAnimationController.builder(this, "tail_controller")
                        .setTransitionLength(5)
                        .build()
        );
        animationControllerContainer.add(
                AzAnimationController.builder(this, "action_controller")
                        .setTransitionLength(5)
                        .build()
        );
        animationControllerContainer.add(
                AzAnimationController.builder(this, "sleep_controller")
                        .build()
        );
    }

    @Override
    public @NotNull ResourceLocation getAnimationLocation(PegasusEntity animatable) {
        return ANIMATIONS;
    }
}