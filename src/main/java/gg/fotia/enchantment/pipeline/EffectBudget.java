package gg.fotia.enchantment.pipeline;

/** 按 tick 为整组动作预留额度，避免限流截断动作链。 */
final class EffectBudget {
    private long tick;
    private int used;

    synchronized int used() {
        return used;
    }

    synchronized long reserve(int actions, int limit) {
        if (actions <= 0 || actions > limit - used) {
            return -1L;
        }
        used += actions;
        return tick;
    }

    synchronized void release(long reservationTick, int actions) {
        if (tick == reservationTick && actions > 0) {
            used = Math.max(0, used - actions);
        }
    }

    synchronized void reset() {
        tick++;
        used = 0;
    }
}
