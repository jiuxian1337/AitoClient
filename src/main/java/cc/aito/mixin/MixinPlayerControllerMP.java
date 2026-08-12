package cc.aito.mixin;

import cc.aito.event.AttackEvent;
import cc.aito.event.BlockBreakEvent;
import cc.aito.event.BlockDamageEvent;
import cc.polyfrost.oneconfig.events.EventManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerControllerMP.class)
public abstract class MixinPlayerControllerMP {

    @Inject(method = "onPlayerDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void aito$onBlockBreak(BlockPos pos, EnumFacing side, CallbackInfoReturnable<Boolean> cir) {
        BlockBreakEvent event = new BlockBreakEvent(pos);
        EventManager.INSTANCE.post(event);
        if (event.cancelled) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "clickBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;onBlockClicked(Lnet/minecraft/world/World;Lnet/minecraft/util/BlockPos;Lnet/minecraft/entity/player/EntityPlayer;)V", shift = At.Shift.AFTER))
    private void aito$onBlockDamage(BlockPos loc, EnumFacing face, CallbackInfoReturnable<Boolean> cir) {
        EventManager.INSTANCE.post(new BlockDamageEvent(Minecraft.getMinecraft().thePlayer, loc));
    }

    @Inject(method = "attackEntity", at = @At("HEAD"), cancellable = true)
    private void aito$onAttack(EntityPlayer playerIn, Entity targetEntity, CallbackInfo ci) {
        AttackEvent event = new AttackEvent(targetEntity);
        EventManager.INSTANCE.post(event);
        if (event.cancelled) {
            ci.cancel();
        }
    }
}
