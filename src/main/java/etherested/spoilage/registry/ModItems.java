package etherested.spoilage.registry;

import etherested.spoilage.Spoilage;

//? if neoforge {
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Spoilage.MODID);

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
//?} else {
/*public class ModItems {
    public static void register() {
        // no custom items to register currently
    }
}
*///?}
