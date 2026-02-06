package etherested.spoilage.network;

import etherested.spoilage.Spoilage;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** registers network packets for the mod */
@EventBusSubscriber(modid = Spoilage.MODID, bus = EventBusSubscriber.Bus.MOD)
public class ModNetworking {

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(Spoilage.MODID);

        // register block spoilage sync packet (server -> client)
        registrar.playToClient(
                BlockSpoilageSyncPacket.TYPE,
                BlockSpoilageSyncPacket.STREAM_CODEC,
                BlockSpoilageSyncPacket::handleClient
        );
    }
}
