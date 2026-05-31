package gg.fotia.enchantment.command;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandManagerStructureTest {

    private static final Path ROOT = Path.of("").toAbsolutePath();

    @Test
    void paperCommandRegistrationDoesNotDependOnAnonymousCommandClasses() throws IOException {
        String source = Files.readString(ROOT.resolve(
                "src/main/java/gg/fotia/enchantment/command/CommandManager.java"));

        assertFalse(source.contains("new BasicCommand()"),
                "Paper command bridge must be a named class so Paper lifecycle loading never depends on CommandManager$N");
        assertFalse(source.contains("new Command(\"fe\") {"),
                "Bukkit fallback command must be a named class so fallback loading never depends on CommandManager$N");
        assertFalse(source.contains("BasicCommand"),
                "CommandManager must not link Paper Brigadier classes on 1.20.x");
        assertFalse(source.contains("LifecycleEvents"),
                "CommandManager must not link Paper lifecycle command events on 1.20.x");
        assertTrue(Files.isRegularFile(ROOT.resolve(
                "src/main/java/gg/fotia/enchantment/command/PaperCommandRegistrar.java")));
        assertTrue(Files.isRegularFile(ROOT.resolve(
                "src/main/java/gg/fotia/enchantment/command/FePaperCommand.java")));
        assertTrue(Files.isRegularFile(ROOT.resolve(
                "src/main/java/gg/fotia/enchantment/command/FeBukkitCommand.java")));
    }
}
