package gg.fotia.enchantment.core;

import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LocationDistanceTest {

    @Test
    void safeDistanceReturnsZeroForDifferentWorlds() {
        Location lobby = new Location(world("lobby"), 0, 0, 0);
        Location overworld = new Location(world("world"), 3, 4, 0);

        assertThrows(IllegalArgumentException.class, () -> lobby.distance(overworld));

        assertDoesNotThrow(() -> LocationDistance.safeDistance(lobby, overworld));
        assertEquals(0.0, LocationDistance.safeDistance(lobby, overworld));
    }

    @Test
    void safeDistanceKeepsBukkitDistanceForSameWorld() {
        World world = world("world");
        Location first = new Location(world, 0, 0, 0);
        Location second = new Location(world, 3, 4, 0);

        assertEquals(5.0, LocationDistance.safeDistance(first, second));
    }

    @Test
    void safeDistanceOrInfinityTreatsDifferentWorldsAsOutOfRange() {
        Location lobby = new Location(world("lobby"), 0, 0, 0);
        Location overworld = new Location(world("world"), 3, 4, 0);

        assertEquals(Double.POSITIVE_INFINITY, LocationDistance.safeDistanceOrInfinity(lobby, overworld));
    }

    private static World world(String name) {
        return (World) Proxy.newProxyInstance(
                LocationDistanceTest.class.getClassLoader(),
                new Class<?>[]{World.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getName" -> name;
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "toString" -> name;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }
}
