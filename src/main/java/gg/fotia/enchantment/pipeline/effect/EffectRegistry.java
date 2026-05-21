package gg.fotia.enchantment.pipeline.effect;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 效果注册表 - 管理所有效果动作实现的注册与实例化
 */
public class EffectRegistry {

    private final Map<String, Supplier<Effect>> effectFactories = new HashMap<>();

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
        effectFactories.put(id.toUpperCase(), factory);
    }

    /**
     * 根据ID获取一个新的效果实例
     *
     * @param id 效果ID
     * @return 效果实例，若未注册返回 null
     */
    public Effect get(String id) {
        if (id == null) {
            return null;
        }
        Supplier<Effect> factory = effectFactories.get(id.toUpperCase());
        return factory == null ? null : factory.get();
    }

    /**
     * 获取所有已注册的效果ID
     */
    public Set<String> getRegisteredIds() {
        return Collections.unmodifiableSet(effectFactories.keySet());
    }
}
