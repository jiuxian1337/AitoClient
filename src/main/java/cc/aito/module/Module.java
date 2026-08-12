package cc.aito.module;

import cc.polyfrost.oneconfig.config.Config;
import cc.polyfrost.oneconfig.config.data.Mod;
import cc.polyfrost.oneconfig.events.EventManager;
import cc.polyfrost.oneconfig.events.event.*;
import cc.polyfrost.oneconfig.libs.eventbus.Subscribe;
import cc.aito.utils.Wrapper;

public class Module extends Config implements Wrapper {

    /**
     * @param modData    information about the mod
     * @param configFile file where config is stored
     * @param enabled    whether the mod is enabled or not
     */
    public Module(Mod modData, String configFile, boolean enabled, boolean canToggle) {
        super(modData, configFile, enabled, canToggle);
    }

    public Module(Mod modData, String configFile, boolean enabled) {
        this(modData, configFile, enabled, true);
    }

    /**
     * Convenience constructor. Modules default to disabled; use the {@link #Module(Mod, String, boolean)}
     * overload to opt in to being enabled by default.
     *
     * @param modData    information about the mod
     * @param configFile file where config is stored
     */
    public Module(Mod modData, String configFile) {
        this(modData, configFile, false);
    }

    /** Called after the game starts. */
    protected void onStart(StartEvent event) {
    }

    /** Called every game tick. Check {@code event.stage} for {@link Stage#START} or {@link Stage#END}. */
    protected void onTick(TickEvent event) {
    }

    /** Called every render frame. Check {@code event.stage} and {@code event.deltaTicks}. */
    protected void onRender(RenderEvent event) {
    }

    /** Called when the HUD is rendered. Use {@code event.matrices} and {@code event.deltaTicks}. */
    protected void onHudRender(HudRenderEvent event) {
    }

    /** Called when the framebuffer is rendered. Check {@code event.stage}. */
    protected void onFramebufferRender(FramebufferRenderEvent event) {
    }

    /** Called when a key is typed. */
    protected void onKeyInput(KeyInputEvent event) {
    }

    /** Called when a mouse button is clicked. */
    protected void onMouseInput(MouseInputEvent event) {
    }

    /** Called on every raw key event. Check {@code event.key} and {@code event.state}. */
    protected void onRawKey(RawKeyEvent event) {
    }

    /** Called on every raw mouse event. Check {@code event.button} and {@code event.state}. */
    protected void onRawMouse(RawMouseEvent event) {
    }

    /** Called when a chat message is received. Can be cancelled. */
    protected void onChatReceive(ChatReceiveEvent event) {
    }

    /** Called when a chat message is sent. Can be cancelled. */
    protected void onChatSend(ChatSendEvent event) {
    }

    /** Called when a packet is received. Can be cancelled. */
    protected void onReceivePacket(ReceivePacketEvent event) {
    }

    /** Called when a packet is sent. Can be cancelled. */
    protected void onSendPacket(SendPacketEvent event) {
    }

    /** Called when a screen is opened. Can be cancelled. */
    protected void onScreenOpen(ScreenOpenEvent event) {
    }

    /** Called when a world is loaded. */
    protected void onWorldLoad(WorldLoadEvent event) {
    }

    /** Called when the server locraw info is received. Check {@code event.info}. */
    protected void onLocraw(LocrawEvent event) {
    }

    /** Called when the timer is updated. Check {@code event.timer} and {@code event.updatedDeltaTicks}. */
    protected void onTimerUpdate(TimerUpdateEvent event) {
    }

    /** Called when OneConfig finishes initializing. */
    protected void onInitialization(InitializationEvent event) {
    }

    /** Called right before the game shuts down. */
    protected void onPreShutdown(PreShutdownEvent event) {
    }

    /** Called when the game shuts down. */
    protected void onShutdown(ShutdownEvent event) {
    }
}
