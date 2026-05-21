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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 自动熔炼效果 - 将挖掘掉落物中的可熔炼物品替换为熔炼产物。
 */
public class SmeltEffect implements Effect {

    private static final Map<Material, Material> SMELT_MAP = new HashMap<>();

    static {
        // 铁
        SMELT_MAP.put(Material.IRON_ORE, Material.IRON_INGOT);
        SMELT_MAP.put(Material.DEEPSLATE_IRON_ORE, Material.IRON_INGOT);
        SMELT_MAP.put(Material.RAW_IRON, Material.IRON_INGOT);
        // 金
        SMELT_MAP.put(Material.GOLD_ORE, Material.GOLD_INGOT);
        SMELT_MAP.put(Material.DEEPSLATE_GOLD_ORE, Material.GOLD_INGOT);
        SMELT_MAP.put(Material.NETHER_GOLD_ORE, Material.GOLD_INGOT);
        SMELT_MAP.put(Material.RAW_GOLD, Material.GOLD_INGOT);
        // 铜
        SMELT_MAP.put(Material.COPPER_ORE, Material.COPPER_INGOT);
        SMELT_MAP.put(Material.DEEPSLATE_COPPER_ORE, Material.COPPER_INGOT);
        SMELT_MAP.put(Material.RAW_COPPER, Material.COPPER_INGOT);
        // 远古残骸
        SMELT_MAP.put(Material.ANCIENT_DEBRIS, Material.NETHERITE_SCRAP);
        // 其它常见
        SMELT_MAP.put(Material.COBBLESTONE, Material.STONE);
        SMELT_MAP.put(Material.COBBLED_DEEPSLATE, Material.DEEPSLATE);
        SMELT_MAP.put(Material.SAND, Material.GLASS);
        SMELT_MAP.put(Material.RED_SAND, Material.GLASS);
        SMELT_MAP.put(Material.CLAY_BALL, Material.BRICK);
        SMELT_MAP.put(Material.NETHERRACK, Material.NETHER_BRICK);
        SMELT_MAP.put(Material.WET_SPONGE, Material.SPONGE);
        SMELT_MAP.put(Material.SEA_PICKLE, Material.LIME_DYE);
        SMELT_MAP.put(Material.KELP, Material.DRIED_KELP);
        SMELT_MAP.put(Material.BASALT, Material.SMOOTH_BASALT);
    }

    @Override
    public String getId() {
        return "SMELT";
    }

    @Override
    public void execute(EffectContext context) {
        Event event = context.getTriggerContext().getEvent();
        if (event instanceof BlockDropItemEvent dropEvent) {
            smeltDroppedItems(dropEvent.getItems());
            return;
        }
        if (event instanceof BlockBreakEvent breakEvent) {
            smeltBlockBreakDrops(context, breakEvent);
        }
    }

    private void smeltDroppedItems(List<Item> items) {
        for (Item itemEntity : items) {
            ItemStack stack = itemEntity.getItemStack();
            if (stack == null) continue;
            ItemStack smelted = smeltStack(stack);
            if (smelted != null) {
                itemEntity.setItemStack(smelted);
            }
        }
    }

    private void smeltBlockBreakDrops(EffectContext context, BlockBreakEvent event) {
        if (!event.isDropItems()) {
            return;
        }

        ItemStack tool = context.getTriggerContext().getItem();
        Collection<ItemStack> drops = event.getBlock().getDrops(tool, event.getPlayer());
        List<ItemStack> replacementDrops = new ArrayList<>();
        boolean changed = false;
        for (ItemStack drop : drops) {
            ItemStack smelted = smeltStack(drop);
            if (smelted == null) {
                replacementDrops.add(drop);
            } else {
                replacementDrops.add(smelted);
                changed = true;
            }
        }
        if (!changed) {
            return;
        }

        event.setDropItems(false);
        Location location = event.getBlock().getLocation().add(0.5, 0.5, 0.5);
        for (ItemStack replacement : replacementDrops) {
            if (replacement != null && !replacement.getType().isAir() && replacement.getAmount() > 0) {
                event.getBlock().getWorld().dropItemNaturally(location, replacement);
            }
        }
    }

    private ItemStack smeltStack(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return null;
        }
        Material smelted = SMELT_MAP.get(stack.getType());
        return smelted == null ? null : new ItemStack(smelted, stack.getAmount());
    }
}
