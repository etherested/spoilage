package etherested.spoilage;

import org.slf4j.LoggerFactory;
import etherested.spoilage.advancement.ModTriggers;
import etherested.spoilage.registry.ModBlocks;
import etherested.spoilage.component.ModDataComponents;
import etherested.spoilage.config.SpoilageConfig;
import etherested.spoilage.registry.ModItems;
import etherested.spoilage.logic.preservation.PreservationManager;
import etherested.spoilage.loot.ModLootFunctions;
import org.slf4j.Logger;

//? if neoforge {
import etherested.spoilage.client.SpoilageConfigScreen;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
//?}

//? if neoforge {
@Mod(Spoilage.MODID)
//?}
public class Spoilage
    //? if fabric {
    /*implements net.fabricmc.api.ModInitializer
    *///?}
{
    public static final String MODID = "spoilage";
    private static final Logger LOGGER = LoggerFactory.getLogger(Spoilage.class);

    //? if neoforge {
    public Spoilage(IEventBus modEventBus, ModContainer modContainer) {
        ModDataComponents.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModLootFunctions.register(modEventBus);
        ModTriggers.register(modEventBus);

        modEventBus.addListener(this::commonSetup);

        SpoilageConfig.load();

        if (SpoilageConfigScreen.isAvailable()) {
            modContainer.registerExtensionPoint(IConfigScreenFactory.class,
                    (container, parent) -> SpoilageConfigScreen.create(parent));
        }

        LOGGER.info("Spoilage mod initialized");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            PreservationManager.init();
            LOGGER.info("Spoilage preservation system initialized");
        });
    }
    //?} else {
    /*@Override
    public void onInitialize() {
        SpoilageConfig.load();
        ModDataComponents.register();
        ModBlocks.register();
        ModItems.register();
        ModLootFunctions.register();
        ModTriggers.register();
        PreservationManager.init();

        etherested.spoilage.event.SpoilageEvents.registerFabricEvents();
        etherested.spoilage.event.ContainerSpoilageHandler.registerFabricEvents();
        etherested.spoilage.event.CakePlacementHandler.registerFabricEvents();
        etherested.spoilage.event.CropBonemealHandler.registerFabricEvents();
        etherested.spoilage.event.BlockSpoilageCleanupHandler.registerFabricEvents();
        etherested.spoilage.network.BlockSpoilageNetworkHandler.registerFabricEvents();
        etherested.spoilage.network.ModNetworking.registerFabric();
        etherested.spoilage.loot.ModLootFunctions.registerFabricLootModification();

        LOGGER.info("Spoilage mod initialized");
    }
    *///?}
}
