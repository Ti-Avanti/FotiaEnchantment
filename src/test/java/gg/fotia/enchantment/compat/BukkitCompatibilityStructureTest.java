package gg.fotia.enchantment.compat;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BukkitCompatibilityStructureTest {

    @Test
    void versionSpecificRuntimeApiCallsStayBehindCompatibilityHelpers() throws IOException {
        Map<Pattern, Predicate<Path>> forbidden = Map.of(
                Pattern.compile("\\bRegistry\\.EFFECT\\b"),
                allowed("src/main/java/gg/fotia/enchantment/compat/BukkitRegistryCompat.java"),
                Pattern.compile("\\bItemFlag\\.HIDE_STORED_ENCHANTS\\b"),
                allowed("src/main/java/gg/fotia/enchantment/compat/BukkitItemFlags.java"),
                Pattern.compile("\\bEnchantment\\.UNBREAKING\\b"),
                path -> false,
                Pattern.compile("\\.setEnchantmentGlintOverride\\("),
                allowed("src/main/java/gg/fotia/enchantment/util/ItemUtils.java"),
                Pattern.compile("\\bio\\.papermc\\.paper\\.ServerBuildInfo\\b"),
                path -> false
        );

        try (Stream<Path> stream = Files.walk(Path.of("src", "main", "java"))) {
            List<Path> offenders = stream
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> containsForbiddenReference(path, forbidden))
                    .toList();

            assertTrue(offenders.isEmpty(), () -> "Move version-specific API usage behind compat helpers: " + offenders);
        }
    }

    private static Predicate<Path> allowed(String normalizedPath) {
        return path -> path.toString().replace('\\', '/').equals(normalizedPath);
    }

    private static boolean containsForbiddenReference(Path path, Map<Pattern, Predicate<Path>> forbidden) {
        try {
            String source = Files.readString(path);
            for (Map.Entry<Pattern, Predicate<Path>> entry : forbidden.entrySet()) {
                if (entry.getKey().matcher(source).find() && !entry.getValue().test(path)) {
                    return true;
                }
            }
            return false;
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
