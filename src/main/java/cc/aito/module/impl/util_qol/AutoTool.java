package cc.aito.module.impl.util_qol;

import cc.aito.event.BlockDamageEvent;
import cc.aito.module.Module;
import cc.aito.utils.SlotUtil;
import cc.polyfrost.oneconfig.config.annotations.Exclude;
import cc.polyfrost.oneconfig.config.data.Mod;
import cc.polyfrost.oneconfig.config.data.ModType;
import cc.polyfrost.oneconfig.events.event.Stage;
import cc.polyfrost.oneconfig.events.event.TickEvent;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;

public class AutoTool extends Module {
    @Exclude
    private BlockPos blockPos;
    @Exclude
    private int lastSlot = -1;

    public AutoTool() {
        super(new Mod("AutoTool", ModType.UTIL_QOL), "autotool.json");
        initialize();
    }

    @Override
    protected void onBlockDamage(BlockDamageEvent event) {
        if (event.player != mc.thePlayer || mc.thePlayer.getDistanceSq(event.blockPos.getX(), event.blockPos.getY(), event.blockPos.getZ()) > 25) {
            return;
        }
        blockPos = event.blockPos;
        update();
    }

    @Override
    protected void onTick(TickEvent event) {
        if (event.stage == Stage.START) {
            update();
        }
    }

    private void update() {
        if (mc.objectMouseOver == null || mc.objectMouseOver.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK || !mc.gameSettings.keyBindAttack.isKeyDown()) {
            if (lastSlot != -1) {
                mc.thePlayer.inventory.currentItem = lastSlot;
                lastSlot = -1;
            }
            return;
        }
        int index = SlotUtil.findTool(blockPos);
        if (index != -1 && mc.thePlayer.inventory.currentItem != index) {
            lastSlot = mc.thePlayer.inventory.currentItem;
            mc.thePlayer.inventory.currentItem = index;
        }
    }
}
