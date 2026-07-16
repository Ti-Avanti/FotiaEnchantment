package gg.fotia.enchantment.pipeline.trigger.impl;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.pipeline.EffectPipeline;
import gg.fotia.enchantment.pipeline.trigger.Trigger;
import gg.fotia.enchantment.pipeline.trigger.TriggerContext;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 助攻触发器 - 玩家参与对实体造成伤害但不是最终击杀者时触发
 *
 * <p>记录每个被伤害实体的伤害贡献者及伤害量，
 * 实体死亡时对所有非击杀者的玩家触发。
 */
public class AssistTrigger implements Trigger, Listener {

    private EffectPipeline pipeline;
    private final Map<UUID, Map<UUID, Double>> damageMap = new ConcurrentHashMap<>();

    @Override
    public String getId() {
        return "ASSIST";
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
        damageMap.clear();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity victim)) {
            return;
        }

        // 解析最终攻击者（处理弹射物）
        Player attacker = resolveAttacker(event);
        if (attacker == null) {
            return;
        }
        // 不记录玩家攻击自己的情况
        if (attacker.getUniqueId().equals(victim.getUniqueId())) {
            return;
        }

        damageMap.computeIfAbsent(victim.getUniqueId(), k -> new ConcurrentHashMap<>())
                .merge(attacker.getUniqueId(), event.getFinalDamage(), Double::sum);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Map<UUID, Double> contributors = damageMap.remove(entity.getUniqueId());
        if (contributors == null || contributors.isEmpty()) {
            return;
        }

        Player killer = entity.getKiller();
        UUID killerId = killer == null ? null : killer.getUniqueId();

        for (Map.Entry<UUID, Double> entry : contributors.entrySet()) {
            UUID playerId = entry.getKey();
            // 击杀者不算助攻
            if (playerId.equals(killerId)) {
                continue;
            }
            Player player = Bukkit.getPlayer(playerId);
            if (player == null || !player.isOnline()) {
                continue;
            }

            TriggerContext context = TriggerContext.builder()
                    .player(player)
                    .target(entity)
                    .event(event)
                    .item(player.getInventory().getItemInMainHand())
                    .value(entry.getValue())
                    .altValue(0)
                    .triggerId(getId())
                    .build();
            pipeline.execute(context);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        damageMap.remove(playerId);
        damageMap.values().removeIf(contributors -> {
            contributors.remove(playerId);
            return contributors.isEmpty();
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntitiesUnload(EntitiesUnloadEvent event) {
        event.getEntities().forEach(entity -> damageMap.remove(entity.getUniqueId()));
    }

    /**
     * 解析最终的玩家攻击者，弹射物会回溯到 Shooter
     */
    private Player resolveAttacker(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player p) {
            return p;
        }
        if (event.getDamager() instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Player p) {
                return p;
            }
        }
        return null;
    }
}
