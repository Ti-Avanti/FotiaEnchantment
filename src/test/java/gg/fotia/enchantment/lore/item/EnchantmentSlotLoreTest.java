package gg.fotia.enchantment.lore.item;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EnchantmentSlotLoreTest {

    @Test
    void fillsRemainingLimitedEnchantSlots() {
        assertEquals(
                List.of("[可附魔槽位]", "[可附魔槽位]", "[可附魔槽位]"),
                EnchantmentSlotLore.emptySlotLines(3, 0, "[可附魔槽位]")
        );
        assertEquals(
                List.of("[可附魔槽位]", "[可附魔槽位]"),
                EnchantmentSlotLore.emptySlotLines(3, 1, "[可附魔槽位]")
        );
    }

    @Test
    void doesNotDisplaySlotsForUnlimitedOrFullItems() {
        assertEquals(List.of(), EnchantmentSlotLore.emptySlotLines(-1, 0, "[可附魔槽位]"));
        assertEquals(List.of(), EnchantmentSlotLore.emptySlotLines(3, 3, "[可附魔槽位]"));
        assertEquals(List.of(), EnchantmentSlotLore.emptySlotLines(3, 5, "[可附魔槽位]"));
    }

    @Test
    void canDisplayCompactUsedAndMaxSlotSummary() {
        assertEquals(
                List.of("可附魔槽位: 1/3"),
                EnchantmentSlotLore.slotLines(
                        3,
                        1,
                        "summary",
                        "[可附魔槽位]",
                        "可附魔槽位: {used}/{max}"
                )
        );
        assertEquals(
                List.of("可附魔槽位: 3/3"),
                EnchantmentSlotLore.slotLines(
                        3,
                        3,
                        "summary",
                        "[可附魔槽位]",
                        "可附魔槽位: {used}/{max}"
                )
        );
    }

    @Test
    void lineModeRemainsDefaultSlotDisplay() {
        assertEquals(
                List.of("[可附魔槽位]", "[可附魔槽位]"),
                EnchantmentSlotLore.slotLines(
                        3,
                        1,
                        "lines",
                        "[可附魔槽位]",
                        "可附魔槽位: {used}/{max}"
                )
        );
    }
}
