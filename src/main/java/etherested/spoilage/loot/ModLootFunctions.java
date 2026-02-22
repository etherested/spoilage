package etherested.spoilage.loot;

import etherested.spoilage.Spoilage;
import etherested.spoilage.config.SpoilageConfig;
import etherested.spoilage.logic.SpoilageCalculator;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;

//? if neoforge {
import com.mojang.serialization.MapCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
//?} else {
/*import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.world.item.ItemStack;
*///?}

public class ModLootFunctions {

    //? if neoforge {
    public static final DeferredRegister<LootItemFunctionType<?>> LOOT_FUNCTIONS =
            DeferredRegister.create(Registries.LOOT_FUNCTION_TYPE, Spoilage.MODID);

    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, Spoilage.MODID);

    public static final DeferredHolder<LootItemFunctionType<?>, LootItemFunctionType<SetSpoilageLootFunction>> SET_SPOILAGE =
            LOOT_FUNCTIONS.register("set_spoilage", () -> new LootItemFunctionType<>(SetSpoilageLootFunction.CODEC));

    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<SpoilageLootModifier>> SPOILAGE_MODIFIER =
            LOOT_MODIFIERS.register("add_spoilage", () -> SpoilageLootModifier.CODEC);

    public static void register(IEventBus modEventBus) {
        LOOT_FUNCTIONS.register(modEventBus);
        LOOT_MODIFIERS.register(modEventBus);
    }
    //?} else {
    /*private static LootItemFunctionType<SetSpoilageLootFunction> SET_SPOILAGE;

    public static void register() {
        SET_SPOILAGE = Registry.register(
                BuiltInRegistries.LOOT_FUNCTION_TYPE,
                ResourceLocation.fromNamespaceAndPath(Spoilage.MODID, "set_spoilage"),
                new LootItemFunctionType<>(SetSpoilageLootFunction.CODEC)
        );
    }

    public static void registerFabricLootModification() {
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
            if (!SpoilageConfig.isEnabled() || !SpoilageConfig.isLootRandomizationEnabled()) {
                return;
            }

            tableBuilder.modifyPools(poolBuilder -> poolBuilder.apply(SetSpoilageLootFunction.builder().withRange(0.0f, 0.5f)));
        });
    }
    *///?}

    public static LootItemFunctionType<SetSpoilageLootFunction> setSpoilage() {
        //? if neoforge {
        return SET_SPOILAGE.get();
        //?} else {
        /*return SET_SPOILAGE;
        *///?}
    }
}
