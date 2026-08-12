package cc.aito.mixin;

import cc.aito.event.BlockDamageEvent;
import cc.polyfrost.oneconfig.events.EventManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderGlobal.class)
public abstract class MixinRenderGlobal {

    @Inject(method = "sendBlockBreakProgress", at = @At("HEAD"))
    private void aito$onBlockProgress(int breakerId, BlockPos pos, int progress, CallbackInfo ci) {
        Minecraft mc = Minecraft.getMinecraft();
        if (progress >= 0 && progress < 10 && mc.objectMouseOver != null
                && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK
                && mc.objectMouseOver.getBlockPos().equals(pos)) {
            EventManager.INSTANCE.post(new BlockDamageEvent(mc.thePlayer, pos));
        }
    }
}
