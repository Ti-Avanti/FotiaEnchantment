package gg.fotia.enchantment.core;

import gg.fotia.enchantment.config.ConfigManager;

import java.util.Collection;
import java.util.Locale;

/**
 * 附魔组数量限制策略 - 实现 groups.yml 中 max-per-item 的同组附魔上限。
 * 附魔的所属组来自附魔配置的 group 字段, 组上限在 ConfigManager 加载时预解析。
 */
public final class EnchantmentGroupPolicy {

    private EnchantmentGroupPolicy() {
    }

    /**
     * 检查在 existingIds 基础上加入 candidate 是否仍满足其所在组的 max-per-item 上限。
     *
     * @param existingIds 物品上已有(或本次已选定)的自定义附魔ID集合
     * @return true=可以添加
     */
    public static boolean canAddToGroup(ConfigManager configManager,
                                        EnchantmentManager manager,
                                        Collection<String> existingIds,
                                        EnchantmentData candidate) {
        if (configManager == null || manager == null || candidate == null) {
            return true;
        }
        String group = candidate.getGroup();
        if (group == null || group.isBlank()) {
            return true;
        }
        int limit = configManager.getGroupMaxPerItem(group);
        if (limit < 0) {
            return true;
        }
        if (limit == 0) {
            return false;
        }
        if (existingIds == null || existingIds.isEmpty()) {
            return true;
        }

        String normalizedGroup = group.toLowerCase(Locale.ROOT);
        String candidateId = candidate.getId();
        int count = 0;
        for (String id : existingIds) {
            // 同一附魔升级等级不新增组占用
            if (id == null || id.equals(candidateId)) {
                continue;
            }
            EnchantmentData existing = manager.getEnchantment(id);
            if (existing == null || existing.getGroup() == null) {
                continue;
            }
            if (normalizedGroup.equals(existing.getGroup().toLowerCase(Locale.ROOT))) {
                count++;
                if (count >= limit) {
                    return false;
                }
            }
        }
        return true;
    }
}
