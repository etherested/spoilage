package etherested.spoilage.client;

//? if fabric {
/*import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        if (SpoilageConfigScreen.isAvailable()) {
            return SpoilageConfigScreen::create;
        }
        return parent -> null;
    }
}
*///?} else {
// ModMenu integration stub - only active on Fabric
public class ModMenuIntegration {}
//?}
