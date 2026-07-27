package gg.fotia.enchantment.core;

import org.bukkit.Location;
import org.bukkit.World;

public final class LocationDistance {

    private LocationDistance() {
    }

    /**
     * 跨世界或参数为 null 时返回 0。
     * 仅适用于"飞行距离"等数值计算场景 (返回无穷会放大数值);
     * 范围判定请使用 {@link #safeDistanceOrInfinity}, 否则跨世界目标会被误判为距离最近。
     */
    public static double safeDistance(Location first, Location second) {
        if (!sameWorld(first, second)) {
            return 0.0D;
        }
        return first.distance(second);
    }

    public static double safeDistanceOrInfinity(Location first, Location second) {
        if (!sameWorld(first, second)) {
            return Double.POSITIVE_INFINITY;
        }
        return first.distance(second);
    }

    public static double safeDistanceSquared(Location first, Location second) {
        if (!sameWorld(first, second)) {
            return Double.POSITIVE_INFINITY;
        }
        return first.distanceSquared(second);
    }

    public static boolean sameWorld(Location first, Location second) {
        if (first == null || second == null) {
            return false;
        }
        World firstWorld = first.getWorld();
        World secondWorld = second.getWorld();
        return firstWorld != null && firstWorld.equals(secondWorld);
    }
}
