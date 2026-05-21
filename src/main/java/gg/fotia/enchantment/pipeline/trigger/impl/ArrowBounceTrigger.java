package gg.fotia.enchantment.pipeline.trigger.impl;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.pipeline.EffectPipeline;
import gg.fotia.enchantment.pipeline.trigger.Trigger;
import gg.fotia.enchantment.pipeline.trigger.TriggerContext;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;

/**
 * 箭矢弹跳触发器 - 箭矢命中后反弹时触发
 * 检查箭矢命中后是否未嵌入方块中（即发生了弹跳）
 */
public class ArrowBounceTrigger implements Trigger, Listener {

    private EffectPipeline pipeline;

    @Override
    public String getId() {
        return "ARROW_BOUNCE";
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
    public void onArrowBounce(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof AbstractArrow arrow)) return;
        if (!(arrow.getShooter() instanceof Player player)) return;

        // 检查箭矢是否设置为可弹跳（getBounce在Paper 1.21+已有）
        // 如果箭矢命中方块但未嵌入，视为弹跳
        if (event.getHitBlock() == null) return;

        // 在Paper API中，如果箭矢会弹跳，isCritical在命中后会保持
        // 使用 AbstractArrow 的 isShotFromCrossbow 或其他特性判断
        // 简化实现：如果箭矢不在方块中，则视为弹跳
        if (arrow.isInBlock()) return;

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
