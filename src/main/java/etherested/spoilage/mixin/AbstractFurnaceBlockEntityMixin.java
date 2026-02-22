package etherested.spoilage.mixin;

import etherested.spoilage.config.SpoilageConfig;
import etherested.spoilage.logic.SpoilageCalculator;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// mixin to transfer spoilage when smelting food items;
// when raw spoiled food is cooked, the result inherits the spoilage level
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceBlockEntityMixin {

    @Shadow protected NonNullList<ItemStack> items;

    @Unique
    private int spoilage$previousInputCount = 0;
    @Unique
    private float spoilage$inputSpoilage = 0f;
    @Unique
    private int spoilage$previousOutputCount = 0;

    // before server tick, capture input spoilage if smelting is about to complete;
    // the items array is: 0 = input, 1 = fuel, 2 = output
    @Inject(method = "serverTick", at = @At("HEAD"))
    private static void spoilage$beforeTick(Level level, net.minecraft.core.BlockPos pos,
                                             net.minecraft.world.level.block.state.BlockState state,
                                             AbstractFurnaceBlockEntity blockEntity, CallbackInfo ci) {
        if (!SpoilageConfig.isEnabled()) {
            return;
        }

        AbstractFurnaceBlockEntityMixin mixin = (AbstractFurnaceBlockEntityMixin)(Object)blockEntity;
        NonNullList<ItemStack> items = mixin.items;

        ItemStack input = items.get(0);
        ItemStack output = items.get(2);

        // store current state
        mixin.spoilage$previousInputCount = input.getCount();
        mixin.spoilage$previousOutputCount = output.getCount();

        // capture input spoilage if the input is spoilable
        if (!input.isEmpty() && SpoilageCalculator.isSpoilable(input)) {
            mixin.spoilage$inputSpoilage = SpoilageCalculator.getSpoilagePercent(input, level.getGameTime());
        } else {
            mixin.spoilage$inputSpoilage = 0f;
        }
    }

    // after server tick, apply spoilage to new output if an item was smelted
    @Inject(method = "serverTick", at = @At("RETURN"))
    private static void spoilage$afterTick(Level level, net.minecraft.core.BlockPos pos,
                                            net.minecraft.world.level.block.state.BlockState state,
                                            AbstractFurnaceBlockEntity blockEntity, CallbackInfo ci) {
        if (!SpoilageConfig.isEnabled()) {
            return;
        }

        AbstractFurnaceBlockEntityMixin mixin = (AbstractFurnaceBlockEntityMixin)(Object)blockEntity;
        NonNullList<ItemStack> items = mixin.items;

        ItemStack input = items.get(0);
        ItemStack output = items.get(2);

        // check if smelting occurred (input decreased and output increased)
        int currentInputCount = input.getCount();
        int currentOutputCount = output.getCount();

        boolean inputDecreased = currentInputCount < mixin.spoilage$previousInputCount;
        boolean outputIncreased = currentOutputCount > mixin.spoilage$previousOutputCount;

        // if smelting happened and we had input spoilage data
        if (inputDecreased && outputIncreased && mixin.spoilage$inputSpoilage > 0f) {
            // apply input spoilage to the output
            if (!output.isEmpty() && SpoilageCalculator.isSpoilable(output)) {
                SpoilageCalculator.initializeSpoilageWithPercent(output, level.getGameTime(), mixin.spoilage$inputSpoilage);
            }
        }
    }
}
