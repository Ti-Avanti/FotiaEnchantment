package gg.fotia.enchantment.integration;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequiredPluginCheckerTest {

    @Test
    void reportsMissingPacketEventsAsRequiredPlugin() {
        assertEquals(List.of("packetevents"),
                RequiredPluginChecker.missingRequiredPlugins(Set.of()));
    }

    @Test
    void acceptsInstalledPacketEventsCaseInsensitively() {
        assertTrue(RequiredPluginChecker.missingRequiredPlugins(Set.of("PacketEvents")).isEmpty());
    }
}
