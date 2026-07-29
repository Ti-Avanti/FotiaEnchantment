package gg.fotia.enchantment.pipeline;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EffectPipelineFoliaCompatibilityTest {

    private static final Path ROOT = Path.of("").toAbsolutePath();

    @Test
    void resettingEffectBudgetDoesNotRequireACurrentTickingRegion() throws IOException {
        String source = Files.readString(ROOT.resolve(
                "src/main/java/gg/fotia/enchantment/pipeline/EffectPipeline.java"));
        int methodStart = source.indexOf("public void resetTickCounter()");
        int methodEnd = source.indexOf("\n    }", methodStart);
        String method = source.substring(methodStart, methodEnd);

        assertFalse(method.contains("Bukkit.getCurrentTick()"));
        assertTrue(method.contains("currentTickStamp = Integer.MIN_VALUE"));
    }
}
