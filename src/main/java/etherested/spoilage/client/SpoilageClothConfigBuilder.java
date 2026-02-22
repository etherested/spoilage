package etherested.spoilage.client;

import etherested.spoilage.config.SpoilageConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

// builds the Cloth Config screen for spoilage settings;
// separated from SpoilageConfigScreen to avoid classloading Cloth Config
// types when the mod is not installed
class SpoilageClothConfigBuilder {

    static Screen build(Screen parent) {
        SpoilageConfig cfg = SpoilageConfig.getInstance();
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("config.spoilage.title"))
                .setSavingRunnable(SpoilageConfig::save);

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        // general category
        ConfigCategory general = builder.getOrCreateCategory(Component.translatable("config.spoilage.general"));
        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.spoilage.enabled"), SpoilageConfig.isEnabled())
                .setDefaultValue(true).setSaveConsumer(cfg::setEnabled).build());
        general.addEntry(entryBuilder.startDoubleField(Component.translatable("config.spoilage.globalSpeedMultiplier"), SpoilageConfig.getGlobalSpeedMultiplier())
                .setDefaultValue(1.0).setMin(0.01).setMax(100.0).setSaveConsumer(cfg::setGlobalSpeedMultiplier).build());
        general.addEntry(entryBuilder.startIntField(Component.translatable("config.spoilage.checkIntervalTicks"), SpoilageConfig.getCheckIntervalTicks())
                .setDefaultValue(100).setMin(1).setMax(12000).setSaveConsumer(cfg::setCheckIntervalTicks).build());

        // tooltip category
        ConfigCategory tooltip = builder.getOrCreateCategory(Component.translatable("config.spoilage.tooltip"));
        tooltip.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.spoilage.showRemainingTime"), SpoilageConfig.showRemainingTime())
                .setDefaultValue(true).setSaveConsumer(cfg::setShowRemainingTime).build());
        tooltip.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.spoilage.showFreshnessWord"), SpoilageConfig.showFreshnessWord())
                .setDefaultValue(true).setSaveConsumer(cfg::setShowFreshnessWord).build());
        tooltip.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.spoilage.showFreshnessPercentage"), SpoilageConfig.showFreshnessPercentage())
                .setDefaultValue(false).setSaveConsumer(cfg::setShowFreshnessPercentage).build());

        // visual category
        ConfigCategory visual = builder.getOrCreateCategory(Component.translatable("config.spoilage.visual"));
        visual.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.spoilage.showTintOverlay"), SpoilageConfig.showTintOverlay())
                .setDefaultValue(true).setSaveConsumer(cfg::setShowTintOverlay).build());
        visual.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.spoilage.tintStyleRotten"), SpoilageConfig.tintStyleRotten())
                .setDefaultValue(true).setSaveConsumer(cfg::setTintStyleRotten).build());
        visual.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.spoilage.useTextureBlending"), SpoilageConfig.useTextureBlending())
                .setDefaultValue(true).setSaveConsumer(cfg::setUseTextureBlending).build());

        // preservation category
        ConfigCategory preservation = builder.getOrCreateCategory(Component.translatable("config.spoilage.preservation"));
        preservation.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.spoilage.yLevelPreservationEnabled"), SpoilageConfig.isYLevelPreservationEnabled())
                .setDefaultValue(true).setSaveConsumer(cfg::setYLevelPreservationEnabled).build());
        preservation.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.spoilage.biomeTemperaturePreservationEnabled"), SpoilageConfig.isBiomeTemperaturePreservationEnabled())
                .setDefaultValue(true).setSaveConsumer(cfg::setBiomeTemperaturePreservationEnabled).build());
        preservation.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.spoilage.coldSweatIntegrationEnabled"), SpoilageConfig.isColdSweatIntegrationEnabled())
                .setDefaultValue(true).setSaveConsumer(cfg::setColdSweatIntegrationEnabled).build());

        // gameplay category
        ConfigCategory gameplay = builder.getOrCreateCategory(Component.translatable("config.spoilage.gameplay"));
        gameplay.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.spoilage.offhandAutoCombineEnabled"), SpoilageConfig.isOffhandAutoCombineEnabled())
                .setDefaultValue(true).setSaveConsumer(cfg::setOffhandAutoCombineEnabled).build());
        gameplay.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.spoilage.villagersIgnoreSpoiled"), SpoilageConfig.doVillagersIgnoreSpoiled())
                .setDefaultValue(true).setSaveConsumer(cfg::setVillagersIgnoreSpoiled).build());
        gameplay.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.spoilage.animalsPoisonedByRotten"), SpoilageConfig.areAnimalsPoisonedByRotten())
                .setDefaultValue(true).setSaveConsumer(cfg::setAnimalsPoisonedByRotten).build());
        gameplay.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.spoilage.preventPlantingSpoiled"), SpoilageConfig.isPreventPlantingSpoiledEnabled())
                .setDefaultValue(true).setSaveConsumer(cfg::setPreventPlantingSpoiled).build());
        gameplay.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.spoilage.lootRandomizationEnabled"), SpoilageConfig.isLootRandomizationEnabled())
                .setDefaultValue(true).setSaveConsumer(cfg::setLootRandomizationEnabled).build());
        gameplay.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.spoilage.contaminationEnabled"), SpoilageConfig.isContaminationEnabled())
                .setDefaultValue(true).setSaveConsumer(cfg::setContaminationEnabled).build());

        // crops category
        ConfigCategory crops = builder.getOrCreateCategory(Component.translatable("config.spoilage.crops"));
        crops.addEntry(entryBuilder.startIntField(Component.translatable("config.spoilage.cropFreshPeriodTicks"), SpoilageConfig.getCropFreshPeriodTicks())
                .setDefaultValue(72000).setMin(0).setMax(1728000).setSaveConsumer(cfg::setCropFreshPeriodTicks).build());
        crops.addEntry(entryBuilder.startIntField(Component.translatable("config.spoilage.cropRotPeriodTicks"), SpoilageConfig.getCropRotPeriodTicks())
                .setDefaultValue(48000).setMin(1).setMax(1728000).setSaveConsumer(cfg::setCropRotPeriodTicks).build());
        crops.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.spoilage.bonemealResetsRot"), SpoilageConfig.doesBonemealResetRot())
                .setDefaultValue(true).setSaveConsumer(cfg::setBonemealResetsRot).build());
        crops.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.spoilage.bonemealBlockedOnRotten"), SpoilageConfig.isBonemealBlockedOnRotten())
                .setDefaultValue(true).setSaveConsumer(cfg::setBonemealBlockedOnRotten).build());
        crops.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.spoilage.staleSeedGrowthPenalty"), SpoilageConfig.isStaleSeedGrowthPenaltyEnabled())
                .setDefaultValue(true).setSaveConsumer(cfg::setStaleSeedGrowthPenalty).build());

        return builder.build();
    }
}
