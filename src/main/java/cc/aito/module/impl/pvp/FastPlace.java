package cc.aito.module.impl.pvp;

import cc.aito.module.Module;
import cc.polyfrost.oneconfig.config.data.Mod;
import cc.polyfrost.oneconfig.config.data.ModType;
import cc.polyfrost.oneconfig.events.event.Stage;
import cc.polyfrost.oneconfig.events.event.TickEvent;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

public class FastPlace extends Module {

    public FastPlace() {
        super(new Mod("FastPlace", ModType.PVP), "fastplace.json");
        initialize();
    }

    @Override
    protected void onTick(TickEvent event) {
        if (event.stage == Stage.START && mc.gameSettings.keyBindUseItem.isKeyDown()) {
            // Pass both MCP and SRG names: dev uses rightClickDelayTimer, production uses field_2568.
            ReflectionHelper.setPrivateValue(Minecraft.class, mc, 0, "rightClickDelayTimer", "field_71467_ac");
        }
    }
}
