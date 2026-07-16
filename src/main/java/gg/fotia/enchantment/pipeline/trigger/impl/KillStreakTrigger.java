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
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 连杀触发器 - 玩家在未死亡的情况下连续击杀实体时触发
 *
 * <p>使用 ConcurrentHashMap 记录每个玩家的连杀数，
 * 玩家自身死亡（PlayerDeathEvent）时清零。
 */
public class KillStreakTrigger implements Trigger, Listener {

    private EffectPipeline pipeline;
    private final Map<UUID, Integer> killStreaks = new ConcurrentHashMap<>();

    @Override
    public String getId() {
        return "KILL_STREAK";
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
        killStreaks.clear();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onKill(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();
        if (killer == null) {
            return;
        }

        // 累加连杀数
        int streak = killStreaks.merge(killer.getUniqueId(), 1, Integer::sum);

        TriggerContext context = TriggerContext.builder()
                .player(killer)
                .target(entity)
                .event(event)
                .item(killer.getInventory().getItemInMainHand())
                .value(streak)
                .altValue(0)
                .triggerId(getId())
                .build();
        pipeline.execute(context);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        // 玩家死亡时重置自己的连杀数
        killStreaks.remove(event.getEntity().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        killStreaks.remove(event.getPlayer().getUniqueId());
    }
}
