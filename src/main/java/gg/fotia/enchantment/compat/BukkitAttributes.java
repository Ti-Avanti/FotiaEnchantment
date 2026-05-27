package gg.fotia.enchantment.compat;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;

import java.util.List;

/**
 * Attribute lookup that prefers modern Bukkit keys and falls back to legacy generic keys.
 */
public final class BukkitAttributes {

    private static final List<String> MAX_HEALTH_KEYS = List.of("max_health", "generic.max_health");
    private static final List<String> ARMOR_KEYS = List.of("armor", "generic.armor");
    private static final List<String> ARMOR_TOUGHNESS_KEYS = List.of("armor_toughness", "generic.armor_toughness");
    private static final List<String> MOVEMENT_SPEED_KEYS = List.of("movement_speed", "generic.movement_speed");
    private static final List<String> LUCK_KEYS = List.of("luck", "generic.luck");

    private BukkitAttributes() {
    }

    public static Attribute maxHealth() {
        return first(MAX_HEALTH_KEYS);
    }

    public static Attribute armor() {
        return first(ARMOR_KEYS);
    }

    public static Attribute armorToughness() {
        return first(ARMOR_TOUGHNESS_KEYS);
    }

    public static Attribute movementSpeed() {
        return first(MOVEMENT_SPEED_KEYS);
    }

    public static Attribute luck() {
        return first(LUCK_KEYS);
    }

    public static AttributeInstance get(LivingEntity entity, Attribute attribute) {
        if (entity == null || attribute == null) {
            return null;
        }
        return entity.getAttribute(attribute);
    }

    public static double value(LivingEntity entity, Attribute attribute, double fallback) {
        AttributeInstance instance = get(entity, attribute);
        return instance == null ? fallback : instance.getValue();
    }

    public static double maxHealthValue(LivingEntity entity) {
        return value(entity, maxHealth(), 20.0D);
    }

    public static double armorValue(LivingEntity entity) {
        return value(entity, armor(), 0.0D);
    }

    public static double armorToughnessValue(LivingEntity entity) {
        return value(entity, armorToughness(), 0.0D);
    }

    public static double movementSpeedValue(LivingEntity entity) {
        return value(entity, movementSpeed(), 0.0D);
    }

    public static double luckValue(LivingEntity entity) {
        return value(entity, luck(), 0.0D);
    }

    static List<String> maxHealthKeys() {
        return MAX_HEALTH_KEYS;
    }

    static List<String> armorKeys() {
        return ARMOR_KEYS;
    }

    static List<String> armorToughnessKeys() {
        return ARMOR_TOUGHNESS_KEYS;
    }

    static List<String> movementSpeedKeys() {
        return MOVEMENT_SPEED_KEYS;
    }

    static List<String> luckKeys() {
        return LUCK_KEYS;
    }

    private static Attribute first(List<String> keys) {
        for (String key : keys) {
            Attribute attribute = Registry.ATTRIBUTE.get(NamespacedKey.minecraft(key));
            if (attribute != null) {
                return attribute;
            }
        }
        return null;
    }
}
