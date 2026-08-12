package cc.aito.module.impl.pvp;

import cc.aito.module.Module;
import cc.polyfrost.oneconfig.config.data.Mod;
import cc.polyfrost.oneconfig.config.data.ModType;
import cc.polyfrost.oneconfig.events.event.Stage;
import cc.polyfrost.oneconfig.events.event.TickEvent;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

public class NoJumpDelay extends Module {

    public NoJumpDelay() {
        super(new Mod("NoJumpDelay", ModType.PVP), "nojumpdelay.json");
        initialize();
    }

    @Override
    protected void onTick(TickEvent event) {
        if (event.stage == Stage.START && mc.thePlayer != null && mc.gameSettings.keyBindJump.isKeyDown()) {
            // Pass both MCP and SRG names: dev uses jumpTicks, production uses field_7484.
            ReflectionHelper.setPrivateValue(EntityLivingBase.class, mc.thePlayer, 0, "jumpTicks", "field_70773_bE");
        }
    }
}
