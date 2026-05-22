package gg.fotia.enchantment.structure;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SourceLayoutStructureTest {

    private static final Path ROOT = Path.of("").toAbsolutePath();

    @Test
    void guiRootContainsOnlySharedInfrastructure() throws IOException {
        Path guiRoot = ROOT.resolve("src/main/java/gg/fotia/enchantment/gui");

        Set<String> directJavaFiles;
        try (Stream<Path> stream = Files.list(guiRoot)) {
            directJavaFiles = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .map(path -> path.getFileName().toString())
                    .collect(Collectors.toSet());
        }

        assertEquals(Set.of("BaseGUI.java", "GUIManager.java"), directJavaFiles);
        assertTrue(Files.isDirectory(guiRoot.resolve("admin")));
        assertTrue(Files.isDirectory(guiRoot.resolve("codex")));
        assertTrue(Files.isDirectory(guiRoot.resolve("disenchant")));
        assertTrue(Files.isDirectory(guiRoot.resolve("fragment")));
        assertTrue(Files.isDirectory(guiRoot.resolve("guide")));
        assertTrue(Files.isDirectory(guiRoot.resolve("menu")));
    }

    @Test
    void loreRootIsSplitByResponsibility() throws IOException {
        Path loreRoot = ROOT.resolve("src/main/java/gg/fotia/enchantment/lore");

        boolean hasDirectJavaFiles;
        try (Stream<Path> stream = Files.list(loreRoot)) {
            hasDirectJavaFiles = stream
                    .filter(Files::isRegularFile)
                    .anyMatch(path -> path.getFileName().toString().endsWith(".java"));
        }

        assertFalse(hasDirectJavaFiles);
        assertTrue(Files.isDirectory(loreRoot.resolve("description")));
        assertTrue(Files.isDirectory(loreRoot.resolve("item")));
    }

    @Test
    void bootstrapSourcesUseLayeredCompatibilityLayout() {
        assertTrue(Files.isDirectory(ROOT.resolve("src/bootstrap/shared/java")));
        assertTrue(Files.isDirectory(ROOT.resolve("src/bootstrap/entrypoint/java")));
        assertTrue(Files.isDirectory(ROOT.resolve("src/bootstrap/paper/v1_21_R1/java")));
        assertTrue(Files.isDirectory(ROOT.resolve("src/bootstrap/paper/v1_21_R6/java")));

        assertFalse(Files.exists(ROOT.resolve("src/bootstrap/api")));
        assertFalse(Files.exists(ROOT.resolve("src/bootstrap/dispatcher")));
        assertFalse(Files.exists(ROOT.resolve("src/bootstrap/java")));
        assertFalse(Files.exists(ROOT.resolve("src/bootstrap/paper-v1_21_R1")));
        assertFalse(Files.exists(ROOT.resolve("src/bootstrap/paper-v1_21_R6")));
        assertFalse(Files.exists(ROOT.resolve("src/compat-paper-1.21.1")));
    }
}
