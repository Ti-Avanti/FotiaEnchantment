package gg.fotia.enchantment.integration;

import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import net.kyori.adventure.text.Component;
import org.bukkit.GameMode;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PacketEventsHookLoreDeduplicationTest {

    @Test
    void decoratesCursorAndPlayerInventoryItemPackets() {
        assertEquals(true, PacketEventsHook.shouldDecorateItemPacket(PacketType.Play.Server.SET_SLOT));
        assertEquals(true, PacketEventsHook.shouldDecorateItemPacket(PacketType.Play.Server.WINDOW_ITEMS));
        assertEquals(true, PacketEventsHook.shouldDecorateItemPacket(PacketType.Play.Server.SET_CURSOR_ITEM));
        assertEquals(true, PacketEventsHook.shouldDecorateItemPacket(PacketType.Play.Server.SET_PLAYER_INVENTORY));
    }

    @Test
    void skipsVisualItemPacketDecorationForCreativeAndSpectatorPlayers() {
        assertEquals(true, PacketEventsHook.shouldDecorateForGameMode(GameMode.SURVIVAL));
        assertEquals(true, PacketEventsHook.shouldDecorateForGameMode(GameMode.ADVENTURE));
        assertEquals(false, PacketEventsHook.shouldDecorateForGameMode(GameMode.CREATIVE));
        assertEquals(false, PacketEventsHook.shouldDecorateForGameMode(GameMode.SPECTATOR));
    }

    @Test
    void stripsRepeatedGeneratedLoreBlocksFromExistingLorePrefix() {
        List<Component> generated = List.of(
                Component.text("经验倍增 III"),
                Component.text("获取经验时获得额外加成"),
                Component.text("烈焰之刃 IV"),
                Component.text("攻击时有概率点燃目标")
        );
        List<Component> playerLore = List.of(Component.text("玩家自定义 lore"));
        List<Component> existing = List.of(
                generated.get(0),
                generated.get(1),
                generated.get(2),
                generated.get(3),
                Component.empty(),
                generated.get(0),
                generated.get(1),
                generated.get(2),
                generated.get(3),
                Component.empty(),
                playerLore.get(0)
        );

        assertEquals(playerLore, PacketEventsHook.stripGeneratedLoreCopies(existing, generated));
    }

    @Test
    void packetLoreDecorationSkipsDisabledVanillaEnchantments() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/gg/fotia/enchantment/integration/PacketEventsHook.java"));

        assertTrue(source.contains("isDisabledVanilla(enchantment)"),
                "Packet-only lore decoration must not show vanilla enchantments disabled by config");
    }

    @Test
    void packetLoreDecorationStripsSourceGeneratedLore() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/gg/fotia/enchantment/integration/PacketEventsHook.java"));

        assertTrue(source.contains("sourceGeneratedLore"),
                "Packet-only lore decoration must build a source generated lore set for stale cleanup");
        assertTrue(source.contains("mergeGeneratedLore(existingLore, generatedLore, sourceGeneratedLore)"),
                "Packet-only lore decoration must remove stale generated lore before prepending current lore");
    }
}
