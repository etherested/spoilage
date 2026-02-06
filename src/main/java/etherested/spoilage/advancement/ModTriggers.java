package etherested.spoilage.advancement;

import etherested.spoilage.Spoilage;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/** registry for custom advancement triggers */
public class ModTriggers {

    public static final DeferredRegister<net.minecraft.advancements.CriterionTrigger<?>> TRIGGERS =
            DeferredRegister.create(Registries.TRIGGER_TYPE, Spoilage.MODID);

    public static final Supplier<EatInedibleTrigger> EAT_INEDIBLE =
            TRIGGERS.register("eat_inedible", EatInedibleTrigger::new);

    public static void register(IEventBus eventBus) {
        TRIGGERS.register(eventBus);
    }
}
