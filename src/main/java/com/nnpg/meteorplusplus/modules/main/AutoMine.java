package com.nnpg.meteorplusplus.modules.main;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.process.IMineProcess;
import com.nnpg.meteorplusplus.MeteorPlusPlusAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BlockListSetting;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.ItemListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

import java.util.List;

public class AutoMine extends Module {
    private static final int INVENTORY_START = 0;
    private static final int INVENTORY_END = 35;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgAutoDrop = settings.createGroup("Auto Drop");

    private final Setting<List<Block>> blocks = sgGeneral.add(new BlockListSetting.Builder()
        .name("blocks")
        .description("Blocks Baritone should search for and mine.")
        .defaultValue(List.of(Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE))
        .onChanged(value -> restartIfActive())
        .build()
    );

    private final Setting<Boolean> autoDropWhenFull = sgAutoDrop.add(new BoolSetting.Builder()
        .name("auto-drop-when-full")
        .description("Drop selected items when the inventory is full.")
        .defaultValue(false)
        .build()
    );

    private final Setting<List<Item>> droppableItems = sgAutoDrop.add(new ItemListSetting.Builder()
        .name("droppable-items")
        .description("Items AutoMine is allowed to drop when the inventory is full.")
        .defaultValue(List.of(Items.COBBLESTONE, Items.COBBLED_DEEPSLATE, Items.DEEPSLATE, Items.NETHERRACK))
        .visible(autoDropWhenFull::get)
        .build()
    );

    private final Setting<Integer> dropDelay = sgAutoDrop.add(new IntSetting.Builder()
        .name("drop-delay")
        .description("Delay in ticks between dropping matching item stacks.")
        .defaultValue(5)
        .min(1)
        .max(40)
        .sliderMax(40)
        .visible(autoDropWhenFull::get)
        .build()
    );

    private final Setting<Boolean> notifications = sgGeneral.add(new BoolSetting.Builder()
        .name("notifications")
        .description("Show chat feedback.")
        .defaultValue(true)
        .build()
    );

    private int dropDelayTimer;

    public AutoMine() {
        super(MeteorPlusPlusAddon.CATEGORY, "auto-mine", "Uses Baritone to mine the selected blocks.");
    }

    @Override
    public void onActivate() {
        dropDelayTimer = 0;

        if (mc.player == null || mc.world == null) {
            if (notifications.get()) error("Cannot start AutoMine while not in a world.");
            toggle();
            return;
        }

        startMining();
    }

    @Override
    public void onDeactivate() {
        dropDelayTimer = 0;
        getMineProcess().cancel();
        if (notifications.get()) info("Stopped Baritone mining.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!autoDropWhenFull.get() || mc.player == null || mc.interactionManager == null) return;
        if (mc.currentScreen != null || mc.player.currentScreenHandler != mc.player.playerScreenHandler) return;

        if (dropDelayTimer > 0) {
            dropDelayTimer--;
            return;
        }

        if (!isInventoryFull()) return;

        int slot = findDroppableSlot();
        if (slot == -1) return;

        mc.interactionManager.clickSlot(
            mc.player.playerScreenHandler.syncId,
            inventorySlotToScreenSlot(slot),
            1,
            SlotActionType.THROW,
            mc.player
        );

        dropDelayTimer = dropDelay.get();
    }

    private void restartIfActive() {
        if (!isActive() || mc.player == null || mc.world == null) return;

        IMineProcess mineProcess = getMineProcess();
        mineProcess.cancel();
        startMining();
    }

    private void startMining() {
        List<Block> selectedBlocks = blocks.get();

        if (selectedBlocks.isEmpty()) {
            if (notifications.get()) error("Select at least one block to mine.");
            toggle();
            return;
        }

        getMineProcess().mine(selectedBlocks.toArray(Block[]::new));

        if (notifications.get()) {
            info("Mining selected blocks with Baritone.");
        }
    }

    private IMineProcess getMineProcess() {
        IBaritone baritone = BaritoneAPI.getProvider().getBaritoneForPlayer(mc.player);
        if (baritone == null) baritone = BaritoneAPI.getProvider().getPrimaryBaritone();
        return baritone.getMineProcess();
    }

    private boolean isInventoryFull() {
        for (int i = INVENTORY_START; i <= INVENTORY_END; i++) {
            if (mc.player.getInventory().getStack(i).isEmpty()) return false;
        }

        return true;
    }

    private int findDroppableSlot() {
        List<Item> allowedItems = droppableItems.get();
        if (allowedItems.isEmpty()) return -1;

        for (int i = INVENTORY_START; i <= INVENTORY_END; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && allowedItems.contains(stack.getItem())) return i;
        }

        return -1;
    }

    private int inventorySlotToScreenSlot(int inventorySlot) {
        return inventorySlot < 9 ? 36 + inventorySlot : inventorySlot;
    }
}
