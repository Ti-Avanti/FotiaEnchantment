package gg.fotia.enchantment.pipeline.trigger.impl;

/**
 * 定时触发器(10秒) - 每 200 tick 遍历在线玩家触发一次
 */
public class Timer10sTrigger extends AbstractPlayerTimerTrigger {

    @Override
    public String getId() {
        return "TIMER_10S";
    }

    @Override
    protected long intervalTicks() {
        return 200L;
    }
}
