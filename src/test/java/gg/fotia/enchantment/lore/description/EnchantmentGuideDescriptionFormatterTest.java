package gg.fotia.enchantment.lore.description;

import gg.fotia.enchantment.core.EnchantmentData;
import gg.fotia.enchantment.lore.description.EnchantmentEffectDescriptionFormatter;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EnchantmentGuideDescriptionFormatterTest {

    @Test
    void computesLevelSpecificChanceHealingAndPotionDuration() {
        EnchantmentData data = new EnchantmentData();
        data.setMaxLevel(5);

        EnchantmentData.EffectBlock block = new EnchantmentData.EffectBlock();
        block.setTrigger("KILL_STREAK");
        block.setConditions(List.of(new EnchantmentData.ConditionConfig("chance", "{level} * 8")));

        EnchantmentData.ActionConfig heal = new EnchantmentData.ActionConfig("HEAL", "{level} * 2");
        EnchantmentData.ActionConfig strength = new EnchantmentData.ActionConfig("ADD_POTION_SELF", null);
        strength.setExtraParams(Map.of(
                "potion", "STRENGTH",
                "duration", "{level} * 30",
                "amplifier", 0
        ));
        block.setActions(List.of(heal, strength));
        data.setEffects(List.of(block));

        List<EnchantmentEffectDescriptionFormatter.LevelSummary> summaries =
                EnchantmentEffectDescriptionFormatter.buildSummaries(data, List.of(1));

        assertEquals(1, summaries.size());
        EnchantmentEffectDescriptionFormatter.LevelSummary summary = summaries.getFirst();
        assertEquals(1, summary.level());
        assertEquals("8", summary.chance());
        assertEquals("HEAL", summary.phrases().get(0).key());
        assertEquals("10", summary.phrases().get(0).placeholders().get("percent"));
        assertEquals("ADD_POTION_SELF", summary.phrases().get(1).key());
        assertEquals("STRENGTH", summary.phrases().get(1).placeholders().get("potion"));
        assertEquals("1.5", summary.phrases().get(1).placeholders().get("seconds"));
        assertEquals("1", summary.phrases().get(1).placeholders().get("amplifier"));
    }

    @Test
    void computesSpeedBoostDisplayedAmplifierAndConfiguredDuration() {
        EnchantmentData data = new EnchantmentData();

        EnchantmentData.EffectBlock block = new EnchantmentData.EffectBlock();
        block.setTrigger("MELEE_ATTACK_COMBO");

        EnchantmentData.ActionConfig speed = new EnchantmentData.ActionConfig("SPEED_BOOST", "{level} - 1");
        speed.setExtraParams(Map.of("duration", "{level} * 35"));
        block.setActions(List.of(speed));
        data.setEffects(List.of(block));

        List<EnchantmentEffectDescriptionFormatter.LevelSummary> summaries =
                EnchantmentEffectDescriptionFormatter.buildSummaries(data, List.of(2));

        assertEquals(1, summaries.size());
        EnchantmentEffectDescriptionFormatter.Phrase phrase = summaries.getFirst().phrases().getFirst();
        assertEquals("SPEED_BOOST", phrase.key());
        assertEquals("2", phrase.placeholders().get("amplifier"));
        assertEquals("3.5", phrase.placeholders().get("seconds"));
    }

    @Test
    void usesBonusDropMultiplierParameterWhenValueFieldIsMissing() {
        EnchantmentData data = new EnchantmentData();

        EnchantmentData.EffectBlock block = new EnchantmentData.EffectBlock();
        block.setTrigger("MINE_BLOCK");
        EnchantmentData.ActionConfig bonusDrop = new EnchantmentData.ActionConfig("BONUS_DROP", null);
        bonusDrop.setExtraParams(Map.of("multiplier", "{level} * 0.5 + 1"));
        block.setActions(List.of(bonusDrop));
        data.setEffects(List.of(block));

        List<EnchantmentEffectDescriptionFormatter.LevelSummary> summaries =
                EnchantmentEffectDescriptionFormatter.buildSummaries(data, List.of(1));

        assertEquals(1, summaries.size());
        EnchantmentEffectDescriptionFormatter.Phrase phrase = summaries.getFirst().phrases().getFirst();
        assertEquals("BONUS_DROP", phrase.key());
        assertEquals("1.5", phrase.placeholders().get("multiplier"));
    }

    @Test
    void usesVeinMineMaxBlocksParameterForDisplayedBlockCount() {
        EnchantmentData data = new EnchantmentData();

        EnchantmentData.EffectBlock block = new EnchantmentData.EffectBlock();
        block.setTrigger("MINE_ORE");
        EnchantmentData.ActionConfig veinMine = new EnchantmentData.ActionConfig("VEIN_MINE", null);
        veinMine.setExtraParams(Map.of("max-blocks", "{level} * 4 + 2"));
        block.setActions(List.of(veinMine));
        data.setEffects(List.of(block));

        List<EnchantmentEffectDescriptionFormatter.LevelSummary> summaries =
                EnchantmentEffectDescriptionFormatter.buildSummaries(data, List.of(3));

        assertEquals(1, summaries.size());
        EnchantmentEffectDescriptionFormatter.Phrase phrase = summaries.getFirst().phrases().getFirst();
        assertEquals("VEIN_MINE", phrase.key());
        assertEquals("14", phrase.placeholders().get("blocks"));
    }

    @Test
    void filtersEffectBlocksByLevelOnlyExpressionConditions() {
        EnchantmentData data = new EnchantmentData();

        EnchantmentData.EffectBlock lowLevel = new EnchantmentData.EffectBlock();
        lowLevel.setConditions(List.of(
                new EnchantmentData.ConditionConfig("chance", "8 + {level} * 2"),
                new EnchantmentData.ConditionConfig("expression_true", "{level} <= 2")
        ));
        EnchantmentData.ActionConfig lowResistance = new EnchantmentData.ActionConfig("ADD_POTION_SELF", null);
        lowResistance.setExtraParams(Map.of(
                "potion", "RESISTANCE",
                "duration", "40 + ({level} - 1) * 30",
                "amplifier", 0
        ));
        lowLevel.setActions(List.of(lowResistance));

        EnchantmentData.EffectBlock highLevel = new EnchantmentData.EffectBlock();
        highLevel.setConditions(List.of(
                new EnchantmentData.ConditionConfig("chance", "8 + {level} * 2"),
                new EnchantmentData.ConditionConfig("expression_true", "{level} >= 3")
        ));
        EnchantmentData.ActionConfig highResistance = new EnchantmentData.ActionConfig("ADD_POTION_SELF", null);
        highResistance.setExtraParams(Map.of(
                "potion", "RESISTANCE",
                "duration", "40 + ({level} - 1) * 30",
                "amplifier", 1
        ));
        highLevel.setActions(List.of(highResistance));
        data.setEffects(List.of(lowLevel, highLevel));

        List<EnchantmentEffectDescriptionFormatter.LevelSummary> summaries =
                EnchantmentEffectDescriptionFormatter.buildSummaries(data, List.of(5));

        assertEquals(1, summaries.size());
        assertEquals("18", summaries.getFirst().chance());
        assertEquals("2", summaries.getFirst().phrases().getFirst().placeholders().get("amplifier"));
    }

    @Test
    void rendersEffectOnlyLineWithCalculatedChanceAndValues() {
        EnchantmentData data = new EnchantmentData();

        EnchantmentData.EffectBlock block = new EnchantmentData.EffectBlock();
        block.setTrigger("KILL_STREAK");
        block.setConditions(List.of(new EnchantmentData.ConditionConfig("chance", "{level} * 8")));

        EnchantmentData.ActionConfig heal = new EnchantmentData.ActionConfig("HEAL", "{level} * 2");
        EnchantmentData.ActionConfig strength = new EnchantmentData.ActionConfig("ADD_POTION_SELF", null);
        strength.setExtraParams(Map.of(
                "potion", "STRENGTH",
                "duration", "{level} * 30",
                "amplifier", 0
        ));
        block.setActions(List.of(heal, strength));
        data.setEffects(List.of(block));

        List<String> lines = EnchantmentEffectDescriptionFormatter.renderLines(data, 1, key -> switch (key) {
            case "guide-gui.detail-line" -> "{level}级：{chance_phrase}{effects}。";
            case "guide-gui.detail-chance" -> "有 {chance}% 概率";
            case "guide-gui.detail-joiner" -> "并";
            case "guide-gui.effect-phrase-HEAL" -> "恢复 {percent}% 生命";
            case "guide-gui.effect-phrase-ADD_POTION_SELF" -> "获得{potion} {amplifier} 级，持续 {seconds} 秒";
            case "guide-gui.potion-STRENGTH" -> "力量";
            default -> key;
        });

        assertEquals(List.of("1级：有 8% 概率恢复 10% 生命并获得力量 1 级，持续 1.5 秒。"), lines);
    }
}
