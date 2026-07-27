package gg.fotia.enchantment.pipeline.condition.impl;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.core.EnchantmentConflictPolicy;
import gg.fotia.enchantment.core.EnchantmentData;
import gg.fotia.enchantment.core.PDCManager;
import gg.fotia.enchantment.pipeline.condition.Condition;
import gg.fotia.enchantment.pipeline.condition.ConditionContext;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

/**
 * 目标拥有指定附魔条件
 * <p>检查目标 6 个装备槽位上是否存在任一指定的自定义附魔 (走 PDCManager 真实附魔存储)。
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
            return false;
        }

        FotiaEnchantment plugin = context.getPlugin() != null
                ? context.getPlugin()
                : FotiaEnchantment.getInstance();
        if (plugin == null || plugin.getEnchantmentManager() == null) {
            return false;
        }
        PDCManager pdc = plugin.getEnchantmentManager().getPdcManager();
        if (pdc == null) {
            return false;
        }

        EntityEquipment eq = target.getEquipment();
        if (eq == null) {
            return false;
        }
        ItemStack[] toCheck = new ItemStack[]{
                eq.getHelmet(), eq.getChestplate(), eq.getLeggings(), eq.getBoots(),
                eq.getItemInMainHand(), eq.getItemInOffHand()
        };
        for (ItemStack item : toCheck) {
            if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
                continue;
            }
            Map<String, Integer> onItem = pdc.getEnchantments(item.getItemMeta());
            if (onItem.isEmpty()) {
                continue;
            }
            for (String id : enchants) {
                if (id == null || id.isEmpty()) {
                    continue;
                }
                if (onItem.containsKey(EnchantmentConflictPolicy.normalizeCustomId(id))) {
                    return true;
                }
            }
        }
        return false;
    }
}
