package gg.fotia.enchantment.pipeline;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BonusDropEffectStructureTest {

    @Test
    void bonusDropHandlesBlockBreakEventDrops() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/gg/fotia/enchantment/pipeline/effect/impl/BonusDropEffect.java"));

        assertTrue(source.contains("BlockBreakEvent"),
                "BONUS_DROP must handle MINE_BLOCK/MINE_ORE triggers, which pass BlockBreakEvent");
        assertTrue(source.contains("event.setDropItems(false)"),
                "BONUS_DROP must cancel original block drops before spawning multiplied replacements");
        assertTrue(source.contains("event.getBlock().getDrops(tool, event.getPlayer())"),
                "BONUS_DROP must calculate drops with the player's tool for BlockBreakEvent");
        assertTrue(source.contains("dropItemNaturally"),
                "BONUS_DROP must spawn multiplied replacement drops for BlockBreakEvent");
    }
}
