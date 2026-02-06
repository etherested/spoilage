package etherested.spoilage.mixin;

import etherested.spoilage.component.ModDataComponents;
import etherested.spoilage.config.SpoilageConfig;
import etherested.spoilage.logic.SpoilageCalculator;
import net.minecraft.core.component.DataComponents;
import net.minecraft.recipebook.ServerPlaceRecipe;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * mixin to make recipe book ignore spoilage data when finding matching items;
 * this allows the recipe book to move spoiled items to the crafting grid
 */
@Mixin(ServerPlaceRecipe.class)
public abstract class ServerPlaceRecipeMixin<I, R> {

    @Shadow protected Inventory inventory;

    /** redirect the findSlotMatchingUnusedItem call to ignore spoilage components */
    @Redirect(method = "moveItemToGrid",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Inventory;findSlotMatchingUnusedItem(Lnet/minecraft/world/item/ItemStack;)I"))
    private int spoilage$findSlotIgnoringSpoilage(Inventory inventory, ItemStack pattern) {
        if (!SpoilageConfig.isEnabled()) {
            return inventory.findSlotMatchingUnusedItem(pattern);
        }

        // if the pattern item is not spoilable, use vanilla behavior
        if (!SpoilageCalculator.isSpoilable(pattern)) {
            return inventory.findSlotMatchingUnusedItem(pattern);
        }

        // search for matching items, ignoring spoilage data
        for (int i = 0; i < inventory.items.size(); i++) {
            ItemStack stack = inventory.items.get(i);
            if (!stack.isEmpty()
                    && spoilage$itemsMatchIgnoringSpoilage(stack, pattern)
                    && !stack.isDamaged()
                    && !stack.isEnchanted()
                    && !stack.has(DataComponents.CUSTOM_NAME)) {
                return i;
            }
        }
        return -1;
    }

    /** check if two items match when ignoring spoilage data */
    private boolean spoilage$itemsMatchIgnoringSpoilage(ItemStack stack, ItemStack pattern) {
        // quick check: same item type
        if (!ItemStack.isSameItem(stack, pattern)) {
            return false;
        }

        // create copies without spoilage data for comparison
        ItemStack stackCopy = stack.copy();
        ItemStack patternCopy = pattern.copy();

        stackCopy.remove(ModDataComponents.SPOILAGE_DATA.get());
        patternCopy.remove(ModDataComponents.SPOILAGE_DATA.get());

        return ItemStack.isSameItemSameComponents(stackCopy, patternCopy);
    }
}
