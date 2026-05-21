package gg.fotia.enchantment.pipeline.effect;

/**
 * 效果动作接口 - 定义附魔实际执行的动作
 */
public interface Effect {

    /**
     * 获取效果ID（对应配置中的 type 字段值，如 "TRUE_DAMAGE"）
     */
    String getId();

    /**
     * 执行效果动作
     */
    void execute(EffectContext context);
}
