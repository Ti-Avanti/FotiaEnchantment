package gg.fotia.enchantment.pipeline.trigger.impl;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.pipeline.EffectPipeline;
import gg.fotia.enchantment.pipeline.trigger.Trigger;
import gg.fotia.enchantment.pipeline.trigger.TriggerContext;
import org.bukkit.Location;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.raid.RaidFinishEvent;

import java.util.List;

/**
 * 赢得袭击触发器 - 袭击结束时通知英雄列表中的玩家
 */
public class WinRaidTrigger implements Trigger, Listener {

    private static final double FALLBACK_RADIUS = 96.0D;

    private EffectPipeline pipeline;

    @Override
    public String getId() {
        return "WIN_RAID";
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

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRaidFinish(RaidFinishEvent event) {
        List<Player> winners = event.getWinners();
        if (winners != null && !winners.isEmpty()) {
            for (Player player : winners) {
                fire(player, event);
            }
            return;
        }
        // 回退: 找袭击地点附近的玩家
        Location loc = event.getRaid() != null ? event.getRaid().getLocation() : null;
        if (loc == null || loc.getWorld() == null) {
            return;
        }
        for (Player player : loc.getWorld().getPlayers()) {
            if (player == null) {
                continue;
            }
            if (player.getLocation().distance(loc) <= FALLBACK_RADIUS) {
                fire(player, event);
            }
        }
    }

    private void fire(Player player, RaidFinishEvent event) {
        if (player == null) {
            return;
        }
        TriggerContext context = TriggerContext.builder()
                .player(player)
                .event(event)
                .item(player.getInventory().getItemInMainHand())
                .value(1)
                .altValue(0)
                .triggerId(getId())
                .build();
        pipeline.execute(context);
    }
}
