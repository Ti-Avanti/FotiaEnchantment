package gg.fotia.enchantment.command;

import gg.fotia.enchantment.FotiaEnchantment;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;

import java.util.List;
import java.util.function.Consumer;

final class PaperCommandRegistrar {

    private PaperCommandRegistrar() {
    }

    static void register(FotiaEnchantment plugin,
                         CommandManager commandManager,
                         Consumer<Throwable> fallback) {
        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            try {
                event.registrar().register(
                        plugin.getPluginMeta(),
                        "fe",
                        "FotiaEnchantment command",
                        List.of("fotia", "fotiaenchant"),
                        new FePaperCommand(commandManager, new FeBukkitCommand(commandManager)));
            } catch (RuntimeException | LinkageError ex) {
                fallback.accept(ex);
            }
        });
    }
}
