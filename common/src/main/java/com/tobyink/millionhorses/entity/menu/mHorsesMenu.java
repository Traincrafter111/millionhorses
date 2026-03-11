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
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.HorseArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class mHorsesMenu extends AbstractContainerMenu {

    private final Container horseContainer;
    private final AbstractMillionHorseEntity pegasus;

    // Coordenadas basadas en medición exacta de horse_gui.png (256x256):
    //
    // Slots equipo (columna izquierda):
    //   Silla    slot 0: x=8,  y=18
    //   Armadura slot 1: x=8,  y=36
    //   Carpet   slot 2: x=8,  y=54
    //
    // Slots cofre (5 col x 3 fil, zona derecha del panel superior):
    //   Fila 0: x=80+col*18, y=18
    //   Fila 1: x=80+col*18, y=36
    //   Fila 2: x=80+col*18, y=54
    //
    // Inventario jugador:
    //   3 filas: x=8+col*18, y=84+row*18
    //   Hotbar:  x=8+col*18, y=142

    public mHorsesMenu(int containerId, Inventory playerInventory,
                       Container horseContainer, AbstractMillionHorseEntity pegasus) {
        super(MenuRegistry.PEGASUS_MENU.get(), containerId);
        this.horseContainer = horseContainer;
        this.pegasus = pegasus;
        horseContainer.startOpen(playerInventory.player);
        buildSlots(playerInventory, pegasus != null && pegasus.hasChest());
    }

    // Constructor cliente — lee entityId y hasChest del buffer
    public mHorsesMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        super(MenuRegistry.PEGASUS_MENU.get(), containerId);
        int entityId  = buf.readInt();
        boolean chest = buf.readBoolean(); // fuente de verdad para el tamaño

        net.minecraft.world.entity.Entity e =
                playerInventory.player.level().getEntity(entityId);
        AbstractMillionHorseEntity found = e instanceof AbstractMillionHorseEntity h ? h : null;
        this.pegasus = found;

        // IMPORTANTE: el tamaño del container SIEMPRE viene del buffer (chest).
        // No usar found.hasChest() porque puede estar desincronizado en el cliente.
        // El servidor ya calculó el tamaño correcto y lo envió en el buffer.
        if (found != null) {
            // Tenemos la entidad — usar su container real para que los items se vean
            // pero solo si el tamaño coincide con lo que dijo el servidor
            SimpleContainer real = found.getHorseInventory();
            int expectedSize = 3 + (chest ? 15 : 0);
            if (real.getContainerSize() == expectedSize) {
                this.horseContainer = real;
            } else {
                // Tamaño no coincide — usar fallback del tamaño correcto
                this.horseContainer = new SimpleContainer(expectedSize);
            }
        } else {
            this.horseContainer = new SimpleContainer(3 + (chest ? 15 : 0));
        }

        horseContainer.startOpen(playerInventory.player);
        buildSlots(playerInventory, chest); // chest del buffer, no de la entidad
    }

    private void buildSlots(Inventory playerInventory, boolean hasChest) {
        // ── Slots de equipamiento ────────────────────────────────────────
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

        // ── Slots del cofre: 3 filas × 5 columnas ───────────────────────
        if (hasChest) {
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 5; col++) {
                    this.addSlot(new Slot(horseContainer, 3 + col + row * 5,
                            80 + col * 18, 18 + row * 18));
                }
            }
        }

        // ── Inventario del jugador ───────────────────────────────────────
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
    public void clicked(int slotId, int button, net.minecraft.world.inventory.ClickType clickType,
                        Player player) {
        // QUICK_CRAFT: button & 3 da la fase del drag (0=inicio, 1=añadir slot, 2=fin)
        // Cuando button=2 (fin del drag) con slotId inválido, cancelar para evitar crash
        if (clickType == net.minecraft.world.inventory.ClickType.QUICK_CRAFT
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

        // Usar tamaño real del container (no depender de pegasus != null)
        int horseSlots = horseContainer.getContainerSize();
        int playerStart = horseSlots;
        int playerEnd = playerStart + 36;

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
}