package gg.fotia.enchantment.pipeline.trigger.impl;

import gg.fotia.enchantment.pipeline.EffectPipeline;
import gg.fotia.enchantment.pipeline.trigger.Trigger;
import gg.fotia.enchantment.pipeline.trigger.TriggerContext;
import org.bukkit.entity.Player;

/**
 * 按固定间隔遍历在线玩家触发的定时触发器基类。
 * 使用共享分发器，玩家回调在其所属实体线程执行。
 */
abstract class AbstractPlayerTimerTrigger implements Trigger {

    private EffectPipeline pipeline;
    private Runnable unsubscribe;
    private volatile boolean active;

    protected final EffectPipeline pipeline() {
        return pipeline;
    }

    /** 触发间隔 (tick) */
    protected abstract long intervalTicks();

    @Override
    public void register(EffectPipeline pipeline) {
        this.pipeline = pipeline;
        this.active = true;
        long interval = Math.max(1L, intervalTicks());
        this.unsubscribe = pipeline.getPlayerTimers().subscribe(interval, player -> {
            if (active) {
                handlePlayer(player);
            }
        });
    }

    /**
     * 处理单个玩家, 默认以主手物品为上下文触发全装备扫描
     */
    protected void handlePlayer(Player player) {
        TriggerContext ctx = TriggerContext.builder()
                .player(player)
                .item(player.getInventory().getItemInMainHand())
                .value(0)
                .altValue(0)
                .triggerId(getId())
                .build();
        pipeline.execute(ctx);
    }

    @Override
    public void unregister() {
        active = false;
        if (unsubscribe != null) {
            unsubscribe.run();
            unsubscribe = null;
        }
    }
}
