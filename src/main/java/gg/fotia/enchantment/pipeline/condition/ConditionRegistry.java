package gg.fotia.enchantment.pipeline.condition;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 条件注册表 - 管理所有条件实现的注册与实例化
 */
public class ConditionRegistry {

    private final Map<String, Supplier<Condition>> conditionFactories = new HashMap<>();

    /**
     * 注册一个条件工厂
     *
     * @param id      条件ID（不区分大小写，统一以小写存储）
     * @param factory 条件实例工厂
     */
    public void register(String id, Supplier<Condition> factory) {
        if (id == null || factory == null) {
            return;
        }
        conditionFactories.put(id.toLowerCase(), factory);
    }

    /**
     * 根据ID获取一个新的条件实例
     *
     * @param id 条件ID
     * @return 条件实例，若未注册返回 null
     */
    public Condition get(String id) {
        if (id == null) {
            return null;
        }
        Supplier<Condition> factory = conditionFactories.get(id.toLowerCase());
        return factory == null ? null : factory.get();
    }

    /**
     * 获取所有已注册的条件ID
     */
    public Set<String> getRegisteredIds() {
        return Collections.unmodifiableSet(conditionFactories.keySet());
    }
}
