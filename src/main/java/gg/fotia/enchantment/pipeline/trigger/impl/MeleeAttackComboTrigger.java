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
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 近战连击触发器 - 对同一目标连续攻击时触发
 * 3秒内对同一目标的连续攻击计为连击
 */
public class MeleeAttackComboTrigger implements Trigger, Listener {

    private EffectPipeline pipeline;

    /** 记录玩家对目标的连击信息：玩家UUID -> (目标UUID -> 连击数据) */
    private final Map<UUID, ComboData> comboMap = new ConcurrentHashMap<>();

    /** 连击超时时间（毫秒） */
    private static final long COMBO_TIMEOUT = 3000L;

    @Override
    public String getId() {
        return "MELEE_ATTACK_COMBO";
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
        comboMap.clear();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onComboAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        UUID playerUuid = player.getUniqueId();
        UUID targetUuid = target.getUniqueId();
        long now = System.currentTimeMillis();

        // 获取或创建连击数据
        ComboData data = comboMap.get(playerUuid);
        if (data == null || !data.targetUuid.equals(targetUuid)
                || (now - data.lastHitTime) > COMBO_TIMEOUT) {
            // 重置连击：新目标或超时
            data = new ComboData(targetUuid, now, 1);
        } else {
            // 继续连击
            data = new ComboData(targetUuid, now, data.comboCount + 1);
        }
        comboMap.put(playerUuid, data);

        double damage = event.getFinalDamage();

        TriggerContext context = TriggerContext.builder()
                .player(player)
                .target(target)
                .event(event)
                .item(player.getInventory().getItemInMainHand())
                .value(damage)
                .altValue(data.comboCount)
                .triggerId(getId())
                .build();
        pipeline.execute(context);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        comboMap.remove(event.getPlayer().getUniqueId());
    }

    /**
     * 连击数据
     */
    private static class ComboData {
        final UUID targetUuid;
        final long lastHitTime;
        final int comboCount;

        ComboData(UUID targetUuid, long lastHitTime, int comboCount) {
            this.targetUuid = targetUuid;
            this.lastHitTime = lastHitTime;
            this.comboCount = comboCount;
        }
    }
}
