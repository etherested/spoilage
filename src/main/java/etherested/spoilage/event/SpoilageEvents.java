package etherested.spoilage.event;

import etherested.spoilage.Spoilage;
import etherested.spoilage.config.SpoilageConfig;
import etherested.spoilage.data.SpoilageGroupRegistry;
import etherested.spoilage.data.SpoilageItemRegistry;
import etherested.spoilage.logic.SpoilageProcessor;
import net.minecraft.server.level.ServerPlayer;

//? if neoforge {
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
//?} else {
/*import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.server.packs.PackType;
*///?}

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// game event handlers for spoilage mechanics
//? if neoforge {
@SuppressWarnings("removal")
@EventBusSubscriber(modid = Spoilage.MODID, bus = EventBusSubscriber.Bus.GAME)
//?}
public class SpoilageEvents {

    private static final Map<UUID, Long> lastProcessTime = new HashMap<>();

    //? if neoforge {
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        handlePlayerTick(player);
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            handlePlayerLogin(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            handlePlayerLogout(player);
        }
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new SpoilageGroupRegistry());
        event.addListener(new SpoilageItemRegistry());
    }
    //?} else {
    /*public static void registerFabricEvents() {
        ServerTickEvents.END_WORLD_TICK.register(level -> {
            for (ServerPlayer player : level.players()) {
                handlePlayerTick(player);
            }
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            handlePlayerLogin(handler.getPlayer());
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            handlePlayerLogout(handler.getPlayer());
        });

        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new SpoilageGroupRegistry());
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new SpoilageItemRegistry());
    }
    *///?}

    // ─── shared logic ───

    private static void handlePlayerTick(ServerPlayer player) {
        if (!SpoilageConfig.isEnabled()) return;

        long currentTime = player.level().getGameTime();
        UUID playerId = player.getUUID();

        Long lastTime = lastProcessTime.get(playerId);
        if (lastTime == null || currentTime - lastTime >= SpoilageConfig.getCheckIntervalTicks()) {
            SpoilageProcessor.processPlayerInventory(player);
            lastProcessTime.put(playerId, currentTime);
        }
    }

    private static void handlePlayerLogin(ServerPlayer player) {
        SpoilageProcessor.onPlayerLogin(player);
    }

    private static void handlePlayerLogout(ServerPlayer player) {
        SpoilageProcessor.onPlayerLogout(player);
        lastProcessTime.remove(player.getUUID());
    }
}
