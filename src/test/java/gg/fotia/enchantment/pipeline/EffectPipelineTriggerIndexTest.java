package gg.fotia.enchantment.pipeline;

import gg.fotia.enchantment.core.EnchantmentData;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EffectPipelineTriggerIndexTest {

    @Test
    void triggerIndexGroupsEnabledEffectBlocksByNormalizedTriggerId() {
        EnchantmentData active = enchantment("active", true,
                block("hold"),
                block("WEAR"),
                block("HOLD"));
        EnchantmentData disabled = enchantment("disabled", false, block("HOLD"));
        EnchantmentData withoutTrigger = enchantment("without_trigger", true, block(null));

        Map<String, List<EffectPipeline.TriggerBinding>> index = EffectPipeline.buildTriggerIndex(
                List.of(active, disabled, withoutTrigger));

        assertEquals(2, index.size());
        assertTrue(index.containsKey("HOLD"));
        assertTrue(index.containsKey("WEAR"));
        assertEquals(2, index.get("HOLD").size());
        assertEquals(1, index.get("WEAR").size());
        assertSame(active, index.get("HOLD").get(0).getData());
        assertEquals(0, index.get("HOLD").get(0).getEffectIndex());
        assertEquals(2, index.get("HOLD").get(1).getEffectIndex());
        assertSame(active, index.get("WEAR").get(0).getData());
    }

    private static EnchantmentData enchantment(String id, boolean enabled, EnchantmentData.EffectBlock... blocks) {
        EnchantmentData data = new EnchantmentData();
        data.setId(id);
        data.setEnabled(enabled);
        data.setEffects(List.of(blocks));
        return data;
    }

    private static EnchantmentData.EffectBlock block(String trigger) {
        EnchantmentData.EffectBlock block = new EnchantmentData.EffectBlock();
        block.setTrigger(trigger);
        return block;
    }
}
