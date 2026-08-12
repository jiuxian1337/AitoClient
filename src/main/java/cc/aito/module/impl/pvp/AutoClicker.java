package cc.aito.module.impl.pvp;

import cc.polyfrost.oneconfig.config.annotations.Exclude;
import cc.polyfrost.oneconfig.config.annotations.Slider;
import cc.polyfrost.oneconfig.config.annotations.Switch;
import cc.polyfrost.oneconfig.config.data.Mod;
import cc.polyfrost.oneconfig.config.data.ModType;
import cc.polyfrost.oneconfig.events.event.RenderEvent;
import cc.polyfrost.oneconfig.events.event.Stage;
import cc.polyfrost.oneconfig.events.event.TickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import cc.aito.module.Module;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Field;
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

    @Exclude
    private static final Field LEFT_CLICK_COUNTER = ReflectionHelper.findField(Minecraft.class, "leftClickCounter", "field_71429_W");

    private int getLeftClickCounter() {
        try {
            return LEFT_CLICK_COUNTER.getInt(mc);
        } catch (IllegalAccessException e) {
            return 0;
        }
    }

    public AutoClicker() {
        super(new Mod("AutoClicker", ModType.PVP), "autoclicker.json");
        initialize();
    }

    @Override
    protected void onTick(TickEvent event) {
        if (event.stage == Stage.START && mc.thePlayer != null && mc.gameSettings.keyBindAttack.isKeyDown()) {
            entityHit = mc.objectMouseOver == null ? null : mc.objectMouseOver.entityHit;
            if (mc.currentScreen == null && !mc.thePlayer.isUsingItem() && getLeftClickCounter() <= 0) {
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
                    }
                } else {
                    for (int i = 0; i < attacks; i++) {
                        mc.thePlayer.swingItem();
                    }
                }
            }
            attacks = 0;
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
