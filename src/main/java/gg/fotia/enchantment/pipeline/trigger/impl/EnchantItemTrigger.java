package gg.fotia.enchantment.pipeline.trigger.impl;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.pipeline.EffectPipeline;
import gg.fotia.enchantment.pipeline.trigger.Trigger;
import gg.fotia.enchantment.pipeline.trigger.TriggerContext;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;

/**
 * 附魔物品触发器 - 玩家在附魔台附魔物品时触发
 */
public class EnchantItemTrigger implements Trigger, Listener {

    private EffectPipeline pipeline;

    @Override
    public String getId() {
        return "ENCHANT_ITEM";
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
    public void onEnchant(EnchantItemEvent event) {
        Player player = event.getEnchanter();
        if (player == null) {
            return;
        }
        // value=经验等级消耗，altValue=最高附魔等级
        int maxLevel = 0;
        if (event.getEnchantsToAdd() != null) {
            for (Integer lvl : event.getEnchantsToAdd().values()) {
                if (lvl != null && lvl > maxLevel) {
                    maxLevel = lvl;
                }
            }
        }
        TriggerContext context = TriggerContext.builder()
                .player(player)
                .event(event)
                .item(event.getItem())
                .value(event.getExpLevelCost())
                .altValue(maxLevel)
                .triggerId(getId())
                .build();
        pipeline.execute(context);
    }
}
