package gg.fotia.enchantment.lore.item;

import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GeneratedLoreMarkerTest {

    @Test
    void stripsOnlyTheGeneratedPrefixRecordedByTheMarker() {
        List<Component> generated = List.of(
                Component.text("韧性 V"),
                Component.text("  受到伤害时有概率恢复生命。")
        );
        Component playerLore = Component.text("玩家自定义 Lore");
        List<Component> existing = List.of(
                generated.get(0),
                generated.get(1),
                Component.empty(),
                playerLore
        );

        String marker = GeneratedLoreMarker.encode(generated);

        assertEquals(
                List.of(playerLore),
                GeneratedLoreMarker.stripMarkedPrefix(existing, marker)
        );
    }

    @Test
    void keepsLoreWhenTheRecordedPrefixWasModified() {
        List<Component> generated = List.of(
                Component.text("韧性 V"),
                Component.text("  受到伤害时有概率恢复生命。")
        );
        List<Component> modified = List.of(
                Component.text("玩家写在最前面的 Lore"),
                generated.get(0),
                generated.get(1)
        );

        String marker = GeneratedLoreMarker.encode(generated);

        assertEquals(modified, GeneratedLoreMarker.stripMarkedPrefix(modified, marker));
    }
}
