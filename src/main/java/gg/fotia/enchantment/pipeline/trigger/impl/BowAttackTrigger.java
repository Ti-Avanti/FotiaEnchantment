package gg.fotia.enchantment.pipeline.trigger.impl;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.core.LocationDistance;
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
 * 弓箭攻击触发器 - 弓射出的箭命中实体时触发
 */
public class BowAttackTrigger implements Trigger, Listener {

    private EffectPipeline pipeline;

    @Override
    public String getId() {
        return "BOW_ATTACK";
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
    public void onBowAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof AbstractArrow arrow)) return;
        if (!(arrow.getShooter() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        // 检查射击武器是否为弓
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();
        boolean hasBow = mainHand.getType() == Material.BOW || offHand.getType() == Material.BOW;
        if (!hasBow) return;

        double damage = event.getFinalDamage();
        // 使用箭的存活tick数估算飞行距离
        double flyDistance = LocationDistance.safeDistance(arrow.getLocation(), player.getLocation());

        ItemStack bowItem = mainHand.getType() == Material.BOW ? mainHand : offHand;

        TriggerContext context = TriggerContext.builder()
                .player(player)
                .target(target)
                .event(event)
                .item(bowItem)
                .value(damage)
                .altValue(flyDistance)
                .triggerId(getId())
                .build();
        pipeline.execute(context);
    }
}
