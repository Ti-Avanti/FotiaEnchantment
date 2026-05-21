package gg.fotia.enchantment.pipeline.trigger.impl;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.pipeline.EffectPipeline;
import gg.fotia.enchantment.pipeline.trigger.Trigger;
import gg.fotia.enchantment.pipeline.trigger.TriggerContext;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;

/**
 * 受到弹射物伤害触发器 - 玩家被弹射物击中时触发
 */
public class TakeProjectileDamageTrigger implements Trigger, Listener {

    private EffectPipeline pipeline;

    @Override
    public String getId() {
        return "TAKE_PROJECTILE_DAMAGE";
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
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!(event.getDamager() instanceof Projectile projectile)) {
            return;
        }

        // 尝试解析弹射物的源 LivingEntity 作为 target
        LivingEntity target = null;
        ProjectileSource shooter = projectile.getShooter();
        if (shooter instanceof LivingEntity le) {
            target = le;
        }

        ItemStack chest = player.getInventory().getChestplate();
        ItemStack item = (chest != null && !chest.getType().isAir())
                ? chest : player.getInventory().getItemInMainHand();

        TriggerContext context = TriggerContext.builder()
                .player(player)
                .target(target)
                .event(event)
                .item(item)
                .value(event.getFinalDamage())
                .altValue(0)
                .triggerId(getId())
                .build();
        pipeline.execute(context);
    }
}
