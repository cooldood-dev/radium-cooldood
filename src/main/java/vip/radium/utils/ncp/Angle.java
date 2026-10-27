package vip.radium.utils.ncp;

import net.minecraft.entity.Entity;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.UUID;

public class Angle {

    public static Angle INSTANCE = new Angle();

    public LinkedList<AttackLocation> angleHits = new LinkedList<>();
    public double angleVL;

    public static class AttackLocation {
        public final double x, y, z;
        public final float yaw;
        public long time;
        public final UUID damagedId;
        public final double distSqLast;
        public final double yawDiffLast;
        public final long timeDiff;
        public final boolean idDiffLast;

        public AttackLocation(final Location loc, final UUID damagedId, final long time, final AttackLocation lastLoc) {
            x = loc.getX();
            y = loc.getY();
            z = loc.getZ();
            yaw = loc.getYaw();
            this.time = time;
            this.damagedId = damagedId;

            if (lastLoc != null) {
                distSqLast = distanceSquared(x, y, z, lastLoc.x, lastLoc.y, lastLoc.z);
                yawDiffLast = yawDiff(yaw, lastLoc.yaw);
                timeDiff = Math.max(0L, time - lastLoc.time);
                idDiffLast = !damagedId.equals(lastLoc.damagedId);
            } else {
                distSqLast = 0.0;
                yawDiffLast = 0f;
                timeDiff = 0L;
                idDiffLast = true;
            }
        }

        public AttackLocation(AttackLocation attackLocation) {
            x = attackLocation.x;
            y = attackLocation.y;
            z = attackLocation.z;
            yaw = attackLocation.yaw;
            this.time = attackLocation.time;
            this.damagedId = attackLocation.damagedId;
            distSqLast = attackLocation.distSqLast;
            yawDiffLast = attackLocation.yawDiffLast;
            timeDiff = attackLocation.timeDiff;
            idDiffLast = attackLocation.idDiffLast;
        }
    }

    public static float yawDiff(float fromYaw, float toYaw) {
        if (fromYaw <= -360f) {
            fromYaw = -((-fromYaw) % 360f);
        } else if (fromYaw >= 360f) {
            fromYaw = fromYaw % 360f;
        }
        if (toYaw <= -360f) {
            toYaw = -((-toYaw) % 360f);
        } else if (toYaw >= 360f) {
            toYaw = toYaw % 360f;
        }
        float yawDiff = toYaw - fromYaw;
        if (yawDiff < -180f) {
            yawDiff += 360f;
        } else if (yawDiff > 180f) {
            yawDiff -= 360f;
        }
        return yawDiff;
    }

    public static double distanceSquared(final double x1, final double y1, final double z1, final double x2, final double y2, final double z2) {
        final double dx = Math.abs(x1 - x2);
        final double dy = Math.abs(y1 - y2);
        final double dz = Math.abs(z1 - z2);
        return dx * dx + dy * dy + dz * dz;
    }

    public static long maxTimeDiff = 1000L;

    public void clear() {
        angleHits.clear();
        angleVL = 0;
    }

    public boolean willViolateYaw(final Location tempLocation, final Entity damagedEntity) {
        LinkedList<AttackLocation> tempAttack = new LinkedList<>();

        for (AttackLocation angleHit : angleHits) {
            tempAttack.add(new AttackLocation(angleHit));
        }

        final long time = System.currentTimeMillis();

        final Iterator<AttackLocation> realIterator = angleHits.iterator();
        while (realIterator.hasNext()) {
            final AttackLocation refLoc = realIterator.next();
            if (time - refLoc.time > maxTimeDiff) {
                realIterator.remove();
            }
        }

        AttackLocation lastLoc = tempAttack.isEmpty() ? null : tempAttack.getLast();
        if (lastLoc != null && time - lastLoc.time > maxTimeDiff) {
            tempAttack.clear();
            lastLoc = null;
        }

        tempAttack.add(new AttackLocation(tempLocation, damagedEntity.getUniqueID(), System.currentTimeMillis(), lastLoc));

        double deltaMove = 0D;
        long deltaTime = 0L;
        float deltaYaw = 0f;
        int deltaSwitchTarget = 0;
        final Iterator<AttackLocation> it = tempAttack.iterator();
        while (it.hasNext()) {
            final AttackLocation refLoc = it.next();
            if (time - refLoc.time > maxTimeDiff) {
                it.remove();
                continue;
            }
            deltaMove += refLoc.distSqLast;
            final double yawDiff = Math.abs(refLoc.yawDiffLast);
            deltaYaw += yawDiff;
            deltaTime += refLoc.timeDiff;
            if (refLoc.idDiffLast && yawDiff > 30.0) {
                deltaSwitchTarget += 1;
            }
        }

        if (tempAttack.size() < 2) {
            return false;
        }

        final double n = tempAttack.size() - 1;
        final double averageMove = deltaMove / n;
        final double averageTime = (double) deltaTime / n;
        final double averageYaw = (double) deltaYaw / n;
        final double averageSwitching = (double) deltaSwitchTarget / n;

        double violation = 0.0;

        if (averageMove >= 0.0 && averageMove < 0.2D) {
            violation += 20.0 * (0.2 - averageMove) / 0.2;
        }

        if (averageTime >= 0.0 && averageTime < 150.0) {
            violation += 30.0 * (150.0 - averageTime) / 150.0;
        }

        if (averageYaw > 50.0) {
            violation += 30.0 * averageYaw / 180.0;
        }

        if (averageSwitching > 0.0) {
            violation += 20.0 * averageSwitching;
        }

        return violation > 50;
    }

    public boolean check(final Location loc, final Entity damagedEntity) {
        boolean cancel = false;

        final long time = System.currentTimeMillis();
        AttackLocation lastLoc = angleHits.isEmpty() ? null : angleHits.getLast();
        if (lastLoc != null && time - lastLoc.time > maxTimeDiff) {
            angleHits.clear();
            lastLoc = null;
        }

        angleHits.add(new AttackLocation(loc, damagedEntity.getUniqueID(), System.currentTimeMillis(), lastLoc));

        double deltaMove = 0D;
        long deltaTime = 0L;
        float deltaYaw = 0f;
        int deltaSwitchTarget = 0;
        final Iterator<AttackLocation> it = angleHits.iterator();
        while (it.hasNext()) {
            final AttackLocation refLoc = it.next();
            if (time - refLoc.time > maxTimeDiff) {
                it.remove();
                continue;
            }
            deltaMove += refLoc.distSqLast;
            final double yawDiff = Math.abs(refLoc.yawDiffLast);
            deltaYaw += yawDiff;
            deltaTime += refLoc.timeDiff;
            if (refLoc.idDiffLast && yawDiff > 30.0) {
                deltaSwitchTarget += 1;
            }
        }

        if (angleHits.size() < 2) {
            return false;
        }

        final double n = angleHits.size() - 1;
        final double averageMove = deltaMove / n;
        final double averageTime = (double) deltaTime / n;
        final double averageYaw = (double) deltaYaw / n;
        final double averageSwitching = (double) deltaSwitchTarget / n;

        double violation = 0.0;

        if (averageMove >= 0.0 && averageMove < 0.2D) {
            violation += 20.0 * (0.2 - averageMove) / 0.2;
        }

        if (averageTime >= 0.0 && averageTime < 150.0) {
            violation += 30.0 * (150.0 - averageTime) / 150.0;
        }

        if (averageYaw > 50.0) {
            violation += 30.0 * averageYaw / 180.0;
        }

        if (averageSwitching > 0.0) {
            violation += 20.0 * averageSwitching;
        }

        if (violation > 50) {
            angleVL += violation;
            cancel = angleVL > 100;
        } else {
            angleVL *= 0.98D;
        }

        return cancel;
    }
}
