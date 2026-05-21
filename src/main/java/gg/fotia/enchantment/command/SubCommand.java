package gg.fotia.enchantment.command;

import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * 子命令接口
 * 所有 /fe 子命令必须实现此接口
 */
public interface SubCommand {

    /**
     * 获取子命令名称
     */
    String getName();

    /**
     * 获取所需权限
     */
    String getPermission();

    /**
     * 获取用法提示
     */
    String getUsage();

    /**
     * 执行命令
     *
     * @param sender 命令发送者
     * @param args   参数（不含子命令名本身）
     */
    void execute(CommandSender sender, String[] args);

    /**
     * Tab补全
     *
     * @param sender 命令发送者
     * @param args   当前参数（不含子命令名本身）
     * @return 补全列表
     */
    List<String> tabComplete(CommandSender sender, String[] args);
}
