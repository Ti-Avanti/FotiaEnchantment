package gg.fotia.enchantment.config;

import gg.fotia.enchantment.core.EnchantmentManager;
import gg.fotia.enchantment.lang.LanguageManager;
import gg.fotia.enchantment.lang.MessageHelper;

/** 后台准备完毕后一次发布的配置集合。 */
public record RuntimeConfiguration(ConfigManager config, LanguageManager language,
                                   EnchantmentManager enchantments, VanillaConfig vanilla,
                                   MessageHelper messages) {
}
