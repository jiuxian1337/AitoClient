package cc.aito.event;

import net.minecraft.util.BlockPos;

public class BlockBreakEvent {
    public final BlockPos blockPos;
    public boolean cancelled;

    public BlockBreakEvent(BlockPos blockPos) {
        this.blockPos = blockPos;
    }
}
