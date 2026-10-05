package gg.fotia.enchantment.lore.item;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GeneratedLoreMarkerTest {

    @Test
    void markerIsStableAcrossAdventureVersionsAndItemSerialization() {
        Component original = Component.text("owned", NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false);
        Component roundTrip = GsonComponentSerializer.gson().deserialize(
                GsonComponentSerializer.gson().serialize(original));
        assertEquals("v2:1:ca050e02bc04da81dc4342878f7d84b770907fbfb542f7e5d1bc1ac31e679d52",
                GeneratedLoreMarker.encode(List.of(roundTrip)));
    }

    @Test
    void acceptsLegacyMarkerWithoutRemovingForeignLore() {
        List<Component> generated = List.of(Component.text("Old enchantment"));
        Component foreign = Component.text("Foreign lore");
        String marker = "v1:1:" + LoreFingerprint.legacy(generated);
        assertEquals(List.of(foreign), GeneratedLoreMarker.stripMarkedPrefix(
                List.of(generated.getFirst(), Component.empty(), foreign), marker));
    }

    @Test
    void preservesEditedStylesAndMalformedMarkers() {
        Component original = Component.text("owned", NamedTextColor.GOLD);
        List<Component> edited = List.of(original.color(NamedTextColor.RED));
        assertEquals(edited, GeneratedLoreMarker.stripMarkedPrefix(edited,
                GeneratedLoreMarker.encode(List.of(original))));
        for (String marker : List.of("v3:1:anything", "v2:-1:bad", "v2:2147483648:bad", "v2:9:bad")) {
            assertEquals(edited, GeneratedLoreMarker.stripMarkedPrefix(edited, marker));
        }
    }

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
