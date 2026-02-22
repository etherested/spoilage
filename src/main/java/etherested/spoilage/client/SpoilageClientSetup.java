package etherested.spoilage.client;

import etherested.spoilage.Spoilage;
import etherested.spoilage.logic.SpoilageCalculator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

//? if neoforge {
import etherested.spoilage.client.data.SpoilageAssetRegistry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
//?}

// client-side setup for spoilage visual effects;
// registers item property overrides for items with texture stages
//? if neoforge {
@EventBusSubscriber(modid = Spoilage.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
//?}
public class SpoilageClientSetup {

    public static final ResourceLocation SPOILAGE_PROPERTY = ResourceLocation.fromNamespaceAndPath(Spoilage.MODID, "spoilage_stage");

    //? if neoforge {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // item property registrations can be added here if needed for texture stages
    }

    @SubscribeEvent
    public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        // register the client-side asset registry for loading texture data from assets/
        event.registerReloadListener(new SpoilageAssetRegistry());
    }
    //?}

    // registers the spoilage_stage property for an item;
    // this property returns the current spoilage percentage (0.0 to 1.0)
    // which can be used in item model predicates to select different textures
    public static void registerSpoilageProperty(Item item) {
        ItemProperties.register(item, SPOILAGE_PROPERTY, (stack, level, entity, seed) -> {
            if (!SpoilageCalculator.isSpoilable(stack)) {
                return 0.0f;
            }

            if (SpoilageCalculator.getInitializedData(stack) == null) {
                return 0.0f;
            }

            // use client level if available, otherwise try the passed level
            Minecraft mc = Minecraft.getInstance();
            long worldTime;
            if (mc.level != null) {
                worldTime = mc.level.getGameTime();
            } else if (level != null) {
                worldTime = level.getGameTime();
            } else {
                return 0.0f;
            }

            // return spoilage percentage directly (0.0 to 1.0)
            return SpoilageCalculator.getSpoilagePercent(stack, worldTime);
        });
    }
}
