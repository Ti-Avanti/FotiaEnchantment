package gg.fotia.enchantment.compat;

import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;

public final class BukkitItemFlags {

    private BukkitItemFlags() {
    }

    public static void hideEnchantments(ItemMeta meta) {
        if (meta == null) {
            return;
        }
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        addHideStoredEnchantments(meta);
    }

    public static boolean hasHideEnchantments(ItemMeta meta) {
        return meta != null && meta.hasItemFlag(ItemFlag.HIDE_ENCHANTS);
    }

    public static boolean hasHideStoredEnchantments(ItemMeta meta) {
        if (meta == null) {
            return true;
        }
        try {
            return meta.hasItemFlag(ItemFlag.HIDE_STORED_ENCHANTS);
        } catch (LinkageError ignored) {
            return true;
        }
    }

    public static boolean addHideStoredEnchantments(ItemMeta meta) {
        if (meta == null) {
            return false;
        }
        try {
            meta.addItemFlags(ItemFlag.HIDE_STORED_ENCHANTS);
            return true;
        } catch (LinkageError ignored) {
            return false;
        }
    }
}
