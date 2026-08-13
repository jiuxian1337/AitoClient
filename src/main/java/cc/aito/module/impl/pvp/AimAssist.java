package cc.aito.module.impl.pvp;

import cc.aito.module.Module;
import cc.aito.utils.RotationUtil;
import cc.aito.utils.Vec3d;
import cc.polyfrost.oneconfig.config.annotations.Dropdown;
import cc.polyfrost.oneconfig.config.annotations.Exclude;
import cc.polyfrost.oneconfig.config.annotations.Slider;
import cc.polyfrost.oneconfig.config.annotations.Switch;
import cc.polyfrost.oneconfig.config.annotations.Text;
import cc.polyfrost.oneconfig.config.data.Mod;
import cc.polyfrost.oneconfig.config.data.ModType;
import cc.polyfrost.oneconfig.events.event.RenderEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.EntityAmbientCreature;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.potion.Potion;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Timer;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AimAssist extends Module {

    @Dropdown(
            name = "Mode",
            options = {"Simple", "Adaptive"}
    )
    public int mode = 0;

    @Switch(
            name = "Require Mouse Down"
    )
    public boolean requireMouseDown = true;

    @Switch(
            name = "Aim Vertically"
    )
    public boolean aimVertically = false;

    @Switch(
            name = "Strafe Increase"
    )
    public boolean strafeIncrease = false;

    @Switch(
            name = "Check Block Break"
    )
    public boolean checkBlockBreak = false;

    @Switch(
            name = "Break Blocks Whitelist"
    )
    public boolean breakBlocksWhitelist = false;

    @Text(
            name = "Break Block Items",
            placeholder = "pickaxe,shovel"
    )
    public String blockBreakItems = "pickaxe,shovel";

    @Switch(
            name = "Limit to Items"
    )
    public boolean limitToItems = false;

    @Text(
            name = "Allowed Items",
            placeholder = "sword,axe"
    )
    public String allowedItems = "sword";

    @Slider(
            name = "Vertical Speed",
            min = 1f, max = 10f,
            step = 1
    )
    public float verticalSpeed = 5.0F;

    @Slider(
            name = "Horizontal Speed",
            min = 1f, max = 10f,
            step = 1
    )
    public float horizontalSpeed = 5.0F;

    @Slider(
            name = "Max Angle",
            min = 1f, max = 360f,
            step = 1
    )
    public int maxAngle = 180;

    @Slider(
            name = "Distance",
            min = 1f, max = 8f,
            step = 1
    )
    public float distance = 5.0F;

    @Dropdown(
            name = "Target Mode",
            options = {"Yaw", "Distance", "Armor", "Threat", "Health"}
    )
    public int targetMode = 0;

    @Dropdown(
            name = "Target Area",
            options = {"Center", "Closest"}
    )
    public int targetArea = 0;

    @Switch(
            name = "Target Players"
    )
    public boolean targetPlayers = true;

    @Switch(
            name = "Target Mobs"
    )
    public boolean targetMobs = true;

    @Switch(
            name = "Target Peaceful"
    )
    public boolean targetPeaceful = false;

    @Switch(
            name = "Ignore Invisible"
    )
    public boolean ignoreInvisible = false;

    @Switch(
            name = "Ignore Behind Walls"
    )
    public boolean ignoreBehindWalls = false;

    @Switch(
            name = "Ignore Naked"
    )
    public boolean ignoreNaked = false;

    @Exclude
    private long blockBreakCooldown;
    @Exclude
    private boolean threadStarted;
    @Exclude
    private final SimpleRotation simple = new SimpleRotation();
    @Exclude
    private final AdaptiveRotation adaptive = new AdaptiveRotation();
    @Exclude
    private static final Field TIMER = ReflectionHelper.findField(Minecraft.class, "timer", "field_71428_T");

    private float getPartialTicks() {
        try {
            return ((Timer) TIMER.get(mc)).renderPartialTicks;
        } catch (IllegalAccessException e) {
            return 0.0F;
        }
    }

    public AimAssist() {
        super(new Mod("AimAssist", ModType.PVP), "aimassist.json");
        initialize();
        hideIf("verticalSpeed", () -> !aimVertically);
        hideIf("breakBlocksWhitelist", () -> !checkBlockBreak);
        hideIf("blockBreakItems", () -> !(checkBlockBreak && breakBlocksWhitelist));
        hideIf("allowedItems", () -> !limitToItems);
    }

    @Override
    protected void onRender(RenderEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) {
            return;
        }
        ensureThread();
        if (mode == 0) {
            simple.onRender();
        } else {
            adaptive.onRender();
        }
    }

    private void ensureThread() {
        if (threadStarted) {
            return;
        }
        threadStarted = true;
        Thread worker = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(1L);
                    if (enabled && mc.thePlayer != null && mc.theWorld != null) {
                        if (mode == 0) {
                            simple.tick();
                        } else {
                            adaptive.tick();
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }, "AimAssist-Worker");
        worker.setDaemon(true);
        worker.start();
    }

    private EntityLivingBase findBestTarget() {
        List<EntityLivingBase> targets = new ArrayList<>();
        for (Object entityObject : new ArrayList<>(mc.theWorld.loadedEntityList)) {
            Entity entity = (Entity) entityObject;
            if (!(entity instanceof EntityLivingBase)) {
                continue;
            }
            EntityLivingBase living = (EntityLivingBase) entity;
            if (!isValidTarget(living)) {
                continue;
            }
            targets.add(living);
        }
        if (targets.isEmpty()) {
            return null;
        }
        switch (targetMode) {
            case 1:
                targets.sort((a, b) -> Float.compare(mc.thePlayer.getDistanceToEntity(a), mc.thePlayer.getDistanceToEntity(b)));
                break;
            case 2:
                targets.sort((a, b) -> Double.compare(getEquipmentValue(a), getEquipmentValue(b)));
                break;
            case 3:
                targets.sort((a, b) -> Float.compare(getThreatValue(a), getThreatValue(b)));
                break;
            case 4:
                targets.sort((a, b) -> Float.compare(a.getHealth(), b.getHealth()));
                break;
            default:
                targets.sort((a, b) -> Integer.compare(RotationUtil.getYawDifference(mc.thePlayer, a), RotationUtil.getYawDifference(mc.thePlayer, b)));
                break;
        }
        return targets.get(0);
    }

    private boolean isValidTarget(EntityLivingBase target) {
        if (target == null) {
            return false;
        }
        if (target == mc.thePlayer) {
            return false;
        }
        if (target.getHealth() <= 0.0F || target.isDead) {
            return false;
        }
        if (mc.thePlayer.getDistanceToEntity(target) >= distance) {
            return false;
        }
        if (RotationUtil.getYawDifference(mc.thePlayer, target) > maxAngle / 2.0) {
            return false;
        }
        if (target == mc.thePlayer.ridingEntity) {
            return false;
        }
        return passesItemFilter(target);
    }

    private boolean passesItemFilter(EntityLivingBase target) {
        if (limitToItems && !isAllowedHeldItem()) {
            return false;
        }
        return isTargetFilterValid(target);
    }

    private boolean isTargetFilterValid(EntityLivingBase target) {
        if (target == mc.thePlayer) {
            return false;
        }
        if (ignoreInvisible && RotationUtil.isInvisible(target)) {
            return false;
        }
        if (ignoreBehindWalls && !mc.thePlayer.canEntityBeSeen(target)) {
            return false;
        }
        boolean isPlayer = target instanceof EntityPlayer;
        boolean isMob = target instanceof IMob || target instanceof EntityMob;
        boolean isPeaceful = (target instanceof EntityAnimal || target instanceof EntityAmbientCreature) && !isMob;
        if (isPlayer) {
            if (!targetPlayers) {
                return false;
            }
            if (Teams.isSameTeam(mc.thePlayer, (EntityPlayer) target)) {
                return false;
            }
            if (ignoreNaked && isNaked(target)) {
                return false;
            }
            return true;
        }
        if (isMob && !targetMobs) {
            return false;
        }
        if (isPeaceful && !targetPeaceful) {
            return false;
        }
        return isMob || isPeaceful;
    }

    private boolean isNaked(EntityLivingBase target) {
        if (!(target instanceof EntityPlayer)) {
            return false;
        }
        EntityPlayer player = (EntityPlayer) target;
        if (player.getHeldItem() != null) {
            return false;
        }
        for (int i = 0; i < 4; i++) {
            if (player.getCurrentArmor(i) != null) {
                return false;
            }
        }
        return true;
    }

    private float getThreatValue(EntityLivingBase target) {
        float value = 0.0F;
        if (target instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) target;
            for (int i = 0; i < 4; i++) {
                ItemStack stack = player.getCurrentArmor(i);
                if (stack != null && stack.getItem() instanceof ItemArmor) {
                    value += ((ItemArmor) stack.getItem()).damageReduceAmount;
                }
            }
            if (player.isPotionActive(Potion.damageBoost)) {
                value *= 1.375F * (player.getActivePotionEffect(Potion.damageBoost).getAmplifier() + 1);
            }
        }
        return value;
    }

    private double getEquipmentValue(EntityLivingBase target) {
        double value = 0.0D;
        if (target instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) target;
            for (int i = 0; i < 4; i++) {
                ItemStack stack = player.getCurrentArmor(i);
                if (stack != null && stack.getItem() instanceof ItemArmor) {
                    value += ((ItemArmor) stack.getItem()).damageReduceAmount * 4.0D;
                }
            }
            ItemStack held = player.getHeldItem();
            if (held != null && held.getItem() instanceof ItemSword) {
                value += ((ItemSword) held.getItem()).getDamageVsEntity();
            }
        }
        return value;
    }

    private boolean hasRequiredItem() {
        if (!limitToItems) {
            return true;
        }
        return isAllowedHeldItem();
    }

    private boolean isAllowedHeldItem() {
        ItemStack held = mc.thePlayer.getHeldItem();
        if (held == null) {
            return false;
        }
        String shortName = getItemShortName(held.getItem());
        for (String item : allowedItems.split(",")) {
            String trimmed = item.trim();
            if (!trimmed.isEmpty() && shortName.contains(trimmed)) {
                return true;
            }
        }
        return false;
    }

    private boolean isBlockBreakItemHeld() {
        ItemStack held = mc.thePlayer.getHeldItem();
        if (held == null) {
            return false;
        }
        String shortName = getItemShortName(held.getItem());
        for (String item : blockBreakItems.split(",")) {
            String trimmed = item.trim();
            if (!trimmed.isEmpty() && shortName.contains(trimmed)) {
                return true;
            }
        }
        return false;
    }

    private String getItemShortName(Item item) {
        String name = Item.itemRegistry.getNameForObject(item).toString();
        return name.contains(":") ? name.substring(name.lastIndexOf(':') + 1) : name;
    }

    private boolean canAim() {
        if (mc.thePlayer == null || mc.playerController == null) {
            return false;
        }
        boolean checkCurrentBlock = checkBlockBreak;
        if (checkCurrentBlock && breakBlocksWhitelist) {
            checkCurrentBlock = isBlockBreakItemHeld();
        }
        if (checkCurrentBlock) {
            boolean aimingAtBlock = mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK;
            if (aimingAtBlock) {
                blockBreakCooldown = System.currentTimeMillis() + 250;
                return false;
            }
            if (System.currentTimeMillis() < blockBreakCooldown) {
                return false;
            }
        }
        return hasRequiredItem();
    }

    private void applyTrackedMouseDelta(float yawDelta, float pitchDelta) {
        EntityPlayerSP player = mc.thePlayer;
        float trackedYaw = player.rotationYaw;
        float trackedPitch = player.rotationPitch;
        player.rotationYaw = trackedYaw + yawDelta * 0.15F;
        player.rotationPitch = trackedPitch - pitchDelta * 0.15F;
        if (player.rotationPitch < -90.0F) {
            player.rotationPitch = -90.0F;
        }
        if (player.rotationPitch > 90.0F) {
            player.rotationPitch = 90.0F;
        }
        player.prevRotationPitch = player.prevRotationPitch + player.rotationPitch - trackedPitch;
        player.prevRotationYaw = player.prevRotationYaw + player.rotationYaw - trackedYaw;
        player.rotationYawHead = player.rotationYaw;
    }

    private class SimpleRotation {
        private final Random random = new Random();
        private final Random sharedRandom = new Random();
        private EntityLivingBase target;
        private float horizontalMouseAccumulator;
        private float verticalMouseAccumulator;
        private float horizontalVelocity;
        private float verticalVelocity;
        private float horizontalVelocityBuffer;
        private float verticalVelocityBuffer;
        private float yawBoost;
        private float pitchBoost;
        private double lastAngleDiff;
        private double driftTimer;
        private int driftX;
        private int driftY;
        private int randomOffsetX;
        private int randomOffsetY;
        private int swapTickCounter;
        private int sampleCounter;
        private int retargetCounter;
        private boolean prevOnLeft;
        private boolean prevAbove;
        private boolean farFromTarget;
        private double targetX;
        private double targetY;
        private double targetZ;
        private double prevTargetX;
        private double prevTargetZ;

        private void onRender() {
            if (target == null) {
                return;
            }
            horizontalMouseAccumulator += driftX;
            verticalMouseAccumulator += driftY;
            int horizontalMouseSteps = (int) horizontalMouseAccumulator;
            int verticalMouseSteps = (int) verticalMouseAccumulator;
            float remainingHorizontalDelta = horizontalMouseAccumulator - horizontalMouseSteps;
            float remainingVerticalDelta = verticalMouseAccumulator - verticalMouseSteps;
            float sensitivity = mc.gameSettings.mouseSensitivity;
            float sensitivityBase = sensitivity * 0.6F + 0.2F;
            float sensitivityScale = sensitivityBase * sensitivityBase * sensitivityBase * 8.0F;
            float horizontalDelta = horizontalMouseSteps * sensitivityScale;
            float verticalDelta = verticalMouseSteps * sensitivityScale;
            applyTrackedMouseDelta(horizontalDelta, -verticalDelta);
            horizontalMouseAccumulator = remainingHorizontalDelta;
            verticalMouseAccumulator = remainingVerticalDelta;
            driftX = 0;
            driftY = 0;
        }

        private void tick() {
            if (mc.theWorld == null || mc.thePlayer == null) {
                return;
            }
            if (!canAim()) {
                resetRotationState();
                return;
            }
            if (target != null && target.isDead) {
                target = null;
            }
            if (requireMouseDown && !mc.gameSettings.keyBindAttack.isKeyDown()) {
                target = null;
                resetRotationState();
                return;
            }
            if (target != null && (RotationUtil.isDead(target) || mc.thePlayer.getDistanceToEntity(target) > distance)) {
                resetRotationState();
                target = null;
            }
            if (requireMouseDown && mc.gameSettings.keyBindAttack.isKeyDown() && target == null || !requireMouseDown) {
                EntityLivingBase candidateTarget = findBestTarget();
                if (!requireMouseDown) {
                    ++retargetCounter;
                    if (retargetCounter > 700 || target == null || !isValidTarget(target)) {
                        target = candidateTarget;
                        retargetCounter = 0;
                    }
                } else {
                    target = candidateTarget;
                }
            }
            if (mc.theWorld == null) {
                return;
            }
            if (target != null && mc.currentScreen == null) {
                updateVelocityBuffers();
                applyRotation();
            } else {
                resetRotationState();
            }
        }

        private void updateVelocityBuffers() {
            ++swapTickCounter;
            if (swapTickCounter > 10) {
                verticalVelocityBuffer = verticalVelocity;
                horizontalVelocity = horizontalVelocityBuffer;
                horizontalVelocityBuffer = 0.0F;
                verticalVelocity = 0.0F;
                swapTickCounter = 0;
            }
        }

        private void applyRotation() {
            updateDrift();
            targetX = target.posX;
            targetY = target.posY;
            targetZ = target.posZ;
            if (targetArea == 1) {
                computeTargetOffset();
            }

            double targetMotionX = targetX - prevTargetX;
            double targetMotionZ = targetZ - prevTargetZ;
            prevTargetX = targetX;
            prevTargetZ = targetZ;

            EntityPlayerSP player = mc.thePlayer;
            float viewYaw = player.rotationYaw;
            double predictionFactor = 1.7;
            double predictedTargetX = targetX + targetMotionX * predictionFactor;
            double predictedTargetZ = targetZ + targetMotionZ * predictionFactor;
            double horizontalAngleDifference = RotationUtil.getHorizontalAngle(player.posX, player.posZ, viewYaw, predictedTargetX, predictedTargetZ);
            boolean targetOnLeft = RotationUtil.isOnLeft(player.posX, player.posZ, viewYaw, predictedTargetX, predictedTargetZ);
            int verticalAngleDifference = RotationUtil.getVerticalAngle(player, targetX, targetY, targetZ);
            boolean targetAbove = verticalAngleDifference < 0;
            int verticalDeadZoneDistance = Math.abs(verticalAngleDifference) - 10;

            float horizontalForce = 1.0F;
            float verticalForce = 1.0F;
            horizontalForce += (float) RotationUtil.randomRange(sharedRandom, 0.0, 2.0);
            horizontalForce += (float) (horizontalAngleDifference / 50.0);
            verticalForce += (float) RotationUtil.randomRange(sharedRandom, 0.0, 2.0);
            verticalForce += (float) Math.abs(verticalDeadZoneDistance) / 50.0F;
            if (Math.abs(horizontalAngleDifference - lastAngleDiff) > 6.0) {
                horizontalForce += (float) (horizontalAngleDifference / 35.0);
            }
            double proximityBoost = Math.max(0.0, (9.0F - player.getDistanceToEntity(target)) / 2.5F - 2.0F);
            horizontalForce += (float) proximityBoost;

            float strafeInput = player.movementInput.moveStrafe;
            boolean strafingAway = targetOnLeft ? strafeInput < 0.0F : strafeInput > 0.0F;
            if (strafeIncrease && strafingAway) {
                horizontalForce *= 1.6F;
            }
            if (player.getDistanceToEntity(target) < 0.5F) {
                horizontalForce /= 5.0F;
            }

            float horizontalAcceleration = horizontalForce / 90.0F * (targetOnLeft ? -1.0F : 1.0F);
            float verticalAcceleration = verticalForce / 90.0F * (targetAbove ? 1.0F : -1.0F);
            if (horizontalAngleDifference < 5.0) {
                horizontalAcceleration = 0.0F;
                horizontalVelocity *= 0.7F;
                boolean strafingToward = targetOnLeft ? strafeInput > 0.0F : strafeInput < 0.0F;
                if (strafingToward) {
                    horizontalVelocity *= 0.5F;
                }
            }
            if (targetOnLeft != prevOnLeft) {
                horizontalVelocity = -horizontalVelocity;
                horizontalVelocityBuffer = -horizontalVelocityBuffer;
                horizontalMouseAccumulator = 0.0F;
            }
            if (targetAbove != prevAbove) {
                verticalVelocityBuffer = -verticalVelocityBuffer;
                verticalVelocity = -verticalVelocity;
                verticalMouseAccumulator = 0.0F;
            }
            if (verticalDeadZoneDistance < 5) {
                verticalAcceleration = 0.0F;
                verticalVelocityBuffer *= 0.7F;
            }

            horizontalVelocityBuffer += horizontalAcceleration;
            verticalVelocity += verticalAcceleration;
            float smoothedHorizontalVelocity = horizontalVelocity;
            float smoothedVerticalVelocity = verticalVelocityBuffer;
            if (Math.abs(smoothedHorizontalVelocity) > 10.0F) {
                horizontalVelocityBuffer = 0.0F;
                horizontalVelocity = 0.0F;
                return;
            }

            float horizontalAdjustment = smoothedHorizontalVelocity * 0.15F;
            if (horizontalAngleDifference <= 9.0) {
                horizontalAdjustment = (float) (horizontalAdjustment / (10.0 - horizontalAngleDifference));
            }
            farFromTarget = horizontalAngleDifference > 5.0;
            if (Float.isNaN(horizontalAdjustment)) {
                horizontalVelocityBuffer = 0.0F;
                horizontalVelocity = 0.0F;
                return;
            }
            queueHorizontalAdjustment(horizontalAdjustment);

            if (aimVertically) {
                float verticalAdjustment = (float) (smoothedVerticalVelocity * 0.15);
                if (Float.isNaN(verticalAdjustment)) {
                    verticalVelocity = 0.0F;
                    verticalVelocityBuffer = 0.0F;
                    return;
                }
                queueVerticalAdjustment(verticalAdjustment, verticalAngleDifference);
            }
            prevAbove = targetAbove;
            prevOnLeft = targetOnLeft;
            ++sampleCounter;
            if (sampleCounter > 10) {
                lastAngleDiff = horizontalAngleDifference;
                sampleCounter = 0;
            }
        }

        private void computeTargetOffset() {
            Vec3d closestPoint = RotationUtil.getClosestPoint(mc.thePlayer, target.getEntityBoundingBox(), 0.0, 0.0, 0.0);
            double targetMotionX = target.posX - target.prevPosX;
            double targetMotionZ = target.posZ - target.prevPosZ;
            double previousClosestX = closestPoint.x - targetMotionX;
            double previousClosestZ = closestPoint.z - targetMotionZ;
            float partialTicks = getPartialTicks();
            targetX = previousClosestX + (closestPoint.x - previousClosestX) * partialTicks;
            targetZ = previousClosestZ + (closestPoint.z - previousClosestZ) * partialTicks;
        }

        private void updateDrift() {
            driftTimer += 1.0;
            if (driftTimer >= 250 + random.nextInt(50)) {
                driftTimer = RotationUtil.randomExclusiveUpper(random, -100, -50);
                randomOffsetX = RotationUtil.randomExclusiveUpper(random, -1, 2);
                randomOffsetY = RotationUtil.randomExclusiveUpper(random, -1, 2);
            }
            int horizontalStep = randomOffsetX;
            int verticalStep = randomOffsetY;
            if (random.nextInt(10) < 2) {
            }
            if (random.nextInt(10) < 2) {
            }
            if (random.nextInt(10) < 2) {
                horizontalStep = 0;
            }
            if (random.nextInt(10) < 2) {
                verticalStep = 0;
            }
            if (driftTimer < 0.0) {
                horizontalStep = 0;
                verticalStep = 0;
            }
            if (random.nextInt(20) == 1) {
                driftX += horizontalStep;
                driftY += verticalStep;
            }
            if (horizontalMouseAccumulator > 0.0F && driftX < 0 || horizontalMouseAccumulator < 0.0F && driftX > 0) {
                driftX = 0;
            }
        }

        private void queueHorizontalAdjustment(float adjustment) {
            if (adjustment != 0.0F) {
                adjustment *= 5.0F;
                float speed = horizontalSpeed;
                float angleDifference = RotationUtil.getYawDifference(mc.thePlayer, target);
                if (angleDifference <= 10.0F) {
                    pitchBoost = speed;
                }
                if (pitchBoost > 0.0F) {
                    speed -= pitchBoost / 3.0F;
                    pitchBoost -= angleDifference / 200.0F;
                }
                horizontalMouseAccumulator += speed * adjustment;
            } else {
                horizontalMouseAccumulator = 0.0F;
            }
        }

        private void queueVerticalAdjustment(float adjustment, float angleDifference) {
            if (adjustment != 0.0F) {
                adjustment *= 5.0F;
                float speed = verticalSpeed;
                float absoluteAngleDifference = Math.abs(angleDifference);
                if (absoluteAngleDifference <= 10.0F) {
                    yawBoost = speed;
                }
                if (yawBoost > 0.0F) {
                    speed -= yawBoost / 3.0F;
                    yawBoost -= absoluteAngleDifference / 200.0F;
                }
                verticalMouseAccumulator += speed * adjustment;
            } else {
                verticalMouseAccumulator = 0.0F;
            }
        }

        private void resetRotationState() {
            horizontalMouseAccumulator = 0.0F;
            verticalMouseAccumulator = 0.0F;
            randomOffsetX = 0;
            randomOffsetY = 0;
            driftX = 0;
            driftY = 0;
        }
    }

    private class AdaptiveRotation {
        private final float SNAP_ENTER_THRESHOLD = 5.0F;
        private final float SNAP_EXIT_THRESHOLD = 7.0F;
        private final Random random = new Random();
        private EntityLivingBase target;
        private float pendingYaw;
        private float pendingPitch;
        private boolean yawSnapped;
        private boolean pitchSnapped;
        private boolean initialized;
        private long lastFrameNanos;
        private double aimX;
        private double aimY;
        private double aimZ;
        private double leadX;
        private double leadY;
        private double leadZ;
        private boolean aimPointInitialized;
        private double predictedX;
        private double predictedY;
        private double predictedZ;
        private boolean predictionInitialized;
        private float aimStrength;
        private float yawAccel;
        private float pitchAccel;
        private float yawBias;
        private float pitchBias;
        private float yawVelocity;
        private float pitchVelocity;
        private float lastYawDiff;
        private float lastPitchDiff;
        private float lastTargetYaw;
        private float lastTargetPitch;
        private float lastPlayerYaw;
        private float lastPlayerPitch;
        private float lastYawSign;
        private float lastPitchSign;
        private float yawFlickTicks;
        private float pitchFlickTicks;
        private float overshoot;
        private double lastEyeY;
        private double lastGroundEyeY;
        private float verticalVelocity;
        private float airFactor;
        private int targetSwitchTicks;
        private long noiseStartNanos;
        private float driftPos;
        private float driftVelocity;
        private float driftTarget;
        private float driftNoise;
        private long driftNextNanos;

        private void onRender() {
            if (mc.theWorld == null || target == null) {
                return;
            }
            boolean yawSnapEnabled = horizontalSpeed > 20.0F;
            boolean pitchSnapEnabled = aimVertically && verticalSpeed > 20.0F;
            boolean snapYaw = yawSnapEnabled && yawSnapped;
            boolean snapPitch = pitchSnapEnabled && pitchSnapped;
            int yawSteps = snapYaw ? Math.round(pendingYaw) : (int) pendingYaw;
            int pitchSteps = snapPitch ? Math.round(pendingPitch) : (int) pendingPitch;
            float remainingYaw = snapYaw ? 0.0F : pendingYaw - yawSteps;
            float remainingPitch = snapPitch ? 0.0F : pendingPitch - pitchSteps;
            float sensitivity = mc.gameSettings.mouseSensitivity;
            float sensitivityBase = sensitivity * 0.6F + 0.2F;
            float sensitivityScale = sensitivityBase * sensitivityBase * sensitivityBase * 8.0F;
            float mouseDeltaX = yawSteps * sensitivityScale;
            float mouseDeltaY = -(float) pitchSteps * sensitivityScale;
            if ((snapYaw || snapPitch) && mc.thePlayer != null) {
                EntityPlayerSP player = mc.thePlayer;
                double[] snapPoint = resolveSnapPoint(player, target);
                double deltaX = snapPoint[0] - player.posX;
                double deltaZ = snapPoint[2] - player.posZ;
                double deltaY = snapPoint[1] - (player.posY + player.getEyeHeight());
                double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
                float targetYaw = (float) (Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0);
                float targetPitch = (float) (-Math.toDegrees(Math.atan2(deltaY, Math.max(horizontalDistance, 1.0E-4))));
                if (snapYaw) {
                    mouseDeltaX = RotationUtil.wrapAngleTo180(targetYaw - player.rotationYaw) / 0.15F;
                }
                if (snapPitch) {
                    mouseDeltaY = -RotationUtil.wrapAngleTo180(targetPitch - player.rotationPitch) / 0.15F;
                }
            }
            applyTrackedMouseDelta(mouseDeltaX, mouseDeltaY);
            pendingYaw = remainingYaw;
            pendingPitch = remainingPitch;
        }

        private void tick() {
            if (mc.theWorld == null || mc.thePlayer == null) {
                return;
            }
            if (!canAim()) {
                reset();
                target = null;
                return;
            }
            if (target != null && target.isDead) {
                target = null;
            }
            if (requireMouseDown && !mc.gameSettings.keyBindAttack.isKeyDown()) {
                target = null;
                reset();
                return;
            }
            if (target != null && (RotationUtil.isDead(target) || mc.thePlayer.getDistanceToEntity(target) > distance)) {
                reset();
                target = null;
            }
            if (requireMouseDown && mc.gameSettings.keyBindAttack.isKeyDown() && target == null || !requireMouseDown) {
                EntityLivingBase candidateTarget = findBestTarget();
                if (!requireMouseDown) {
                    ++targetSwitchTicks;
                    if (targetSwitchTicks > 700 || target == null) {
                        if (target == null || target != candidateTarget) {
                            reset();
                        }
                        target = candidateTarget;
                        targetSwitchTicks = 0;
                    }
                } else {
                    if (target == null || target != candidateTarget) {
                        reset();
                    }
                    target = candidateTarget;
                }
            }
            if (mc.theWorld == null) {
                return;
            }
            if (target != null && mc.currentScreen == null) {
                updateAim();
            } else {
                target = null;
                reset();
            }
        }

        private void updateAim() {
            boolean yawSnapEnabled = horizontalSpeed > 20.0F;
            boolean pitchSnapEnabled = aimVertically && verticalSpeed > 20.0F;
            boolean snapYaw = yawSnapEnabled && yawSnapped;
            boolean snapPitch = pitchSnapEnabled && pitchSnapped;
            if (target == null || target.isDead) {
                reset();
                return;
            }
            EntityPlayerSP player = mc.thePlayer;
            if (player == null) {
                reset();
                return;
            }
            long nowNanos = System.nanoTime();
            float deltaTime;
            if (!initialized || lastFrameNanos == 0L) {
                deltaTime = 0.016666668F;
            } else {
                deltaTime = (float) (nowNanos - lastFrameNanos) / 1.0E9F;
                deltaTime = Math.max(0.008333334F, Math.min(0.12F, deltaTime));
            }
            lastFrameNanos = nowNanos;

            double[] resolvedAimPoint = resolveAimTarget(player, target);
            double resolvedAimX = resolvedAimPoint[0];
            double resolvedAimY = resolvedAimPoint[1];
            double resolvedAimZ = resolvedAimPoint[2];
            double targetMotionX = target.motionX;
            double targetMotionY = target.motionY;
            double targetMotionZ = target.motionZ;
            double relativeMotionX = targetMotionX - player.motionX;
            double relativeMotionY = targetMotionY - player.motionY;
            double relativeMotionZ = targetMotionZ - player.motionZ;
            double targetLeadX = targetMotionX + relativeMotionX * 0.08;
            double targetLeadY = targetMotionY + relativeMotionY * 0.1;
            double targetLeadZ = targetMotionZ + relativeMotionZ * 0.08;
            if (!aimPointInitialized) {
                aimX = resolvedAimX;
                aimY = resolvedAimY;
                aimZ = resolvedAimZ;
                leadX = targetLeadX;
                leadY = targetLeadY;
                leadZ = targetLeadZ;
                aimPointInitialized = true;
            }
            double targetHorizontalSpeed = Math.sqrt(targetMotionX * targetMotionX + targetMotionZ * targetMotionZ);
            double relativeHorizontalSpeed = Math.sqrt(relativeMotionX * relativeMotionX + relativeMotionZ * relativeMotionZ);
            double targetDistance = player.getDistanceToEntity(target);
            double baseAimSmoothing = targetArea == 1 ? 0.3 : 0.28;
            double aimSmoothing = Math.max(0.08, Math.min(0.75, baseAimSmoothing + Math.min(0.35, relativeHorizontalSpeed * 0.5)));
            double leadMotionScale = targetHorizontalSpeed + relativeHorizontalSpeed * 0.25;
            double leadSmoothing = Math.max(0.12, Math.min(0.68, 0.18 + Math.min(0.42, leadMotionScale * 0.72)));
            double frameScale = Math.max(0.45, Math.min(2.4, deltaTime * 60.0));
            double aimBlend = 1.0 - Math.pow(1.0 - aimSmoothing, frameScale);
            double leadBlend = 1.0 - Math.pow(1.0 - leadSmoothing, frameScale);
            aimX += (resolvedAimX - aimX) * aimBlend;
            aimY += (resolvedAimY - aimY) * aimBlend;
            aimZ += (resolvedAimZ - aimZ) * aimBlend;
            leadX += (targetLeadX - leadX) * leadBlend;
            leadY += (targetLeadY - leadY) * leadBlend;
            leadZ += (targetLeadZ - leadZ) * leadBlend;

            double aimLagX = resolvedAimX - aimX;
            double aimLagZ = resolvedAimZ - aimZ;
            double horizontalAimLag = Math.sqrt(aimLagX * aimLagX + aimLagZ * aimLagZ);
            double maximumAimLag = 0.3 + targetHorizontalSpeed * 1.3 + relativeHorizontalSpeed * 0.4;
            if (horizontalAimLag > maximumAimLag) {
                aimX = resolvedAimX;
                aimY = resolvedAimY;
                aimZ = resolvedAimZ;
                leadX = targetLeadX;
                leadY = targetLeadY;
                leadZ = targetLeadZ;
            }

            double leadAmount = 0.35 + targetDistance * 0.045 + targetHorizontalSpeed * 0.95 + Math.min(0.18, relativeHorizontalSpeed * 0.12);
            leadAmount = Math.max(0.1, Math.min(1.05, leadAmount));
            double distanceLeadScale = Math.max(0.0, Math.min(1.0, (targetDistance - 0.8) / 2.5));
            leadAmount *= 0.3 + 0.7 * distanceLeadScale;
            leadAmount = Math.max(0.05, leadAmount);
            double playerX = player.posX;
            double playerZ = player.posZ;
            double playerEyeY = player.posY + player.getEyeHeight();
            float measuredVerticalVelocity = 0.0F;
            if (initialized && lastEyeY != 0.0) {
                measuredVerticalVelocity = (float) ((playerEyeY - lastEyeY) / deltaTime);
            }
            verticalVelocity += (measuredVerticalVelocity - verticalVelocity) * 0.65F;
            lastEyeY = playerEyeY;
            if (player.onGround) {
                lastGroundEyeY = playerEyeY;
                airFactor *= 0.35F;
            } else {
                if (lastGroundEyeY == 0.0) {
                    lastGroundEyeY = playerEyeY;
                }
                double airborneHeight = Math.max(0.0, playerEyeY - lastGroundEyeY);
                float velocityAirFactor = (float) Math.min(0.7, Math.max(0.0, verticalVelocity) * 0.34);
                float heightAirFactor = (float) Math.min(0.82, airborneHeight * 0.58);
                airFactor = Math.max(airFactor * 0.92F, Math.max(velocityAirFactor, heightAirFactor));
            }

            double adjustedAimX = aimX;
            double adjustedAimZ = aimZ;
            double verticalAimDelta = aimY - playerEyeY;
            double verticalOffsetScale = Math.max(0.0, Math.min(1.0, Math.abs(verticalAimDelta) / 0.7));
            double verticalLeadAmount = Math.min(leadAmount, 0.7 + targetDistance * 0.04);
            double verticalLeadScale = 0.28 + 0.52 * verticalOffsetScale;
            double verticalLead = leadY * verticalLeadAmount * verticalLeadScale;
            double verticalLeadLimit = 0.16 + targetDistance * 0.055;
            verticalLead = Math.max(-verticalLeadLimit, Math.min(verticalLeadLimit, verticalLead));
            double adjustedAimY = aimY + verticalLead;
            if (snapYaw) {
                adjustedAimX = resolvedAimX;
                adjustedAimZ = resolvedAimZ;
            }
            if (snapPitch) {
                adjustedAimY = resolvedAimY;
            }
            double aimDeltaX = adjustedAimX - playerX;
            double aimDeltaZ = adjustedAimZ - playerZ;
            double aimDeltaY = adjustedAimY - playerEyeY;
            double horizontalAimDistance = Math.sqrt(aimDeltaX * aimDeltaX + aimDeltaZ * aimDeltaZ);
            float targetYaw = (float) (Math.toDegrees(Math.atan2(aimDeltaZ, aimDeltaX)) - 90.0);
            float targetPitch = (float) (-Math.toDegrees(Math.atan2(aimDeltaY, Math.max(horizontalAimDistance, 1.0E-4))));
            if (initialized) {
                float yawTargetChange = Math.abs(RotationUtil.wrapAngleTo180(targetYaw - lastTargetYaw));
                float pitchTargetChange = Math.abs(RotationUtil.wrapAngleTo180(targetPitch - lastTargetPitch));
                float abruptChangeThreshold = targetArea == 1 ? 20.0F : 12.0F;
                float maximumTargetChange = Math.max(yawTargetChange, pitchTargetChange);
                float abruptChangeFactor = Math.max(0.0F, Math.min(1.0F, (maximumTargetChange - abruptChangeThreshold) / 50.0F));
                overshoot = Math.max(overshoot, abruptChangeFactor);
                if (abruptChangeFactor > 0.3F) {
                    aimStrength *= 0.3F;
                    aimX = resolvedAimX;
                    aimY = resolvedAimY;
                    aimZ = resolvedAimZ;
                }
                float yawRateScale = 1.0F + Math.min(2.0F, yawTargetChange / 60.0F);
                float pitchRateScale = 1.0F + Math.min(2.0F, pitchTargetChange / 60.0F);
                float maximumYawRate = (120.0F + (float) (relativeHorizontalSpeed * 700.0)) * yawRateScale;
                float maximumPitchRate = (95.0F + (float) (Math.abs(leadY) * 550.0)) * pitchRateScale;
                float viewYawDifference = Math.abs(RotationUtil.wrapAngleTo180(targetYaw - player.rotationYaw));
                float wideTurnScale = smoothStep(10.0F, 35.0F, viewYawDifference);
                maximumYawRate *= 1.0F + wideTurnScale * 1.5F;
                maximumYawRate = Math.max(90.0F, Math.min(1080.0F, maximumYawRate));
                maximumPitchRate = Math.max(70.0F, Math.min(500.0F, maximumPitchRate));
                float yawStep = RotationUtil.wrapAngleTo180(targetYaw - lastTargetYaw);
                float pitchStep = RotationUtil.wrapAngleTo180(targetPitch - lastTargetPitch);
                if (!snapYaw) {
                    yawStep = Math.max(-maximumYawRate * deltaTime, Math.min(maximumYawRate * deltaTime, yawStep));
                }
                if (!snapPitch) {
                    pitchStep = Math.max(-maximumPitchRate * deltaTime, Math.min(maximumPitchRate * deltaTime, pitchStep));
                }
                targetYaw = lastTargetYaw + yawStep;
                targetPitch = lastTargetPitch + pitchStep;
            }
            float overshootDecay = (float) Math.pow(0.02, deltaTime);
            overshoot *= overshootDecay;
            if (overshoot < 0.01F) {
                overshoot = 0.0F;
            }
            float overshootMultiplier = 1.0F + 3.0F * overshoot;
            float playerYaw = player.rotationYaw;
            float playerPitch = player.rotationPitch;
            float yawError = RotationUtil.wrapAngleTo180(targetYaw - playerYaw);
            float pitchError = RotationUtil.wrapAngleTo180(targetPitch - playerPitch);
            if (!aimVertically) {
                pitchError = 0.0F;
            }
            float absoluteYawError = Math.abs(yawError);
            float absolutePitchError = Math.abs(pitchError);
            float combinedAngleError = (float) Math.sqrt(absoluteYawError * absoluteYawError + absolutePitchError * absolutePitchError);
            yawSnapped = shouldSnap(yawSnapEnabled, yawSnapped, absoluteYawError);
            pitchSnapped = shouldSnap(pitchSnapEnabled, pitchSnapped, absolutePitchError);
            snapYaw = yawSnapEnabled && yawSnapped;
            snapPitch = pitchSnapEnabled && pitchSnapped;
            float desiredAimStrength = 1.0F - smoothStep(1.5F, 8.0F, combinedAngleError);
            if (initialized) {
                float yawClosingSpeed = -(absoluteYawError - Math.abs(lastYawDiff)) / deltaTime;
                float closingAdjustment = Math.max(-0.3F, Math.min(0.3F, yawClosingSpeed / 20.0F));
                desiredAimStrength += closingAdjustment;
                desiredAimStrength = Math.max(0.0F, Math.min(1.0F, desiredAimStrength));
            }
            float aimStrengthBlend = desiredAimStrength > aimStrength
                    ? Math.max(0.01F, Math.min(0.25F, deltaTime * 3.0F))
                    : Math.max(0.05F, Math.min(0.8F, deltaTime * 20.0F));
            aimStrength += (desiredAimStrength - aimStrength) * aimStrengthBlend;
            float currentAimStrength = aimStrength;

            float smoothedTargetYawRate = 0.0F;
            float smoothedTargetPitchRate = 0.0F;
            float playerYawRate = 0.0F;
            float playerPitchRate = 0.0F;
            if (initialized) {
                float targetYawRate = RotationUtil.wrapAngleTo180(targetYaw - lastTargetYaw) / deltaTime;
                float targetPitchRate = RotationUtil.wrapAngleTo180(targetPitch - lastTargetPitch) / deltaTime;
                playerYawRate = RotationUtil.wrapAngleTo180(playerYaw - lastPlayerYaw) / deltaTime;
                playerPitchRate = RotationUtil.wrapAngleTo180(playerPitch - lastPlayerPitch) / deltaTime;
                float accelerationBlend = Math.max(0.05F, Math.min(0.45F, deltaTime * 12.0F));
                yawAccel += (targetYawRate - yawAccel) * accelerationBlend;
                pitchAccel += (targetPitchRate - pitchAccel) * accelerationBlend;
                smoothedTargetYawRate = yawAccel;
                smoothedTargetPitchRate = pitchAccel;
            }
            float horizontalSpeedValue = horizontalSpeed * 0.75F;
            float verticalSpeedValue = verticalSpeed * 0.75F;
            float horizontalSpeedFactor = Math.max(0.0F, Math.min(1.0F, (horizontalSpeedValue - 10.0F) / 90.0F));
            float verticalSpeedFactor = Math.max(0.0F, Math.min(1.0F, (verticalSpeedValue - 10.0F) / 90.0F));
            if (Math.signum(yawError) != Math.signum(lastYawDiff) && Math.abs(yawError) > 0.1F && Math.abs(lastYawDiff) > 0.1F) {
                yawBias *= 0.3F;
            }
            if (Math.signum(pitchError) != Math.signum(lastPitchDiff) && Math.abs(pitchError) > 0.1F && Math.abs(lastPitchDiff) > 0.1F) {
                pitchBias *= 0.3F;
            }
            float squaredAimStrength = currentAimStrength * currentAimStrength;
            yawBias += yawError * deltaTime * squaredAimStrength * (1.0F - horizontalSpeedFactor);
            pitchBias += pitchError * deltaTime * squaredAimStrength * (1.0F - verticalSpeedFactor);
            float biasRetention = 1.0F - (1.0F - currentAimStrength) * Math.max(0.0F, Math.min(0.5F, deltaTime * 5.0F));
            yawBias *= biasRetention;
            pitchBias *= biasRetention;
            float yawBiasLimit = 15.0F * (1.0F - horizontalSpeedFactor * 0.9F);
            float pitchBiasLimit = 10.0F * (1.0F - verticalSpeedFactor * 0.9F);
            yawBias = Math.max(-yawBiasLimit, Math.min(yawBiasLimit, yawBias));
            pitchBias = Math.max(-pitchBiasLimit, Math.min(pitchBiasLimit, pitchBias));

            float yawErrorVelocity = initialized ? (yawError - lastYawDiff) / deltaTime : 0.0F;
            float pitchErrorVelocity = initialized ? (pitchError - lastPitchDiff) / deltaTime : 0.0F;
            float velocityBlend = 0.15F;
            yawVelocity = yawVelocity * (1.0F - velocityBlend) + yawErrorVelocity * velocityBlend;
            pitchVelocity = pitchVelocity * (1.0F - velocityBlend) + pitchErrorVelocity * velocityBlend;

            float flickThreshold = targetArea == 1 ? 1.5F : 0.5F;
            float yawSign = Math.signum(yawError);
            yawFlickTicks = yawSign != lastYawSign && Math.abs(yawError) > flickThreshold
                    ? Math.min(yawFlickTicks + 1.0F, 8.0F)
                    : Math.max(0.0F, yawFlickTicks - deltaTime * 3.0F);
            lastYawSign = yawSign;
            float pitchSign = Math.signum(pitchError);
            pitchFlickTicks = pitchSign != lastPitchSign && Math.abs(pitchError) > flickThreshold
                    ? Math.min(pitchFlickTicks + 1.0F, 8.0F)
                    : Math.max(0.0F, pitchFlickTicks - deltaTime * 3.0F);
            lastPitchSign = pitchSign;
            float yawFlickFactor = Math.max(0.0F, Math.min(1.0F, yawFlickTicks / 5.0F));
            float pitchFlickFactor = Math.max(0.0F, Math.min(1.0F, pitchFlickTicks / 5.0F));

            float limitedHorizontalSpeed = Math.min(horizontalSpeedValue, 10.0F);
            float limitedVerticalSpeed = Math.min(verticalSpeedValue, 10.0F);
            float horizontalGainInput = (limitedHorizontalSpeed - 1.0F) / 9.0F;
            float verticalGainInput = (limitedVerticalSpeed - 1.0F) / 9.0F;
            horizontalGainInput *= horizontalGainInput;
            verticalGainInput *= verticalGainInput;
            float horizontalGainScale = 0.15F + 0.85F * Math.max(0.0F, Math.min(1.0F, horizontalGainInput));
            float verticalGainScale = 0.15F + 0.85F * Math.max(0.0F, Math.min(1.0F, verticalGainInput));
            float yawErrorGain = lerp(currentAimStrength, 8.0F, 2.5F + limitedHorizontalSpeed * 0.15F) * horizontalGainScale;
            float pitchErrorGain = lerp(currentAimStrength, 7.0F, 2.2F + limitedVerticalSpeed * 0.13F) * verticalGainScale;
            float yawBiasGain = lerp(currentAimStrength, 0.15F, 0.8F + limitedHorizontalSpeed * 0.04F) * horizontalGainScale;
            float pitchBiasGain = lerp(currentAimStrength, 0.12F, 0.65F + limitedVerticalSpeed * 0.035F) * verticalGainScale;
            float yawVelocityGain = lerp(currentAimStrength, 0.08F, 0.25F) * horizontalGainScale;
            float pitchVelocityGain = lerp(currentAimStrength, 0.06F, 0.2F) * verticalGainScale;
            float targetYawRateGain = (0.85F + limitedHorizontalSpeed * 0.015F) * horizontalGainScale;
            float targetPitchRateGain = (0.82F + limitedVerticalSpeed * 0.013F) * verticalGainScale;
            float playerYawCompensation = lerp(currentAimStrength, 0.1F, 0.3F);
            float playerPitchCompensation = lerp(currentAimStrength, 0.08F, 0.25F);
            yawErrorGain *= 1.0F - 0.6F * yawFlickFactor;
            yawVelocityGain *= 1.0F + 2.0F * yawFlickFactor;
            pitchErrorGain *= 1.0F - 0.6F * pitchFlickFactor;
            pitchVelocityGain *= 1.0F + 2.0F * pitchFlickFactor;

            float yawRate = yawErrorGain * yawError + yawBiasGain * yawBias + yawVelocityGain * yawVelocity
                    + targetYawRateGain * smoothedTargetYawRate - playerYawCompensation * playerYawRate;
            float pitchRate = 0.0F;
            if (aimVertically) {
                pitchRate = pitchErrorGain * pitchError + pitchBiasGain * pitchBias + pitchVelocityGain * pitchVelocity
                        + targetPitchRateGain * smoothedTargetPitchRate - playerPitchCompensation * playerPitchRate;
            } else {
                pitchBias = 0.0F;
                pitchVelocity = 0.0F;
            }
            if (!snapPitch) {
                pitchRate += computePitchDrift(playerPitch, deltaTime, nowNanos);
            }
            float strafeMultiplier = 1.0F;
            float strafeInput = player.movementInput.moveStrafe;
            if (strafeIncrease && Math.abs(strafeInput) > 0.01F) {
                boolean targetToRight = yawError > 0.0F;
                boolean strafingAway = targetToRight && strafeInput < 0.0F || !targetToRight && strafeInput > 0.0F;
                if (strafingAway) {
                    strafeMultiplier = 1.15F;
                }
            }
            yawRate *= strafeMultiplier;

            float wideYawScale = smoothStep(8.0F, 40.0F, absoluteYawError);
            float wideTurnMultiplier = 1.0F + wideYawScale * 2.5F;
            float yawBaseLimit = (22.0F + limitedHorizontalSpeed * 15.0F) * horizontalGainScale * overshootMultiplier * wideTurnMultiplier;
            float pitchBaseLimit = (18.0F + limitedVerticalSpeed * 13.0F) * verticalGainScale * overshootMultiplier;
            float yawMotionLimit = (Math.abs(smoothedTargetYawRate) * 0.4F + 18.0F) * (0.35F + horizontalGainScale * 0.65F) * wideTurnMultiplier;
            float pitchMotionLimit = (Math.abs(smoothedTargetPitchRate) * 0.38F + 14.0F) * (0.35F + verticalGainScale * 0.65F);
            float maximumYawOutput = Math.min(400.0F * overshootMultiplier * wideTurnMultiplier, Math.max(yawBaseLimit, yawMotionLimit));
            float maximumPitchOutput = Math.min(300.0F * overshootMultiplier, Math.max(pitchBaseLimit, pitchMotionLimit));
            yawRate = Math.max(-maximumYawOutput, Math.min(maximumYawOutput, yawRate));
            pitchRate = Math.max(-maximumPitchOutput, Math.min(maximumPitchOutput, pitchRate));

            float distanceScale = smoothStep(0.5F, 3.0F, (float) targetDistance);
            float wideTurnBaseScale = lerp(wideYawScale, 0.15F, 0.65F);
            float yawDistanceMultiplier = lerp(currentAimStrength,
                    wideTurnBaseScale + (1.0F - wideTurnBaseScale) * distanceScale, 0.4F + 0.6F * distanceScale);
            float pitchDistanceMultiplier = lerp(currentAimStrength,
                    0.2F + 0.8F * distanceScale, 0.45F + 0.55F * distanceScale);
            yawRate *= yawDistanceMultiplier;
            pitchRate *= pitchDistanceMultiplier;
            float noiseElapsedSeconds = (float) (nowNanos - noiseStartNanos) / 1.0E9F;
            float highFrequencyYawNoise = (float) (Math.sin(noiseElapsedSeconds * 62.83) * 0.4
                    + Math.sin(noiseElapsedSeconds * 47.12) * 0.25
                    + Math.sin(noiseElapsedSeconds * 78.54) * 0.15);
            float highFrequencyPitchNoise = (float) (Math.sin(noiseElapsedSeconds * 56.55 + 1.3) * 0.35
                    + Math.sin(noiseElapsedSeconds * 43.98 + 0.7) * 0.2
                    + Math.sin(noiseElapsedSeconds * 72.26 + 2.1) * 0.12);
            float mediumFrequencyYawNoise = (float) (Math.sin(noiseElapsedSeconds * 12.57) * 0.8
                    + Math.sin(noiseElapsedSeconds * 7.85) * 0.5);
            float mediumFrequencyPitchNoise = (float) (Math.sin(noiseElapsedSeconds * 10.47 + 0.9) * 0.6
                    + Math.sin(noiseElapsedSeconds * 5.65 + 1.8) * 0.4);
            float lowFrequencyYawNoise = (float) (Math.sin(noiseElapsedSeconds * 1.26) * 0.3);
            float lowFrequencyPitchNoise = (float) (Math.sin(noiseElapsedSeconds * 0.94 + 0.5) * 0.2);
            float noiseStrength = 0.15F + 0.85F * currentAimStrength;
            float speedNoiseScale = 0.5F + 0.5F * (1.0F - horizontalGainInput);
            float yawNoise = (highFrequencyYawNoise + mediumFrequencyYawNoise + lowFrequencyYawNoise)
                    * noiseStrength * speedNoiseScale * 1.5F;
            float pitchNoise = (highFrequencyPitchNoise + mediumFrequencyPitchNoise + lowFrequencyPitchNoise)
                    * noiseStrength * speedNoiseScale;
            if (!snapYaw) {
                yawRate += yawNoise;
            }
            if (aimVertically && !snapPitch) {
                pitchRate += pitchNoise;
            }

            float yawFrameDelta = yawRate * deltaTime;
            float pitchFrameDelta = pitchRate * deltaTime;
            float sensitivity = mc.gameSettings.mouseSensitivity;
            float sensitivityBase = sensitivity * 0.6F + 0.2F;
            float sensitivityScale = sensitivityBase * sensitivityBase * sensitivityBase * 8.0F;
            float mouseAngleUnit = sensitivityScale * 0.15F;
            if (mouseAngleUnit > 1.0E-5F) {
                float yawVelocityUnits = yawFrameDelta / mouseAngleUnit;
                float pitchVelocityUnits = pitchFrameDelta / mouseAngleUnit;
                float pitchBlendFactor = aimVertically ? verticalSpeedFactor : 0.0F;
                if (snapYaw) {
                    pendingYaw = 0.0F;
                }
                if (snapPitch) {
                    pendingPitch = 0.0F;
                }
                if (!snapYaw) {
                    if (horizontalSpeedFactor > 0.0F) {
                        float targetYawUnits = yawError / mouseAngleUnit;
                        float yawCorrection = targetYawUnits - pendingYaw;
                        float maximumYawCorrection = horizontalSpeedValue * 2.0F;
                        yawCorrection = Math.max(-maximumYawCorrection, Math.min(maximumYawCorrection, yawCorrection));
                        pendingYaw += yawVelocityUnits * (1.0F - horizontalSpeedFactor) + yawCorrection * horizontalSpeedFactor;
                    } else {
                        pendingYaw += yawVelocityUnits;
                    }
                }
                if (!snapPitch) {
                    if (pitchBlendFactor > 0.0F) {
                        float targetPitchUnits = aimVertically ? pitchError / mouseAngleUnit : 0.0F;
                        float pitchCorrection = targetPitchUnits - pendingPitch;
                        float maximumPitchCorrection = verticalSpeedValue * 2.0F;
                        pitchCorrection = Math.max(-maximumPitchCorrection, Math.min(maximumPitchCorrection, pitchCorrection));
                        pendingPitch += pitchVelocityUnits * (1.0F - pitchBlendFactor) + pitchCorrection * pitchBlendFactor;
                    } else {
                        pendingPitch += pitchVelocityUnits;
                    }
                }
            }
            if (!initialized) {
                initialized = true;
            }
            lastYawDiff = yawError;
            lastPitchDiff = pitchError;
            lastTargetYaw = targetYaw;
            lastTargetPitch = targetPitch;
            lastPlayerYaw = playerYaw;
            lastPlayerPitch = playerPitch;
        }

        private double[] resolveAimTarget(EntityPlayerSP player, EntityLivingBase target) {
            double playerEyeY = player.posY + player.getEyeHeight();
            AxisAlignedBB box = target.getEntityBoundingBox();
            double targetMinY = box.minY;
            double targetMaxY = box.maxY;
            double targetHeight = targetMaxY - targetMinY;
            double targetCenterY = targetMinY + targetHeight * 0.65;
            double airborneOffset = Math.min(0.85, airFactor);
            double eyeOffset = 0.1 + airborneOffset;
            double eyeAlignedY = Math.max(targetMinY + 0.01, Math.min(targetMaxY - 0.01, playerEyeY - eyeOffset));
            double centerBlend = Math.max(0.0, Math.min(1.0, airborneOffset / 0.55));
            double targetY = eyeAlignedY + (targetCenterY - eyeAlignedY) * centerBlend;
            targetY = Math.max(targetMinY + 0.01, Math.min(targetMaxY - 0.01, targetY));
            if (targetArea == 1) {
                Vec3d closestPoint = RotationUtil.getClosestPoint(player, box, 0.0, 0.0, 0.0);
                double closestY = Math.max(targetMinY + 0.01, Math.min(targetMaxY - 0.01, closestPoint.y - eyeOffset));
                closestY += (targetCenterY - closestY) * centerBlend;
                closestY = Math.max(targetMinY + 0.01, Math.min(targetMaxY - 0.01, closestY));
                double resolvedX = closestPoint.x;
                double resolvedY = closestY;
                double resolvedZ = closestPoint.z;
                if (predictionInitialized) {
                    double smoothingFactor = 0.35;
                    resolvedX = predictedX + (resolvedX - predictedX) * smoothingFactor;
                    resolvedY = predictedY + (resolvedY - predictedY) * smoothingFactor;
                    resolvedZ = predictedZ + (resolvedZ - predictedZ) * smoothingFactor;
                }
                predictedX = resolvedX;
                predictedY = resolvedY;
                predictedZ = resolvedZ;
                predictionInitialized = true;
                return new double[]{resolvedX, resolvedY, resolvedZ};
            }
            return new double[]{target.posX, targetY, target.posZ};
        }

        private double[] resolveSnapPoint(EntityPlayerSP player, EntityLivingBase target) {
            double playerEyeY = player.posY + player.getEyeHeight();
            AxisAlignedBB box = target.getEntityBoundingBox();
            double targetMinY = box.minY;
            double targetMaxY = box.maxY;
            double targetHeight = targetMaxY - targetMinY;
            double targetCenterY = targetMinY + targetHeight * 0.65;
            double airborneOffset = Math.min(0.85, airFactor);
            double eyeOffset = 0.1 + airborneOffset;
            double eyeAlignedY = Math.max(targetMinY + 0.01, Math.min(targetMaxY - 0.01, playerEyeY - eyeOffset));
            double centerBlend = Math.max(0.0, Math.min(1.0, airborneOffset / 0.55));
            double targetY = eyeAlignedY + (targetCenterY - eyeAlignedY) * centerBlend;
            targetY = Math.max(targetMinY + 0.01, Math.min(targetMaxY - 0.01, targetY));
            if (targetArea == 1) {
                Vec3d closestPoint = RotationUtil.getClosestPoint(player, box, 0.0, 0.0, 0.0);
                double closestY = Math.max(targetMinY + 0.01, Math.min(targetMaxY - 0.01, closestPoint.y - eyeOffset));
                closestY += (targetCenterY - closestY) * centerBlend;
                closestY = Math.max(targetMinY + 0.01, Math.min(targetMaxY - 0.01, closestY));
                return new double[]{closestPoint.x, closestY, closestPoint.z};
            }
            return new double[]{target.posX, targetY, target.posZ};
        }

        private float computePitchDrift(float playerPitch, float deltaTime, long nowNanos) {
            float strength = 0.65F + 0.35F * aimStrength;
            if (driftNextNanos == 0L || nowNanos >= driftNextNanos) {
                float randomAmplitude = lerp(strength, 0.05F, 0.15F);
                float pitchCorrection = playerPitch > 22.0F ? -0.18F : (playerPitch < -22.0F ? 0.18F : 0.0F);
                driftTarget = (random.nextFloat() * 2.0F - 1.0F) * randomAmplitude + pitchCorrection;
                driftTarget = Math.max(-0.5F, Math.min(0.5F, driftTarget));
                long intervalMillis = 300L + random.nextInt(420);
                driftNextNanos = nowNanos + intervalMillis * 1000000L;
            }
            float springStrength = lerp(strength, 2.0F, 5.0F);
            float damping = (float) Math.pow(0.04F, deltaTime);
            driftVelocity += (driftTarget - driftPos) * springStrength * deltaTime;
            driftVelocity *= damping;
            driftPos += driftVelocity * deltaTime;
            float positionLimit = lerp(strength, 0.45F, 0.8F);
            if (driftPos > positionLimit) {
                driftPos = positionLimit;
                driftVelocity = Math.min(0.0F, driftVelocity);
            } else if (driftPos < -positionLimit) {
                driftPos = -positionLimit;
                driftVelocity = Math.max(0.0F, driftVelocity);
            }
            driftNoise += (random.nextFloat() * 2.0F - 1.0F - driftNoise) * Math.max(0.02F, Math.min(0.18F, deltaTime * 5.0F));
            float elapsedSeconds = (float) (nowNanos - noiseStartNanos) / 1.0E9F;
            float noise = (float) (Math.sin(elapsedSeconds * 8.7 + 0.4) * 0.35F + Math.sin(elapsedSeconds * 13.1 + 2.2) * 0.22F + Math.sin(elapsedSeconds * 19.6 + 1.1) * 0.12F + driftNoise * 0.3F);
            float restoringForce = -driftPos * lerp(strength, 1.4F, 3.0F);
            float output = driftVelocity * 0.25F + restoringForce + noise * lerp(strength, 0.35F, 0.9F);
            float pitchLimitScale = 1.0F - 0.85F * smoothStep(72.0F, 88.0F, Math.abs(playerPitch));
            float outputLimit = lerp(strength, 0.25F, 0.55F) * pitchLimitScale;
            return Math.max(-outputLimit, Math.min(outputLimit, output));
        }

        private boolean shouldSnap(boolean enabled, boolean currentlySnapped, float angleDifference) {
            if (!enabled) {
                return false;
            }
            return currentlySnapped ? angleDifference <= SNAP_EXIT_THRESHOLD : angleDifference <= SNAP_ENTER_THRESHOLD;
        }

        private float smoothStep(float edgeStart, float edgeEnd, float value) {
            float normalized = Math.max(0.0F, Math.min(1.0F, (value - edgeStart) / (edgeEnd - edgeStart)));
            return normalized * normalized * (3.0F - 2.0F * normalized);
        }

        private float lerp(float factor, float start, float end) {
            return start + factor * (end - start);
        }

        private void reset() {
            pendingYaw = 0.0F;
            pendingPitch = 0.0F;
            yawSnapped = false;
            pitchSnapped = false;
            yawBias = 0.0F;
            pitchBias = 0.0F;
            yawVelocity = 0.0F;
            pitchVelocity = 0.0F;
            lastYawDiff = 0.0F;
            lastPitchDiff = 0.0F;
            lastTargetYaw = 0.0F;
            lastTargetPitch = 0.0F;
            yawAccel = 0.0F;
            pitchAccel = 0.0F;
            lastPlayerYaw = 0.0F;
            lastPlayerPitch = 0.0F;
            initialized = false;
            lastFrameNanos = 0L;
            aimPointInitialized = false;
            aimX = 0.0;
            aimY = 0.0;
            aimZ = 0.0;
            leadX = 0.0;
            leadY = 0.0;
            leadZ = 0.0;
            aimStrength = 0.0F;
            yawFlickTicks = 0.0F;
            pitchFlickTicks = 0.0F;
            lastYawSign = 0.0F;
            lastPitchSign = 0.0F;
            overshoot = 0.0F;
            lastEyeY = 0.0;
            lastGroundEyeY = 0.0;
            verticalVelocity = 0.0F;
            airFactor = 0.0F;
            predictionInitialized = false;
            predictedX = 0.0;
            predictedY = 0.0;
            predictedZ = 0.0;
            noiseStartNanos = System.nanoTime();
            driftPos = 0.0F;
            driftVelocity = 0.0F;
            driftTarget = 0.0F;
            driftNoise = 0.0F;
            driftNextNanos = 0L;
        }
    }
}
