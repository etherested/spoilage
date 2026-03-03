package etherested.spoilage.mixin;

import etherested.spoilage.component.ModDataComponents;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// prevents held item bob animation when only spoilage data changes;
// without this, every spoilage update causes a visible pop/flicker on held food items
@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererMixin {

    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;matches(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z"
            )
    )
    private boolean spoilage$matchesIgnoringSpoilage(ItemStack first, ItemStack second) {
        if (ItemStack.matches(first, second)) {
            return true;
        }
        return spoilage$differOnlyInSpoilage(first, second);
    }

    // checks whether two stacks are identical except for their spoilage data component
    @Unique
    private static boolean spoilage$differOnlyInSpoilage(ItemStack first, ItemStack second) {
        if (first.isEmpty() || second.isEmpty()) return false;
        if (!first.is(second.getItem())) return false;
        if (first.getCount() != second.getCount()) return false;

        ItemStack firstCopy = first.copy();
        ItemStack secondCopy = second.copy();
        firstCopy.remove(ModDataComponents.spoilageData());
        secondCopy.remove(ModDataComponents.spoilageData());
        return ItemStack.isSameItemSameComponents(firstCopy, secondCopy);
    }
}
