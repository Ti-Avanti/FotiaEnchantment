package gg.fotia.enchantment.pipeline.trigger.impl;

import gg.fotia.enchantment.FotiaEnchantment;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * 自定义定时触发器 - 间隔从 config.yml 的 triggers.timer-custom-interval 读取(默认 200 tick)
 */
public class TimerCustomTrigger extends AbstractPlayerTimerTrigger {

    private static final long DEFAULT_INTERVAL_TICKS = 200L;

    @Override
    public String getId() {
        return "TIMER_CUSTOM";
    }

    @Override
    protected long intervalTicks() {
        long interval = DEFAULT_INTERVAL_TICKS;
        try {
            YamlConfiguration mainConfig = FotiaEnchantment.getInstance().getConfigManager().getMainConfig();
            if (mainConfig != null) {
                interval = mainConfig.getInt("triggers.timer-custom-interval", (int) DEFAULT_INTERVAL_TICKS);
            }
        } catch (Throwable ignored) {
            // 配置不可用时使用默认值
        }
        return interval <= 0L ? DEFAULT_INTERVAL_TICKS : interval;
    }
}
