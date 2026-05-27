package gg.fotia.enchantment.pipeline.condition.impl;

import gg.fotia.enchantment.compat.BukkitAttributes;
import gg.fotia.enchantment.core.EnchantmentData;
import gg.fotia.enchantment.pipeline.condition.Condition;
import gg.fotia.enchantment.pipeline.condition.ConditionContext;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * 目标护甲值条件 - 目标的护甲点数位于 [min, max] 之间
 */
public class TargetArmorPointsCondition implements Condition {

    @Override
    public String getId() {
        return "target_armor_points";
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
        double min = context.evaluateExpression(cfg.getString("min", "0"));
        double max = context.evaluateExpression(cfg.getString("max", String.valueOf(Integer.MAX_VALUE)));

        double armor;
        if (target instanceof Player p) {
            armor = BukkitAttributes.armorValue(p);
        } else {
            armor = computeFromEquipment(target.getEquipment());
        }
        return armor >= min && armor <= max;
    }

    private double computeFromEquipment(EntityEquipment eq) {
        if (eq == null) return 0;
        double sum = 0;
        ItemStack[] armorItems = eq.getArmorContents();
        if (armorItems == null) return 0;
        for (ItemStack it : armorItems) {
            if (it == null) continue;
            ItemMeta meta = it.getItemMeta();
            if (meta instanceof ArmorMeta) {
                // 简化处理：根据材料估算
            }
            sum += estimate(it);
        }
        return sum;
    }

    private double estimate(ItemStack item) {
        String name = item.getType().name();
        // 粗略估算：仅用于无属性目标的兜底
        if (name.endsWith("_HELMET")) return 2;
        if (name.endsWith("_CHESTPLATE")) return 6;
        if (name.endsWith("_LEGGINGS")) return 5;
        if (name.endsWith("_BOOTS")) return 2;
        return 0;
    }
}
