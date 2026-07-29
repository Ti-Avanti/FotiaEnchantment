package gg.fotia.enchantment.pipeline.trigger.impl;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.pipeline.EffectPipeline;
import gg.fotia.enchantment.pipeline.trigger.Trigger;
import gg.fotia.enchantment.pipeline.trigger.TriggerContext;
import gg.fotia.enchantment.util.SchedulerUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;

/**
 * 按固定间隔遍历在线玩家触发的定时触发器基类。
 * 统一通过 SchedulerUtils 调度以兼容 Folia。
 */
abstract class AbstractPlayerTimerTrigger implements Trigger {

    private EffectPipeline pipeline;
    private Object task;
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
        this.task = SchedulerUtils.runTaskTimer(
                FotiaEnchantment.getInstance(), this::tick, interval, interval);
    }

    protected void tick() {
        if (!active) {
            return;
        }
        for (Player player : new ArrayList<>(Bukkit.getOnlinePlayers())) {
            dispatchPlayer(player);
        }
    }

    private void dispatchPlayer(Player player) {
        SchedulerUtils.runEntityTask(FotiaEnchantment.getInstance(), player, () -> {
            if (active && player.isOnline()) {
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
        SchedulerUtils.cancelTask(task);
        task = null;
    }
}
