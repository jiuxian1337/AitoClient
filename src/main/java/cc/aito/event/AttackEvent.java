package cc.aito.event;

import net.minecraft.entity.Entity;

public class AttackEvent {
    public final Entity target;
    public boolean cancelled;

    public AttackEvent(Entity target) {
        this.target = target;
    }
}
