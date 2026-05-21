package gg.fotia.enchantment.pipeline.trigger.impl;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.pipeline.EffectPipeline;
import gg.fotia.enchantment.pipeline.trigger.Trigger;
import gg.fotia.enchantment.pipeline.trigger.TriggerContext;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;

import java.util.EnumSet;
import java.util.Set;

/**
 * 钓到垃圾触发器
 */
public class CatchJunkTrigger implements Trigger, Listener {

    private static final Set<Material> JUNK = EnumSet.of(
            Material.LILY_PAD,
            Material.LEATHER_BOOTS,
            Material.LEATHER,
            Material.BOWL,
            Material.STICK,
            Material.STRING,
            Material.POTION,
            Material.BONE,
            Material.INK_SAC,
            Material.TRIPWIRE_HOOK,
            Material.ROTTEN_FLESH
    );

    private EffectPipeline pipeline;

    @Override
    public String getId() {
        return "CATCH_JUNK";
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
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
            return;
        }
        if (!(event.getCaught() instanceof Item itemEntity)) {
            return;
        }
        ItemStack stack = itemEntity.getItemStack();
        if (stack == null || !JUNK.contains(stack.getType())) {
            return;
        }
        Player player = event.getPlayer();
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
