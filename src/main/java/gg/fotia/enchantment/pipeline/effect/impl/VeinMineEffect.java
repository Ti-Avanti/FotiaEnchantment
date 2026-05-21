package gg.fotia.enchantment.pipeline.effect.impl;

import gg.fotia.enchantment.pipeline.effect.Effect;
import gg.fotia.enchantment.pipeline.effect.EffectContext;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

/**
 * 连锁挖矿效果 - 破坏周围同类方块
 *
 * <p>参数：max-blocks - 最大连锁数量（默认 16）
 */
public class VeinMineEffect implements Effect {

    @Override
    public String getId() {
        return "VEIN_MINE";
    }

    @Override
    public void execute(EffectContext context) {
        Event event = context.getTriggerContext().getEvent();
        if (!(event instanceof BlockBreakEvent breakEvent)) return;

        Player player = context.getTriggerContext().getPlayer();
        if (player == null) return;

        Block origin = breakEvent.getBlock();
        Material targetType = origin.getType();
        if (targetType.isAir()) return;

        int max = Math.max(1, context.getIntParam("max-blocks", 16));
        ItemStack tool = player.getInventory().getItemInMainHand();

        // BFS 搜索相邻同类方块（不包含起点，因为起点本身已被破坏）
        Set<Block> visited = new HashSet<>();
        Deque<Block> queue = new ArrayDeque<>();
        visited.add(origin);
        queue.add(origin);

        int broken = 0;
        while (!queue.isEmpty() && broken < max) {
            Block current = queue.poll();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        Block neighbor = current.getRelative(dx, dy, dz);
                        if (!visited.add(neighbor)) continue;
                        if (neighbor.getType() != targetType) continue;
                        if (broken >= max) break;

                        // 调用 breakNaturally 以正确触发掉落与耐久消耗
                        if (neighbor.breakNaturally(tool)) {
                            broken++;
                            queue.add(neighbor);
                        }
                    }
                }
            }
        }
    }
}
