package etherested.spoilage.client;

import etherested.spoilage.FreshnessLevel;
import etherested.spoilage.Spoilage;
import etherested.spoilage.component.SpoilageData;
import etherested.spoilage.config.SpoilageConfig;
import etherested.spoilage.data.SpoilageGroupData;
import etherested.spoilage.logic.SpoilageCalculator;
import net.minecraft.ChatFormatting;
import etherested.spoilage.logic.preservation.PreservationManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * adds spoilage information to item tooltips;
 * each tooltip element can be toggled via config
 */
@SuppressWarnings("removal")
@EventBusSubscriber(modid = Spoilage.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class SpoilageTooltipHandler {

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        if (!SpoilageConfig.isEnabled()) {
            return;
        }

        // skip tooltip in recipe viewers (EMI/JEI/REI)
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) {
            String screenClassName = mc.screen.getClass().getName().toLowerCase();
            if (screenClassName.contains("emi") ||
                screenClassName.contains("jei") ||
                screenClassName.contains("rei") ||
                screenClassName.contains("recipeviewer")) {
                return;
            }
        }

        // skip tooltip when recipe viewer sidebars have a hovered stack
        // (sidebars render as overlays while mc.screen remains the container screen)
        if (isRecipeViewerStackHovered()) {
            return;
        }

        // check if any tooltip elements are enabled
        if (!SpoilageConfig.showFreshnessWord() && !SpoilageConfig.showFreshnessPercentage() && !SpoilageConfig.showRemainingTime()) {
            return;
        }

        ItemStack stack = event.getItemStack();
        if (!SpoilageCalculator.isSpoilable(stack)) {
            return;
        }

        // check if this item's group allows tooltip display
        SpoilageGroupData groupData = SpoilageCalculator.getGroupData(stack);
        if (groupData != null && !groupData.showTooltip()) {
            return;
        }

        // only show tooltip for items that have been initialized with spoilage data
        // this prevents showing spoilage info for creative inventory template items
        SpoilageData data = SpoilageCalculator.getInitializedData(stack);
        if (data == null) {
            return;
        }

        if (mc.level == null) {
            return;
        }

        long worldTime = mc.level.getGameTime();
        float spoilage = SpoilageCalculator.getSpoilagePercent(stack, worldTime);

        // calculate remaining ticks with real-time preservation context
        long remainingTicks = calculateRemainingTicksRealtime(stack, data, worldTime);

        // add blank line before spoilage info
        event.getToolTip().add(Component.empty());

        // add freshness line (word and/or percentage based on config)
        if (SpoilageConfig.showFreshnessWord() || SpoilageConfig.showFreshnessPercentage()) {
            Component freshnessLine = buildFreshnessComponent(spoilage);
            if (freshnessLine != null) {
                event.getToolTip().add(freshnessLine);
            }
        }

        // add time remaining (expiration) before preservation
        if (SpoilageConfig.showRemainingTime() && remainingTicks > 0 && remainingTicks < Long.MAX_VALUE) {
            event.getToolTip().add(Component.empty());
            Component timeLine = getTimeRemainingComponent(remainingTicks);
            event.getToolTip().add(timeLine);
        }

        // add preservation info last, show all active preservation bonuses/penalties
        addPreservationTooltips(event, data, worldTime);
    }

    /**
     * calculates remaining ticks with real-time preservation context;
     * unlike SpoilageCalculator.getRemainingTicksForDisplay(),
     * this checks the current container context rather than relying on cached preservation data
     */
    private static long calculateRemainingTicksRealtime(ItemStack stack, SpoilageData data, long worldTime) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return SpoilageCalculator.getRemainingTicksForDisplay(stack, worldTime);
        }

        if (data.isPaused()) {
            return data.remainingLifetime();
        }

        long lifetime = SpoilageCalculator.getLifetime(stack);
        long elapsed = (long) ((worldTime - data.creationTime()) * data.preservationMultiplier() * SpoilageConfig.getGlobalSpeedMultiplier());

        // use already accumulated savings
        long totalSavings = data.yLevelSavedTicks();

        // calculate remaining lifetime ticks
        long effectiveElapsed = elapsed - totalSavings;
        long remainingTicks = Math.max(0, lifetime - effectiveElapsed);

        // check if item is currently in a container (real-time check, not cached)
        if (isItemInOpenContainer(stack)) {
            BlockPos containerPos = getOpenContainerPos();
            if (containerPos != null) {
                // calculate real-time preservation multiplier
                PreservationManager.PreservationInfo info = PreservationManager.getPreservationInfo(mc.level, containerPos);
                float combinedMultiplier = info.getCombinedMultiplier();

                if (combinedMultiplier < 1.0f) {
                    // show real wall-clock time until spoiled at current preservation rate
                    remainingTicks = (long) (remainingTicks / combinedMultiplier);
                }
            }
        }
        // if not in container, show base remaining time (no preservation bonus applied)

        return remainingTicks;
    }

    /**
     * adds preservation bonus/penalty tooltips;
     * calculates preservation in real-time for items in containers to provide instant tooltip updates
     */
    private static void addPreservationTooltips(ItemTooltipEvent event, SpoilageData data, long worldTime) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        // don't show preservation info for fully spoiled (inedible) items
        float spoilage = SpoilageCalculator.getSpoilagePercent(event.getItemStack(), worldTime);
        if (spoilage >= 1.0f) {
            return; // item is inedible, preservation is pointless
        }

        java.util.List<Component> preservationLines = new java.util.ArrayList<>();

        // check if item is currently in a container (not carried by cursor or in player inventory screen)
        // calculate y-level, biome, and container bonuses in real-time for instant updates
        if (isItemInOpenContainer(event.getItemStack())) {
            BlockPos containerPos = getOpenContainerPos();
            if (containerPos != null) {
                // get the block entity for container-specific multipliers
                BlockEntity blockEntity = mc.level.getBlockEntity(containerPos);

                // calculate preservation in real-time including container bonus
                PreservationManager.PreservationInfo info = PreservationManager.getContainerPreservationInfo(mc.level, containerPos, blockEntity);

                // container preservation
                if (info.hasContainerBonus()) {
                    int containerPercent = info.getContainerBonusPercent();
                    preservationLines.add(Component.translatable("tooltip.spoilage.container_preservation", containerPercent)
                            .withStyle(ChatFormatting.BLUE));
                }

                // y-level preservation
                if (info.hasYLevelBonus()) {
                    int depthPercent = info.getYLevelBonusPercent();
                    preservationLines.add(Component.translatable("tooltip.spoilage.depth_preservation", depthPercent)
                            .withStyle(ChatFormatting.BLUE));
                }

                // biome temperature preservation
                if (info.hasBiomeBonus()) {
                    int biomePercent = info.getBiomeBonusPercent();
                    preservationLines.add(Component.translatable("tooltip.spoilage.cold_biome", biomePercent)
                            .withStyle(ChatFormatting.BLUE));
                } else if (info.hasBiomePenalty()) {
                    int biomePercent = -info.getBiomeBonusPercent(); // getBiomeBonusPercent returns negative for penalty
                    preservationLines.add(Component.translatable("tooltip.spoilage.hot_biome", biomePercent)
                            .withStyle(ChatFormatting.RED));
                }
            }
        }

        // add preservation section if there are any effects
        if (!preservationLines.isEmpty()) {
            event.getToolTip().add(Component.empty());
            event.getToolTip().add(Component.translatable("tooltip.spoilage.preservation.header")
                    .withStyle(ChatFormatting.GRAY));
            preservationLines.forEach(event.getToolTip()::add);
        }
    }

    /**
     * checks if the item being hovered is in an open container's slots;
     * (not player inventory slots and not being carried by cursor)
     */
    private static boolean isItemInOpenContainer(ItemStack stack) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen == null) return false;

        // must be a container screen but not the player inventory
        if (!(mc.screen instanceof AbstractContainerScreen<?> containerScreen)) return false;
        if (containerScreen instanceof InventoryScreen) return false;

        AbstractContainerMenu menu = containerScreen.getMenu();

        // if the item is being carried by cursor, it's not "in" the container
        if (!menu.getCarried().isEmpty() && ItemStack.isSameItemSameComponents(menu.getCarried(), stack)) {
            return false;
        }

        // find which slot contains this exact item stack
        // container slots come before player inventory slots in the menu
        // we need to check if the item is in a container slot, not a player inventory slot
        int containerSlotCount = getContainerSlotCount(menu);
        if (containerSlotCount <= 0) return false;

        for (int i = 0; i < containerSlotCount; i++) {
            if (i < menu.slots.size()) {
                ItemStack slotStack = menu.slots.get(i).getItem();
                // check if this is the exact same stack instance (reference equality)
                // or if it matches by item and components (for when tooltip checks by value)
                if (slotStack == stack || (!slotStack.isEmpty() && ItemStack.isSameItemSameComponents(slotStack, stack)
                        && slotStack.getCount() == stack.getCount())) {
                    return true;
                }
            }
        }

        return false;
    }

    /** gets the number of container slots (excluding player inventory) in a menu */
    private static int getContainerSlotCount(AbstractContainerMenu menu) {
        // player inventory takes 36 slots (27 main + 9 hotbar),
        // container slots = total slots - 36
        int totalSlots = menu.slots.size();
        int playerSlots = 36;
        return Math.max(0, totalSlots - playerSlots);
    }

    /**
     * gets the position of the currently open container, if available;
     * works for common container types that store block entity position
     */
    private static BlockPos getOpenContainerPos() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen == null || mc.level == null) return null;

        if (!(mc.screen instanceof AbstractContainerScreen<?> containerScreen)) return null;

        AbstractContainerMenu menu = containerScreen.getMenu();

        // try to get position from common container menu types
        // ChestMenu has a public getContainer() method that returns the underlying Container
        try {
            if (menu instanceof ChestMenu chestMenu) {
                if (chestMenu.getContainer() instanceof BlockEntity be) {
                    return be.getBlockPos();
                }
            }
            // HopperMenu and ShulkerBoxMenu don't expose their container publicly
            // so we rely on the fallback below
        } catch (Exception ignored) {
            // field access might fail for modded containers, fall back to hit result
        }

        // fallback: try to find any block entity container at the player's target position
        // this is a last resort and might not always be accurate
        if (mc.hitResult instanceof BlockHitResult blockHit) {
            BlockEntity be = mc.level.getBlockEntity(blockHit.getBlockPos());
            if (be instanceof net.minecraft.world.Container) {
                return blockHit.getBlockPos();
            }
        }

        return null;
    }

    /**
     * builds the freshness component based on config settings;
     * can show: word only, percentage only, or both
     */
    private static Component buildFreshnessComponent(float spoilage) {
        int freshPercent = Math.round((1.0f - spoilage) * 100);
        freshPercent = Math.max(0, Math.min(100, freshPercent));

        ChatFormatting color = getFreshnessColor(freshPercent);
        String wordKey = getFreshnessWordKey(freshPercent);

        boolean showWord = SpoilageConfig.showFreshnessWord();
        boolean showPercent = SpoilageConfig.showFreshnessPercentage();

        MutableComponent result = Component.empty();

        if (showWord && showPercent) {
            // show both: "Freshness: Fresh (85%)"
            result.append(Component.translatable("tooltip.spoilage.freshness.label").withStyle(ChatFormatting.GRAY));
            result.append(Component.literal(": ").withStyle(ChatFormatting.GRAY));
            result.append(Component.translatable(wordKey).withStyle(color));
            result.append(Component.literal(" (" + freshPercent + "%)").withStyle(ChatFormatting.GRAY));
        } else if (showWord) {
            // word only: "Freshness: Fresh"
            result.append(Component.translatable("tooltip.spoilage.freshness.label").withStyle(ChatFormatting.GRAY));
            result.append(Component.literal(": ").withStyle(ChatFormatting.GRAY));
            result.append(Component.translatable(wordKey).withStyle(color));
        } else if (showPercent) {
            // percentage only: "Freshness: 85%"
            result.append(Component.translatable("tooltip.spoilage.freshness.label").withStyle(ChatFormatting.GRAY));
            result.append(Component.literal(": ").withStyle(ChatFormatting.GRAY));
            result.append(Component.literal(freshPercent + "%").withStyle(color));
        }

        return result;
    }

    private static ChatFormatting getFreshnessColor(int freshPercent) {
        if (freshPercent >= 80) {
            return ChatFormatting.GREEN;
        } else if (freshPercent >= 60) {
            return ChatFormatting.YELLOW;
        } else if (freshPercent >= 40) {
            return ChatFormatting.GOLD;
        } else if (freshPercent >= 20) {
            return ChatFormatting.RED;
        } else if (freshPercent > 0) {
            return ChatFormatting.DARK_RED;
        } else {
            return ChatFormatting.DARK_GRAY;  // inedible (0%)
        }
    }

    private static String getFreshnessWordKey(int freshPercent) {
        return FreshnessLevel.fromFreshnessPercent(freshPercent).getTranslationKey();
    }

    private static Component getTimeRemainingComponent(long ticks) {
        long seconds = ticks / 20;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        String timeStr;
        if (days > 0) {
            timeStr = String.format("%dd %dh", days, hours % 24);
        } else if (hours > 0) {
            timeStr = String.format("%dh %dm", hours, minutes % 60);
        } else if (minutes > 0) {
            timeStr = String.format("%dm %ds", minutes, seconds % 60);
        } else {
            timeStr = String.format("%ds", seconds);
        }

        return Component.translatable("tooltip.spoilage.expiration.label")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(" "))
                .append(Component.literal(timeStr).withStyle(ChatFormatting.DARK_GRAY));
    }

    /**
     * detects if a recipe viewer mod (EMI/JEI/REI) has a hovered stack in its sidebar;
     * uses reflection to avoid hard dependencies on these optional mods
     */
    private static boolean isRecipeViewerStackHovered() {
        // try EMI first
        if (isEmiStackHovered()) {
            return true;
        }

        // try REI
        if (isReiStackHovered()) {
            return true;
        }

        // try JEI
        if (isJeiStackHovered()) {
            return true;
        }

        return false;
    }

    /**
     * EMI: check dev.emi.emi.api.EmiApi.getHoveredStack(),
     * returns EmiStackInteraction, check if getStack() is not empty
     */
    private static boolean isEmiStackHovered() {
        try {
            Class<?> emiApiClass = Class.forName("dev.emi.emi.api.EmiApi");
            java.lang.reflect.Method getHoveredStack = emiApiClass.getMethod("getHoveredStack");
            Object stackInteraction = getHoveredStack.invoke(null);

            if (stackInteraction != null) {
                java.lang.reflect.Method getStack = stackInteraction.getClass().getMethod("getStack");
                Object emiStack = getStack.invoke(stackInteraction);

                if (emiStack != null) {
                    java.lang.reflect.Method isEmpty = emiStack.getClass().getMethod("isEmpty");
                    Object result = isEmpty.invoke(emiStack);
                    if (result instanceof Boolean && !(Boolean) result) {
                        return true;
                    }
                }
            }
        } catch (ClassNotFoundException ignored) {
            // EMI not installed
        } catch (Exception ignored) {
            // API changed or error occurred, fail silently
        }
        return false;
    }

    /**
     * REI: check me.shedaniel.rei.api.client.REIRuntime.getInstance().getOverlay(),
     * then check if mouse is in zone using isInZone(mouseX, mouseY)
     */
    private static boolean isReiStackHovered() {
        try {
            Class<?> reiRuntimeClass = Class.forName("me.shedaniel.rei.api.client.REIRuntime");
            java.lang.reflect.Method getInstance = reiRuntimeClass.getMethod("getInstance");
            Object runtime = getInstance.invoke(null);

            if (runtime != null) {
                java.lang.reflect.Method getOverlay = runtime.getClass().getMethod("getOverlay");
                Object overlayOptional = getOverlay.invoke(runtime);

                if (overlayOptional instanceof java.util.Optional<?> optional && optional.isPresent()) {
                    Object overlay = optional.get();

                    // get mouse position
                    Minecraft mc = Minecraft.getInstance();
                    double mouseX = mc.mouseHandler.xpos() * mc.getWindow().getGuiScaledWidth() / mc.getWindow().getScreenWidth();
                    double mouseY = mc.mouseHandler.ypos() * mc.getWindow().getGuiScaledHeight() / mc.getWindow().getScreenHeight();

                    // try to find isInZone or similar method
                    try {
                        java.lang.reflect.Method isInZone = overlay.getClass().getMethod("isInZone", double.class, double.class);
                        Object result = isInZone.invoke(overlay, mouseX, mouseY);
                        if (result instanceof Boolean && (Boolean) result) {
                            return true;
                        }
                    } catch (NoSuchMethodException e) {
                        // try alternative: getEntryList and check containsMouse
                        try {
                            java.lang.reflect.Method getEntryList = overlay.getClass().getMethod("getEntryList");
                            Object entryList = getEntryList.invoke(overlay);
                            if (entryList != null) {
                                java.lang.reflect.Method containsMouse = entryList.getClass().getMethod("containsMouse", double.class, double.class);
                                Object result = containsMouse.invoke(entryList, mouseX, mouseY);
                                if (result instanceof Boolean && (Boolean) result) {
                                    return true;
                                }
                            }
                        } catch (Exception ignored) {
                            // method not found
                        }

                        // try favorites panel
                        try {
                            java.lang.reflect.Method getFavoritesListWidget = overlay.getClass().getMethod("getFavoritesListWidget");
                            Object favoritesOptional = getFavoritesListWidget.invoke(overlay);
                            if (favoritesOptional instanceof java.util.Optional<?> favOpt && favOpt.isPresent()) {
                                Object favorites = favOpt.get();
                                java.lang.reflect.Method containsMouse = favorites.getClass().getMethod("containsMouse", double.class, double.class);
                                Object result = containsMouse.invoke(favorites, mouseX, mouseY);
                                if (result instanceof Boolean && (Boolean) result) {
                                    return true;
                                }
                            }
                        } catch (Exception ignored) {
                            // method not found
                        }
                    }
                }
            }
        } catch (ClassNotFoundException ignored) {
            // REI not installed
        } catch (Exception ignored) {
            // API changed or error occurred, fail silently
        }
        return false;
    }

    /** JEI: check mezz.jei.api.runtime.IJeiRuntime for ingredient under mouse */
    private static boolean isJeiStackHovered() {
        try {
            // JEI stores runtime in Internal class
            Class<?> internalClass = Class.forName("mezz.jei.common.Internal");
            java.lang.reflect.Method getRuntime = internalClass.getMethod("getRuntime");
            Object runtimeOptional = getRuntime.invoke(null);

            if (runtimeOptional instanceof java.util.Optional<?> optional && optional.isPresent()) {
                Object runtime = optional.get();

                // get ingredient list overlay
                java.lang.reflect.Method getIngredientListOverlay = runtime.getClass().getMethod("getIngredientListOverlay");
                Object ingredientOverlay = getIngredientListOverlay.invoke(runtime);

                if (ingredientOverlay != null) {
                    // check getIngredientUnderMouse()
                    java.lang.reflect.Method getIngredientUnderMouse = ingredientOverlay.getClass().getMethod("getIngredientUnderMouse");
                    Object ingredientOptional = getIngredientUnderMouse.invoke(ingredientOverlay);

                    if (ingredientOptional instanceof java.util.Optional<?> ingredientOpt && ingredientOpt.isPresent()) {
                        return true;
                    }
                }

                // also check bookmark overlay
                try {
                    java.lang.reflect.Method getBookmarkOverlay = runtime.getClass().getMethod("getBookmarkOverlay");
                    Object bookmarkOverlay = getBookmarkOverlay.invoke(runtime);

                    if (bookmarkOverlay != null) {
                        java.lang.reflect.Method getIngredientUnderMouse = bookmarkOverlay.getClass().getMethod("getIngredientUnderMouse");
                        Object ingredientOptional = getIngredientUnderMouse.invoke(bookmarkOverlay);

                        if (ingredientOptional instanceof java.util.Optional<?> ingredientOpt && ingredientOpt.isPresent()) {
                            return true;
                        }
                    }
                } catch (Exception ignored) {
                    // bookmark overlay not available
                }
            }
        } catch (ClassNotFoundException ignored) {
            // JEI not installed
        } catch (Exception ignored) {
            // API changed or error occurred, fail silently
        }
        return false;
    }
}
