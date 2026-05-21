package gg.fotia.enchantment.pipeline.trigger.impl;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.pipeline.EffectPipeline;
import gg.fotia.enchantment.pipeline.trigger.Trigger;
import gg.fotia.enchantment.pipeline.trigger.TriggerContext;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * 空中近战攻击触发器 - 攻击者不在地面时攻击触发
 */
public class MeleeAttackWhileAirborneTrigger implements Trigger, Listener {

    private EffectPipeline pipeline;

    @Override
    public String getId() {
        return "MELEE_ATTACK_WHILE_AIRBORNE";
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
    public void onAirborneAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        // 攻击者必须没有站在实体方块上
        if (isSupported(player)) return;

        double damage = event.getFinalDamage();
        double fallDistance = player.getFallDistance();

        TriggerContext context = TriggerContext.builder()
                .player(player)
                .target(target)
                .event(event)
                .item(player.getInventory().getItemInMainHand())
                .value(damage)
                .altValue(fallDistance)
                .triggerId(getId())
                .build();
        pipeline.execute(context);
    }

    private boolean isSupported(Player player) {
        Material below = player.getLocation().clone().subtract(0, 0.1, 0).getBlock().getType();
        return below.isSolid();
    }
}
