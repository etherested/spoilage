package etherested.spoilage.platform;

import java.nio.file.Path;

// loader-agnostic utility methods for cross-cutting platform operations
public final class PlatformHelper {

    private PlatformHelper() {}

    public static boolean isModLoaded(String modId) {
        //? if neoforge {
        return net.neoforged.fml.ModList.get().isLoaded(modId);
        //?} else {
        /*return net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded(modId);
        *///?}
    }

    public static boolean isClient() {
        //? if neoforge {
        return net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.CLIENT;
        //?} else {
        /*return net.fabricmc.loader.api.FabricLoader.getInstance().getEnvironmentType() == net.fabricmc.api.EnvType.CLIENT;
        *///?}
    }

    public static Path getConfigDir() {
        //? if neoforge {
        return net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get();
        //?} else {
        /*return net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir();
        *///?}
    }
}
