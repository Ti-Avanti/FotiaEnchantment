package gg.fotia.enchantment.pipeline.trigger.impl;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.pipeline.EffectPipeline;
import gg.fotia.enchantment.pipeline.trigger.Trigger;
import gg.fotia.enchantment.pipeline.trigger.TriggerContext;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * 种植种子触发器 - 玩家放置种子/作物方块时触发
 */
public class PlantSeedTrigger implements Trigger, Listener {

    private EffectPipeline pipeline;

    @Override
    public String getId() {
        return "PLANT_SEED";
    }

    @Override
    public void register(EffectPipeline pipeline) {
        this.pipeline = pipeline;
        FotiaEnchantment.getInstance().getServer().getPluginManager()
                .registerEvents(this, FotiaEnchantment.getInstance());
    }

    @Override
    public void unregister() {
        HandlerList.unregisterAll(this);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }
        Material type = event.getBlockPlaced().getType();
        if (!isSeedOrCrop(type)) {
            return;
        }
        TriggerContext context = TriggerContext.builder()
                .player(player)
                .event(event)
                .item(event.getItemInHand())
                .value(1)
                .altValue(0)
                .triggerId(getId())
                .build();
        pipeline.execute(context);
    }

    /**
     * 判断方块是否为种子或作物
     */
    private boolean isSeedOrCrop(Material type) {
        if (type == null) {
            return false;
        }
        switch (type) {
            case WHEAT:
            case CARROTS:
            case POTATOES:
            case BEETROOTS:
            case PUMPKIN_STEM:
            case MELON_STEM:
            case NETHER_WART:
            case COCOA:
            case SUGAR_CANE:
            case BAMBOO:
            case OAK_SAPLING:
            case SPRUCE_SAPLING:
            case BIRCH_SAPLING:
            case JUNGLE_SAPLING:
            case ACACIA_SAPLING:
            case DARK_OAK_SAPLING:
            case CHERRY_SAPLING:
            case MANGROVE_PROPAGULE:
            case SWEET_BERRY_BUSH:
            case GLOW_BERRIES:
            case TORCHFLOWER_CROP:
            case PITCHER_CROP:
            case KELP:
            case SEA_PICKLE:
                return true;
            default:
                return false;
        }
    }
}
