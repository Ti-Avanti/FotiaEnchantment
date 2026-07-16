package gg.fotia.enchantment.pipeline.condition;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.pipeline.CooldownManager;
import gg.fotia.enchantment.pipeline.condition.impl.ConsecutiveHitsCondition;
import gg.fotia.enchantment.pipeline.condition.impl.CooldownCheckCondition;
import gg.fotia.enchantment.pipeline.condition.impl.KillStreakCondition;
import gg.fotia.enchantment.pipeline.condition.impl.LastDamageTracker;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

/**
 * 维护需要跨事件累计的条件状态，确保条件本身只负责读取和判断。
 */
public final class ConditionStateListener implements Listener {

    private final FotiaEnchantment plugin;
    private final CooldownManager cooldownManager;

    public ConditionStateListener(FotiaEnchantment plugin, CooldownManager cooldownManager) {
        this.plugin = plugin;
        this.cooldownManager = cooldownManager;
    }

    public void register() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void shutdown() {
        HandlerList.unregisterAll(this);
        LastDamageTracker.clearAll();
        KillStreakCondition.clearAll();
        ConsecutiveHitsCondition.clearAll();
        CooldownCheckCondition.clearAll();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (event.getFinalDamage() > 0.0D && event.getEntity() instanceof Player player) {
            LastDamageTracker.recordDamage(player.getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getFinalDamage() <= 0.0D || !(event.getEntity() instanceof LivingEntity target)) {
            return;
        }
        Player attacker = resolveAttacker(event);
        if (attacker == null || attacker.getUniqueId().equals(target.getUniqueId())) {
            return;
        }
        ConsecutiveHitsCondition.recordHit(attacker.getUniqueId(), target.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer != null) {
            KillStreakCondition.recordKill(killer.getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        UUID playerId = event.getEntity().getUniqueId();
        LastDamageTracker.clear(playerId);
        KillStreakCondition.resetStreak(playerId);
        ConsecutiveHitsCondition.reset(playerId);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        LastDamageTracker.clear(playerId);
        KillStreakCondition.resetStreak(playerId);
        ConsecutiveHitsCondition.reset(playerId);
        CooldownCheckCondition.clearPlayer(playerId);
        cooldownManager.clearPlayer(playerId);
    }

    private Player resolveAttacker(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            return player;
        }
        if (event.getDamager() instanceof Projectile projectile
                && projectile.getShooter() instanceof Player player) {
            return player;
        }
        return null;
    }
}
