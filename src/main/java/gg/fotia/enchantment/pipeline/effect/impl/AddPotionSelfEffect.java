package gg.fotia.enchantment.pipeline.effect.impl;

import gg.fotia.enchantment.compat.BukkitRegistryCompat;
import gg.fotia.enchantment.pipeline.effect.Effect;
import gg.fotia.enchantment.pipeline.effect.EffectContext;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Locale;

/**
 * 给自己施加药水效果
 *
 * <p>参数：
 * <ul>
 *     <li>potion - 药水类型 ID（如 SPEED, REGENERATION）</li>
 *     <li>duration - 持续 ticks</li>
 *     <li>amplifier - 等级（0 表示 I 级）</li>
 * </ul>
 */
public class AddPotionSelfEffect implements Effect {

    @Override
    public String getId() {
        return "ADD_POTION_SELF";
    }

    @Override
    public void execute(EffectContext context) {
        Player player = context.getTriggerContext().getPlayer();
        if (player == null) return;

        String potionName = context.getExtraParam("potion");
        if (potionName == null || potionName.isEmpty()) return;

        PotionEffectType type = resolvePotionType(potionName);
        if (type == null) return;

        int duration = context.getIntParam("duration", 100);
        int amplifier = context.getIntParam("amplifier", 0);

        player.addPotionEffect(new PotionEffect(type, Math.max(1, duration), Math.max(0, amplifier), true, true, true));
    }

    static PotionEffectType resolvePotionType(String name) {
        String normalized = name.trim().toLowerCase(Locale.ROOT);
        // 同时支持带命名空间或裸名称
        NamespacedKey key = normalized.contains(":")
                ? NamespacedKey.fromString(normalized)
                : NamespacedKey.minecraft(normalized);
        if (key == null) return null;
        return BukkitRegistryCompat.potionEffect(key);
    }
}
