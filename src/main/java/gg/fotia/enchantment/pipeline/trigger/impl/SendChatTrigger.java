package gg.fotia.enchantment.pipeline.trigger.impl;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.pipeline.EffectPipeline;
import gg.fotia.enchantment.pipeline.trigger.Trigger;
import gg.fotia.enchantment.pipeline.trigger.TriggerContext;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

/**
 * 发送聊天触发器 - 玩家在聊天框发送消息时触发
 *
 * <p>AsyncChatEvent 是异步事件，需要切回主线程执行 pipeline。
 */
public class SendChatTrigger implements Trigger, Listener {

    private EffectPipeline pipeline;

    @Override
    public String getId() {
        return "SEND_CHAT";
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
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }
        FotiaEnchantment plugin = FotiaEnchantment.getInstance();
        // 异步事件 -> 切回主线程
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) {
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
        });
    }
}
