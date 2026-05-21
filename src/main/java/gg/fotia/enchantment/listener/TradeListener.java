package gg.fotia.enchantment.listener;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.core.EnchantmentData;
import gg.fotia.enchantment.core.EnchantmentManager;
import gg.fotia.enchantment.item.StellarisCodex;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.VillagerAcquireTradeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 村民交易监听器
 * <p>
 * 监听 {@link VillagerAcquireTradeEvent}, 按概率为图书管理员添加自定义附魔书交易。
 * 价格根据稀有度的 trade-price-multiplier 计算。
 */
public class TradeListener implements Listener {

    /** 加入自定义交易的总概率 */
    private static final double TRADE_INJECT_CHANCE = 0.25;

    private final FotiaEnchantment plugin;

    public TradeListener(FotiaEnchantment plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onVillagerAcquireTrade(VillagerAcquireTradeEvent event) {
        if (!plugin.getConfigManager().isVillagerTradeEnabled()) {
            return;
        }
        if (!(event.getEntity() instanceof Villager villager)) {
            return;
        }
        // 仅图书管理员
        if (villager.getProfession() != Villager.Profession.LIBRARIAN) {
            return;
        }

        if (ThreadLocalRandom.current().nextDouble() >= TRADE_INJECT_CHANCE) {
            return;
        }

        EnchantmentManager enchantManager = plugin.getEnchantmentManager();
        List<EnchantmentData> pool = new ArrayList<>();
        for (EnchantmentData data : enchantManager.getEnabled()) {
            if (data.getObtain().isVillagerTrade()) {
                pool.add(data);
            }
        }
        if (pool.isEmpty()) {
            return;
        }

        EnchantmentData picked = pool.get(ThreadLocalRandom.current().nextInt(pool.size()));

        // 等级随机 (1 ~ maxLevel)
        int maxLevel = picked.getMaxLevel();
        int level = maxLevel > 1 ? ThreadLocalRandom.current().nextInt(1, maxLevel + 1) : 1;

        // 创建附魔书
        StellarisCodex codex = plugin.getCustomItemManager().getStellarisCodex();
        ItemStack book = codex.createEnchantedBook(null, picked, level);
        if (book == null) {
            return;
        }

        // 计算价格
        int price = calcPrice(picked, level);
        ItemStack emerald = new ItemStack(Material.EMERALD, Math.max(1, Math.min(64, price)));
        ItemStack ingredient2 = new ItemStack(Material.BOOK);

        MerchantRecipe recipe = new MerchantRecipe(book, 0, 5, true, 5, 0.05f);
        recipe.addIngredient(emerald);
        recipe.addIngredient(ingredient2);
        event.setRecipe(recipe);
    }

    /**
     * 根据稀有度倍率与等级计算交易价格 (绿宝石数量)
     */
    private int calcPrice(EnchantmentData data, int level) {
        int[] range = data.getObtain().getVillagerTradePriceRange();
        int low = range != null && range.length > 0 ? range[0] : 10;
        int high = range != null && range.length > 1 ? range[1] : 30;
        if (high < low) high = low;

        int base = low + ThreadLocalRandom.current().nextInt(high - low + 1);

        // 应用稀有度倍率
        double multiplier = 1.0;
        if (data.getRarity() != null) {
            YamlConfiguration rarityConfig = plugin.getConfigManager().getRarityConfig();
            multiplier = rarityConfig.getDouble(data.getRarity() + ".trade-price-multiplier", 1.0);
        }
        int price = (int) Math.round(base * multiplier);

        // 等级缩放
        if (level > 1) {
            price = (int) Math.round(price * (1.0 + 0.5 * (level - 1)));
        }
        return price;
    }
}
