package vip.radium.utils.ncp;

import net.minecraft.util.Vec3;

public class Direction {

    public static final double DIRECT_PRECISION = 2.6;

    public static double directionCheck(Vec3 sourceFoot, final double eyeHeight, Vec3 dir, final double targetX, final double targetY, final double targetZ, final double targetWidth, final double targetHeight, final double precision) {
        return directionCheck(sourceFoot.xCoord, sourceFoot.yCoord + eyeHeight, sourceFoot.zCoord, dir.xCoord, dir.yCoord, dir.zCoord, targetX, targetY, targetZ, targetWidth, targetHeight, precision);
    }

    public static double directionCheck(final double sourceX, final double sourceY, final double sourceZ, final double dirX, final double dirY, final double dirZ, final double targetX, final double targetY, final double targetZ, final double targetWidth, final double targetHeight, final double precision) {
        double dirLength = Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
        if (dirLength == 0.0) dirLength = 1.0;

        final double dX = targetX - sourceX;
        final double dY = targetY - sourceY;
        final double dZ = targetZ - sourceZ;

        final double targetDist = Math.sqrt(dX * dX + dY * dY + dZ * dZ);

        final double xPrediction = targetDist * dirX / dirLength;
        final double yPrediction = targetDist * dirY / dirLength;
        final double zPrediction = targetDist * dirZ / dirLength;

        double off = 0.0D;

        off += Math.max(Math.abs(dX - xPrediction) - (targetWidth / 2 + precision), 0.0D);
        off += Math.max(Math.abs(dZ - zPrediction) - (targetWidth / 2 + precision), 0.0D);
        off += Math.max(Math.abs(dY - yPrediction) - (targetHeight / 2 + precision), 0.0D);

        if (off > 1) off = Math.sqrt(off);

        return off;
    }
}
