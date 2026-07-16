package gg.fotia.enchantment.pipeline.trigger;

import gg.fotia.enchantment.pipeline.EffectPipeline;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 触发器注册表 - 管理所有触发器的注册和实例化
 *
 * <p>插件启动时通过 {@link #register(String, Supplier)} 注册触发器工厂，
 * 然后按当前附魔配置实例化并激活实际使用的触发器。
 */
public class TriggerRegistry {

    private final Map<String, Supplier<Trigger>> triggerFactories = new HashMap<>();
    private final Map<String, Trigger> activeTriggers = new HashMap<>();

    /**
     * 注册一个触发器工厂
     *
     * @param id      触发器ID（不区分大小写，统一存为大写）
     * @param factory 触发器实例工厂
     */
    public void register(String id, Supplier<Trigger> factory) {
        if (id == null || factory == null) {
            return;
        }
        triggerFactories.put(id.toUpperCase(), factory);
    }

    /**
     * 获取已激活的触发器实例
     *
     * @param id 触发器ID
     * @return 触发器实例，若未激活返回 null
     */
    public Trigger get(String id) {
        if (id == null) {
            return null;
        }
        return activeTriggers.get(id.toUpperCase());
    }

    /**
     * 激活所有已注册的触发器
     */
    public void activateAll(EffectPipeline pipeline) {
        activateOnly(pipeline, triggerFactories.keySet());
    }

    /**
     * 仅激活指定触发器，并注销配置中已不再使用的触发器。
     */
    public void activateOnly(EffectPipeline pipeline, Collection<String> ids) {
        Set<String> requestedIds = new HashSet<>();
        if (ids != null) {
            for (String id : ids) {
                if (id != null) {
                    requestedIds.add(id.toUpperCase());
                }
            }
        }

        for (String activeId : Set.copyOf(activeTriggers.keySet())) {
            if (requestedIds.contains(activeId)) {
                continue;
            }
            Trigger trigger = activeTriggers.remove(activeId);
            if (trigger != null) {
                try {
                    trigger.unregister();
                } catch (Exception ignored) {
                    // 忽略单个触发器注销异常
                }
            }
        }

        for (String id : requestedIds) {
            if (activeTriggers.containsKey(id)) {
                continue;
            }
            Supplier<Trigger> factory = triggerFactories.get(id);
            if (factory == null) {
                continue;
            }
            Trigger trigger = factory.get();
            if (trigger == null) {
                continue;
            }
            trigger.register(pipeline);
            activeTriggers.put(id, trigger);
        }
    }

    /**
     * 停止所有已激活的触发器
     */
    public void deactivateAll() {
        for (Trigger trigger : activeTriggers.values()) {
            try {
                trigger.unregister();
            } catch (Exception ignored) {
                // 忽略单个触发器注销异常
            }
        }
        activeTriggers.clear();
    }

    /**
     * 获取所有已注册的触发器ID
     */
    public Set<String> getRegisteredIds() {
        return Collections.unmodifiableSet(triggerFactories.keySet());
    }

    /**
     * 获取所有已激活的触发器ID
     */
    public Set<String> getActiveIds() {
        return Collections.unmodifiableSet(activeTriggers.keySet());
    }
}
