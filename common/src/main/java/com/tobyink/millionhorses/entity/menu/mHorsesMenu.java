package com.tobyink.millionhorses.entity.menu;

import com.tobyink.millionhorses.entity.client.renderer.layer.HorseCarpetLayer;
import com.tobyink.millionhorses.entity.mobs.AbstractMillionHorseEntity;
import com.tobyink.millionhorses.registry.MenuRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.HorseArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class mHorsesMenu extends AbstractContainerMenu {

    private final Container horseContainer;
    private final AbstractMillionHorseEntity pegasus;
    private final int chestSize;

    public mHorsesMenu(int containerId, Inventory playerInventory,
                       Container horseContainer, AbstractMillionHorseEntity horse) {
        super(MenuRegistry.PEGASUS_MENU.get(), containerId);
        this.horseContainer = horseContainer;
        this.pegasus = horse;
        this.chestSize = horse != null && horse.hasChest() ? horse.getChestSize() : 0;
        horseContainer.startOpen(playerInventory.player);
        buildSlots(playerInventory);
    }

    public mHorsesMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        super(MenuRegistry.PEGASUS_MENU.get(), containerId);
        int entityId  = buf.readInt();
        boolean chest = buf.readBoolean();
        int chestSz   = buf.readInt();

        net.minecraft.world.entity.Entity e =
                playerInventory.player.level().getEntity(entityId);
        AbstractMillionHorseEntity found = e instanceof AbstractMillionHorseEntity h ? h : null;
        this.pegasus   = found;
        this.chestSize = chest ? chestSz : 0;

        int expectedSize = 3 + this.chestSize;
        if (found != null) {
            SimpleContainer real = found.getHorseInventory();
            this.horseContainer = real.getContainerSize() == expectedSize
                    ? real : new SimpleContainer(expectedSize);
        } else {
            this.horseContainer = new SimpleContainer(expectedSize);
        }

        horseContainer.startOpen(playerInventory.player);
        buildSlots(playerInventory);
    }

    public int getChestColumns() {
        if (chestSize <= 9)  return 3;
        if (chestSize <= 12) return 4;
        return 5;
    }

    private void buildSlots(Inventory playerInventory) {
        this.addSlot(new Slot(horseContainer, 0, 8, 18) {
            @Override public boolean mayPlace(ItemStack s) { return s.is(Items.SADDLE); }
            @Override public int getMaxStackSize() { return 1; }
        });
        this.addSlot(new Slot(horseContainer, 1, 8, 36) {
            @Override public boolean mayPlace(ItemStack s) { return s.getItem() instanceof HorseArmorItem; }
            @Override public int getMaxStackSize() { return 1; }
        });
        this.addSlot(new Slot(horseContainer, 2, 8, 54) {
            @Override public boolean mayPlace(ItemStack s) { return HorseCarpetLayer.isCarpet(s); }
            @Override public int getMaxStackSize() { return 1; }
        });

        if (chestSize > 0) {
            int cols = getChestColumns();
            int rows = (int) Math.ceil((double) chestSize / cols);
            for (int row = 0; row < rows; row++) {
                for (int col = 0; col < cols; col++) {
                    int slot = 3 + col + row * cols;
                    if (slot >= 3 + chestSize) break;
                    this.addSlot(new Slot(horseContainer, slot,
                            80 + col * 18, 18 + row * 18));
                }
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9,
                        8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (clickType == ClickType.QUICK_CRAFT
                && (button & 3) == 2
                && (slotId < 0 || slotId >= this.slots.size())) {
            this.resetQuickCraft();
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public boolean stillValid(Player player) {
        return pegasus != null && pegasus.isAlive()
                && pegasus.distanceTo(player) < 8.0F;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) return result;

        ItemStack stack = slot.getItem();
        result = stack.copy();

        int horseSlots  = horseContainer.getContainerSize();
        int playerStart = horseSlots;
        int playerEnd   = playerStart + 36;

        if (index < horseSlots) {
            if (!this.moveItemStackTo(stack, playerStart, playerEnd, true))
                return ItemStack.EMPTY;
        } else {
            if (stack.is(Items.SADDLE)) {
                if (!this.moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
            } else if (stack.getItem() instanceof HorseArmorItem) {
                if (!this.moveItemStackTo(stack, 1, 2, false)) return ItemStack.EMPTY;
            } else if (HorseCarpetLayer.isCarpet(stack)) {
                if (!this.moveItemStackTo(stack, 2, 3, false)) return ItemStack.EMPTY;
            } else if (horseSlots > 3) {
                if (!this.moveItemStackTo(stack, 3, horseSlots, false)) return ItemStack.EMPTY;
            } else {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return result;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        horseContainer.stopOpen(player);
    }

    public AbstractMillionHorseEntity getPegasus() { return pegasus; }
    public int getChestSize() { return chestSize; }
}