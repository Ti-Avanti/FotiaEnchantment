package gg.fotia.enchantment.core;

import org.bukkit.Location;
import org.bukkit.World;

public final class LocationDistance {

    private LocationDistance() {
    }

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
