package gg.fotia.enchantment.config;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnchantmentConfigValidationTest {

    @TempDir
    Path tempDir;

    @Test
    void invalidMaterialReportsExactConfigPath() {
        File file = tempDir.resolve("bad_material.yml").toFile();
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("id", "bad_material");
        yaml.set("applicable-items", List.of("NOT_A_MATERIAL"));

        List<EnchantmentConfig.ConfigIssue> issues = EnchantmentConfig.validateForLoad(yaml, file);

        assertEquals(1, issues.size());
        assertEquals("bad_material", issues.get(0).enchantmentId());
        assertEquals(file.getAbsolutePath(), issues.get(0).filePath());
        assertEquals("applicable-items[0]", issues.get(0).path());
        assertTrue(issues.get(0).message().contains("未知物品或物品类别"));
    }

    @Test
    void invalidEffectReportsExactNestedConfigPaths() {
        File file = tempDir.resolve("bad_effect.yml").toFile();
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("id", "bad_effect");
        yaml.set("effects", List.of(Map.of(
                "cooldown", -1,
                "actions", List.of(Map.of("value", 1))
        )));

        List<EnchantmentConfig.ConfigIssue> issues = EnchantmentConfig.validateForLoad(yaml, file);

        assertTrue(issues.stream().anyMatch(issue ->
                "effects[0].trigger".equals(issue.path())
                        && issue.message().contains("缺少必填字段")));
        assertTrue(issues.stream().anyMatch(issue ->
                "effects[0].cooldown".equals(issue.path())
                        && issue.message().contains("不能小于 0")));
        assertTrue(issues.stream().anyMatch(issue ->
                "effects[0].actions[0].type".equals(issue.path())
                        && issue.message().contains("缺少必填字段")));
    }

    @Test
    void invalidYamlReportsFileAndLineLocation() throws IOException {
        File file = tempDir.resolve("broken.yml").toFile();
        Files.writeString(file.toPath(), "id: broken\neffects:\n  - trigger: HOLD\n    actions: [\n",
                StandardCharsets.UTF_8);

        Exception ex = assertThrows(Exception.class, () -> EnchantmentConfig.loadYaml(file));
        EnchantmentConfig.ConfigIssue issue = EnchantmentConfig.yamlLoadIssue(file, ex);

        assertEquals("broken", issue.enchantmentId());
        assertEquals(file.getAbsolutePath(), issue.filePath());
        assertTrue(issue.path().contains("line"));
        assertTrue(issue.path().contains("column"));
        assertTrue(issue.message().contains("YAML 语法错误"));
    }

    @Test
    void villagerTradePriceRangeAcceptsLegacyMinMaxSection() {
        File file = tempDir.resolve("legacy_price_range.yml").toFile();
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("id", "legacy_price_range");
        yaml.createSection("villager-trade-price-range", Map.of(
                "min", 12,
                "max", 32
        ));

        List<EnchantmentConfig.ConfigIssue> issues = EnchantmentConfig.validateForLoad(yaml, file);

        assertTrue(issues.stream().noneMatch(issue ->
                "villager-trade-price-range".equals(issue.path())), () -> issues.toString());
    }

    @Test
    void bundledEnchantmentResourcesPassRuntimeValidation() throws IOException, InvalidConfigurationException {
        Path enchantments = Path.of("src", "main", "resources", "enchantments");
        try (Stream<Path> stream = Files.walk(enchantments)) {
            for (Path path : stream.filter(file -> file.toString().endsWith(".yml")).toList()) {
                File file = path.toFile();
                YamlConfiguration yaml = EnchantmentConfig.loadYaml(file);
                List<EnchantmentConfig.ConfigIssue> issues = EnchantmentConfig.validateForLoad(yaml, file);

                assertTrue(issues.isEmpty(), () -> path + " should pass runtime validation: " + issues);
            }
        }
    }
}
