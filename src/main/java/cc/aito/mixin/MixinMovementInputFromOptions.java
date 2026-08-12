package cc.aito.mixin;

import cc.aito.event.MoveInputEvent;
import cc.polyfrost.oneconfig.events.EventManager;
import net.minecraft.util.MovementInput;
import net.minecraft.util.MovementInputFromOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MovementInputFromOptions.class)
public abstract class MixinMovementInputFromOptions {

    @Inject(method = "updatePlayerMoveState", at = @At(value = "FIELD", target = "Lnet/minecraft/util/MovementInputFromOptions;sneak:Z", ordinal = 0, shift = At.Shift.AFTER))
    private void aito$onMoveInput(CallbackInfo ci) {
        MovementInput input = (MovementInput) (Object) this;
        MoveInputEvent event = new MoveInputEvent(input.moveForward, input.moveStrafe, input.jump, input.sneak);
        EventManager.INSTANCE.post(event);
        input.moveForward = event.forward;
        input.moveStrafe = event.strafe;
        input.jump = event.jump;
        input.sneak = event.sneak;
    }
}
