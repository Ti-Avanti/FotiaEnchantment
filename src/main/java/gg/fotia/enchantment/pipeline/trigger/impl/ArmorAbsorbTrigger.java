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
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

/**
 * 护甲吸收触发器 - 玩家受到伤害且护甲吸收了一部分伤害时触发
 *
 * <p>value 为护甲吸收量（原始 - 最终），altValue 为最终实际伤害。
 */
public class ArmorAbsorbTrigger implements Trigger, Listener {

    private EffectPipeline pipeline;

    @Override
    public String getId() {
        return "ARMOR_ABSORB";
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
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        double rawDamage = event.getDamage();
        double finalDamage = event.getFinalDamage();
        double absorbed = rawDamage - finalDamage;
        // 没有护甲吸收时不触发
        if (absorbed <= 0) {
            return;
        }

        ItemStack chest = player.getInventory().getChestplate();
        ItemStack item = (chest != null && !chest.getType().isAir())
                ? chest : player.getInventory().getItemInMainHand();

        TriggerContext context = TriggerContext.builder()
                .player(player)
                .event(event)
                .item(item)
                .value(absorbed)
                .altValue(finalDamage)
                .triggerId(getId())
                .build();
        pipeline.execute(context);
    }
}
