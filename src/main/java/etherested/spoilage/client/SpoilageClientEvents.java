package etherested.spoilage.client;

import etherested.spoilage.Spoilage;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;

/**
 * client-side event handlers for spoilage visuals;
 * handles cache cleanup when disconnecting or unloading chunks
 */
@EventBusSubscriber(modid = Spoilage.MODID, value = Dist.CLIENT)
public class SpoilageClientEvents {

    /** clears the block spoilage cache when disconnecting from a server */
    @SubscribeEvent
    public static void onClientDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        BlockSpoilageClientCache.clearAll();
    }

    /** clears cached spoilage data when a chunk is unloaded */
    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            ChunkPos chunkPos = event.getChunk().getPos();
            BlockSpoilageClientCache.clearChunk(chunkPos);
        }
    }
}
