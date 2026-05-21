package gg.fotia.enchantment.pipeline.effect.impl;

import gg.fotia.enchantment.pipeline.effect.Effect;
import gg.fotia.enchantment.pipeline.effect.EffectContext;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Locale;

/**
 * 音效效果 - 在目标或玩家位置播放音效
 *
 * <p>参数：
 * <ul>
 *     <li>sound - 音效名称（如 ENTITY_BLAZE_SHOOT 或 entity.blaze.shoot）</li>
 *     <li>volume - 音量，默认 1.0</li>
 *     <li>pitch - 音高，默认 1.0</li>
 *     <li>at - 播放位置：SELF / TARGET，默认 TARGET</li>
 * </ul>
 */
public class SoundEffect implements Effect {

    @Override
    public String getId() {
        return "SOUND";
    }

    @Override
    public void execute(EffectContext context) {
        String soundName = context.getExtraParam("sound");
        if (soundName == null || soundName.isEmpty()) return;

        Sound sound = resolveSound(soundName);
        if (sound == null) return;

        float volume = (float) context.getDoubleParam("volume", 1.0);
        float pitch = (float) context.getDoubleParam("pitch", 1.0);

        String at = context.getExtraParam("at", "TARGET").toUpperCase(Locale.ROOT);
        Location location = resolveLocation(at, context);
        if (location == null) return;

        World world = location.getWorld();
        if (world == null) return;

        world.playSound(location, sound, SoundCategory.PLAYERS, volume, pitch);
    }

    private Sound resolveSound(String name) {
        String normalized = name.trim();
        String lower = normalized.toLowerCase(Locale.ROOT).replace('_', '.');
        NamespacedKey key = lower.contains(":") ? NamespacedKey.fromString(lower) : NamespacedKey.minecraft(lower);
        if (key == null) return null;
        return Registry.SOUNDS.get(key);
    }

    private Location resolveLocation(String at, EffectContext context) {
        Player player = context.getTriggerContext().getPlayer();
        LivingEntity target = context.getTriggerContext().getTarget();
        if ("SELF".equals(at)) {
            return player != null ? player.getLocation() : null;
        }
        if (target != null) return target.getLocation();
        return player != null ? player.getLocation() : null;
    }
}
