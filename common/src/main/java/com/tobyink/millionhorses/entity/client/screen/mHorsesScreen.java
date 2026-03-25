package com.tobyink.millionhorses.entity.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.tobyink.millionhorses.entity.menu.mHorsesMenu;
import com.tobyink.millionhorses.entity.mobs.AbstractMillionHorseEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import org.joml.Quaternionf;

public class mHorsesScreen extends AbstractContainerScreen<mHorsesMenu> {

    private static final ResourceLocation GUI_TEXTURE =
            new ResourceLocation("millionhorses", "textures/gui/container/horse_gui.png");

    private static final int GUI_W = 176;
    private static final int GUI_H = 166;

    // Fondo de slot genérico (sin icono) en la textura — u=7, v=83, 18x18
    // Se usa para que el slot se vea correctamente cuando hay un item equipado
    private static final int SLOT_BG_U = 7;
    private static final int SLOT_BG_V = 83;

    // Iconos de slots vacíos en la textura — zona v=220, cada 18px
    // El icono interior (sin el borde del slot) es 16x16 desde u+1, v+1
    //   Silla:    u=1,  v=221
    //   Armadura: u=19, v=221
    //   Carpet:   u=37, v=221
    private static final int ICON_V = 221;
    private static final int[] ICON_U = { 19, 1, 37 }; // armadura(slot0-silla), silla(slot1-armor), carpet(slot2)

    public mHorsesScreen(mHorsesMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth  = GUI_W;
        this.imageHeight = GUI_H;
        this.inventoryLabelY = 74;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int x = this.leftPos;
        int y = this.topPos;

        // ── Fondo principal 176x166 ──────────────────────────────────────
        graphics.blit(GUI_TEXTURE, x, y, 0, 0, GUI_W, GUI_H);

        AbstractMillionHorseEntity pegasus = this.menu.getPegasus();

        // ── Slots del cofre: blit grilla según columnas reales ───────────
        if (pegasus != null && pegasus.hasChest()) {
            int cols  = this.menu.getChestColumns();
            int gridW = cols * 18;   // 3 cols → 54px, 5 cols → 90px
            int gridH = 54;          // siempre 3 filas
            graphics.blit(GUI_TEXTURE, x + 79, y + 17, 0, 166, gridW, gridH);
        }

        // ── Slots de equipo (silla, armadura, carpet) ────────────────────
        for (int i = 0; i < 3; i++) {
            Slot slot = this.menu.slots.get(i);
            int sx = x + slot.x - 1; // -1 para incluir el borde del slot
            int sy = y + slot.y - 1;

            if (slot.hasItem()) {
                // Cuando hay item: dibujar fondo limpio de slot (sin icono)
                // para que no se vea el icono debajo del item
                graphics.blit(GUI_TEXTURE, sx, sy, SLOT_BG_U, SLOT_BG_V, 18, 18);
            } else {
                // Cuando está vacío: dibujar el icono del tipo de slot
                // El icono completo (18x18 con borde) está en ICON_U[i]-1, ICON_V-1
                // Pero para evitar el doble borde, usamos el interior 16x16
                // y lo centramos dentro del slot
                graphics.blit(GUI_TEXTURE, sx, sy, SLOT_BG_U, SLOT_BG_V, 18, 18);
                // Icono interior 16x16 encima del fondo
                graphics.blit(GUI_TEXTURE, sx + 1, sy + 1, ICON_U[i], ICON_V, 16, 16);
            }
        }

        // ── Modelo del pegaso mirando al mouse ───────────────────────────
        if (pegasus != null) {
            int entityX = x + 51;
            int entityY = y + 66;

            // Guardamos y sobreescribimos yaw/pitch para congelar la orientación
            // evitando que el modelo siga la rotación real de la entidad en el mundo
            float savedYaw      = pegasus.yBodyRot;
            float savedHeadYaw  = pegasus.getYHeadRot();
            float savedPitch    = pegasus.getXRot();
            float savedOldYaw   = pegasus.yBodyRotO;

            // Orientar mirando hacia el jugador (sur = 180°) y sin pitch
            pegasus.yBodyRot    = 180.0F;
            pegasus.yBodyRotO   = 180.0F;
            pegasus.setYHeadRot(180.0F);
            pegasus.setXRot(0.0F);

            // Rotación de cámara: flip Z para que no esté al revés,
            // rotar Y según posición del mouse para que siga el cursor
            Quaternionf pose = new Quaternionf().rotateZ((float) Math.PI);
            float yaw = (float) Math.atan((mouseX - entityX) / 40.0F); // negado para espejo
            pose.rotateY(yaw);

            // Firma 1.20.1: (GuiGraphics, x, y, scale, Quaternionf, @Nullable Quaternionf, LivingEntity)
            InventoryScreen.renderEntityInInventory(
                    graphics, entityX, entityY, 17, pose, null, pegasus);

            // Restaurar rotación original para no afectar al pegaso en el mundo
            pegasus.yBodyRot    = savedYaw;
            pegasus.yBodyRotO   = savedOldYaw;
            pegasus.setYHeadRot(savedHeadYaw);
            pegasus.setXRot(savedPitch);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, 8, 6, 0x404040, false);
        graphics.drawString(this.font, this.playerInventoryTitle, 8, this.inventoryLabelY, 0x404040, false);
    }
}