package gg.fotia.enchantment.compat;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.potion.PotionEffectType;

public final class BukkitRegistryCompat {

    private BukkitRegistryCompat() {
    }

    public static PotionEffectType potionEffect(NamespacedKey key) {
        if (key == null) {
            return null;
        }
        try {
            return Registry.EFFECT.get(key);
        } catch (LinkageError ignored) {
            return Registry.POTION_EFFECT_TYPE.get(key);
        }
    }

    public static Enchantment enchantment(NamespacedKey key) {
        return key == null ? null : Registry.ENCHANTMENT.get(key);
    }

    public static Enchantment unbreakingEnchantment() {
        return enchantment(NamespacedKey.minecraft("unbreaking"));
    }
}
