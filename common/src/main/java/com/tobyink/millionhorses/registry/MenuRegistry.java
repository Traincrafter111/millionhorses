package com.tobyink.millionhorses.registry;

import com.tobyink.millionhorses.MillionHorsesMod;
import com.tobyink.millionhorses.entity.menu.mHorsesMenu;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;

public class MenuRegistry {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(MillionHorsesMod.MOD_ID, Registries.MENU);

    public static final RegistrySupplier<MenuType<mHorsesMenu>> PEGASUS_MENU =
            MENUS.register("pegasus_menu", () ->
                    dev.architectury.registry.menu.MenuRegistry.ofExtended(
                            (id, inv, buf) -> new mHorsesMenu(id, inv, buf)));

    public static void init() {
        MENUS.register();
    }
}