package cc.aito;

import cc.polyfrost.oneconfig.events.event.InitializationEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import cc.aito.module.ModuleManager;

/**
 * The entrypoint of AitoClient that initializes it.
 *
 * @see Mod
 * @see InitializationEvent
 */
@Mod(modid = AitoClient.MODID, name = AitoClient.NAME, version = AitoClient.VERSION)
public class AitoClient {

    // Sets the variables from `gradle.properties`. See the `blossom` config in `build.gradle.kts`.
    public static final String MODID = "@ID@";
    public static final String NAME = "@NAME@";
    public static final String VERSION = "@VER@";
    @Mod.Instance(MODID)
    public static AitoClient INSTANCE; // Adds the instance of the mod, so we can access other variables.

    public ModuleManager moduleManager;

    // Register the config and commands.
    @Mod.EventHandler
    public void onInit(FMLInitializationEvent event) {
        moduleManager = new ModuleManager();
    }
}
