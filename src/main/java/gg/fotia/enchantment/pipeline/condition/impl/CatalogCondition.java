package gg.fotia.enchantment.pipeline.condition.impl;

import gg.fotia.enchantment.core.EnchantmentData;
import gg.fotia.enchantment.integration.WorldGuardHook;
import gg.fotia.enchantment.pipeline.condition.Condition;
import gg.fotia.enchantment.pipeline.condition.ConditionContext;
import gg.fotia.enchantment.pipeline.trigger.TriggerContext;
import gg.fotia.enchantment.util.ExpressionPredicate;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Statistic;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Boss;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.util.RayTraceResult;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 对标目录条件实现。
 *
 * <p>这些条件共享一套 Bukkit 上下文读取逻辑；没有安装外部插件时，外部生态类条件
 * 会尝试读取同名 scoreboard objective，保证配置有明确可落地的数据来源。</p>
 */
public class CatalogCondition implements Condition {

    private final String id;

    public CatalogCondition(String id) {
        this.id = id == null ? "" : id.toLowerCase(Locale.ROOT);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean check(ConditionContext context) {
        if (context == null || context.getTriggerContext() == null) {
            return false;
        }
        Player player = context.getTriggerContext().getPlayer();
        LivingEntity target = context.getTriggerContext().getTarget();
        EnchantmentData.ConditionConfig cfg = context.getConfig();
        if (player == null || cfg == null) {
            return false;
        }

        return switch (id) {
            case "health_percent_above" -> healthPercent(player) > number(context, "value", value(context, 0));
            case "health_percent_below" -> healthPercent(player) < number(context, "value", value(context, 100));
            case "health_percent_between" -> between(healthPercent(player), context, 0, 100);
            case "xp_level_at_least" -> player.getLevel() >= number(context, "value", 0);
            case "xp_level_below" -> player.getLevel() < number(context, "value", 0);
            case "food_percent_above" -> player.getFoodLevel() / 20.0 * 100.0 > number(context, "value", 0);
            case "food_percent_below" -> player.getFoodLevel() / 20.0 * 100.0 < number(context, "value", 100);
            case "saturation_above" -> player.getSaturation() > number(context, "value", 0);
            case "oxygen_above" -> player.getRemainingAir() > number(context, "value", 0);
            case "oxygen_below" -> player.getRemainingAir() < number(context, "value", player.getMaximumAir());
            case "absorption_above" -> player.getAbsorptionAmount() > number(context, "value", 0);
            case "armor_points_above" -> attribute(player, Attribute.ARMOR) > number(context, "value", 0);
            case "armor_toughness_above" -> attribute(player, Attribute.ARMOR_TOUGHNESS) > number(context, "value", 0);
            case "luck_above" -> attribute(player, Attribute.LUCK) > number(context, "value", 0);
            case "stat_value_above" -> statistic(player, text(cfg, "stat", "value", "PLAY_ONE_MINUTE")) > number(context, "value", 0);

            case "is_on_ground" -> player.isOnGround();
            case "is_in_air" -> !player.isOnGround();
            case "is_falling" -> player.getFallDistance() > 0.0F || player.getVelocity().getY() < -0.08;
            case "is_flying" -> player.isFlying();
            case "is_gliding" -> player.isGliding();
            case "is_swimming" -> player.isSwimming();
            case "is_sneaking" -> player.isSneaking();
            case "is_sprinting" -> player.isSprinting();
            case "is_riding" -> player.isInsideVehicle();
            case "is_climbing" -> isClimbable(player.getLocation().getBlock().getType());
            case "is_submerged" -> player.getEyeLocation().getBlock().isLiquid();
            case "is_frozen" -> player.getFreezeTicks() > 0;
            case "velocity_below" -> player.getVelocity().length() < number(context, "value", 0);
            case "fall_distance_above" -> player.getFallDistance() > number(context, "value", 0);
            case "movement_speed_above" -> attribute(player, Attribute.MOVEMENT_SPEED) > number(context, "value", 0);
            case "looking_at_block" -> lookingAtBlock(player, cfg);

            case "mainhand_is" -> materialMatches(player.getInventory().getItemInMainHand(), cfg);
            case "offhand_is" -> materialMatches(player.getInventory().getItemInOffHand(), cfg);
            case "wearing_helmet" -> itemPresent(player.getInventory().getHelmet(), cfg);
            case "wearing_chestplate" -> itemPresent(player.getInventory().getChestplate(), cfg);
            case "wearing_leggings" -> itemPresent(player.getInventory().getLeggings(), cfg);
            case "wearing_boots" -> itemPresent(player.getInventory().getBoots(), cfg);
            case "wearing_full_set" -> wearingFullSet(player, cfg);
            case "item_has_lore" -> meta(playerItem(context), ItemMeta::hasLore);
            case "item_has_name" -> meta(playerItem(context), ItemMeta::hasDisplayName);
            case "item_has_model_data" -> meta(playerItem(context), ItemMeta::hasCustomModelData);
            case "item_has_custom_data" -> meta(playerItem(context), meta -> !meta.getPersistentDataContainer().getKeys().isEmpty());
            case "item_durability_above" -> durability(playerItem(context), true, number(context, "value", 0));
            case "item_durability_below" -> durability(playerItem(context), false, number(context, "value", 100));
            case "item_has_vanilla_enchant" -> hasVanillaEnchant(playerItem(context), cfg);
            case "item_has_custom_enchant" -> hasCustomEnchant(playerItem(context), cfg);
            case "inventory_contains" -> inventoryContains(player, cfg);
            case "inventory_has_space" -> player.getInventory().firstEmpty() >= 0;
            case "slot_empty" -> slotEmpty(player, cfg);

            case "target_exists" -> target != null;
            case "target_is_living" -> target != null;
            case "target_is_monster" -> target instanceof Monster;
            case "target_is_boss" -> target instanceof Boss;
            case "target_is_tamed" -> target instanceof Tameable tameable && tameable.isTamed();
            case "target_is_named" -> target != null && target.customName() != null;
            case "target_on_fire" -> target != null && target.getFireTicks() > 0;
            case "target_in_water" -> target != null && target.getLocation().getBlock().isLiquid();
            case "target_health_percent_above" -> target != null && healthPercent(target) > number(context, "value", 0);
            case "target_health_percent_below" -> target != null && healthPercent(target) < number(context, "value", 100);
            case "target_distance_above" -> target != null && player.getWorld().equals(target.getWorld())
                    && player.getLocation().distance(target.getLocation()) > number(context, "value", 0);
            case "target_distance_below" -> target != null && player.getWorld().equals(target.getWorld())
                    && player.getLocation().distance(target.getLocation()) < number(context, "value", 0);
            case "target_has_permission" -> target instanceof Player p && p.hasPermission(text(cfg, "permission", "value", ""));
            case "target_in_region" -> target != null && inRegion(context, target.getLocation(), false);
            case "target_is_same_world" -> target != null && player.getWorld().equals(target.getWorld());
            case "target_line_of_sight" -> target != null && player.hasLineOfSight(target);
            case "damage_above" -> damage(context) > number(context, "value", 0);
            case "damage_below" -> damage(context) < number(context, "value", 0);

            case "in_region" -> inRegion(context, player.getLocation(), false);
            case "not_in_region" -> !inRegion(context, player.getLocation(), false);
            case "in_claim", "in_safe_zone", "in_pvp_zone" -> hasScoreboardTag(player, tagName(cfg, id));
            case "in_light_level_above" -> player.getLocation().getBlock().getLightLevel() > number(context, "value", 0);
            case "in_light_level_below" -> player.getLocation().getBlock().getLightLevel() < number(context, "value", 15);
            case "standing_on_block" -> blockMatches(player.getLocation().getBlock().getRelative(BlockFace.DOWN), cfg);
            case "inside_block" -> blockMatches(player.getLocation().getBlock(), cfg);
            case "near_block" -> nearBlock(player, cfg);
            case "near_entity_type" -> nearEntityType(player, cfg);
            case "near_player_count" -> nearPlayerCount(player, cfg);
            case "moon_phase" -> moonPhase(player.getWorld()) == (int) number(context, "value", 0);
            case "season_is" -> scoreOrTag(player, cfg, "season");

            case "balance_above", "points_above", "job_level_above", "skill_level_above",
                    "mcmmo_level_above", "aura_skill_level_above" -> score(player, objective(cfg, id)) > number(context, "value", 0);
            case "balance_below", "points_below" -> score(player, objective(cfg, id)) < number(context, "value", 0);
            case "has_job", "quest_active", "quest_completed", "town_role_is", "lands_role_is" -> hasScoreboardTag(player, tagName(cfg, id));
            case "placeholder_equals" -> placeholderValue(player, cfg).equalsIgnoreCase(text(cfg, "expected", "value", ""));
            case "placeholder_contains" -> placeholderValue(player, cfg).contains(text(cfg, "expected", "value", ""));
            case "placeholder_greater_than" -> parseDouble(placeholderValue(player, cfg), 0) > number(context, "value", 0);

            case "any_of" -> nested(context, 1, false);
            case "all_of" -> nested(context, -1, false);
            case "none_of" -> nested(context, 1, true);
            case "at_least_of" -> nested(context, (int) number(context, "required", 1), false);
            case "expression_true" -> ExpressionPredicate.evaluate(text(cfg, "expression", "value", "0"), context.getVariables());
            case "expression_false" -> !ExpressionPredicate.evaluate(text(cfg, "expression", "value", "0"), context.getVariables());
            case "cooldown_ready" -> !hasScoreboardTag(player, tagName(cfg, "cooldown_active"));
            case "cooldown_active" -> hasScoreboardTag(player, tagName(cfg, "cooldown_active"));
            case "random_weight_passed" -> Math.random() * 100.0 < number(context, "value", 50);
            case "trigger_value_above" -> context.getTriggerContext().getValue() > number(context, "value", 0);
            case "trigger_value_below" -> context.getTriggerContext().getValue() < number(context, "value", 0);
            case "alt_value_present" -> context.getTriggerContext().getAltValue() != 0.0D;
            case "context_has_block" -> eventBlock(context.getTriggerContext().getEvent()) != null;
            case "context_has_projectile" -> context.getTriggerContext().getEvent() instanceof org.bukkit.event.entity.ProjectileHitEvent;

            case "is_op" -> player.isOp();
            case "is_online_longer_than" -> player.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20.0D > number(context, "seconds", value(context, 0));
            case "joined_before" -> player.hasPlayedBefore();
            case "has_playtime_above" -> player.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20.0D > number(context, "value", 0);
            case "in_permission_group" -> player.hasPermission("group." + text(cfg, "group", "value", ""));
            case "has_scoreboard_tag" -> hasScoreboardTag(player, text(cfg, "tag", "value", ""));
            case "has_advancement" -> hasAdvancement(player, cfg);
            case "has_recipe" -> hasRecipe(player, cfg);
            case "language_is" -> player.locale().toLanguageTag().equalsIgnoreCase(text(cfg, "language", "value", ""));
            case "client_brand_is" -> String.valueOf(player.getClientBrandName()).equalsIgnoreCase(text(cfg, "brand", "value", ""));
            case "ping_below" -> player.getPing() < number(context, "value", 0);
            case "ping_above" -> player.getPing() > number(context, "value", 0);
            case "worldguard_flag_allowed" -> worldGuardAllowed(context, player);
            case "blacklist_exempt" -> player.hasPermission(text(cfg, "permission", "value", "fotia.enchantment.blacklist.exempt"));
            default -> false;
        };
    }

    private static double value(ConditionContext context, double fallback) {
        return number(context, "value", fallback);
    }

    private static double number(ConditionContext context, String key, double fallback) {
        String raw = text(context.getConfig(), key, null, null);
        if (raw == null || raw.isEmpty()) {
            return fallback;
        }
        try {
            return context.evaluateExpression(raw);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static String text(EnchantmentData.ConditionConfig cfg, String key, String alternate, String fallback) {
        String value = cfg.getString(key);
        if ((value == null || value.isEmpty()) && alternate != null) {
            value = cfg.getString(alternate);
        }
        return value == null ? fallback : value;
    }

    private static boolean between(double actual, ConditionContext context, double minFallback, double maxFallback) {
        return actual >= number(context, "min", minFallback) && actual <= number(context, "max", maxFallback);
    }

    private static double healthPercent(LivingEntity entity) {
        AttributeInstance attr = entity.getAttribute(Attribute.MAX_HEALTH);
        double max = attr == null ? 20.0D : attr.getValue();
        return max <= 0 ? 0 : entity.getHealth() / max * 100.0D;
    }

    private static double attribute(LivingEntity entity, Attribute attribute) {
        AttributeInstance instance = entity.getAttribute(attribute);
        return instance == null ? 0.0D : instance.getValue();
    }

    private static int statistic(Player player, String statName) {
        try {
            return player.getStatistic(Statistic.valueOf(statName.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ignored) {
            return 0;
        }
    }

    private static boolean isClimbable(Material material) {
        String name = material.name();
        return name.contains("LADDER") || name.contains("VINE") || name.contains("SCAFFOLDING");
    }

    private static boolean lookingAtBlock(Player player, EnchantmentData.ConditionConfig cfg) {
        RayTraceResult result = player.rayTraceBlocks(parseDouble(cfg.getString("distance", "5"), 5));
        return result != null && result.getHitBlock() != null && blockMatches(result.getHitBlock(), cfg);
    }

    private static ItemStack playerItem(ConditionContext context) {
        ItemStack item = context.getTriggerContext().getItem();
        return item != null ? item : context.getTriggerContext().getPlayer().getInventory().getItemInMainHand();
    }

    private static boolean itemPresent(ItemStack item, EnchantmentData.ConditionConfig cfg) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        String material = text(cfg, "material", "value", "");
        return material.isEmpty() || item.getType().name().equalsIgnoreCase(material);
    }

    private static boolean materialMatches(ItemStack item, EnchantmentData.ConditionConfig cfg) {
        return itemPresent(item, cfg);
    }

    private static boolean wearingFullSet(Player player, EnchantmentData.ConditionConfig cfg) {
        PlayerInventory inv = player.getInventory();
        return itemPresent(inv.getHelmet(), cfg) && itemPresent(inv.getChestplate(), cfg)
                && itemPresent(inv.getLeggings(), cfg) && itemPresent(inv.getBoots(), cfg);
    }

    private interface MetaCheck {
        boolean test(ItemMeta meta);
    }

    private static boolean meta(ItemStack item, MetaCheck check) {
        return item != null && item.hasItemMeta() && check.test(item.getItemMeta());
    }

    private static boolean durability(ItemStack item, boolean above, double threshold) {
        if (item == null || !(item.getItemMeta() instanceof org.bukkit.inventory.meta.Damageable damageable)) {
            return false;
        }
        int max = item.getType().getMaxDurability();
        if (max <= 0) {
            return false;
        }
        double left = (max - damageable.getDamage()) / (double) max * 100.0D;
        return above ? left > threshold : left < threshold;
    }

    private static boolean hasVanillaEnchant(ItemStack item, EnchantmentData.ConditionConfig cfg) {
        if (item == null) {
            return false;
        }
        String enchant = text(cfg, "enchant", "value", "");
        if (enchant.isEmpty()) {
            return !item.getEnchantments().isEmpty();
        }
        return item.getEnchantments().keySet().stream()
                .anyMatch(key -> key.getKey().getKey().equalsIgnoreCase(enchant)
                        || key.getKey().asString().equalsIgnoreCase(enchant));
    }

    private static boolean hasCustomEnchant(ItemStack item, EnchantmentData.ConditionConfig cfg) {
        return meta(item, itemMeta -> {
            String enchant = text(cfg, "enchant", "value", "");
            if (enchant.isEmpty()) {
                return !itemMeta.getPersistentDataContainer().getKeys().isEmpty();
            }
            return itemMeta.getPersistentDataContainer().getKeys().stream()
                    .anyMatch(key -> key.getKey().equalsIgnoreCase(enchant));
        });
    }

    private static boolean inventoryContains(Player player, EnchantmentData.ConditionConfig cfg) {
        String material = text(cfg, "material", "value", "");
        int amount = (int) parseDouble(cfg.getString("amount", "1"), 1);
        int found = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && (material.isEmpty() || item.getType().name().equalsIgnoreCase(material))) {
                found += item.getAmount();
            }
        }
        return found >= amount;
    }

    private static boolean slotEmpty(Player player, EnchantmentData.ConditionConfig cfg) {
        String slot = text(cfg, "slot", "value", "mainhand").toLowerCase(Locale.ROOT);
        ItemStack item = switch (slot) {
            case "offhand" -> player.getInventory().getItemInOffHand();
            case "helmet" -> player.getInventory().getHelmet();
            case "chestplate" -> player.getInventory().getChestplate();
            case "leggings" -> player.getInventory().getLeggings();
            case "boots" -> player.getInventory().getBoots();
            default -> player.getInventory().getItemInMainHand();
        };
        return item == null || item.getType().isAir();
    }

    private static double damage(ConditionContext context) {
        return context.getTriggerContext().getEvent() instanceof EntityDamageEvent event ? event.getDamage() : context.getTriggerContext().getValue();
    }

    private static boolean inRegion(ConditionContext context, Location location, boolean defaultValue) {
        if (context.getPlugin() == null || context.getPlugin().getIntegrationManager() == null) {
            return defaultValue;
        }
        WorldGuardHook hook = context.getPlugin().getIntegrationManager().getWorldGuardHook();
        if (hook == null || !hook.isAvailable()) {
            return defaultValue;
        }
        Set<String> regionIds = hook.getRegionIds(location);
        String targetRegion = text(context.getConfig(), "region", "value", "");
        return targetRegion.isEmpty() ? !regionIds.isEmpty() : regionIds.contains(targetRegion);
    }

    private static boolean blockMatches(Block block, EnchantmentData.ConditionConfig cfg) {
        if (block == null) {
            return false;
        }
        String material = text(cfg, "block", "material", "");
        if (material.isEmpty()) {
            material = cfg.getString("value", "");
        }
        return material.isEmpty() || block.getType().name().equalsIgnoreCase(material);
    }

    private static boolean nearBlock(Player player, EnchantmentData.ConditionConfig cfg) {
        int radius = (int) parseDouble(cfg.getString("radius", "5"), 5);
        Location base = player.getLocation();
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (blockMatches(base.clone().add(x, y, z).getBlock(), cfg)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean nearEntityType(Player player, EnchantmentData.ConditionConfig cfg) {
        String type = text(cfg, "entity", "value", "");
        double radius = parseDouble(cfg.getString("radius", "8"), 8);
        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (type.isEmpty() || entity.getType().name().equalsIgnoreCase(type)) {
                return true;
            }
        }
        return false;
    }

    private static boolean nearPlayerCount(Player player, EnchantmentData.ConditionConfig cfg) {
        double radius = parseDouble(cfg.getString("radius", "8"), 8);
        int min = (int) parseDouble(cfg.getString("min", cfg.getString("value", "1")), 1);
        int count = 0;
        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof Player && !entity.equals(player)) {
                count++;
            }
        }
        return count >= min;
    }

    private static int moonPhase(World world) {
        return (int) ((world.getFullTime() / 24000L) % 8L);
    }

    private static boolean scoreOrTag(Player player, EnchantmentData.ConditionConfig cfg, String prefix) {
        String expected = text(cfg, prefix, "value", "");
        return !expected.isEmpty() && hasScoreboardTag(player, prefix + ":" + expected);
    }

    private static String objective(EnchantmentData.ConditionConfig cfg, String fallback) {
        return text(cfg, "objective", "key", fallback);
    }

    private static int score(Player player, String objectiveName) {
        if (objectiveName == null || objectiveName.isEmpty() || Bukkit.getScoreboardManager() == null) {
            return 0;
        }
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        Objective objective = scoreboard.getObjective(objectiveName);
        if (objective == null) {
            return 0;
        }
        Score score = objective.getScore(player.getName());
        return score.isScoreSet() ? score.getScore() : 0;
    }

    private static String tagName(EnchantmentData.ConditionConfig cfg, String fallback) {
        return text(cfg, "tag", "value", fallback);
    }

    private static boolean hasScoreboardTag(Player player, String tag) {
        return tag != null && !tag.isEmpty() && player.getScoreboardTags().contains(tag);
    }

    private static String placeholderValue(Player player, EnchantmentData.ConditionConfig cfg) {
        String objective = cfg.getString("objective");
        if (objective != null && !objective.isEmpty()) {
            return String.valueOf(score(player, objective));
        }
        return cfg.getString("actual", "");
    }

    @SuppressWarnings("unchecked")
    private static boolean nested(ConditionContext context, int required, boolean invert) {
        Object raw = context.getConfig().getExtraParams().get("conditions");
        if (!(raw instanceof List<?> list)) {
            return invert;
        }
        int passed = 0;
        int requiredCount = required < 0 ? list.size() : required;
        for (Object item : list) {
            if (!(item instanceof java.util.Map<?, ?> map)) {
                continue;
            }
            Object type = map.get("type");
            if (type == null) {
                continue;
            }
            EnchantmentData.ConditionConfig cfg = new EnchantmentData.ConditionConfig();
            cfg.setType(String.valueOf(type));
            Object value = map.get("value");
            if (value != null) {
                cfg.setValue(String.valueOf(value));
            }
            cfg.setExtraParams((java.util.Map<String, Object>) map);
            if (new CatalogCondition(cfg.getType()).check(new ConditionContext(
                    context.getPlugin(), context.getTriggerContext(), cfg, context.getEnchantLevel(), context.getVariables()))) {
                passed++;
            }
        }
        boolean result = passed >= requiredCount;
        return invert ? !result : result;
    }

    private static Block eventBlock(Event event) {
        if (event instanceof BlockEvent blockEvent) {
            return blockEvent.getBlock();
        }
        if (event instanceof PlayerInteractEvent interactEvent) {
            return interactEvent.getClickedBlock();
        }
        return null;
    }

    private static boolean hasAdvancement(Player player, EnchantmentData.ConditionConfig cfg) {
        NamespacedKey key = NamespacedKey.fromString(text(cfg, "advancement", "value", ""));
        if (key == null || Bukkit.getAdvancement(key) == null) {
            return false;
        }
        return player.getAdvancementProgress(Bukkit.getAdvancement(key)).isDone();
    }

    private static boolean hasRecipe(Player player, EnchantmentData.ConditionConfig cfg) {
        NamespacedKey key = NamespacedKey.fromString(text(cfg, "recipe", "value", ""));
        if (key == null) {
            return false;
        }
        return player.hasDiscoveredRecipe(key);
    }

    private static boolean worldGuardAllowed(ConditionContext context, Player player) {
        if (context.getPlugin() == null || context.getPlugin().getIntegrationManager() == null) {
            return true;
        }
        WorldGuardHook hook = context.getPlugin().getIntegrationManager().getWorldGuardHook();
        return hook == null || hook.isEnchantAllowed(player, player.getLocation());
    }

    private static double parseDouble(String raw, double fallback) {
        if (raw == null || raw.isEmpty()) {
            return fallback;
        }
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
