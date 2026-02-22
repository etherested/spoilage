package etherested.spoilage.mixin;

import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

// accessor mixin to access private fields of ResultSlot
@Mixin(ResultSlot.class)
public interface ResultSlotAccessor {
    @Accessor("craftSlots")
    CraftingContainer getCraftSlots();
}
