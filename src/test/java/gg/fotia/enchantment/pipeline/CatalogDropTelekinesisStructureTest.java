package gg.fotia.enchantment.pipeline;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CatalogDropTelekinesisStructureTest {

    @Test
    void dropTelekinesisConsumesOriginalDropsBeforeAddingToInventory() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/gg/fotia/enchantment/pipeline/effect/impl/CatalogEffect.java"));

        assertTrue(source.contains("handleDropPickup"),
                "DROP_TELEKINESIS must use a dedicated drop pickup path instead of cloning items blindly");
        assertTrue(source.contains("blockBreakEvent.setDropItems(false)"),
                "DROP_TELEKINESIS must cancel original BlockBreakEvent drops before adding them to inventory");
        assertTrue(source.contains("dropEvent.getItemDrop().remove()"),
                "DROP_TELEKINESIS must remove the original dropped item entity after pickup");
        assertTrue(source.contains("dropInventoryOverflow"),
                "DROP_TELEKINESIS must drop only inventory overflow instead of duplicating the full stack");
    }
}
