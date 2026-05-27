package gg.fotia.enchantment.pipeline.trigger.impl;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.compat.BukkitAttributes;
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
 * 濒死触发器 - 玩家受到伤害后剩余血量低于最大血量 20% 时触发
 */
public class NearDeathTrigger implements Trigger, Listener {

    private static final double THRESHOLD = 0.2;

    private EffectPipeline pipeline;

    @Override
    public String getId() {
        return "NEAR_DEATH";
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

        double finalDamage = event.getFinalDamage();
        double remaining = player.getHealth() - finalDamage;
        // 致命伤害不算濒死
        if (remaining <= 0) {
            return;
        }

        double maxHealth = BukkitAttributes.maxHealthValue(player);
        if (remaining > maxHealth * THRESHOLD) {
            return;
        }

        ItemStack chest = player.getInventory().getChestplate();
        ItemStack item = (chest != null && !chest.getType().isAir())
                ? chest : player.getInventory().getItemInMainHand();

        TriggerContext context = TriggerContext.builder()
                .player(player)
                .event(event)
                .item(item)
                .value(remaining)
                .altValue(maxHealth)
                .triggerId(getId())
                .build();
        pipeline.execute(context);
    }
}
