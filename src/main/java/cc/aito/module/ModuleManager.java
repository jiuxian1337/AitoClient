package cc.aito.module;

import cc.aito.event.AttackEvent;
import cc.aito.event.BlockBreakEvent;
import cc.aito.event.BlockDamageEvent;
import cc.aito.event.MoveInputEvent;
import cc.polyfrost.oneconfig.events.EventManager;
import cc.polyfrost.oneconfig.events.event.*;
import cc.polyfrost.oneconfig.libs.eventbus.Subscribe;
import cc.aito.module.impl.pvp.AimAssist;
import cc.aito.module.impl.pvp.AutoClicker;
import cc.aito.module.impl.pvp.FastPlace;
import cc.aito.module.impl.pvp.MoreKB;
import cc.aito.module.impl.pvp.Teams;
import cc.aito.module.impl.pvp.NoClickDelay;
import cc.aito.module.impl.util_qol.AutoTool;
import cc.aito.module.impl.util_qol.NoJumpDelay;
import cc.aito.module.impl.pvp.Eagle;
import cc.aito.utils.PingUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ModuleManager {

    public List<Module> modules = new ArrayList<>();

    public ModuleManager() {
        EventManager.INSTANCE.register(this);

        modules.add(new AutoTool());
        modules.add(new AutoClicker());
        modules.add(new Teams());
        modules.add(new AimAssist());
        modules.add(new MoreKB());
        modules.add(new NoJumpDelay());
        modules.add(new NoClickDelay());
        modules.add(new FastPlace());
        modules.add(new Eagle());
    }

    public List<Module> getModules() {
        return new ArrayList<>(modules);
    }

    public Module getModule(String name) {
        for (Module module : modules) {
            if (module.mod.name.equalsIgnoreCase(name)) {
                return module;
            }
        }
        return null;
    }

    public <T extends Module> T getModule(Class<T> clazz) {
        for (Module module : modules) {
            if (clazz.isInstance(module)) {
                return clazz.cast(module);
            }
        }
        return null;
    }

    /** Dispatches an event to every enabled module, over a snapshot to avoid concurrent modification.
     * Cancellation does not stop dispatch: setting {@code event.isCancelled = true} only affects
     * the underlying action, and the event is still delivered to all remaining modules. */
    private void dispatch(Consumer<Module> action) {
        for (Module module : new ArrayList<>(modules)) {
            if (module.enabled) action.accept(module);
        }
    }

    @Subscribe
    public void onStart(StartEvent event) {
        dispatch(module -> module.onStart(event));
    }

    @Subscribe
    public void onTick(TickEvent event) {
        if (event.stage == Stage.START) {
            PingUtils.tick();
        }
        dispatch(module -> module.onTick(event));
    }

    @Subscribe
    public void onRender(RenderEvent event) {
        dispatch(module -> module.onRender(event));
    }

    @Subscribe
    public void onHudRender(HudRenderEvent event) {
        dispatch(module -> module.onHudRender(event));
    }

    @Subscribe
    public void onFramebufferRender(FramebufferRenderEvent event) {
        dispatch(module -> module.onFramebufferRender(event));
    }

    @Subscribe
    public void onKeyInput(KeyInputEvent event) {
        dispatch(module -> module.onKeyInput(event));
    }

    @Subscribe
    public void onMouseInput(MouseInputEvent event) {
        dispatch(module -> module.onMouseInput(event));
    }

    @Subscribe
    public void onRawKey(RawKeyEvent event) {
        dispatch(module -> module.onRawKey(event));
    }

    @Subscribe
    public void onRawMouse(RawMouseEvent event) {
        dispatch(module -> module.onRawMouse(event));
    }

    @Subscribe
    public void onChatReceive(ChatReceiveEvent event) {
        dispatch(module -> module.onChatReceive(event));
    }

    @Subscribe
    public void onChatSend(ChatSendEvent event) {
        dispatch(module -> module.onChatSend(event));
    }

    @Subscribe
    public void onBlockDamage(BlockDamageEvent event) {
        dispatch(module -> module.onBlockDamage(event));
    }

    @Subscribe
    public void onBlockBreak(BlockBreakEvent event) {
        dispatch(module -> module.onBlockBreak(event));
    }

    @Subscribe
    public void onAttack(AttackEvent event) {
        dispatch(module -> module.onAttack(event));
    }

    @Subscribe
    public void onMoveInput(MoveInputEvent event) {
        dispatch(module -> module.onMoveInput(event));
    }

    @Subscribe
    public void onReceivePacket(ReceivePacketEvent event) {
        dispatch(module -> module.onReceivePacket(event));
    }

    @Subscribe
    public void onSendPacket(SendPacketEvent event) {
        dispatch(module -> module.onSendPacket(event));
    }

    @Subscribe
    public void onScreenOpen(ScreenOpenEvent event) {
        dispatch(module -> module.onScreenOpen(event));
    }

    @Subscribe
    public void onWorldLoad(WorldLoadEvent event) {
        dispatch(module -> module.onWorldLoad(event));
    }

    @Subscribe
    public void onLocraw(LocrawEvent event) {
        dispatch(module -> module.onLocraw(event));
    }

    @Subscribe
    public void onTimerUpdate(TimerUpdateEvent event) {
        dispatch(module -> module.onTimerUpdate(event));
    }

    @Subscribe
    public void onInitialization(InitializationEvent event) {
        dispatch(module -> module.onInitialization(event));
    }

    @Subscribe
    public void onPreShutdown(PreShutdownEvent event) {
        dispatch(module -> module.onPreShutdown(event));
    }

    @Subscribe
    public void onShutdown(ShutdownEvent event) {
        dispatch(module -> module.onShutdown(event));
    }
}
