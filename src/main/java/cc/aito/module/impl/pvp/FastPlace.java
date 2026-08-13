package cc.aito.module.impl.pvp;

import cc.aito.module.Module;
import cc.polyfrost.oneconfig.config.annotations.Switch;
import cc.polyfrost.oneconfig.config.data.Mod;
import cc.polyfrost.oneconfig.config.data.ModType;
import cc.polyfrost.oneconfig.events.event.Stage;
import cc.polyfrost.oneconfig.events.event.TickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

public class FastPlace extends Module {

    @Switch(
            name = "Only Blocks"
    )
    public boolean onlyBlocks = true;

    public FastPlace() {
        super(new Mod("FastPlace", ModType.PVP), "fastplace.json");
        initialize();
    }

    @Override
    protected void onTick(TickEvent event) {
        if (event.stage == Stage.START && mc.gameSettings.keyBindUseItem.isKeyDown()) {
            if (onlyBlocks && !isHoldingBlockAndAimingAtBlock()) {
                return;
            }
            // Pass both MCP and SRG names: dev uses rightClickDelayTimer, production uses field_2568.
            ReflectionHelper.setPrivateValue(Minecraft.class, mc, 0, "rightClickDelayTimer", "field_71467_ac");
        }
    }

    private boolean isHoldingBlockAndAimingAtBlock() {
        if (mc.thePlayer == null) {
            return false;
        }
        ItemStack held = mc.thePlayer.getHeldItem();
        if (held == null || !(held.getItem() instanceof ItemBlock)) {
            return false;
        }
        return mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK;
    }
}
