package etherested.spoilage.network;

import etherested.spoilage.Spoilage;

//? if neoforge {
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

// registers network packets for the mod
@EventBusSubscriber(modid = Spoilage.MODID, bus = EventBusSubscriber.Bus.MOD)
public class ModNetworking {

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(Spoilage.MODID);

        // register block spoilage sync packet (server -> client)
        registrar.playToClient(
                BlockSpoilageSyncPacket.TYPE,
                BlockSpoilageSyncPacket.STREAM_CODEC,
                BlockSpoilageSyncPacket::handleClientNeoForge
        );
    }
}
//?} else {
/*import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class ModNetworking {

    public static void registerFabric() {
        // register the packet type on the server side
        PayloadTypeRegistry.playS2C().register(
                BlockSpoilageSyncPacket.TYPE,
                BlockSpoilageSyncPacket.STREAM_CODEC
        );
    }

    public static void registerFabricClient() {
        // register the client-side handler
        ClientPlayNetworking.registerGlobalReceiver(
                BlockSpoilageSyncPacket.TYPE,
                (packet, context) -> {
                    context.client().execute(() -> BlockSpoilageSyncPacket.handleClientShared(packet));
                }
        );
    }
}
*///?}
