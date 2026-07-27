package gg.fotia.enchantment.pipeline.condition;

/**
 * 条件接口 - 判断是否满足执行条件
 */
public interface Condition {

    /**
     * 获取条件ID（对应配置中的 type 字段值，如 "chance"）
     */
    String getId();

    /**
     * 检查条件是否满足
     *
     * @param context 条件上下文
     * @return true=满足，效果可以继续执行
     */
    boolean check(ConditionContext context);

    /**
     * 是否需要在动作实际执行后回调 {@link #onEffectsExecuted}。
     * 带副作用的条件(如冷却记录)应返回 true, 把副作用推迟到动作真正执行之后,
     * 避免后续条件失败时白白消耗资源。
     */
    default boolean requiresPostCommit() {
        return false;
    }

    /**
     * 动作执行完成后的提交回调 (仅在 {@link #requiresPostCommit()} 为 true 且本次动作已执行时调用)
     */
    default void onEffectsExecuted(ConditionContext context) {
    }
}
