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
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * 食用物品触发器 - 玩家吃下食物或喝下药水时触发
 */
public class ConsumeTrigger implements Trigger, Listener {

    private EffectPipeline pipeline;

    @Override
    public String getId() {
        return "CONSUME";
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
    public void onConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }
        ItemStack item = event.getItem();
        // altValue = 食物饱食度回复量（如果有 FoodComponent）
        double nutrition = 0;
        if (item != null && item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            try {
                if (meta.hasFood()) {
                    nutrition = meta.getFood().getNutrition();
                }
            } catch (NoSuchMethodError | NoClassDefFoundError ignored) {
                // 旧版本无 FoodComponent API
            }
        }
        TriggerContext context = TriggerContext.builder()
                .player(player)
                .event(event)
                .item(item)
                .value(1)
                .altValue(nutrition)
                .triggerId(getId())
                .build();
        pipeline.execute(context);
    }
}
