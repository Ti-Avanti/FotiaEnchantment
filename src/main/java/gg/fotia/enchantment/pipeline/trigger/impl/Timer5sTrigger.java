package gg.fotia.enchantment.pipeline.trigger.impl;

/**
 * 定时触发器(5秒) - 每 100 tick 遍历在线玩家触发一次
 */
public class Timer5sTrigger extends AbstractPlayerTimerTrigger {

    @Override
    public String getId() {
        return "TIMER_5S";
    }

    @Override
    protected long intervalTicks() {
        return 100L;
    }
}
