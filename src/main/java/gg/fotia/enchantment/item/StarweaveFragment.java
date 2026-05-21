package gg.fotia.enchantment.item;

import gg.fotia.enchantment.FotiaEnchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * 星辉残页逻辑
 * 无稀有度区分，统一物品
 * 从 config.yml 读取合成所需数量: stellaris-codex.fragment-cost
 * 从 custom-items.yml 读取物品外观配置
 */
public class StarweaveFragment {

    private final FotiaEnchantment plugin;
    private final CustomItemManager itemManager;

    public StarweaveFragment(FotiaEnchantment plugin, CustomItemManager itemManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;
    }

    /**
     * 创建星辉残页
     *
     * @param player 玩家（多语言）
     * @param amount 数量
     * @return 星辉残页物品
     */
    public ItemStack create(Player player, int amount) {
        return itemManager.createStarweaveFragment(player, amount);
    }

    /**
     * 获取合成魔典所需残页数量
     *
     * @return 所需数量
     */
    public int getFragmentCost() {
        return plugin.getConfigManager().getStellarisCodexFragmentCost();
    }

    /**
     * 检查玩家背包中星辉残页数量是否足够合成
     *
     * @param player 玩家
     * @return 玩家拥有的星辉残页数量
     */
    public int countInInventory(Player player) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null) continue;
            String type = itemManager.identifyItem(item);
            if ("starweave-fragment".equals(type)) {
                count += item.getAmount();
            }
        }
        return count;
    }

    /**
     * 从玩家背包中消耗指定数量的星辉残页
     *
     * @param player 玩家
     * @param amount 要消耗的数量
     * @return 是否成功消耗
     */
    public boolean consume(Player player, int amount) {
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack item = contents[i];
            if (item == null) continue;
            String type = itemManager.identifyItem(item);
            if ("starweave-fragment".equals(type)) {
                int stackAmount = item.getAmount();
                if (stackAmount <= remaining) {
                    remaining -= stackAmount;
                    player.getInventory().setItem(i, null);
                } else {
                    item.setAmount(stackAmount - remaining);
                    remaining = 0;
                }
            }
        }
        return remaining == 0;
    }

    /**
     * 检查物品是否为星辉残页
     *
     * @param item 物品
     * @return 是否为星辉残页
     */
    public boolean isStarweaveFragment(ItemStack item) {
        return "starweave-fragment".equals(itemManager.identifyItem(item));
    }
}
