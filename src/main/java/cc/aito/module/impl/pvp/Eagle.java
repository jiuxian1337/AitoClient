package cc.aito.module.impl.pvp;

import cc.aito.module.Module;
import cc.polyfrost.oneconfig.config.annotations.Exclude;
import cc.polyfrost.oneconfig.config.annotations.Slider;
import cc.polyfrost.oneconfig.config.annotations.Switch;
import cc.polyfrost.oneconfig.config.annotations.Text;
import cc.polyfrost.oneconfig.config.data.Mod;
import cc.polyfrost.oneconfig.config.data.ModType;
import cc.polyfrost.oneconfig.events.event.HudRenderEvent;
import cc.polyfrost.oneconfig.events.event.Stage;
import cc.polyfrost.oneconfig.events.event.TickEvent;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import org.lwjgl.input.Keyboard;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Auto-sneaks at the edge of a block while bridging, matching the "legit" mode of Vape's
 * Scaffold: sneak only when not moving forward, looking down, and holding a usable block.
 * No block placement / rotation (the "blatant" part).
 */
public class Eagle extends Module {

    @Slider(
            name = "Sneak Delay",
            min = 0f, max = 500f,
            step = 10 // ms to keep sneaking after leaving the edge, randomized per sneak
    )
    public int sneakDelay = 200;

    @Switch(
            name = "Require Sneak"
    )
    public boolean requireSneak = false;

    @Switch(
            name = "Pitch Check"
    )
    public boolean pitchCheck = false;

    @Slider(
            name = "Pitch",
            min = 0f, max = 90f,
            step = 1 // only sneak when looking down below this angle
    )
    public float pitchThreshold = 45f;

    @Switch(
            name = "Require Block"
    )
    public boolean requireBlock = false;

    @Switch(
            name = "Blacklist"
    )
    public boolean blacklist = true;

    @Text(
            name = "Blacklist Blocks",
            placeholder = "tnt,chest,crafting_table"
    )
    public String blacklistBlocks = "tnt,chest,crafting_table";

    @Switch(
            name = "Block Count"
    )
    public boolean blockCountDisplay = false;

    @Exclude
    private long sneakDelayMs;
    @Exclude
    private boolean sneakKeyWasDown;
    @Exclude
    private long edgeSneakStart;
    @Exclude
    private long standDelayStart;

    public Eagle() {
        super(new Mod("Eagle", ModType.PVP), "eagle.json");
        initialize();
    }

    @Override
    protected void onTick(TickEvent event) {
        if (mc.thePlayer == null || mc.currentScreen != null) return;
        if (event.stage == Stage.START) {
            onPreEntityUpdate();
        } else {
            onPostTick();
        }
    }

    /** Injects the sneak state before the world tick so the movement input sees it. */
    private void onPreEntityUpdate() {
        if (!canActivate()) return;

        KeyBinding sneakKey = mc.gameSettings.keyBindSneak;
        // Physical key state, not KeyBinding.pressed (which our own injection would pollute).
        sneakKeyWasDown = Keyboard.isKeyDown(sneakKey.getKeyCode());
        boolean shouldSneak = false;

        // Vape's LegitScaffoldMode only sneaks when NOT moving forward: while you walk
        // forward the (scaffold) placement handles the bridging, the sneak kicks in when
        // you stop at the edge. Without this gate, Eagle would block you while walking.
        float forwardInput = mc.thePlayer.movementInput.moveForward;
        boolean atEdge = forwardInput <= 0.0f;
        if (forwardInput > 0.0f) {
            atEdge = false;
        }

        // Edge detection: no ground below the player's next position -> about to walk off.
        if (atEdge && mc.thePlayer.onGround) {
            AxisAlignedBB checkBox = mc.thePlayer.getEntityBoundingBox()
                    .expand(-0.2, 0, -0.2)
                    .offset(mc.thePlayer.motionX, -1, mc.thePlayer.motionZ);
            if (mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, checkBox).isEmpty()) {
                shouldSneak = true;
            }
        }

        // Keep sneaking for a randomized delay after leaving the edge, so it doesn't flicker.
        boolean skipTimerReset = false;
        if (!shouldSneak && System.currentTimeMillis() - edgeSneakStart < sneakDelayMs && sneakDelayMs > 30) {
            shouldSneak = true;
            skipTimerReset = true;
        }

        if (mc.thePlayer.onGround) {
            if (shouldSneak) {
                if (!mc.thePlayer.isSneaking()) {
                    // Vape's RandomValue: uniform in [defaultMin, defaultMax] = [sneakDelay/2, sneakDelay]
                    sneakDelayMs = sneakDelay / 2 + (long) (ThreadLocalRandom.current().nextDouble() * (sneakDelay / 2.0));
                }
                setSneak(true);
                standDelayStart = System.currentTimeMillis();
                if (!skipTimerReset) {
                    edgeSneakStart = System.currentTimeMillis();
                }
            } else if (requireSneak) {
                // Vape's stand-delay rhythm: after leaving the edge, keep sneak released for 1s while backing up.
                if (System.currentTimeMillis() - standDelayStart < 1000L && forwardInput < 0.0f) {
                    setSneak(false);
                }
            } else if (!sneakKeyWasDown) {
                setSneak(false);
            }
        }
    }

    /** Restores the player's physical sneak state after the world tick. */
    private void onPostTick() {
        if (!canActivate()) return;
        setSneak(sneakKeyWasDown);
    }

    private boolean canActivate() {
        if (mc.thePlayer.isInWater()) return false;
        if (requireSneak && !Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode())) return false;
        if (pitchCheck && mc.thePlayer.rotationPitch < pitchThreshold) return false;
        if (requireBlock && !isHoldingBlock()) return false;
        if (blacklist && isBlacklisted(mc.thePlayer.getHeldItem())) return false;
        return true;
    }

    private boolean isHoldingBlock() {
        ItemStack held = mc.thePlayer.getHeldItem();
        return held != null && held.getItem() instanceof ItemBlock;
    }

    private boolean isBlacklisted(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return false;
        String name = Item.itemRegistry.getNameForObject(stack.getItem()).toString(); // e.g. "minecraft:tnt"
        String shortName = name.contains(":") ? name.substring(name.lastIndexOf(':') + 1) : name;
        for (String block : blacklistBlocks.split(",")) {
            String trimmed = block.trim();
            if (!trimmed.isEmpty() && trimmed.equalsIgnoreCase(shortName)) {
                return true;
            }
        }
        return false;
    }

    private int countBlocksInHotbar() {
        int count = 0;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.thePlayer.inventory.mainInventory[i];
            if (stack != null && stack.getItem() instanceof ItemBlock) {
                count += stack.stackSize;
            }
        }
        return count;
    }

    @Override
    protected void onHudRender(HudRenderEvent event) {
        if (blockCountDisplay && mc.thePlayer != null) {
            String text = "Blocks: " + countBlocksInHotbar();
            ScaledResolution sr = new ScaledResolution(mc);
            float x = (sr.getScaledWidth() - mc.fontRendererObj.getStringWidth(text)) / 2.0f;
            mc.fontRendererObj.drawStringWithShadow(text, x, sr.getScaledHeight() / 2.0f, 0xFFFFFFFF);
        }
    }

    private void setSneak(boolean pressed) {
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), pressed);
    }
}
