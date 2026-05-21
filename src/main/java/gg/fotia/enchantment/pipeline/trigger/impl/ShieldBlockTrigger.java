package gg.fotia.enchantment.pipeline.trigger.impl;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.pipeline.EffectPipeline;
import gg.fotia.enchantment.pipeline.trigger.Trigger;
import gg.fotia.enchantment.pipeline.trigger.TriggerContext;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

/**
 * 盾牌格挡触发器 - 玩家在格挡状态下受到伤害时触发
 *
 * <p>value 为原始伤害（getDamage），altValue 为格挡掉的伤害（原始 - 最终）。
 */
public class ShieldBlockTrigger implements Trigger, Listener {

    private EffectPipeline pipeline;

    @Override
    public String getId() {
        return "SHIELD_BLOCK";
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
        if (!player.isBlocking()) {
            return;
        }

        // 攻击者作为目标传入（若是 LivingEntity）
        LivingEntity target = (event.getDamager() instanceof LivingEntity le) ? le : null;

        // 盾牌格挡时使用副手或主手物品（盾牌通常在副手）
        ItemStack offhand = player.getInventory().getItemInOffHand();
        ItemStack item = (offhand != null && !offhand.getType().isAir())
                ? offhand : player.getInventory().getItemInMainHand();

        double rawDamage = event.getDamage();
        double blockedAmount = rawDamage - event.getFinalDamage();

        TriggerContext context = TriggerContext.builder()
                .player(player)
                .target(target)
                .event(event)
                .item(item)
                .value(rawDamage)
                .altValue(blockedAmount)
                .triggerId(getId())
                .build();
        pipeline.execute(context);
    }
}
