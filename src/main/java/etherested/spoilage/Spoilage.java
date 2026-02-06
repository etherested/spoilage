package etherested.spoilage;

import org.slf4j.LoggerFactory;
import etherested.spoilage.advancement.ModTriggers;
import etherested.spoilage.registry.ModBlocks;
import etherested.spoilage.component.ModDataComponents;
import etherested.spoilage.config.SpoilageConfig;
import etherested.spoilage.registry.ModItems;
import etherested.spoilage.logic.preservation.PreservationManager;
import etherested.spoilage.loot.ModLootFunctions;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import org.slf4j.Logger;

@Mod(Spoilage.MODID)
public class Spoilage {
    public static final String MODID = "spoilage";
    private static final Logger LOGGER = LoggerFactory.getLogger(Spoilage.class);

    public Spoilage(IEventBus modEventBus, ModContainer modContainer) {
        ModDataComponents.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModLootFunctions.register(modEventBus);
        ModTriggers.register(modEventBus);

        modEventBus.addListener(this::commonSetup);

        modContainer.registerConfig(ModConfig.Type.CLIENT, SpoilageConfig.SPEC, "spoilage.toml");
        modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);

        LOGGER.info("Spoilage mod initialized");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // initialize preservation system (including Cold Sweat integration if available)
            PreservationManager.init();
            LOGGER.info("Spoilage preservation system initialized");
        });
    }
}
