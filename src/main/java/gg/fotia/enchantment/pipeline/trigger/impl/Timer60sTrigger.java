package gg.fotia.enchantment.pipeline.trigger.impl;

/**
 * 定时触发器(60秒) - 每 1200 tick 遍历在线玩家触发一次
 */
public class Timer60sTrigger extends AbstractPlayerTimerTrigger {

    @Override
    public String getId() {
        return "TIMER_60S";
    }

    @Override
    protected long intervalTicks() {
        return 1200L;
    }
}
