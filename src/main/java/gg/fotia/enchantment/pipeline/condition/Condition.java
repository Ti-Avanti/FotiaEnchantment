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
}
