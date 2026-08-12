package cc.aito.module.impl.util_qol;

import cc.aito.module.Module;
import cc.polyfrost.oneconfig.config.data.Mod;
import cc.polyfrost.oneconfig.config.data.ModType;
import cc.polyfrost.oneconfig.events.event.Stage;
import cc.polyfrost.oneconfig.events.event.TickEvent;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

public class NoJumpDelay extends Module {

    public NoJumpDelay() {
        super(new Mod("NoJumpDelay", ModType.UTIL_QOL), "nojumpdelay.json");
        initialize();
    }

    @Override
    protected void onTick(TickEvent event) {
        if (event.stage == Stage.START && mc.thePlayer != null && mc.gameSettings.keyBindJump.isKeyDown()) {
            ReflectionHelper.setPrivateValue(EntityLivingBase.class, mc.thePlayer, 0, "jumpTicks", "field_70773_bE");
        }
    }
}
