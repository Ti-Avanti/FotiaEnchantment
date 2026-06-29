package gg.fotia.enchantment.pipeline.effect.impl;

import gg.fotia.enchantment.compat.BukkitAttributes;
import gg.fotia.enchantment.compat.BukkitRegistryCompat;
import gg.fotia.enchantment.pipeline.effect.Effect;
import gg.fotia.enchantment.pipeline.effect.EffectContext;
import gg.fotia.enchantment.pipeline.trigger.TriggerContext;
import gg.fotia.enchantment.util.LegacyColorConverter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 对标目录效果实现。
 *
 * <p>目录 ID 形如 TARGET_DAMAGE_ADD、AREA_EXPLOSION。前缀决定作用对象或形状，
 * 后缀决定动作。所有 ID 都进入同一套执行器，保持配置可解析、可测试。</p>
 */
public class CatalogEffect implements Effect {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final List<String> TARGET_PREFIXES = List.of(
            "NEARBY_ENEMY", "NEARBY_ALLY", "HELD_ITEM", "TARGET_ITEM", "INVENTORY",
            "PROJECTILE", "TARGET", "SPHERE", "BLOCK", "AREA", "LINE", "DROP", "SELF");

    private final String id;

    public CatalogEffect(String id) {
        this.id = id == null ? "" : id.toUpperCase(Locale.ROOT);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void execute(EffectContext context) {
        Parts parts = parse(id);
        if (parts == null || context == null || context.getTriggerContext() == null) {
            return;
        }

        if (isLivingOperation(parts.operation())) {
            livingOperation(parts, context);
            return;
        }
        if (isItemOperation(parts.operation())) {
            itemOperation(parts, context);
            return;
        }
        worldOperation(parts, context);
    }

    private static Parts parse(String id) {
        for (String prefix : TARGET_PREFIXES) {
            String marker = prefix + "_";
            if (id.startsWith(marker)) {
                return new Parts(prefix, id.substring(marker.length()));
            }
        }
        return null;
    }

    private static boolean isLivingOperation(String op) {
        return switch (op) {
            case "DAMAGE_ADD", "DAMAGE_MULTIPLY", "DAMAGE_REDUCE", "TRUE_DAMAGE", "HEAL",
                    "LIFESTEAL", "ABSORB", "BLEED", "POISON", "WITHER", "BURN", "FREEZE",
                    "EXECUTE", "REFLECT", "SHIELD", "ARMOR_PIERCE", "CRIT_BOOST", "DAMAGE_CAP",
                    "PUSH", "PULL", "KNOCKUP", "TELEPORT", "DASH", "BLINK", "HOMING",
                    "ROOT", "STUN", "SILENCE", "SLOW", "SPEED", "GRAVITY", "GLIDE_BOOST",
                    "SAFE_FALL", "SWAP_POSITION", "ROTATE", "VORTEX" -> true;
            default -> false;
        };
    }

    private static boolean isItemOperation(String op) {
        return switch (op) {
            case "REPAIR", "DAMAGE", "DUPLICATE", "CONSUME", "TRANSFORM", "ADD_LORE",
                    "SET_NAME", "SET_MODEL", "ADD_ENCHANT", "REMOVE_ENCHANT",
                    "TRANSFER_ENCHANT", "AUTO_SMELT", "AUTO_PICKUP", "MULTIPLY_DROPS",
                    "FILTER_DROPS", "TELEKINESIS" -> true;
            default -> false;
        };
    }

    private static void livingOperation(Parts parts, EffectContext context) {
        Player player = context.getTriggerContext().getPlayer();
        if (player == null) {
            return;
        }
        List<LivingEntity> targets = resolveLivingTargets(parts.target(), context);
        double value = amount(context, 1.0D);
        int duration = context.getIntParam("duration", 100);
        int amplifier = context.getIntParam("amplifier", Math.max(0, (int) value - 1));

        for (LivingEntity entity : targets) {
            if (entity == null || entity.isDead()) {
                continue;
            }
            switch (parts.operation()) {
                case "DAMAGE_ADD", "TRUE_DAMAGE", "ARMOR_PIERCE", "CRIT_BOOST" -> damage(entity, Math.max(0, value), player);
                case "DAMAGE_MULTIPLY" -> multiplyDamageEvent(context, value);
                case "DAMAGE_REDUCE" -> reduceDamageEvent(context, value);
                case "DAMAGE_CAP" -> capDamageEvent(context, value);
                case "HEAL" -> heal(entity, value);
                case "LIFESTEAL" -> heal(player, Math.max(0, value));
                case "ABSORB", "SHIELD" -> entity.setAbsorptionAmount(Math.max(entity.getAbsorptionAmount(), value));
                case "BLEED" -> damage(entity, Math.max(1, value), player);
                case "POISON" -> potion(entity, "poison", duration, amplifier);
                case "WITHER" -> potion(entity, "wither", duration, amplifier);
                case "BURN" -> entity.setFireTicks(Math.max(entity.getFireTicks(), Math.max(1, duration)));
                case "FREEZE" -> entity.setFreezeTicks(Math.max(entity.getFreezeTicks(), Math.max(1, duration)));
                case "EXECUTE" -> {
                    if (entity.getHealth() <= Math.max(1, value)) {
                        damage(entity, entity.getHealth() + 100.0D, player);
                    }
                }
                case "REFLECT" -> {
                    if (context.getTriggerContext().getTarget() != null) {
                        damage(context.getTriggerContext().getTarget(), Math.max(0, value), player);
                    }
                }
                case "PUSH" -> pushAway(entity, player.getLocation(), value);
                case "PULL" -> pullTo(entity, player.getLocation(), value);
                case "KNOCKUP" -> entity.setVelocity(entity.getVelocity().setY(Math.max(value, 0.5D)));
                case "TELEPORT", "BLINK" -> entity.teleport(offsetLocation(player.getLocation(), context));
                case "DASH", "GLIDE_BOOST" -> entity.setVelocity(player.getLocation().getDirection().normalize().multiply(Math.max(0.1D, value)));
                case "HOMING" -> homeProjectile(context, entity);
                case "ROOT", "STUN" -> potion(entity, "slowness", duration, Math.max(amplifier, 4));
                case "SILENCE" -> entity.addScoreboardTag("fotia:silenced");
                case "SLOW" -> potion(entity, "slowness", duration, amplifier);
                case "SPEED" -> potion(entity, "speed", duration, amplifier);
                case "GRAVITY" -> potion(entity, "levitation", duration, 0);
                case "SAFE_FALL" -> entity.setFallDistance(0);
                case "SWAP_POSITION" -> swap(player, entity);
                case "ROTATE" -> rotate(entity, value);
                case "VORTEX" -> pullNearby(player.getLocation(), value, context.getDoubleParam("radius", 6.0D));
                default -> {
                }
            }
        }
    }

    private static List<LivingEntity> resolveLivingTargets(String target, EffectContext context) {
        TriggerContext trigger = context.getTriggerContext();
        Player player = trigger.getPlayer();
        LivingEntity primary = trigger.getTarget();
        List<LivingEntity> result = new ArrayList<>();
        switch (target) {
            case "SELF" -> result.add(player);
            case "TARGET", "PROJECTILE" -> {
                if (primary != null) {
                    result.add(primary);
                } else if (player != null) {
                    result.add(player);
                }
            }
            case "NEARBY_ALLY", "NEARBY_ENEMY" -> {
                if (player != null) {
                    double radius = context.getDoubleParam("radius", 6.0D);
                    for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
                        if (entity instanceof LivingEntity living && !living.equals(player)) {
                            if ("NEARBY_ALLY".equals(target) == (living instanceof Player)) {
                                result.add(living);
                            }
                        }
                    }
                }
            }
            default -> {
            }
        }
        return result;
    }

    private static void itemOperation(Parts parts, EffectContext context) {
        Player player = context.getTriggerContext().getPlayer();
        if (player == null) {
            return;
        }
        if ("DROP".equals(parts.target()) && isDropPickupOperation(parts.operation())) {
            handleDropPickup(context, player);
            return;
        }

        List<ItemStack> items = resolveItems(parts.target(), context);
        if (items.isEmpty()) {
            return;
        }
        for (ItemStack item : items) {
            if (item == null || item.getType().isAir()) {
                continue;
            }
            switch (parts.operation()) {
                case "REPAIR" -> damageItem(item, -context.getIntParam("amount", (int) amount(context, 1)));
                case "DAMAGE" -> damageItem(item, context.getIntParam("amount", (int) amount(context, 1)));
                case "DUPLICATE" -> player.getInventory().addItem(item.clone());
                case "CONSUME" -> item.setAmount(Math.max(0, item.getAmount() - context.getIntParam("amount", 1)));
                case "TRANSFORM" -> transform(item, context);
                case "ADD_LORE" -> addLore(item, context.getExtraParam("lore", context.getExtraParam("text", "")));
                case "SET_NAME" -> setName(item, context.getExtraParam("name", context.getExtraParam("text", "")));
                case "SET_MODEL" -> setModel(item, context.getIntParam("model", context.getIntParam("custom-model-data", 0)));
                case "ADD_ENCHANT" -> addEnchant(item, context);
                case "REMOVE_ENCHANT" -> removeEnchant(item, context);
                case "TRANSFER_ENCHANT" -> transferEnchant(player, item, context);
                case "AUTO_SMELT" -> smelt(item);
                case "AUTO_PICKUP", "TELEKINESIS" -> player.getInventory().addItem(item.clone());
                case "MULTIPLY_DROPS" -> item.setAmount(Math.min(item.getMaxStackSize(), item.getAmount() * Math.max(1, context.getIntParam("multiplier", (int) amount(context, 2)))));
                case "FILTER_DROPS" -> filterItem(item, context);
                default -> {
                }
            }
        }
    }

    private static boolean isDropPickupOperation(String operation) {
        return "AUTO_PICKUP".equals(operation) || "TELEKINESIS".equals(operation);
    }

    private static void handleDropPickup(EffectContext context, Player player) {
        Event event = context.getTriggerContext().getEvent();
        if (event instanceof BlockBreakEvent blockBreakEvent) {
            handleBlockBreakDropPickup(context, player, blockBreakEvent);
            return;
        }
        if (event instanceof BlockDropItemEvent blockDropEvent) {
            handleBlockItemDropPickup(player, blockDropEvent);
            return;
        }
        if (event instanceof EntityDropItemEvent dropEvent) {
            handleEntityDropPickup(player, dropEvent);
            return;
        }

        List<ItemStack> items = resolveItems("DROP", context);
        dropInventoryOverflow(player, items, player.getLocation());
    }

    private static void handleBlockBreakDropPickup(EffectContext context, Player player, BlockBreakEvent blockBreakEvent) {
        if (!blockBreakEvent.isDropItems()) {
            return;
        }

        ItemStack tool = context.getTriggerContext().getItem();
        Collection<ItemStack> drops = blockBreakEvent.getBlock().getDrops(tool, player);
        if (drops.isEmpty()) {
            return;
        }

        blockBreakEvent.setDropItems(false);
        Location location = blockBreakEvent.getBlock().getLocation().add(0.5, 0.5, 0.5);
        dropInventoryOverflow(player, drops, location);
    }

    private static void handleBlockItemDropPickup(Player player, BlockDropItemEvent blockDropEvent) {
        List<ItemStack> drops = new ArrayList<>();
        for (Item itemEntity : blockDropEvent.getItems()) {
            if (itemEntity == null) {
                continue;
            }
            ItemStack stack = itemEntity.getItemStack();
            if (stack != null && !stack.getType().isAir() && stack.getAmount() > 0) {
                drops.add(stack.clone());
            }
            itemEntity.remove();
        }
        if (drops.isEmpty()) {
            return;
        }

        if (blockDropEvent instanceof Cancellable cancellable) {
            cancellable.setCancelled(true);
        }
        Location location = blockDropEvent.getBlock().getLocation().add(0.5, 0.5, 0.5);
        dropInventoryOverflow(player, drops, location);
    }

    private static void handleEntityDropPickup(Player player, EntityDropItemEvent dropEvent) {
        Item itemEntity = dropEvent.getItemDrop();
        if (itemEntity == null) {
            return;
        }
        ItemStack stack = itemEntity.getItemStack();
        if (stack == null || stack.getType().isAir() || stack.getAmount() <= 0) {
            return;
        }

        if (dropEvent instanceof Cancellable cancellable) {
            cancellable.setCancelled(true);
        }
        Location location = itemEntity.getLocation();
        dropEvent.getItemDrop().remove();
        dropInventoryOverflow(player, List.of(stack.clone()), location);
    }

    private static void dropInventoryOverflow(Player player, Collection<ItemStack> items, Location location) {
        if (items == null || items.isEmpty()) {
            return;
        }
        Location dropLocation = location == null ? player.getLocation() : location;
        World world = dropLocation.getWorld() == null ? player.getWorld() : dropLocation.getWorld();
        for (ItemStack item : items) {
            if (item == null || item.getType().isAir() || item.getAmount() <= 0) {
                continue;
            }
            Map<Integer, ItemStack> overflow = player.getInventory().addItem(item.clone());
            for (ItemStack remaining : overflow.values()) {
                if (remaining != null && !remaining.getType().isAir() && remaining.getAmount() > 0) {
                    world.dropItemNaturally(dropLocation, remaining);
                }
            }
        }
    }

    private static List<ItemStack> resolveItems(String target, EffectContext context) {
        Player player = context.getTriggerContext().getPlayer();
        List<ItemStack> result = new ArrayList<>();
        if (player == null) {
            return result;
        }
        PlayerInventory inv = player.getInventory();
        switch (target) {
            case "HELD_ITEM", "TARGET_ITEM" -> result.add(inv.getItemInMainHand());
            case "INVENTORY" -> {
                for (ItemStack item : inv.getContents()) {
                    if (item != null) {
                        result.add(item);
                    }
                }
            }
            case "DROP" -> {
                Event event = context.getTriggerContext().getEvent();
                if (event instanceof EntityDropItemEvent dropEvent) {
                    result.add(dropEvent.getItemDrop().getItemStack());
                } else if (event instanceof BlockBreakEvent blockBreakEvent) {
                    result.addAll(blockBreakEvent.getBlock().getDrops(inv.getItemInMainHand(), player));
                } else {
                    result.add(context.getTriggerContext().getItem());
                }
            }
            default -> result.add(inv.getItemInMainHand());
        }
        return result;
    }

    private static void worldOperation(Parts parts, EffectContext context) {
        List<Block> blocks = resolveBlocks(parts.target(), context);
        Player player = context.getTriggerContext().getPlayer();
        if (blocks.isEmpty() || player == null) {
            return;
        }
        for (Block block : blocks) {
            if (block == null) {
                continue;
            }
            switch (parts.operation()) {
                case "BREAK" -> block.breakNaturally(player.getInventory().getItemInMainHand());
                case "PLACE", "REPLACE" -> block.setType(material(context, "material", "block", Material.STONE));
                case "AGE_CROP" -> ageCrop(block);
                case "REPLANT" -> replant(block, context);
                case "TILL" -> block.setType(Material.FARMLAND);
                case "MELT" -> {
                    if (block.getType() == Material.ICE || block.getType() == Material.PACKED_ICE || block.getType() == Material.BLUE_ICE) {
                        block.setType(Material.WATER);
                    }
                }
                case "FREEZE_WATER" -> {
                    if (block.getType() == Material.WATER) {
                        block.setType(Material.ICE);
                    }
                }
                case "LIGHTNING" -> block.getWorld().strikeLightningEffect(block.getLocation().add(0.5, 0, 0.5));
                case "EXPLOSION" -> block.getWorld().createExplosion(block.getLocation(), (float) context.getDoubleParam("power", (float) amount(context, 2)), false, context.getBooleanParam("break-blocks", false), player);
                case "SHOCKWAVE" -> shockwave(block.getLocation(), amount(context, 1), context.getDoubleParam("radius", 4.0D), player);
                default -> {
                }
            }
        }
    }

    private static List<Block> resolveBlocks(String target, EffectContext context) {
        Player player = context.getTriggerContext().getPlayer();
        Block base = contextBlock(context);
        if (base == null && player != null) {
            base = player.getTargetBlockExact(context.getIntParam("distance", 6));
        }
        if (base == null) {
            return List.of();
        }
        int radius = Math.max(0, context.getIntParam("radius", "BLOCK".equals(target) ? 0 : 2));
        List<Block> blocks = new ArrayList<>();
        if ("BLOCK".equals(target)) {
            blocks.add(base);
            return blocks;
        }
        Location location = base.getLocation();
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if ("LINE".equals(target) && (x != 0 || z != 0)) {
                        continue;
                    }
                    if ("SPHERE".equals(target) && x * x + y * y + z * z > radius * radius) {
                        continue;
                    }
                    blocks.add(location.clone().add(x, y, z).getBlock());
                }
            }
        }
        return blocks;
    }

    private static Block contextBlock(EffectContext context) {
        Event event = context.getTriggerContext().getEvent();
        if (event instanceof BlockEvent blockEvent) {
            return blockEvent.getBlock();
        }
        if (event instanceof PlayerInteractEvent interactEvent) {
            return interactEvent.getClickedBlock();
        }
        if (event instanceof ProjectileHitEvent projectileHitEvent) {
            return projectileHitEvent.getHitBlock();
        }
        return null;
    }

    private static double amount(EffectContext context, double fallback) {
        String value = context.getConfigValue();
        if (value == null || value.isEmpty()) {
            value = context.getExtraParam("amount");
        }
        if (value == null || value.isEmpty()) {
            value = context.getExtraParam("value");
        }
        if (value == null || value.isEmpty()) {
            return fallback;
        }
        try {
            return context.evaluateExpression(value);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static void damage(LivingEntity entity, double amount, Entity source) {
        if (amount > 0) {
            entity.damage(amount, source);
        }
    }

    private static void multiplyDamageEvent(EffectContext context, double multiplier) {
        if (context.getTriggerContext().getEvent() instanceof EntityDamageEvent event) {
            event.setDamage(Math.max(0, event.getDamage() * multiplier));
        }
    }

    private static void reduceDamageEvent(EffectContext context, double value) {
        if (context.getTriggerContext().getEvent() instanceof EntityDamageEvent event) {
            double amount = value <= 1.0D ? event.getDamage() * value : value;
            event.setDamage(Math.max(0, event.getDamage() - amount));
        }
    }

    private static void capDamageEvent(EffectContext context, double value) {
        if (context.getTriggerContext().getEvent() instanceof EntityDamageEvent event) {
            event.setDamage(Math.min(event.getDamage(), Math.max(0, value)));
        }
    }

    private static void heal(LivingEntity entity, double amount) {
        double max = BukkitAttributes.maxHealthValue(entity);
        entity.setHealth(Math.min(max, Math.max(0, entity.getHealth() + amount)));
    }

    private static void potion(LivingEntity entity, String potion, int duration, int amplifier) {
        NamespacedKey key = NamespacedKey.minecraft(potion.toLowerCase(Locale.ROOT));
        PotionEffectType type = BukkitRegistryCompat.potionEffect(key);
        if (type != null) {
            entity.addPotionEffect(new PotionEffect(type, Math.max(1, duration), Math.max(0, amplifier), true, true, true));
        }
    }

    private static void pushAway(LivingEntity entity, Location from, double power) {
        Vector vector = entity.getLocation().toVector().subtract(from.toVector());
        if (vector.lengthSquared() == 0) {
            vector = from.getDirection();
        }
        entity.setVelocity(vector.normalize().multiply(Math.max(0.1D, power)));
    }

    private static void pullTo(LivingEntity entity, Location to, double power) {
        Vector vector = to.toVector().subtract(entity.getLocation().toVector());
        if (vector.lengthSquared() > 0) {
            entity.setVelocity(vector.normalize().multiply(Math.max(0.1D, power)));
        }
    }

    private static Location offsetLocation(Location base, EffectContext context) {
        return base.clone().add(base.getDirection().normalize().multiply(context.getDoubleParam("distance", 4.0D)));
    }

    private static void homeProjectile(EffectContext context, LivingEntity target) {
        Projectile projectile = projectile(context.getTriggerContext().getEvent());
        if (projectile == null || target == null) {
            return;
        }
        Vector vector = target.getEyeLocation().toVector().subtract(projectile.getLocation().toVector());
        if (vector.lengthSquared() > 0) {
            projectile.setVelocity(vector.normalize().multiply(Math.max(0.2D, amount(context, 1.5D))));
        }
    }

    private static Projectile projectile(Event event) {
        if (event instanceof ProjectileHitEvent projectileHitEvent) {
            return projectileHitEvent.getEntity();
        }
        return null;
    }

    private static void swap(Player player, LivingEntity entity) {
        Location playerLocation = player.getLocation();
        Location entityLocation = entity.getLocation();
        player.teleport(entityLocation);
        entity.teleport(playerLocation);
    }

    private static void rotate(LivingEntity entity, double degrees) {
        Location location = entity.getLocation();
        location.setYaw(location.getYaw() + (float) degrees);
        entity.teleport(location);
    }

    private static void pullNearby(Location center, double power, double radius) {
        World world = center.getWorld();
        if (world == null) {
            return;
        }
        for (Entity entity : world.getNearbyEntities(center, radius, radius, radius)) {
            if (entity instanceof LivingEntity living) {
                pullTo(living, center, power);
            }
        }
    }

    private static void damageItem(ItemStack item, int delta) {
        if (!(item.getItemMeta() instanceof org.bukkit.inventory.meta.Damageable damageable)) {
            return;
        }
        damageable.setDamage(Math.max(0, damageable.getDamage() + delta));
        item.setItemMeta(damageable);
    }

    private static void transform(ItemStack item, EffectContext context) {
        Material material = material(context, "material", "item", null);
        if (material != null) {
            item.setType(material);
        }
    }

    private static void addLore(ItemStack item, String loreLine) {
        if (loreLine == null || loreLine.isEmpty()) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
        lore.add(component(loreLine));
        meta.lore(lore);
        item.setItemMeta(meta);
    }

    private static void setName(ItemStack item, String name) {
        if (name == null || name.isEmpty()) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        meta.displayName(component(name));
        item.setItemMeta(meta);
    }

    private static Component component(String text) {
        return MINI_MESSAGE.deserialize(LegacyColorConverter.convert("<!i>" + text));
    }

    private static void setModel(ItemStack item, int model) {
        ItemMeta meta = item.getItemMeta();
        meta.setCustomModelData(model);
        item.setItemMeta(meta);
    }

    private static void addEnchant(ItemStack item, EffectContext context) {
        Enchantment enchantment = enchantment(context);
        if (enchantment != null) {
            item.addUnsafeEnchantment(enchantment, Math.max(1, context.getIntParam("level", 1)));
        }
    }

    private static void removeEnchant(ItemStack item, EffectContext context) {
        Enchantment enchantment = enchantment(context);
        if (enchantment != null) {
            item.removeEnchantment(enchantment);
        }
    }

    private static Enchantment enchantment(EffectContext context) {
        String name = context.getExtraParam("enchant", context.getExtraParam("enchantment", ""));
        NamespacedKey key = name.contains(":") ? NamespacedKey.fromString(name.toLowerCase(Locale.ROOT))
                : NamespacedKey.minecraft(name.toLowerCase(Locale.ROOT));
        return key == null ? null : Registry.ENCHANTMENT.get(key);
    }

    private static void transferEnchant(Player player, ItemStack source, EffectContext context) {
        Enchantment enchantment = enchantment(context);
        if (enchantment == null || !source.containsEnchantment(enchantment)) {
            return;
        }
        source.removeEnchantment(enchantment);
        player.getInventory().getItemInOffHand().addUnsafeEnchantment(enchantment, Math.max(1, context.getIntParam("level", 1)));
    }

    private static void smelt(ItemStack item) {
        Material result = switch (item.getType()) {
            case RAW_IRON -> Material.IRON_INGOT;
            case RAW_GOLD -> Material.GOLD_INGOT;
            case RAW_COPPER -> Material.COPPER_INGOT;
            case COBBLESTONE -> Material.STONE;
            case SAND -> Material.GLASS;
            default -> null;
        };
        if (result != null) {
            item.setType(result);
        }
    }

    private static void filterItem(ItemStack item, EffectContext context) {
        Material allowed = material(context, "material", "item", null);
        if (allowed != null && item.getType() != allowed) {
            item.setAmount(0);
        }
    }

    private static void ageCrop(Block block) {
        BlockData data = block.getBlockData();
        if (data instanceof Ageable ageable) {
            ageable.setAge(ageable.getMaximumAge());
            block.setBlockData(ageable);
        }
    }

    private static void replant(Block block, EffectContext context) {
        Material material = material(context, "material", "block", block.getType());
        block.setType(material);
    }

    private static void shockwave(Location location, double power, double radius, Entity source) {
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        world.spawnParticle(Particle.EXPLOSION, location, 1);
        for (Entity entity : world.getNearbyEntities(location, radius, radius, radius)) {
            if (entity instanceof LivingEntity living && !entity.equals(source)) {
                pushAway(living, location, power);
                living.damage(Math.max(0.5D, power), source);
            }
        }
    }

    private static Material material(EffectContext context, String primary, String secondary, Material fallback) {
        String raw = context.getExtraParam(primary, context.getExtraParam(secondary, ""));
        if (raw == null || raw.isEmpty()) {
            return fallback;
        }
        try {
            return Material.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    private record Parts(String target, String operation) {
    }
}
