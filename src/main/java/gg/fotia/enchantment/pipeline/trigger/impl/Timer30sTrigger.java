package gg.fotia.enchantment.pipeline.trigger.impl;

/**
 * 定时触发器(30秒) - 每 600 tick 遍历在线玩家触发一次
 */
public class Timer30sTrigger extends AbstractPlayerTimerTrigger {

    @Override
    public String getId() {
        return "TIMER_30S";
    }

    @Override
    protected long intervalTicks() {
        return 600L;
    }
}
