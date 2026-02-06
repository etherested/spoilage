package etherested.spoilage.loot;

import com.mojang.serialization.MapCodec;
import etherested.spoilage.Spoilage;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class ModLootFunctions {
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
}
