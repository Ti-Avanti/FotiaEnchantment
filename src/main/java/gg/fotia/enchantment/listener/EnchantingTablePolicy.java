package gg.fotia.enchantment.listener;

public final class EnchantingTablePolicy {

    private EnchantingTablePolicy() {
    }

    public static int customRollAttempts(int expLevelCost, int configuredRolls) {
        if (configuredRolls <= 0 || expLevelCost <= 0) {
            return 0;
        }
        int costScaledAttempts = 1 + Math.max(0, expLevelCost - 1) / 12;
        return Math.max(1, Math.min(configuredRolls, costScaledAttempts));
    }

    public static double customRollChance(int expLevelCost,
                                          double baseChance,
                                          double chancePerLevel,
                                          double maxChance) {
        double upper = clamp(maxChance, 0.0, 1.0);
        double chance = baseChance + Math.max(0, expLevelCost) * chancePerLevel;
        return clamp(chance, 0.0, upper);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
