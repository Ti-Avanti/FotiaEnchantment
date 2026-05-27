package gg.fotia.enchantment.compat;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BukkitAttributesTest {

    @Test
    void attributeKeyCandidatesPreferModernNamesBeforeLegacyNames() {
        assertEquals(List.of("max_health", "generic.max_health"), BukkitAttributes.maxHealthKeys());
        assertEquals(List.of("armor", "generic.armor"), BukkitAttributes.armorKeys());
        assertEquals(List.of("armor_toughness", "generic.armor_toughness"), BukkitAttributes.armorToughnessKeys());
        assertEquals(List.of("movement_speed", "generic.movement_speed"), BukkitAttributes.movementSpeedKeys());
        assertEquals(List.of("luck", "generic.luck"), BukkitAttributes.luckKeys());
    }

    @Test
    void sourceDoesNotDirectlyReferenceVersionSpecificAttributeFields() throws IOException {
        Pattern forbidden = Pattern.compile("\\bAttribute\\.(MAX_HEALTH|ARMOR|ARMOR_TOUGHNESS|MOVEMENT_SPEED|LUCK)\\b");
        try (Stream<Path> stream = Files.walk(Path.of("src", "main", "java"))) {
            List<Path> offenders = stream
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> {
                        try {
                            return forbidden.matcher(Files.readString(path)).find();
                        } catch (IOException ex) {
                            throw new IllegalStateException(ex);
                        }
                    })
                    .toList();

            assertTrue(offenders.isEmpty(), () -> "Use BukkitAttributes instead of direct Attribute fields: " + offenders);
        }
    }
}
