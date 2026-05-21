package gg.fotia.enchantment.pipeline.trigger.impl;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.pipeline.EffectPipeline;
import gg.fotia.enchantment.pipeline.trigger.Trigger;
import gg.fotia.enchantment.pipeline.trigger.TriggerContext;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.NotePlayEvent;

/**
 * 音符盒演奏触发器 - 音符盒发声时触发附近最近的玩家
 *
 * <p>NotePlayEvent 是方块事件，没有直接的触发者。这里以方块为中心，
 * 找最近的 6 格内玩家作为触发者；找不到则不触发。
 */
public class NoteBlockPlayTrigger implements Trigger, Listener {

    private static final double SEARCH_RADIUS = 6.0D;

    private EffectPipeline pipeline;

    @Override
    public String getId() {
        return "NOTE_BLOCK_PLAY";
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
    public void onNotePlay(NotePlayEvent event) {
        Block block = event.getBlock();
        if (block == null) {
            return;
        }
        Location blockLoc = block.getLocation().add(0.5, 0.5, 0.5);
        if (blockLoc.getWorld() == null) {
            return;
        }
        Player nearest = null;
        double nearestDist = Double.MAX_VALUE;
        for (Player player : blockLoc.getWorld().getPlayers()) {
            double dist = player.getLocation().distance(blockLoc);
            if (dist <= SEARCH_RADIUS && dist < nearestDist) {
                nearestDist = dist;
                nearest = player;
            }
        }
        if (nearest == null) {
            return;
        }
        int noteId = event.getNote() != null ? event.getNote().getId() : 0;
        TriggerContext context = TriggerContext.builder()
                .player(nearest)
                .event(event)
                .item(nearest.getInventory().getItemInMainHand())
                .value(1)
                .altValue(noteId)
                .triggerId(getId())
                .build();
        pipeline.execute(context);
    }
}
