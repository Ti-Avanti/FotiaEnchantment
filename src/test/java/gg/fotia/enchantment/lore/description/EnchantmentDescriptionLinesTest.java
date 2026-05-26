package gg.fotia.enchantment.lore.description;

import gg.fotia.enchantment.core.EnchantmentData;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnchantmentDescriptionLinesTest {

    @Test
    void configuredLanguageDescriptionWinsOverGeneratedEffectText() {
        EnchantmentData data = damageEnchant();

        List<String> lines = EnchantmentDescriptionLines.customDescriptionOrGenerated(
                List.of("语言文件里的描述"),
                data,
                1,
                key -> key,
                "未配置附魔描述。"
        );

        assertEquals(List.of("语言文件里的描述"), lines);
    }

    @Test
    void configuredLanguageDescriptionRendersCurrentLevelPlaceholders() {
        EnchantmentData data = scalingEnchant();

        List<String> lines = EnchantmentDescriptionLines.customDescriptionOrGenerated(
                List.of("等级 {level}: {chance}% 概率，范围 {radius}，伤害 {amount}，持续 {seconds} 秒"),
                data,
                3,
                key -> key,
                "未配置附魔描述。"
        );

        assertEquals(List.of("等级 3: 36% 概率，范围 5，伤害 4.5，持续 6 秒"), lines);
    }

    @Test
    void unknownConfiguredDescriptionPlaceholdersStayVisible() {
        EnchantmentData data = scalingEnchant();

        List<String> lines = EnchantmentDescriptionLines.customDescriptionOrGenerated(
                List.of("等级 {level}: {custom_text}"),
                data,
                2,
                key -> key,
                "未配置附魔描述。"
        );

        assertEquals(List.of("等级 2: {custom_text}"), lines);
    }

    @Test
    void configuredLanguageDescriptionRendersHealingPercentAndPotionLevel() {
        EnchantmentData data = healingPotionEnchant();

        List<String> lines = EnchantmentDescriptionLines.customDescriptionOrGenerated(
                List.of("恢复 {amount} 点生命（{percent}%），抗性 {amplifier} 级，持续 {seconds} 秒"),
                data,
                2,
                key -> key,
                "未配置附魔描述。"
        );

        assertEquals(List.of("恢复 4 点生命（20%），抗性 1 级，持续 3 秒"), lines);
    }

    @Test
    void configuredLanguageDescriptionRendersNumberedRepeatedPlaceholders() {
        EnchantmentData data = repeatedPlaceholderEnchant();

        List<String> lines = EnchantmentDescriptionLines.customDescriptionOrGenerated(
                List.of(
                        "first {chance}/{chance1} for {seconds}/{seconds1}s",
                        "second {chance2}%",
                        "third {chance3}% for {seconds2}s"
                ),
                data,
                3,
                key -> key,
                "missing"
        );

        assertEquals(List.of(
                "first 20/20 for 3/3s",
                "second 13%",
                "third 12% for 6s"
        ), lines);
    }

    @Test
    void generatedEffectTextIsFallbackWhenLanguageDescriptionMissing() {
        EnchantmentData data = damageEnchant();

        List<String> lines = EnchantmentDescriptionLines.customDescriptionOrGenerated(
                List.of(),
                data,
                1,
                key -> switch (key) {
                    case "guide-gui.detail-line" -> "{level}: {effects}";
                    case "guide-gui.detail-joiner" -> " and ";
                    case "guide-gui.detail-chance" -> "{chance}% ";
                    case "guide-gui.effect-phrase-DAMAGE_ADD" -> "add {amount} damage";
                    default -> key;
                },
                "未配置附魔描述。"
        );

        assertEquals(List.of("1: add 2 damage"), lines);
    }

    @Test
    void missingTextIsLastFallback() {
        List<String> lines = EnchantmentDescriptionLines.customDescriptionOrGenerated(
                List.of(),
                new EnchantmentData(),
                1,
                key -> key,
                "未配置附魔描述。"
        );

        assertEquals(List.of("未配置附魔描述。"), lines);
    }

    private static EnchantmentData damageEnchant() {
        EnchantmentData data = new EnchantmentData();
        EnchantmentData.EffectBlock block = new EnchantmentData.EffectBlock();
        block.setTrigger("MELEE_ATTACK");
        EnchantmentData.ActionConfig action = new EnchantmentData.ActionConfig("DAMAGE_ADD", "2");
        action.setExtraParams(Map.of());
        block.setActions(List.of(action));
        data.setEffects(List.of(block));
        assertTrue(data.getEffects().size() == 1);
        return data;
    }

    private static EnchantmentData scalingEnchant() {
        EnchantmentData data = new EnchantmentData();
        EnchantmentData.EffectBlock block = new EnchantmentData.EffectBlock();
        block.setTrigger("MELEE_ATTACK");

        EnchantmentData.ConditionConfig chance = new EnchantmentData.ConditionConfig("chance", "{level} * 12");
        EnchantmentData.ActionConfig action = new EnchantmentData.ActionConfig("DAMAGE_ADD", "{level} * 1.5");
        action.setExtraParams(Map.of(
                "radius", "2 + {level}",
                "duration", "{level} * 40"
        ));

        block.setConditions(List.of(chance));
        block.setActions(List.of(action));
        data.setEffects(List.of(block));
        return data;
    }

    private static EnchantmentData repeatedPlaceholderEnchant() {
        EnchantmentData data = new EnchantmentData();
        data.setEffects(List.of(
                potionBlock("JUMP", "20", "{level} * 20"),
                potionBlock("TAKE_DAMAGE", "10 + {level}", null),
                potionBlock("TAKE_DAMAGE", "{level} * 4", "{level} * 40")
        ));
        return data;
    }

    private static EnchantmentData.EffectBlock potionBlock(String trigger, String chanceValue, String duration) {
        EnchantmentData.EffectBlock block = new EnchantmentData.EffectBlock();
        block.setTrigger(trigger);
        block.setConditions(List.of(new EnchantmentData.ConditionConfig("chance", chanceValue)));

        EnchantmentData.ActionConfig potion = new EnchantmentData.ActionConfig("ADD_POTION_SELF", null);
        Map<String, Object> params = new java.util.LinkedHashMap<>();
        params.put("potion", "REGENERATION");
        params.put("amplifier", 0);
        if (duration != null) {
            params.put("duration", duration);
        }
        potion.setExtraParams(params);
        block.setActions(List.of(potion));
        return block;
    }

    private static EnchantmentData healingPotionEnchant() {
        EnchantmentData data = new EnchantmentData();
        EnchantmentData.EffectBlock block = new EnchantmentData.EffectBlock();
        block.setTrigger("TAKE_DAMAGE");

        EnchantmentData.ActionConfig heal = new EnchantmentData.ActionConfig("HEAL", "{level} * 2");
        EnchantmentData.ActionConfig potion = new EnchantmentData.ActionConfig("ADD_POTION_SELF", null);
        potion.setExtraParams(Map.of(
                "potion", "DAMAGE_RESISTANCE",
                "amplifier", 0,
                "duration", "{level} * 30"
        ));

        block.setActions(List.of(heal, potion));
        data.setEffects(List.of(block));
        return data;
    }
}
