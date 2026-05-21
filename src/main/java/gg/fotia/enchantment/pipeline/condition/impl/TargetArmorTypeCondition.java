package gg.fotia.enchantment.pipeline.condition.impl;

import gg.fotia.enchantment.pipeline.condition.Condition;
import gg.fotia.enchantment.pipeline.condition.ConditionContext;
import gg.fotia.enchantment.core.EnchantmentData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * 目标护甲材质条件 - 目标穿戴护甲的材质前缀（如 DIAMOND, NETHERITE）
 * <p>仅判断是否至少有一件护甲材质命中。
 */
public class TargetArmorTypeCondition implements Condition {

    @Override
    public String getId() {
        return "target_armor_type";
    }

    @Override
    public boolean check(ConditionContext context) {
        LivingEntity target = context.getTriggerContext().getTarget();
        if (target == null) {
            return false;
        }
        EnchantmentData.ConditionConfig cfg = context.getConfig();
        if (cfg == null) {
            return false;
        }
        List<String> types = cfg.getStringList("value");
        if (types.isEmpty()) {
            String single = cfg.getString("value");
            if (single == null || single.isEmpty()) {
                return false;
            }
            types = List.of(single);
        }

        EntityEquipment eq = target.getEquipment();
        if (eq == null) {
            return false;
        }
        ItemStack[] armor = eq.getArmorContents();
        if (armor == null) {
            return false;
        }
        for (ItemStack it : armor) {
            if (it == null) continue;
            String name = it.getType().name();
            for (String t : types) {
                if (t == null) continue;
                if (name.startsWith(t.toUpperCase() + "_")) {
                    return true;
                }
            }
        }
        return false;
    }
}
