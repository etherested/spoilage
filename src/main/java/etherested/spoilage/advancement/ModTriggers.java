package etherested.spoilage.advancement;

import etherested.spoilage.Spoilage;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;

//? if neoforge {
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.function.Supplier;
//?} else {
/*import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
*///?}

/** registry for custom advancement triggers */
public class ModTriggers {

    //? if neoforge {
    public static final DeferredRegister<net.minecraft.advancements.CriterionTrigger<?>> TRIGGERS =
            DeferredRegister.create(Registries.TRIGGER_TYPE, Spoilage.MODID);

    public static final Supplier<EatInedibleTrigger> EAT_INEDIBLE =
            TRIGGERS.register("eat_inedible", EatInedibleTrigger::new);

    public static void register(IEventBus eventBus) {
        TRIGGERS.register(eventBus);
    }
    //?} else {
    /*private static EatInedibleTrigger EAT_INEDIBLE;

    public static void register() {
        EAT_INEDIBLE = Registry.register(
                BuiltInRegistries.TRIGGER_TYPES,
                ResourceLocation.fromNamespaceAndPath(Spoilage.MODID, "eat_inedible"),
                new EatInedibleTrigger()
        );
    }
    *///?}

    public static EatInedibleTrigger eatInedible() {
        //? if neoforge {
        return EAT_INEDIBLE.get();
        //?} else {
        /*return EAT_INEDIBLE;
        *///?}
    }
}
