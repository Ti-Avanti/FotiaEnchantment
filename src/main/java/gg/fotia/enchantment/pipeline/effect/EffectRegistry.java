package gg.fotia.enchantment.pipeline.effect;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 效果注册表 - 管理所有效果动作实现的注册与实例化。
 * 效果实现均为无状态对象, get 返回按 ID 缓存的共享实例, 避免每次执行都新建对象。
 */
public class EffectRegistry {

    private final Map<String, Supplier<Effect>> effectFactories = new HashMap<>();
    private final Map<String, Effect> sharedInstances = new ConcurrentHashMap<>();

    /**
     * 注册一个效果工厂
     *
     * @param id      效果ID（不区分大小写，统一以大写存储）
     * @param factory 效果实例工厂
     */
    public void register(String id, Supplier<Effect> factory) {
        if (id == null || factory == null) {
            return;
        }
        String key = id.toUpperCase();
        effectFactories.put(key, factory);
        sharedInstances.remove(key);
    }

    /**
     * 根据ID获取共享效果实例
     *
     * @param id 效果ID
     * @return 效果实例，若未注册返回 null
     */
    public Effect get(String id) {
        if (id == null) {
            return null;
        }
        String key = id.toUpperCase();
        Effect cached = sharedInstances.get(key);
        if (cached != null) {
            return cached;
        }
        Supplier<Effect> factory = effectFactories.get(key);
        if (factory == null) {
            return null;
        }
        Effect instance = factory.get();
        if (instance != null) {
            sharedInstances.put(key, instance);
        }
        return instance;
    }

    /**
     * 获取所有已注册的效果ID
     */
    public Set<String> getRegisteredIds() {
        return Collections.unmodifiableSet(effectFactories.keySet());
    }
}
