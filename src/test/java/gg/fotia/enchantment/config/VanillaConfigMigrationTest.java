package gg.fotia.enchantment.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VanillaConfigMigrationTest {

    @Test
    void oldBundledSharpnessApplicabilityGainsSpear() {
        assertEquals(
                List.of("SWORD", "AXE", "MACE", "SPEAR"),
                VanillaConfig.migrateApplicableItems("sharpness", List.of("SWORD", "AXE", "MACE"))
        );
    }

    @Test
    void customSharpnessApplicabilityIsPreserved() {
        assertEquals(
                List.of("SWORD"),
                VanillaConfig.migrateApplicableItems("sharpness", List.of("SWORD"))
        );
    }

    @Test
    void generatedLungeFallbackNameMigratesToChineseDefault() {
        assertEquals("突进", VanillaConfig.migrateDisplayName("lunge", "Lunge"));
    }

    @Test
    void customLungeNameIsPreserved() {
        assertEquals("自定义突刺", VanillaConfig.migrateDisplayName("lunge", "自定义突刺"));
    }

    @Test
    void generatedLungeFallbackDescriptionMigratesToChineseDefault() {
        assertEquals(
                List.of("提高持矛突进攻击造成的伤害。"),
                VanillaConfig.migrateDescription("lunge", List.of("原版附魔，具体效果遵循服务器当前 Minecraft 版本。"))
        );
    }

    @Test
    void customLungeDescriptionIsPreserved() {
        assertEquals(
                List.of("自定义描述"),
                VanillaConfig.migrateDescription("lunge", List.of("自定义描述"))
        );
    }
}
