package gg.fotia.enchantment.pipeline.trigger.impl;

/**
 * 定时触发器(1秒) - 每 20 tick 遍历在线玩家触发一次
 */
public class Timer1sTrigger extends AbstractPlayerTimerTrigger {

    @Override
    public String getId() {
        return "TIMER_1S";
    }

    @Override
    protected long intervalTicks() {
        return 20L;
    }
}
