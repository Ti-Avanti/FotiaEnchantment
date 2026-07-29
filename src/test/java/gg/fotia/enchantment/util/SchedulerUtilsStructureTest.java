package gg.fotia.enchantment.util;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchedulerUtilsStructureTest {

    private static final Path ROOT = Path.of("").toAbsolutePath();

    @Test
    void schedulerFacadeUsesRegionSchedulersWithoutPlatformNameDetection() throws IOException {
        String source = Files.readString(ROOT.resolve(
                "src/main/java/gg/fotia/enchantment/util/SchedulerUtils.java"));

        assertFalse(source.contains("Bukkit.getServer().getName()"));
        assertFalse(source.contains("Bukkit.getServer().getVersion()"));
        assertFalse(source.contains("Bukkit.getScheduler()"));
        assertFalse(source.contains("IS_FOLIA"));

        assertTrue(source.contains("Bukkit.getGlobalRegionScheduler().run("));
        assertTrue(source.contains("Bukkit.getGlobalRegionScheduler().runDelayed("));
        assertTrue(source.contains("Bukkit.getGlobalRegionScheduler().runAtFixedRate("));
        assertTrue(source.contains("entity.getScheduler().run("));
        assertTrue(source.contains("entity.getScheduler().runDelayed("));
        assertTrue(source.contains("Bukkit.getRegionScheduler().run("));
    }

    @Test
    void productionCodeDoesNotBranchOnFoliaIdentity() throws IOException {
        Path mainJava = ROOT.resolve("src/main/java");
        try (Stream<Path> files = Files.walk(mainJava)) {
            boolean hasIdentityBranch = files
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .anyMatch(SchedulerUtilsStructureTest::usesFoliaIdentityCheck);

            assertFalse(hasIdentityBranch);
        }
    }

    private static boolean usesFoliaIdentityCheck(Path path) {
        try {
            return Files.readString(path).contains("SchedulerUtils.isFolia()");
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to inspect " + path, exception);
        }
    }
}
