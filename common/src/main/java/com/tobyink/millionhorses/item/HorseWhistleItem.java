package com.tobyink.millionhorses.item;

import com.tobyink.millionhorses.entity.constant.MovementMode;
import com.tobyink.millionhorses.entity.mobs.PegasusEntity;
import com.tobyink.millionhorses.registry.SoundRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class HorseWhistleItem extends Item {

    private static final String TAG_MODE = "WhistleMode";

    public HorseWhistleItem() {
        super(new Item.Properties().stacksTo(1));
    }

    // --- Modo guardado en NBT del item ---
    public static MovementMode getMode(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_MODE)) return MovementMode.WANDERING;
        try {
            return MovementMode.valueOf(tag.getString(TAG_MODE));
        } catch (IllegalArgumentException e) {
            return MovementMode.WANDERING;
        }
    }

    private static void setMode(ItemStack stack, MovementMode mode) {
        stack.getOrCreateTag().putString(TAG_MODE, mode.name());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.isShiftKeyDown()) {
            MovementMode mode = getMode(stack);

            if (level.isClientSide) {
                level.playSound(player, player.blockPosition(),
                        getSoundForMode(mode), SoundSource.NEUTRAL, 1.0F, 1.0F);
                return InteractionResultHolder.success(stack);
            }

            List<PegasusEntity> nearby = level.getEntitiesOfClass(
                    PegasusEntity.class,
                    player.getBoundingBox().inflate(32.0),
                    p -> p.isTamed() && player.getUUID().equals(p.getOwnerUUID())
            );

            if (nearby.isEmpty()) {
                player.displayClientMessage(
                        Component.translatable("item.millionhorses.horse_whistle.no_horse"), true);
                return InteractionResultHolder.fail(stack);
            }

            for (PegasusEntity pegasus : nearby) {
                pegasus.setMovementMode(mode);
            }

            if (player instanceof ServerPlayer) {
                player.displayClientMessage(
                        Component.translatable(
                                "item.millionhorses.horse_whistle.activate." + mode.name().toLowerCase(),
                                nearby.size()),
                        true);
            }

        } else {
            MovementMode next = getMode(stack).next();
            setMode(stack, next);

            if (level.isClientSide) {
                return InteractionResultHolder.success(stack);
            }

            if (player instanceof ServerPlayer) {
                player.displayClientMessage(
                        Component.translatable("item.millionhorses.horse_whistle.mode." + next.name().toLowerCase()),
                        true);
            }
        }

        return InteractionResultHolder.success(stack);
    }

    private SoundEvent getSoundForMode(MovementMode mode) {
        return switch (mode) {
            case WANDERING -> SoundRegistry.WHISTLE_WANDER.get();
            case FOLLOWING -> SoundRegistry.WHISTLE_FOLLOW.get();
            case SITTING   -> SoundRegistry.WHISTLE_STAY.get();
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        MovementMode mode = getMode(stack);
        tooltip.add(Component.translatable(
                "item.millionhorses.horse_whistle.tooltip.mode." + mode.name().toLowerCase()));
        tooltip.add(Component.translatable("item.millionhorses.horse_whistle.tooltip.hint"));
    }
}