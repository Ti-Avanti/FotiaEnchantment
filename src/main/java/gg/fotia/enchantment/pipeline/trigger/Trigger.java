package gg.fotia.enchantment.pipeline.trigger;

import gg.fotia.enchantment.pipeline.EffectPipeline;

/**
 * 触发器接口 - 定义何时激活附魔效果
 *
 * <p>触发器负责监听某种 Bukkit 事件，在事件发生时构建 {@link TriggerContext}
 * 并调用 {@link EffectPipeline#execute(TriggerContext)} 驱动后续流程。
 */
public interface Trigger {

    /**
     * 获取触发器ID（对应配置中的 trigger 字段值，如 "MELEE_ATTACK"）
     */
    String getId();

    /**
     * 注册该触发器需要的事件监听器
     *
     * @param pipeline 效果管道引用，触发时调用 pipeline.execute()
     */
    void register(EffectPipeline pipeline);

    /**
     * 取消注册（插件关闭或重载时调用）
     */
    void unregister();
}
