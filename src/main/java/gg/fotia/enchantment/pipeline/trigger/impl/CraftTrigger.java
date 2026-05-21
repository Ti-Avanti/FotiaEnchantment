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
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;

/**
 * 合成物品触发器 - 玩家在工作台合成物品时触发
 */
public class CraftTrigger implements Trigger, Listener {

    private EffectPipeline pipeline;

    @Override
    public String getId() {
        return "CRAFT";
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
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        ItemStack result = event.getRecipe() != null ? event.getRecipe().getResult() : null;
        int amount = result != null ? result.getAmount() : 1;
        TriggerContext context = TriggerContext.builder()
                .player(player)
                .event(event)
                .item(result)
                .value(amount)
                .altValue(0)
                .triggerId(getId())
                .build();
        pipeline.execute(context);
    }
}
