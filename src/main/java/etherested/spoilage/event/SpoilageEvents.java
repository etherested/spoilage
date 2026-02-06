package etherested.spoilage.event;

import etherested.spoilage.Spoilage;
import etherested.spoilage.config.SpoilageConfig;
import etherested.spoilage.data.SpoilageGroupRegistry;
import etherested.spoilage.data.SpoilageItemRegistry;
import etherested.spoilage.logic.SpoilageProcessor;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** game event handlers for spoilage mechanics */
@SuppressWarnings("removal")
@EventBusSubscriber(modid = Spoilage.MODID, bus = EventBusSubscriber.Bus.GAME)
public class SpoilageEvents {

    private static final Map<UUID, Long> lastProcessTime = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!SpoilageConfig.isEnabled()) {
            return;
        }

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        long currentTime = player.level().getGameTime();
        UUID playerId = player.getUUID();

        Long lastTime = lastProcessTime.get(playerId);
        if (lastTime == null || currentTime - lastTime >= SpoilageConfig.getCheckIntervalTicks()) {
            SpoilageProcessor.processPlayerInventory(player);
            lastProcessTime.put(playerId, currentTime);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SpoilageProcessor.onPlayerLogin(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SpoilageProcessor.onPlayerLogout(player);
            lastProcessTime.remove(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new SpoilageGroupRegistry());
        event.addListener(new SpoilageItemRegistry());
    }
}
