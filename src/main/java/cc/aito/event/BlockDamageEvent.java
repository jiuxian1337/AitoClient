package cc.aito.event;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.BlockPos;

public class BlockDamageEvent {
    public final EntityPlayer player;
    public final BlockPos blockPos;

    public BlockDamageEvent(EntityPlayer player, BlockPos blockPos) {
        this.player = player;
        this.blockPos = blockPos;
    }
}
