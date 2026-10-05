package gg.fotia.enchantment.integration;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequiredPluginCheckerTest {

    @Test
    void enforcesNewProtocolsWithoutChangingLegacyRequirements() {
        assertTrue(RequiredPluginChecker.supportsPacketEvents("1.21.1", "2.11.2"));
        assertTrue(RequiredPluginChecker.supportsPacketEvents("1.21.11", "2.13.0"));
        assertFalse(RequiredPluginChecker.supportsPacketEvents("26.2", "2.12.2"));
        assertTrue(RequiredPluginChecker.supportsPacketEvents("26.2", "2.13.0"));
        assertFalse(RequiredPluginChecker.supportsPacketEvents("26.3", "2.13.0"));
        assertTrue(RequiredPluginChecker.supportsPacketEvents("26.3.1", "2.14.0"));
        assertTrue(RequiredPluginChecker.supportsPacketEvents("26.3", "2.14.1-SNAPSHOT"));
        assertFalse(RequiredPluginChecker.supportsPacketEvents("26.3", "unknown"));
        assertFalse(RequiredPluginChecker.supportsPacketEvents("26.3", null));
    }

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
