package cc.aito.module.impl.pvp;

import cc.polyfrost.oneconfig.config.annotations.Exclude;
import cc.polyfrost.oneconfig.config.annotations.Slider;
import cc.polyfrost.oneconfig.config.annotations.Switch;
import cc.polyfrost.oneconfig.config.data.Mod;
import cc.polyfrost.oneconfig.config.data.ModType;
import cc.polyfrost.oneconfig.events.event.RenderEvent;
import cc.polyfrost.oneconfig.events.event.Stage;
import cc.polyfrost.oneconfig.events.event.TickEvent;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import cc.aito.module.Module;

import java.util.concurrent.ThreadLocalRandom;

public class AutoClicker extends Module {

    @Slider(
            name = "CPS",
            min = 0f, max = 40f,
            step = 1
    )
    public int cps = 10;

    @Slider(
            name = "NoTargetCPS",
            min = 0f, max = 40f,
            step = 1
    )
    public int noTargetCps = 10;

    @Slider(
            name = "AttackReduceTick",
            min = 0f, max = 5f,
            step = 1
    )
    public int attackReduceTick = 3;

    @Switch(
            name = "HitSelect"
    )
    public boolean hitSelect = false;

    @Exclude
    private Entity entityHit;
    @Exclude
    private long lastClick;
    @Exclude
    private int attacks;

    public AutoClicker() {
        super(new Mod("AutoClicker", ModType.PVP), "autoclicker.json");
        initialize();
        hideIf("cps", () -> hitSelect);
    }

    @Override
    protected void onTick(TickEvent event) {
        if (event.stage == Stage.START && mc.gameSettings.keyBindAttack.isKeyDown()) {
            entityHit = mc.objectMouseOver.entityHit;
            if (entityHit != null) {
                if (hitSelect && entityHit instanceof EntityPlayer) {
                    EntityPlayer player = (EntityPlayer) entityHit;
                    NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID());
                    int ping = info == null ? 0 : info.getResponseTime();
                    if (mc.thePlayer.hurtTime >= 10 - attackReduceTick || player.hurtTime <= ping / 50) {
                        mc.thePlayer.swingItem();
                        mc.playerController.attackEntity(mc.thePlayer, entityHit);
                    }

                } else {
                    for (int i = 0; i < attacks; i++) {
                        mc.thePlayer.swingItem();
                        mc.playerController.attackEntity(mc.thePlayer, entityHit);
                    }
                    attacks = 0;
                }
            } else {
                for (int i = 0; i < attacks; i++) {
                    mc.thePlayer.swingItem();
                }
                attacks = 0;
            }
        }
    }

    @Override
    protected void onRender(RenderEvent event) {
        if (mc.gameSettings.keyBindAttack.isKeyDown() && (!hitSelect || entityHit == null || !(entityHit instanceof EntityPlayer))) {
            int clickDelay;
            if (entityHit != null) {
                clickDelay = (int) (1000 / (Math.max(0, cps + ThreadLocalRandom.current().nextGaussian())));
            } else {
                clickDelay = (int) (1000 / (Math.max(0, noTargetCps + ThreadLocalRandom.current().nextGaussian())));
            }
            if (System.currentTimeMillis() - lastClick >= clickDelay) {
                attacks++;
                lastClick = System.currentTimeMillis();
            }
        }
    }
}
