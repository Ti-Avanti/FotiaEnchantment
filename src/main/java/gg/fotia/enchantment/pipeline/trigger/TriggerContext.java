package gg.fotia.enchantment.pipeline.trigger;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;

/**
 * 触发器上下文 - 携带触发事件的所有信息
 *
 * <p>由触发器在事件发生时构建，并传递给 {@link gg.fotia.enchantment.pipeline.EffectPipeline}
 * 用于驱动条件判断与效果执行。
 */
public class TriggerContext {

    private final Player player;
    private final LivingEntity target;
    private final Event event;
    private final ItemStack item;
    private final double value;
    private final double altValue;
    private final String triggerId;

    private TriggerContext(Builder builder) {
        this.player = builder.player;
        this.target = builder.target;
        this.event = builder.event;
        this.item = builder.item;
        this.value = builder.value;
        this.altValue = builder.altValue;
        this.triggerId = builder.triggerId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Player getPlayer() {
        return player;
    }

    public LivingEntity getTarget() {
        return target;
    }

    public Event getEvent() {
        return event;
    }

    public ItemStack getItem() {
        return item;
    }

    public double getValue() {
        return value;
    }

    public double getAltValue() {
        return altValue;
    }

    public String getTriggerId() {
        return triggerId;
    }

    /**
     * Builder 构建器
     */
    public static class Builder {
        private Player player;
        private LivingEntity target;
        private Event event;
        private ItemStack item;
        private double value;
        private double altValue;
        private String triggerId;

        public Builder player(Player player) {
            this.player = player;
            return this;
        }

        public Builder target(LivingEntity target) {
            this.target = target;
            return this;
        }

        public Builder event(Event event) {
            this.event = event;
            return this;
        }

        public Builder item(ItemStack item) {
            this.item = item;
            return this;
        }

        public Builder value(double value) {
            this.value = value;
            return this;
        }

        public Builder altValue(double altValue) {
            this.altValue = altValue;
            return this;
        }

        public Builder triggerId(String triggerId) {
            this.triggerId = triggerId;
            return this;
        }

        public TriggerContext build() {
            return new TriggerContext(this);
        }
    }
}
