package gg.fotia.enchantment.pipeline.condition;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 条件注册表 - 管理所有条件实现的注册与实例化。
 * 条件实现均为无状态对象, get 返回按 ID 缓存的共享实例, 避免每次检查都新建对象。
 */
public class ConditionRegistry {

    private final Map<String, Supplier<Condition>> conditionFactories = new HashMap<>();
    private final Map<String, Condition> sharedInstances = new ConcurrentHashMap<>();

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
        String key = id.toLowerCase();
        conditionFactories.put(key, factory);
        sharedInstances.remove(key);
    }

    /**
     * 根据ID获取共享条件实例
     *
     * @param id 条件ID
     * @return 条件实例，若未注册返回 null
     */
    public Condition get(String id) {
        if (id == null) {
            return null;
        }
        String key = id.toLowerCase();
        Condition cached = sharedInstances.get(key);
        if (cached != null) {
            return cached;
        }
        Supplier<Condition> factory = conditionFactories.get(key);
        if (factory == null) {
            return null;
        }
        Condition instance = factory.get();
        if (instance != null) {
            sharedInstances.put(key, instance);
        }
        return instance;
    }

    /**
     * 获取所有已注册的条件ID
     */
    public Set<String> getRegisteredIds() {
        return Collections.unmodifiableSet(conditionFactories.keySet());
    }
}
