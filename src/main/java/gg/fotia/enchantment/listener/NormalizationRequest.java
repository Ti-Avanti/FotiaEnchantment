package gg.fotia.enchantment.listener;

import java.util.BitSet;

/** 合并下一次玩家任务要检查的背包槽位，完整巡检覆盖所有局部请求。 */
final class NormalizationRequest {
    private boolean full;
    private final BitSet slots = new BitSet();

    synchronized void merge(int[] changedSlots) {
        if (changedSlots == null) {
            full = true;
        } else if (!full) {
            for (int slot : changedSlots) {
                if (slot >= 0) {
                    slots.set(slot);
                }
            }
        }
    }

    synchronized int[] slots() {
        return full ? null : slots.stream().toArray();
    }
}
