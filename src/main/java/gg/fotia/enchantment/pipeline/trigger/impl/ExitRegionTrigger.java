package gg.fotia.enchantment.pipeline.trigger.impl;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.integration.WorldGuardHook;
import gg.fotia.enchantment.pipeline.EffectPipeline;
import gg.fotia.enchantment.pipeline.trigger.Trigger;
import gg.fotia.enchantment.pipeline.trigger.TriggerContext;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 离开 WorldGuard 区域触发器
 */
public class ExitRegionTrigger implements Trigger, Listener {

    private EffectPipeline pipeline;
    private final Map<UUID, Set<String>> knownRegions = new HashMap<>();

    @Override
    public String getId() {
        return "EXIT_REGION";
    }

    @Override
    public void register(EffectPipeline pipeline) {
        this.pipeline = pipeline;
        if (getWorldGuardHook() == null) {
            return;
        }
        FotiaEnchantment.getInstance().getServer().getPluginManager()
                .registerEvents(this, FotiaEnchantment.getInstance());
    }

    @Override
    public void unregister() {
        HandlerList.unregisterAll(this);
        knownRegions.clear();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Location to = event.getTo();
        if (to == null || isSameBlock(event.getFrom(), to)) {
            return;
        }
        WorldGuardHook hook = getWorldGuardHook();
        if (hook == null) {
            return;
        }

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        Set<String> previous = knownRegions.computeIfAbsent(uuid,
                ignored -> hook.getRegionIds(event.getFrom()));
        Set<String> current = hook.getRegionIds(to);
        if (current.equals(previous)) {
            return;
        }

        Set<String> exited = new HashSet<>(previous);
        exited.removeAll(current);
        knownRegions.put(uuid, current);
        if (exited.isEmpty()) {
            return;
        }

        TriggerContext context = TriggerContext.builder()
                .player(player)
                .event(event)
                .item(player.getInventory().getItemInMainHand())
                .value(exited.size())
                .altValue(current.size())
                .triggerId(getId())
                .build();
        pipeline.execute(context);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        knownRegions.remove(event.getPlayer().getUniqueId());
    }

    private boolean isSameBlock(Location from, Location to) {
        return from.getWorld() == to.getWorld()
                && from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ();
    }

    private WorldGuardHook getWorldGuardHook() {
        FotiaEnchantment plugin = FotiaEnchantment.getInstance();
        if (plugin == null || plugin.getIntegrationManager() == null) {
            return null;
        }
        WorldGuardHook hook = plugin.getIntegrationManager().getWorldGuardHook();
        return hook != null && hook.isAvailable() ? hook : null;
    }
}
