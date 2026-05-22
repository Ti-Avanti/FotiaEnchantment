package gg.fotia.enchantment.pipeline;

import gg.fotia.enchantment.pipeline.condition.ConditionRegistry;
import gg.fotia.enchantment.pipeline.effect.EffectRegistry;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EffectPipelineCatalogTest {

    @Test
    void runtimeConditionRegistryMatchesWikiCatalog() throws IOException {
        Set<String> expected = wikiConditionIds();
        ConditionRegistry registry = new ConditionRegistry();

        EffectPipeline.registerBuiltinConditions(registry);

        Set<String> actual = new TreeSet<>(registry.getRegisteredIds());
        Set<String> missing = new TreeSet<>(expected);
        missing.removeAll(actual);

        assertTrue(missing.isEmpty(), "Missing condition registrations: " + missing);
        assertEquals(expected, actual);
        assertEquals(155, actual.size());
    }

    @Test
    void runtimeEffectRegistryMatchesWikiCatalog() throws IOException {
        Set<String> expected = wikiEffectIds();
        EffectRegistry registry = new EffectRegistry();

        EffectPipeline.registerBuiltinEffects(registry);

        Set<String> actual = new TreeSet<>(registry.getRegisteredIds());
        Set<String> missing = new TreeSet<>(expected);
        missing.removeAll(actual);

        assertTrue(missing.isEmpty(), "Missing effect registrations: " + missing);
        assertEquals(expected, actual);
        assertEquals(291, actual.size());
    }

    private static Set<String> wikiConditionIds() throws IOException {
        String source = wikiApp();
        Set<String> ids = new TreeSet<>();
        ids.addAll(extractEntryIds(block(source, "const conditions", "const effects"), false));
        return ids;
    }

    private static Set<String> wikiEffectIds() throws IOException {
        String source = wikiApp();
        return extractEntryIds(block(source, "const effects", "function td"), true);
    }

    private static String wikiApp() throws IOException {
        Path path = Path.of("..", "wiki", "app.js");
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private static String block(String source, String startToken, String endToken) {
        int start = source.indexOf(startToken);
        int end = source.indexOf(endToken, start + startToken.length());
        if (start < 0 || end < 0) {
            throw new IllegalStateException("Cannot locate wiki block " + startToken + " -> " + endToken);
        }
        return source.substring(start, end);
    }

    private static Set<String> extractEntryIds(String source, boolean upper) {
        Set<String> ids = new TreeSet<>();
        Matcher matcher = Pattern.compile("\"id\"\\s*:\\s*\"([^\"]+)\"").matcher(source);
        while (matcher.find()) {
            ids.add(normalize(matcher.group(1), upper));
        }
        return ids;
    }

    private static String normalize(String id, boolean upper) {
        return upper ? id.toUpperCase(Locale.ROOT) : id.toLowerCase(Locale.ROOT);
    }
}
