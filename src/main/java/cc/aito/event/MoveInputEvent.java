package cc.aito.event;

public class MoveInputEvent {
    public float forward;
    public float strafe;
    public boolean jump;
    public boolean sneak;

    public MoveInputEvent(float forward, float strafe, boolean jump, boolean sneak) {
        this.forward = forward;
        this.strafe = strafe;
        this.jump = jump;
        this.sneak = sneak;
    }
}
