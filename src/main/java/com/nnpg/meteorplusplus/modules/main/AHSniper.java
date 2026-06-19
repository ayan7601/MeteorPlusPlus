package com.nnpg.meteorplusplus.modules.main;

import com.nnpg.meteorplusplus.MeteorPlusPlusAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AHSniper extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> searchItem = sgGeneral.add(new StringSetting.Builder()
        .name("item-name")
        .description("Item name to look for in the auction house.")
        .defaultValue("diamond")
        .build()
    );

    private final Setting<String> maxPrice = sgGeneral.add(new StringSetting.Builder()
        .name("max-price")
        .description("Maximum price to pay. Supports K/M/B suffixes.")
        .defaultValue("100k")
        .build()
    );

    private final Setting<Integer> actionDelay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("Ticks to wait between GUI actions.")
        .defaultValue(4)
        .min(0)
        .max(20)
        .sliderMax(20)
        .build()
    );

    private final Setting<Boolean> notifications = sgGeneral.add(new BoolSetting.Builder()
        .name("notifications")
        .description("Show chat feedback.")
        .defaultValue(true)
        .build()
    );

    private enum Stage {
        OPEN_AH,
        WAIT_AH_GUI,
        SCAN_LISTINGS,
        WAIT_CONFIRM,
        COOLDOWN
    }

    private Stage stage = Stage.OPEN_AH;
    private int stageTicks = 0;
    private int cooldownTicks = 0;

    public AHSniper() {
        super(MeteorPlusPlusAddon.CATEGORY, "ah-sniper", "Snipes cheap items from the auction house.");
    }

    @Override
    public void onActivate() {
        if (parsePrice(maxPrice.get()) <= 0) {
            if (notifications.get()) error("Invalid max-price format: " + maxPrice.get());
            toggle();
            return;
        }

        stage = Stage.OPEN_AH;
        stageTicks = 0;
        cooldownTicks = 0;
    }

    @Override
    public void onDeactivate() {
        stage = Stage.OPEN_AH;
        stageTicks = 0;
        cooldownTicks = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;

        stageTicks++;

        switch (stage) {
            case OPEN_AH -> {
                ChatUtils.sendPlayerMsg("/ah");
                stage = Stage.WAIT_AH_GUI;
                stageTicks = 0;
            }

            case WAIT_AH_GUI -> {
                if (mc.currentScreen instanceof GenericContainerScreen) {
                    stage = Stage.SCAN_LISTINGS;
                    stageTicks = 0;
                } else if (stageTicks > 40) {
                    stage = Stage.OPEN_AH;
                    stageTicks = 0;
                }
            }

            case SCAN_LISTINGS -> {
                if (!(mc.currentScreen instanceof GenericContainerScreen screen)) {
                    stage = Stage.OPEN_AH;
                    stageTicks = 0;
                    return;
                }

                ListingMatch match = findMatchingListing(screen.getScreenHandler());
                if (match == null) {
                    if (stageTicks > 20) {
                        if (notifications.get()) info("No matching listings found. Reopening auction house.");
                        closeScreen();
                        stage = Stage.COOLDOWN;
                        cooldownTicks = Math.max(10, actionDelay.get());
                        stageTicks = 0;
                    }
                    return;
                }

                if (notifications.get()) {
                    info("Buying %s for %s.", match.stack.getName().getString(), formatPrice(match.price));
                }

                mc.interactionManager.clickSlot(
                    screen.getScreenHandler().syncId,
                    match.slot.id,
                    0,
                    SlotActionType.PICKUP,
                    mc.player
                );

                stage = Stage.WAIT_CONFIRM;
                stageTicks = 0;
            }

            case WAIT_CONFIRM -> {
                if (!(mc.currentScreen instanceof GenericContainerScreen screen)) {
                    stage = Stage.COOLDOWN;
                    cooldownTicks = Math.max(10, actionDelay.get());
                    stageTicks = 0;
                    return;
                }

                int confirmSlot = findConfirmSlot(screen.getScreenHandler());
                if (confirmSlot >= 0) {
                    mc.interactionManager.clickSlot(
                        screen.getScreenHandler().syncId,
                        confirmSlot,
                        0,
                        SlotActionType.PICKUP,
                        mc.player
                    );
                    closeScreen();
                    stage = Stage.COOLDOWN;
                    cooldownTicks = Math.max(10, actionDelay.get());
                    stageTicks = 0;
                } else if (stageTicks > 40) {
                    if (notifications.get()) warning("No confirm button found. Reopening auction house.");
                    closeScreen();
                    stage = Stage.COOLDOWN;
                    cooldownTicks = Math.max(10, actionDelay.get());
                    stageTicks = 0;
                }
            }

            case COOLDOWN -> {
                if (cooldownTicks > 0) {
                    cooldownTicks--;
                    return;
                }

                stage = Stage.OPEN_AH;
                stageTicks = 0;
            }
        }
    }

    private ListingMatch findMatchingListing(ScreenHandler handler) {
        String needle = searchItem.get().trim().toLowerCase(Locale.ROOT);
        double limit = parsePrice(maxPrice.get());

        for (Slot slot : handler.slots) {
            if (slot.inventory == mc.player.getInventory()) continue;

            ItemStack stack = slot.getStack();
            if (stack.isEmpty()) continue;
            if (!matchesSearch(stack, needle)) continue;

            double price = getListingPrice(stack);
            if (price <= 0 || price > limit) continue;

            return new ListingMatch(slot, stack, price);
        }

        return null;
    }

    private boolean matchesSearch(ItemStack stack, String needle) {
        if (needle.isEmpty()) return true;

        String itemName = stack.getName().getString().toLowerCase(Locale.ROOT);
        String registryName = stack.getItem().getTranslationKey().toLowerCase(Locale.ROOT);

        return itemName.contains(needle)
            || registryName.contains(needle)
            || registryName.replace(".", " ").contains(needle)
            || registryName.replace("_", " ").contains(needle);
    }

    private double getListingPrice(ItemStack stack) {
        Item.TooltipContext context = Item.TooltipContext.create(mc.world);
        List<Text> tooltip = stack.getTooltip(context, mc.player, TooltipType.BASIC);
        return parseTooltipPrice(tooltip);
    }

    private double parseTooltipPrice(List<Text> tooltip) {
        Pattern[] patterns = {
            Pattern.compile("\\$([\\d,]+(?:\\.\\d+)?)([kmbKMB])?", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)price\\s*:?\\s*([\\d,]+(?:\\.\\d+)?)([kmbKMB])?", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)buy\\s*for\\s*:?\\s*([\\d,]+(?:\\.\\d+)?)([kmbKMB])?", Pattern.CASE_INSENSITIVE),
            Pattern.compile("([\\d,]+(?:\\.\\d+)?)([kmbKMB])?\\s*coins?", Pattern.CASE_INSENSITIVE)
        };

        for (Text line : tooltip) {
            String text = line.getString();
            for (Pattern pattern : patterns) {
                Matcher matcher = pattern.matcher(text);
                if (!matcher.find()) continue;

                try {
                    double value = Double.parseDouble(matcher.group(1).replace(",", ""));
                    String suffix = matcher.groupCount() >= 2 && matcher.group(2) != null
                        ? matcher.group(2).toLowerCase(Locale.ROOT)
                        : "";

                    return switch (suffix) {
                        case "k" -> value * 1_000.0;
                        case "m" -> value * 1_000_000.0;
                        case "b" -> value * 1_000_000_000.0;
                        default -> value;
                    };
                } catch (NumberFormatException ignored) {
                }
            }
        }

        return -1.0;
    }

    private int findConfirmSlot(ScreenHandler handler) {
        for (int i = 0; i < handler.slots.size(); i++) {
            ItemStack stack = handler.getSlot(i).getStack();
            if (stack.isEmpty()) continue;
            String name = stack.getName().getString().toLowerCase(Locale.ROOT);
            if (name.contains("confirm") || name.contains("buy") || isGreenGlass(stack)) {
                return i;
            }
        }
        return -1;
    }

    private boolean isGreenGlass(ItemStack stack) {
        String name = stack.getName().getString().toLowerCase(Locale.ROOT);
        return name.contains("green stained glass pane") || name.contains("lime stained glass pane");
    }

    private void closeScreen() {
        if (mc.player != null && mc.currentScreen != null) {
            mc.player.closeHandledScreen();
        }
    }

    private double parsePrice(String priceStr) {
        if (priceStr == null || priceStr.isBlank()) return -1.0;

        try {
            String cleaned = priceStr.trim().toLowerCase(Locale.ROOT).replace(",", "");
            double multiplier = 1.0;

            if (cleaned.endsWith("b")) {
                multiplier = 1_000_000_000.0;
                cleaned = cleaned.substring(0, cleaned.length() - 1);
            } else if (cleaned.endsWith("m")) {
                multiplier = 1_000_000.0;
                cleaned = cleaned.substring(0, cleaned.length() - 1);
            } else if (cleaned.endsWith("k")) {
                multiplier = 1_000.0;
                cleaned = cleaned.substring(0, cleaned.length() - 1);
            }

            return Double.parseDouble(cleaned) * multiplier;
        } catch (Exception e) {
            return -1.0;
        }
    }

    private String formatPrice(double price) {
        if (price >= 1_000_000_000) return String.format("%.2fB", price / 1_000_000_000.0);
        if (price >= 1_000_000) return String.format("%.2fM", price / 1_000_000.0);
        if (price >= 1_000) return String.format("%.2fK", price / 1_000.0);
        return String.format("%.2f", price);
    }

    @Override
    public String getInfoString() {
        if (!isActive()) return null;
        return searchItem.get() + " -> " + formatPrice(parsePrice(maxPrice.get()));
    }

    private static class ListingMatch {
        private final Slot slot;
        private final ItemStack stack;
        private final double price;

        private ListingMatch(Slot slot, ItemStack stack, double price) {
            this.slot = slot;
            this.stack = stack;
            this.price = price;
        }
    }
}