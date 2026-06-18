package com.nnpg.meteorplusplus;

import com.nnpg.meteorplusplus.utils.InventoryUtils;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;

public class VersionUtil {

    public static ItemStack getArmorStack(ClientPlayerEntity player, int slot) {
    return player.getInventory().getStack(slot);
    }

    public static ItemStack getArmorStackByType(ClientPlayerEntity player, int armorType) {
    return player.getInventory().getStack(armorType);
    }

    public static int getSelectedSlot(ClientPlayerEntity player) {
    return InventoryUtils.getSelectedSlot(player.getInventory());
    }

    public static void setSelectedSlot(ClientPlayerEntity player, int slot) {
    player.getInventory().setSelectedSlot(slot);
    }

    public static double getPrevX(net.minecraft.entity.Entity entity) {
    return entity.lastRenderX;
    }

    public static double getPrevY(net.minecraft.entity.Entity entity) {
    return entity.lastRenderY;
    }

    public static double getPrevZ(net.minecraft.entity.Entity entity) {
    return entity.lastRenderZ;
    }

    public static DefaultedList<ItemStack> getMainInventory(ClientPlayerEntity player) {
    DefaultedList<ItemStack> items = DefaultedList.ofSize(player.getInventory().size(), ItemStack.EMPTY);
    for (int slot = 0; slot < player.getInventory().size(); slot++) {
        items.set(slot, player.getInventory().getStack(slot));
    }
    return items;
    }
}
