package gg.fotia.enchantment.pipeline.effect.impl;

import gg.fotia.enchantment.pipeline.effect.Effect;
import gg.fotia.enchantment.pipeline.effect.EffectContext;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 额外掉落效果 - 修改 BlockDropItemEvent 中所有掉落物的数量
 */
public class BonusDropEffect implements Effect {

    @Override
    public String getId() {
        return "BONUS_DROP";
    }

    @Override
    public void execute(EffectContext context) {
        Event event = context.getTriggerContext().getEvent();
        Map<String, Object> extraParams = context.getConfig() == null ? Map.of() : context.getConfig().getExtraParams();
        if (isExcludedBlock(blockType(event), extraParams)) {
            return;
        }

        double multiplier = multiplier(context);
        if (multiplier <= 1.0) return;

        if (event instanceof BlockDropItemEvent dropEvent) {
            multiplyDroppedItems(dropEvent.getItems(), multiplier);
            return;
        }
        if (event instanceof BlockBreakEvent breakEvent) {
            multiplyBlockBreakDrops(context, breakEvent, multiplier);
        }
    }

    private void multiplyDroppedItems(List<Item> items, double multiplier) {
        for (Item itemEntity : items) {
            ItemStack stack = itemEntity.getItemStack();
            if (stack == null) continue;
            stack.setAmount(multipliedAmount(stack, multiplier));
            itemEntity.setItemStack(stack);
        }
    }

    private void multiplyBlockBreakDrops(EffectContext context, BlockBreakEvent event, double multiplier) {
        if (!event.isDropItems()) {
            return;
        }

        ItemStack tool = context.getTriggerContext().getItem();
        Collection<ItemStack> drops = event.getBlock().getDrops(tool, event.getPlayer());
        List<ItemStack> replacementDrops = new ArrayList<>();
        for (ItemStack drop : drops) {
            if (drop == null || drop.getType().isAir() || drop.getAmount() <= 0) {
                continue;
            }
            ItemStack replacement = drop.clone();
            replacement.setAmount(multipliedAmount(replacement, multiplier));
            replacementDrops.add(replacement);
        }
        if (replacementDrops.isEmpty()) {
            return;
        }

        event.setDropItems(false);
        Location location = event.getBlock().getLocation().add(0.5, 0.5, 0.5);
        for (ItemStack replacement : replacementDrops) {
            event.getBlock().getWorld().dropItemNaturally(location, replacement);
        }
    }

    private Material blockType(Event event) {
        if (event instanceof BlockDropItemEvent dropEvent) {
            return dropEvent.getBlockState() == null ? null : dropEvent.getBlockState().getType();
        }
        if (event instanceof BlockBreakEvent breakEvent) {
            return breakEvent.getBlock().getType();
        }
        return null;
    }

    static boolean isExcludedBlock(Material blockType, Map<String, Object> extraParams) {
        if (blockType == null || extraParams == null || extraParams.isEmpty()) {
            return false;
        }

        String materialName = blockType.name();
        for (String token : configuredValues(extraParams,
                "excluded-blocks", "exclude-blocks", "excluded-materials", "exclude-materials")) {
            if (materialName.equals(normalizeToken(token))) {
                return true;
            }
        }

        for (String token : configuredValues(extraParams,
                "excluded-block-groups", "exclude-block-groups", "excluded-groups", "exclude-groups")) {
            if (matchesExcludedGroup(blockType, normalizeToken(token))) {
                return true;
            }
        }
        return false;
    }

    private static List<String> configuredValues(Map<String, Object> extraParams, String... keys) {
        List<String> values = new ArrayList<>();
        for (String key : keys) {
            Object raw = extraParams.get(key);
            if (raw instanceof Iterable<?> iterable) {
                for (Object item : iterable) {
                    if (item != null) {
                        values.add(String.valueOf(item));
                    }
                }
                continue;
            }
            if (raw != null) {
                String text = String.valueOf(raw);
                for (String part : text.split("[,;]")) {
                    values.add(part);
                }
            }
        }
        return values;
    }

    private static boolean matchesExcludedGroup(Material blockType, String group) {
        return switch (group) {
            case "SHULKER", "SHULKER_BOX", "SHULKER_BOXES" -> isShulkerBox(blockType);
            default -> false;
        };
    }

    private static boolean isShulkerBox(Material blockType) {
        String name = blockType.name();
        return name.equals("SHULKER_BOX") || name.endsWith("_SHULKER_BOX");
    }

    private static String normalizeToken(String token) {
        if (token == null) {
            return "";
        }
        String normalized = token.trim();
        int namespace = normalized.indexOf(':');
        if (namespace >= 0 && namespace + 1 < normalized.length()) {
            normalized = normalized.substring(namespace + 1);
        }
        return normalized
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase(Locale.ROOT);
    }

    private double multiplier(EffectContext context) {
        String explicitMultiplier = context.getExtraParam("multiplier");
        if (explicitMultiplier != null && !explicitMultiplier.isEmpty()) {
            return context.evaluateExpression(explicitMultiplier);
        }

        String value = context.getConfigValue();
        if (value == null || value.isEmpty()) {
            return 1.0D;
        }
        double bonus = context.evaluateExpression(value);
        return bonus <= 0.0D ? 1.0D : 1.0D + bonus;
    }

    static int multipliedAmount(ItemStack stack, double multiplier) {
        int original = stack.getAmount();
        int newAmount = (int) Math.max(1, Math.ceil(original * multiplier));
        return Math.min(newAmount, stack.getMaxStackSize());
    }
}
