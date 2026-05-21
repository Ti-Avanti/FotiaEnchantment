package gg.fotia.enchantment.pipeline.trigger.impl;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.pipeline.EffectPipeline;
import gg.fotia.enchantment.pipeline.trigger.Trigger;
import gg.fotia.enchantment.pipeline.trigger.TriggerContext;
import org.bukkit.Material;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

/**
 * 弩攻击触发器 - 弩射出的箭命中实体时触发
 */
public class CrossbowAttackTrigger implements Trigger, Listener {

    private EffectPipeline pipeline;

    @Override
    public String getId() {
        return "CROSSBOW_ATTACK";
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
    public void onCrossbowAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof AbstractArrow arrow)) return;
        if (!(arrow.getShooter() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        // 检查射击武器是否为弩
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();
        boolean hasCrossbow = mainHand.getType() == Material.CROSSBOW || offHand.getType() == Material.CROSSBOW;
        if (!hasCrossbow) return;

        double damage = event.getFinalDamage();
        double flyDistance = arrow.getLocation().distance(player.getLocation());

        ItemStack crossbowItem = mainHand.getType() == Material.CROSSBOW ? mainHand : offHand;

        TriggerContext context = TriggerContext.builder()
                .player(player)
                .target(target)
                .event(event)
                .item(crossbowItem)
                .value(damage)
                .altValue(flyDistance)
                .triggerId(getId())
                .build();
        pipeline.execute(context);
    }
}
