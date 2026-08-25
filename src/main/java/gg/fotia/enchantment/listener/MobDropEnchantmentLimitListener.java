package gg.fotia.enchantment.listener;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.core.MobDropOverflowAction;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ListIterator;

public final class MobDropEnchantmentLimitListener implements Listener {

    private final FotiaEnchantment plugin;
    private final MobDropEnchantmentLimitService service;

    public MobDropEnchantmentLimitListener(FotiaEnchantment plugin) {
        this.plugin = plugin;
        this.service = new MobDropEnchantmentLimitService(plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Player
                || !plugin.getConfigManager().isMobDropEnchantmentLimitEnabled()) {
            return;
        }

        MobDropOverflowAction action = plugin.getConfigManager().getMobDropOverflowAction();
        ListIterator<ItemStack> drops = event.getDrops().listIterator();
        while (drops.hasNext()) {
            ItemStack drop = drops.next();
            int max = plugin.getConfigManager().getMaxEnchantmentsForMaterial(drop.getType());
            MobDropEnchantmentLimitService.Outcome outcome = service.enforce(drop, max, action);
            if (outcome == MobDropEnchantmentLimitService.Outcome.REMOVE_DROP) {
                drops.remove();
            }
        }
    }
}
