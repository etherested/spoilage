package etherested.spoilage.component;

import etherested.spoilage.Spoilage;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;

//? if neoforge {
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
//?} else {
/*import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
*///?}

public class ModDataComponents {

    //? if neoforge {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, Spoilage.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SpoilageData>> SPOILAGE_DATA =
            DATA_COMPONENTS.register("spoilage_data", () -> DataComponentType.<SpoilageData>builder()
                    .persistent(SpoilageData.CODEC)
                    .networkSynchronized(SpoilageData.STREAM_CODEC)
                    .build());

    public static void register(IEventBus modEventBus) {
        DATA_COMPONENTS.register(modEventBus);
    }
    //?} else {
    /*private static DataComponentType<SpoilageData> SPOILAGE_DATA;

    public static void register() {
        SPOILAGE_DATA = Registry.register(
                BuiltInRegistries.DATA_COMPONENT_TYPE,
                ResourceLocation.fromNamespaceAndPath(Spoilage.MODID, "spoilage_data"),
                DataComponentType.<SpoilageData>builder()
                        .persistent(SpoilageData.CODEC)
                        .networkSynchronized(SpoilageData.STREAM_CODEC)
                        .build()
        );
    }
    *///?}

    public static DataComponentType<SpoilageData> spoilageData() {
        //? if neoforge {
        return SPOILAGE_DATA.get();
        //?} else {
        /*return SPOILAGE_DATA;
        *///?}
    }
}
