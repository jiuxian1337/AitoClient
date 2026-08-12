package cc.aito.module.impl.pvp;

import cc.aito.event.AttackEvent;
import cc.aito.event.MoveInputEvent;
import cc.aito.module.Module;
import cc.polyfrost.oneconfig.config.annotations.Exclude;
import cc.polyfrost.oneconfig.config.data.Mod;
import cc.polyfrost.oneconfig.config.data.ModType;

public class MoreKB extends Module {

    @Exclude
    private boolean stopMove;

    public MoreKB() {
        super(new Mod("MoreKB", ModType.PVP), "morekb.json");
        initialize();
    }

    @Override
    protected void onAttack(AttackEvent event) {
        stopMove = true;
    }

    @Override
    protected void onMoveInput(MoveInputEvent event) {
        if (stopMove) {
            event.forward = 0;
            event.strafe = 0;
            stopMove = false;
        }
    }
}
