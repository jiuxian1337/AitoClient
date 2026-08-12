package cc.aito.module.impl.pvp;

import cc.aito.module.Module;
import cc.polyfrost.oneconfig.config.data.Mod;
import cc.polyfrost.oneconfig.config.data.ModType;
import cc.polyfrost.oneconfig.events.event.Stage;
import cc.polyfrost.oneconfig.events.event.TickEvent;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

public class NoClickDelay extends Module {

    public NoClickDelay() {
        super(new Mod("NoClickDelay", ModType.PVP), "noclickdelay.json");
        initialize();
    }

    @Override
    protected void onTick(TickEvent event) {
        if (event.stage == Stage.START) {
            ObfuscationReflectionHelper.setPrivateValue(Minecraft.class, mc, 0, "leftClickCounter");
        }
    }
}
