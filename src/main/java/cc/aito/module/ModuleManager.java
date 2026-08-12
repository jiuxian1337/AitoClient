package cc.aito.module;

import cc.polyfrost.oneconfig.events.EventManager;
import cc.polyfrost.oneconfig.events.event.*;
import cc.polyfrost.oneconfig.libs.eventbus.Subscribe;
import cc.aito.module.impl.pvp.AutoClicker;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ModuleManager {

    public List<Module> modules = new ArrayList<>();

    public ModuleManager() {
        EventManager.INSTANCE.register(this);

        modules.add(new AutoClicker());
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
