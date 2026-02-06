package etherested.spoilage.network;

import etherested.spoilage.client.BlockSpoilageClientCache;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashMap;
import java.util.Map;

/**
 * network packet for syncing block spoilage data from server to client;
 * contains a map of block positions to their spoilage percentages
 */
public record BlockSpoilageSyncPacket(Map<BlockPos, Float> spoilageData) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<BlockSpoilageSyncPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("spoilage", "block_spoilage_sync"));

    public static final StreamCodec<FriendlyByteBuf, BlockSpoilageSyncPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.map(
                    HashMap::new,
                    BlockPos.STREAM_CODEC,
                    ByteBufCodecs.FLOAT
            ),
            BlockSpoilageSyncPacket::spoilageData,
            BlockSpoilageSyncPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * handles the packet on the client side;
     * updates the client cache with received spoilage data
     */
    public static void handleClient(BlockSpoilageSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            for (Map.Entry<BlockPos, Float> entry : packet.spoilageData().entrySet()) {
                BlockSpoilageClientCache.updateSpoilage(entry.getKey(), entry.getValue());
            }
        });
    }
}
