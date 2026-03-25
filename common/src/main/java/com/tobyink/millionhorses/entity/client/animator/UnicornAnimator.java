package com.tobyink.millionhorses.entity.client.animator;


import com.tobyink.millionhorses.entity.mobs.UnicornEntity;
import mod.azure.azurelib.animation.AzAnimatorConfig;
import mod.azure.azurelib.animation.controller.AzAnimationController;
import mod.azure.azurelib.animation.controller.AzAnimationControllerContainer;
import mod.azure.azurelib.animation.impl.AzEntityAnimator;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class UnicornAnimator extends AzEntityAnimator<UnicornEntity> {

    private static final ResourceLocation ANIMATIONS = new ResourceLocation(
            "millionhorses", "animations/entity/cyn_horse.animation.json"
    );

    public UnicornAnimator() {
        super(AzAnimatorConfig.defaultConfig());
    }

    @Override
    public void registerControllers(AzAnimationControllerContainer<UnicornEntity> c) {
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
    public @NotNull ResourceLocation getAnimationLocation(UnicornEntity animatable) {
        return ANIMATIONS;
    }
}