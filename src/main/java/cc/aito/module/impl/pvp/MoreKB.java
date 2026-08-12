package cc.aito.module.impl.pvp;

import cc.aito.event.AttackEvent;
import cc.aito.event.MoveInputEvent;
import cc.aito.module.Module;
import cc.polyfrost.oneconfig.config.annotations.Exclude;
import cc.polyfrost.oneconfig.config.annotations.Slider;
import cc.polyfrost.oneconfig.config.data.Mod;
import cc.polyfrost.oneconfig.config.data.ModType;

public class MoreKB extends Module {

    @Slider(
            name = "Delay",
            min = 0f, max = 500f,
            step = 1
    )
    public int delay = 75;

    @Slider(
            name = "HurtTime",
            min = 0f, max = 10f,
            step = 1
    )
    public int hurtTime = 9;

    @Exclude
    private boolean stopMove;
    @Exclude
    private long lastStop;

    public MoreKB() {
        super(new Mod("MoreKB", ModType.PVP), "morekb.json");
        initialize();
    }

    @Override
    protected void onAttack(AttackEvent event) {
        if (mc.thePlayer.hurtTime < hurtTime) {
            stopMove = true;
        } else {
            stopMove = false;
        }
    }

    @Override
    protected void onMoveInput(MoveInputEvent event) {
        if (stopMove) {
            stopMove = false;
            if (System.currentTimeMillis() - lastStop > delay) {
                lastStop = System.currentTimeMillis();
                event.forward = 0;
                event.strafe = 0;
            }
        }
    }
}
