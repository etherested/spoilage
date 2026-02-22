package etherested.spoilage.client;

//? if fabric {
/*import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.server.packs.PackType;
import etherested.spoilage.client.data.SpoilageAssetRegistry;

public class SpoilageFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // register client-side reload listeners
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES)
                .registerReloadListener(new SpoilageAssetRegistry());

        // register client event handlers
        SpoilageClientEvents.registerFabricEvents();
        SpoilageTooltipHandler.registerFabricEvents();
        BlockSpoilageOverlayRenderer.registerFabricEvents();
        CropBonemealClientHandler.registerFabricEvents();
        SpoilageRottenTextureManager.registerFabricEvents();

        // register client networking
        etherested.spoilage.network.ModNetworking.registerFabricClient();
    }
}
*///?} else {
// Fabric client entrypoint stub — only active on Fabric
public class SpoilageFabricClient {}
//?}
