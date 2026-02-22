package etherested.spoilage.client;

import etherested.spoilage.Spoilage;
import net.minecraft.world.level.ChunkPos;

//? if neoforge {
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
//?} else {
/*import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
*///?}

// client-side event handlers for spoilage visuals;
// handles cache cleanup when disconnecting or unloading chunks
//? if neoforge {
@EventBusSubscriber(modid = Spoilage.MODID, value = Dist.CLIENT)
//?}
public class SpoilageClientEvents {

    //? if neoforge {
    // clears the block spoilage cache when disconnecting from a server
    @SubscribeEvent
    public static void onClientDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        BlockSpoilageClientCache.clearAll();
    }

    // clears cached spoilage data when a chunk is unloaded
    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            ChunkPos chunkPos = event.getChunk().getPos();
            BlockSpoilageClientCache.clearChunk(chunkPos);
        }
    }
    //?} else {
    /*public static void registerFabricEvents() {
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            BlockSpoilageClientCache.clearAll();
        });

        ClientChunkEvents.CHUNK_UNLOAD.register((level, chunk) -> {
            BlockSpoilageClientCache.clearChunk(chunk.getPos());
        });
    }
    *///?}
}
