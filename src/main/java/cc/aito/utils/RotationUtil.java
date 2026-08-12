package cc.aito.utils;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;

import java.util.Random;

public final class RotationUtil {

    private RotationUtil() {
    }

    public static float wrapAngleTo180(float angle) {
        angle %= 360.0F;
        if (angle >= 180.0F) {
            angle -= 360.0F;
        }
        if (angle < -180.0F) {
            angle += 360.0F;
        }
        return angle;
    }

    public static double wrapAngleTo180(double angle) {
        angle %= 360.0D;
        if (angle >= 180.0D) {
            angle -= 360.0D;
        }
        if (angle < -180.0D) {
            angle += 360.0D;
        }
        return angle;
    }

    public static int randomExclusiveUpper(Random random, int min, int max) {
        return random.nextInt(max - min) + min;
    }

    public static double randomRange(Random random, double min, double max) {
        return min + (max - min) * random.nextDouble();
    }

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public static int getYawDifference(Entity from, Entity to) {
        double dx = to.posX - from.posX;
        double dz = to.posZ - from.posZ;
        double yaw = 0.0D;
        if (dz > 0.0D && dx > 0.0D) {
            yaw = Math.toDegrees(-Math.atan(dx / dz));
        } else if (dz > 0.0D && dx < 0.0D) {
            yaw = Math.toDegrees(-Math.atan(dx / dz));
        } else if (dz < 0.0D && dx > 0.0D) {
            yaw = -90.0D + Math.toDegrees(Math.atan(dz / dx));
        } else if (dz < 0.0D && dx < 0.0D) {
            yaw = 90.0D + Math.toDegrees(Math.atan(dz / dx));
        }
        int difference = (int) (Math.abs(yaw - from.rotationYaw) % 360.0D);
        return difference > 180 ? 360 - difference : difference;
    }

    public static double getHorizontalAngle(double x1, double z1, double yaw, double x2, double z2) {
        double dx = x2 - x1;
        double dz = z2 - z1;
        double angle = 0.0D;
        if (dz > 0.0D && dx > 0.0D) {
            angle = Math.toDegrees(-Math.atan(dx / dz));
        } else if (dz > 0.0D && dx < 0.0D) {
            angle = Math.toDegrees(-Math.atan(dx / dz));
        } else if (dz < 0.0D && dx > 0.0D) {
            angle = -90.0D + Math.toDegrees(Math.atan(dz / dx));
        } else if (dz < 0.0D && dx < 0.0D) {
            angle = 90.0D + Math.toDegrees(Math.atan(dz / dx));
        }
        double wrapped = Math.abs(angle - yaw) % 360.0D;
        return wrapped > 180.0D ? 360.0D - wrapped : wrapped;
    }

    public static boolean isOnLeft(double x1, double z1, double yaw, double x2, double z2) {
        double dx = x2 - x1;
        double dz = z2 - z1;
        double angle = 0.0D;
        if (dz > 0.0D && dx > 0.0D) {
            angle = Math.toDegrees(-Math.atan(dx / dz));
        } else if (dz > 0.0D && dx < 0.0D) {
            angle = Math.toDegrees(-Math.atan(dx / dz));
        } else if (dz < 0.0D && dx > 0.0D) {
            angle = -90.0D + Math.toDegrees(Math.atan(dz / dx));
        } else if (dz < 0.0D && dx < 0.0D) {
            angle = 90.0D + Math.toDegrees(Math.atan(dz / dx));
        }
        int wrapped = (int) wrapAngleTo180((angle - yaw) % 360.0D);
        return wrapped < 0;
    }

    public static int getVerticalAngle(Entity entity, double x, double y, double z) {
        double dx = x - entity.posX;
        double dy = y - entity.posY;
        double dz = z - entity.posZ;
        double distance = Math.sqrt(dx * dx + dz * dz);
        float pitch = (float) (-(Math.atan2(dy, distance) * 180.0D / Math.PI));
        float wrapped = (float) wrapAngleTo180((double) (entity.rotationPitch - pitch));
        return (int) wrapped;
    }

    public static Vec3d getClosestPoint(EntityLivingBase entity, AxisAlignedBB box, double dx, double dy, double dz) {
        double minX = box.minX + dx;
        double minY = box.minY + dy;
        double minZ = box.minZ + dz;
        double maxX = box.maxX + dx;
        double maxY = box.maxY + dy;
        double maxZ = box.maxZ + dz;
        double eyeY = entity.getPositionEyes(0.0F).yCoord;
        double closestX = clamp(entity.posX, minX, maxX);
        double closestY = clamp(eyeY, minY, maxY);
        double closestZ = clamp(entity.posZ, minZ, maxZ);
        if (closestX == entity.posX) {
            closestX = entity.posX + 0.01;
        }
        if (closestZ == entity.posZ) {
            closestZ = entity.posZ + 0.01;
        }
        return new Vec3d(closestX, closestY, closestZ);
    }

    public static boolean isDead(Entity entity) {
        if (entity instanceof EntityLivingBase) {
            EntityLivingBase living = (EntityLivingBase) entity;
            return entity.isDead || living.getHealth() <= 0.0F;
        }
        return entity.isDead;
    }

    public static boolean isInvisible(EntityLivingBase entity) {
        return entity != null && entity.isInvisible() && entity.getHeldItem() == null;
    }
}
