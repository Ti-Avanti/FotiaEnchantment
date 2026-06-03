package gg.fotia.enchantment.gui;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GUIManagerLifecycleTest {

    @Test
    void openRegistersGuiAfterInventoryIsOpened() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/gg/fotia/enchantment/gui/GUIManager.java"));

        int openCall = source.indexOf("gui.open();");
        int trackingCall = source.indexOf("openGUIs.put(gui.getPlayer().getUniqueId(), gui);");

        assertTrue(openCall >= 0, "GUIManager.open must call gui.open()");
        assertTrue(trackingCall >= 0, "GUIManager.open must track the opened GUI");
        assertTrue(openCall < trackingCall,
                "Opening from DeluxeMenus fires the old inventory close event during gui.open(); tracking first lets it remove the new GUI");
    }

    @Test
    void closePolicyOnlyMatchesTheTrackedInventory() {
        Object tracked = new Object();

        assertTrue(GUIManager.isTrackedInventoryClose(tracked, tracked));
        assertFalse(GUIManager.isTrackedInventoryClose(tracked, new Object()));
        assertFalse(GUIManager.isTrackedInventoryClose(null, tracked));
        assertFalse(GUIManager.isTrackedInventoryClose(tracked, null));
    }

    @Test
    void closeHandlerComparesClosedInventoryBeforeRemovingTrackedGui() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/gg/fotia/enchantment/gui/GUIManager.java"));

        assertTrue(source.contains("isTrackedInventoryClose(gui.getInventory(), event.getInventory())"),
                "Close events from a previous DeluxeMenus inventory must not remove the newly opened Fotia GUI");
        assertFalse(source.contains("BaseGUI gui = openGUIs.remove(player.getUniqueId());"),
                "GUIManager must not remove the tracked GUI before verifying the closed inventory");
    }
}
