package com.tobyink.millionhorses.entity.client.animator;

import com.tobyink.millionhorses.entity.mobs.AlicornEntity;
import mod.azure.azurelib.animation.AzAnimatorConfig;
import mod.azure.azurelib.animation.controller.AzAnimationController;
import mod.azure.azurelib.animation.controller.AzAnimationControllerContainer;
import mod.azure.azurelib.animation.impl.AzEntityAnimator;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * Animator for AlicornEntity.
 * Reuses the same animation file and controller logic as PegasusAnimator,
 * but typed to AlicornEntity so the renderer generic types match.
 */
public class AlicornAnimator extends AzEntityAnimator<AlicornEntity> {


    private static final ResourceLocation ANIMATIONS = new ResourceLocation(
            "millionhorses", "animations/entity/cyn_horse.animation.json"
    );

    public AlicornAnimator() {
        super(AzAnimatorConfig.defaultConfig());
    }

    @Override
    public @NotNull ResourceLocation getAnimationLocation(AlicornEntity entity) {
        return ANIMATIONS;
    }

    @Override
    public void registerControllers(
            @NotNull AzAnimationControllerContainer<AlicornEntity> c) {
        c.add(AzAnimationController.builder(this, "base_controller").build());
        c.add(AzAnimationController.builder(this, "tail_controller").build());
        c.add(AzAnimationController.builder(this, "action_controller")
                .setTransitionLength(5)
                .build());
        c.add(AzAnimationController.builder(this, "sleep_controller").build());
        c.add(AzAnimationController.builder(this, "baby_controller").build());
    }
}