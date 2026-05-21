package gg.fotia.enchantment.gui;

import gg.fotia.enchantment.core.EnchantmentData;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnchantmentGuideEffectFormatterTest {

    @Test
    void expandsOnlyActionEffects() {
        EnchantmentData data = new EnchantmentData();

        EnchantmentData.EffectBlock first = new EnchantmentData.EffectBlock();
        first.setTrigger("MELEE_ATTACK");
        first.setCooldown(5);
        EnchantmentData.ConditionConfig chance = new EnchantmentData.ConditionConfig("chance", "35");
        first.setConditions(List.of(chance));
        EnchantmentData.ActionConfig damage = new EnchantmentData.ActionConfig("DAMAGE_ADD", null);
        damage.setExtraParams(Map.of("amount", "{level} * 1.5"));
        EnchantmentData.ActionConfig particle = new EnchantmentData.ActionConfig("PARTICLE", null);
        particle.setExtraParams(Map.of("particle", "FLAME", "count", 8));
        first.setActions(List.of(damage, particle));

        EnchantmentData.EffectBlock second = new EnchantmentData.EffectBlock();
        second.setTrigger("FIRE_DAMAGE");
        EnchantmentData.ActionConfig potion = new EnchantmentData.ActionConfig("ADD_POTION_SELF", null);
        potion.setExtraParams(Map.of("potion", "FIRE_RESISTANCE", "duration", "{level} * 40", "amplifier", 0));
        second.setActions(List.of(potion));

        data.setEffects(List.of(first, second));

        List<EnchantmentGuideEffectFormatter.DetailLine> lines = EnchantmentGuideEffectFormatter.buildActionLines(
                data,
                action -> "action:" + action
        );

        assertEquals(3, lines.size());
        assertEquals(0, lines.stream().filter(line -> line.type() == EnchantmentGuideEffectFormatter.LineType.TRIGGER).count());
        assertEquals(0, lines.stream().filter(line -> line.type() == EnchantmentGuideEffectFormatter.LineType.CONDITION).count());
        assertEquals(3, lines.stream().filter(line -> line.type() == EnchantmentGuideEffectFormatter.LineType.ACTION).count());
        assertEquals(0, lines.stream().filter(line -> line.type() == EnchantmentGuideEffectFormatter.LineType.COOLDOWN).count());
        assertTrue(lines.stream().anyMatch(line -> line.name().equals("action:DAMAGE_ADD") && line.parameters().contains("amount={level} * 1.5")));
        assertTrue(lines.stream().anyMatch(line -> line.name().equals("action:PARTICLE") && line.parameters().contains("particle=FLAME")));
        assertTrue(lines.stream().anyMatch(line -> line.name().equals("action:ADD_POTION_SELF") && line.parameters().contains("potion=FIRE_RESISTANCE")));
    }
}
