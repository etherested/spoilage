package etherested.spoilage.mixin;

import etherested.spoilage.logic.SpoilageCalculator;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/** mixin to handle spoilage initialization on item tooltip display */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    /**
     * initialize spoilage when tooltip is shown,
     * but NOT for creative inventory items or crafting result preview items
     */
    @Inject(method = "getTooltipLines", at = @At("HEAD"))
    private void spoilage$onGetTooltipLines(Item.TooltipContext context, Player player, TooltipFlag flag, CallbackInfoReturnable<List<Component>> cir) {
        ItemStack self = (ItemStack) (Object) this;

        if (player == null || player.level() == null) {
            return;
        }

        // don't initialize spoilage for creative mode players viewing creative inventory
        // creative inventory items are template items that shouldn't spoil
        if (player.isCreative() && isCreativeOrPreviewItem(self, player)) {
            return;
        }

        // don't initialize spoilage for crafting result preview items
        if (isResultSlotPreview(self, player)) {
            return;
        }

        if (!SpoilageCalculator.isSpoilable(self)) {
            return;
        }

        if (SpoilageCalculator.getInitializedData(self) == null) {
            long worldTime = player.level().getGameTime();
            SpoilageCalculator.initializeSpoilage(self, worldTime);
        }
    }

    /**
     * check if the item stack is from the creative inventory (not player's actual inventory);
     * creative inventory items don't have spoilage data and are just templates
     */
    private boolean isCreativeOrPreviewItem(ItemStack stack, Player player) {
        // If the item already has spoilage data, it's from the player's inventory
        if (SpoilageCalculator.getInitializedData(stack) != null) {
            return false;
        }

        // check if this stack is in the player's inventory
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack invStack = player.getInventory().getItem(i);
            if (invStack == stack) {
                return false; // It's in the player's inventory, not creative menu
            }
        }

        // not in player inventory - likely creative menu item or preview
        return true;
    }

    /**
     * check if the item stack is in a result slot (crafting result, furnace output, etc.);
     * these are preview items that shouldn't have spoilage initialized until taken
     */
    private boolean isResultSlotPreview(ItemStack stack, Player player) {
        // if the item already has spoilage data, it's not a preview
        if (SpoilageCalculator.getInitializedData(stack) != null) {
            return false;
        }

        // check if this stack is in a result slot
        AbstractContainerMenu menu = player.containerMenu;
        if (menu != null) {
            for (Slot slot : menu.slots) {
                if (slot instanceof ResultSlot && slot.getItem() == stack) {
                    return true;
                }
            }
        }

        return false;
    }
}
