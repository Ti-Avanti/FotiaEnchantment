package gg.fotia.enchantment.pipeline.condition.impl;

import gg.fotia.enchantment.pipeline.condition.Condition;
import gg.fotia.enchantment.pipeline.condition.ConditionContext;
import gg.fotia.enchantment.util.ItemUtils;
import gg.fotia.enchantment.core.EnchantmentData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * 目标拥有指定附魔条件
 * <p>检查目标装备物品 PDC 中是否存在 enchant_&lt;id&gt; 标签。
 */
public class TargetHasEnchantCondition implements Condition {

    @Override
    public String getId() {
        return "target_has_enchant";
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
        List<String> enchants = cfg.getStringList("value");
        if (enchants.isEmpty()) {
            String single = cfg.getString("value");
            if (single == null || single.isEmpty()) {
                return false;
            }
            enchants = List.of(single);
        }

        EntityEquipment eq = target.getEquipment();
        if (eq == null) {
            return false;
        }
        ItemStack[] toCheck = new ItemStack[]{
                eq.getHelmet(), eq.getChestplate(), eq.getLeggings(), eq.getBoots(),
                eq.getItemInMainHand(), eq.getItemInOffHand()
        };
        for (String id : enchants) {
            if (id == null || id.isEmpty()) continue;
            String tag = "enchant_" + id.toLowerCase();
            for (ItemStack it : toCheck) {
                if (it != null && ItemUtils.hasCustomTag(it, tag)) {
                    return true;
                }
            }
        }
        return false;
    }
}
