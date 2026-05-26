package gg.fotia.enchantment.bootstrap.paper.v1_21_R1;

import gg.fotia.enchantment.bootstrap.api.FotiaBootstrapImplementation;
import gg.fotia.enchantment.core.EnchantmentRegistry;
import gg.fotia.enchantment.util.LegacyColorConverter;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.data.EnchantmentRegistryEntry;
import io.papermc.paper.registry.event.RegistryEvents;
import io.papermc.paper.registry.keys.tags.EnchantmentTagKeys;
import io.papermc.paper.registry.keys.tags.ItemTypeTagKeys;
import io.papermc.paper.registry.set.RegistryKeySet;
import io.papermc.paper.registry.set.RegistrySet;
import io.papermc.paper.tag.PostFlattenTagRegistrar;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemType;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

@SuppressWarnings("UnstableApiUsage")
public final class PaperV1_21_R1Bootstrap implements FotiaBootstrapImplementation {

    private static final String[] DEFAULT_ENCHANTMENT_RESOURCES = {
            "enchantments/melee/blazing_blade.yml",
            "enchantments/melee/thunder_smash.yml",
            "enchantments/melee/vampiric_strike.yml",
            "enchantments/ranged/explosive_arrow.yml",
            "enchantments/ranged/homing_arrow.yml",
            "enchantments/armor/frost_shield.yml",
            "enchantments/armor/thorn_nova.yml",
            "enchantments/armor/vitality_spring.yml",
            "enchantments/tools/auto_smelt.yml",
            "enchantments/tools/bountiful.yml",
            "enchantments/tools/vein_miner.yml",
            "enchantments/universal/windwalker.yml",
            "enchantments/universal/xp_boost.yml"
    };

    private static final Map<String, List<Material>> CATEGORY_MATERIALS = buildCategoryMaterials();
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    @Override
    public void bootstrap(BootstrapContext context) {
        BootstrapData bootstrapData = loadBootstrapData(context.getDataDirectory());
        List<BootstrapEnchantment> enchantments = bootstrapData.enchantments().stream()
                .filter(BootstrapEnchantment::enabled)
                .toList();
        Set<String> enabledIds = new LinkedHashSet<>(enchantments.stream()
                .map(BootstrapEnchantment::id)
                .toList());

        context.getLifecycleManager().registerEventHandler(RegistryEvents.ENCHANTMENT.freeze(), event -> {
            for (BootstrapEnchantment enchantment : enchantments) {
                RegistryKeySet<ItemType> supportedItems = enchantment.applicableItems().isEmpty()
                        ? event.getOrCreateTag(ItemTypeTagKeys.ENCHANTABLE_DURABILITY)
                        : itemKeySet(enchantment.applicableItems());
                RegistryKeySet<Enchantment> exclusiveWith = enchantment.conflicts().isEmpty()
                        ? RegistrySet.keySet(RegistryKey.ENCHANTMENT, Collections.emptyList())
                        : RegistrySet.keySet(RegistryKey.ENCHANTMENT, enchantment.conflictKeys(enabledIds));

                event.registry().register(enchantment.typedKey(), builder -> builder
                        .description(enchantment.displayName())
                        .supportedItems(supportedItems)
                        .primaryItems(supportedItems)
                        .weight(enchantment.weight())
                        .maxLevel(enchantment.maxLevel())
                        .minimumCost(EnchantmentRegistryEntry.EnchantmentCost.of(
                                enchantment.minCostBase(), enchantment.minCostPerLevel()))
                        .maximumCost(EnchantmentRegistryEntry.EnchantmentCost.of(
                                enchantment.maxCostBase(), enchantment.maxCostPerLevel()))
                        .anvilCost(enchantment.anvilCost())
                        .activeSlots(enchantment.slots())
                        .exclusiveWith(exclusiveWith));
            }
        });

        context.getLifecycleManager().registerEventHandler(
                LifecycleEvents.TAGS.postFlatten(RegistryKey.ENCHANTMENT),
                event -> registerTags(event.registrar(), bootstrapData.globalEnchantingTableEnabled(), enchantments));
    }

    private void registerTags(PostFlattenTagRegistrar<Enchantment> registrar,
                              boolean globalEnchantingTableEnabled,
                              List<BootstrapEnchantment> enchantments) {
        List<TypedKey<Enchantment>> all = enchantments.stream()
                .map(BootstrapEnchantment::typedKey)
                .toList();
        if (!all.isEmpty()) {
            registrar.addToTag(EnchantmentTagKeys.TOOLTIP_ORDER, all);
        }

        List<TypedKey<Enchantment>> enchantingTable = enchantments.stream()
                .filter(enchantment -> globalEnchantingTableEnabled
                        && enchantment.enchantingTable()
                        && enchantment.weight() > 0)
                .map(BootstrapEnchantment::typedKey)
                .toList();
        if (!enchantingTable.isEmpty()) {
            registrar.addToTag(EnchantmentTagKeys.IN_ENCHANTING_TABLE, enchantingTable);
            registrar.addToTag(EnchantmentTagKeys.NON_TREASURE, enchantingTable);
        }

        List<TypedKey<Enchantment>> tradeable = enchantments.stream()
                .filter(BootstrapEnchantment::villagerTrade)
                .map(BootstrapEnchantment::typedKey)
                .toList();
        if (!tradeable.isEmpty()) {
            registrar.addToTag(EnchantmentTagKeys.TRADEABLE, tradeable);
            registrar.addToTag(EnchantmentTagKeys.ON_TRADED_EQUIPMENT, tradeable);
        }

        List<TypedKey<Enchantment>> curses = enchantments.stream()
                .filter(BootstrapEnchantment::curse)
                .map(BootstrapEnchantment::typedKey)
                .toList();
        if (!curses.isEmpty()) {
            registrar.addToTag(EnchantmentTagKeys.CURSE, curses);
            registrar.addToTag(EnchantmentTagKeys.DOUBLE_TRADE_PRICE, curses);
        }
    }

    private static RegistryKeySet<ItemType> itemKeySet(Collection<Material> materials) {
        List<ItemType> itemTypes = materials.stream()
                .filter(material -> !material.isAir())
                .map(Material::asItemType)
                .toList();
        return RegistrySet.keySetFromValues(RegistryKey.ITEM, itemTypes);
    }

    private static BootstrapData loadBootstrapData(Path dataDirectory) {
        ClassLoader classLoader = PaperV1_21_R1Bootstrap.class.getClassLoader();
        Map<String, String> names = loadNames(dataDirectory, classLoader);
        Map<String, BootstrapEnchantment> enchantments = new LinkedHashMap<>();
        Path externalEnchantments = dataDirectory.resolve("enchantments");

        if (Files.isDirectory(externalEnchantments)) {
            loadExternalEnchantments(externalEnchantments, names, enchantments);
        } else {
            loadBundledDefaultEnchantments(classLoader, names, enchantments);
        }

        boolean globalEnchantingTable = loadGlobalEnchantingTableFlag(dataDirectory, classLoader);
        return new BootstrapData(globalEnchantingTable, new ArrayList<>(enchantments.values()));
    }

    private static void loadBundledDefaultEnchantments(ClassLoader classLoader,
                                                       Map<String, String> names,
                                                       Map<String, BootstrapEnchantment> enchantments) {
        for (String resource : DEFAULT_ENCHANTMENT_RESOURCES) {
            try (InputStream input = classLoader.getResourceAsStream(resource)) {
                if (input == null) {
                    continue;
                }
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(input, StandardCharsets.UTF_8));
                BootstrapEnchantment enchantment = parseEnchantment(yaml, resource, names);
                if (enchantment != null) {
                    enchantments.put(enchantment.id(), enchantment);
                }
            } catch (Exception ignored) {
                // Invalid default resources are ignored here; the normal plugin loader will log detailed errors.
            }
        }
    }

    private static void loadExternalEnchantments(Path externalEnchantments,
                                                 Map<String, String> names,
                                                 Map<String, BootstrapEnchantment> enchantments) {
        try (Stream<Path> stream = Files.walk(externalEnchantments)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".yml"))
                    .forEach(path -> {
                        BootstrapEnchantment enchantment = parseEnchantment(
                                YamlConfiguration.loadConfiguration(path.toFile()),
                                path.getFileName().toString(), names);
                        if (enchantment != null) {
                            enchantments.put(enchantment.id(), enchantment);
                        }
                    });
        } catch (Exception ignored) {
            // Runtime config loading still reports detailed file errors after the plugin is enabled.
        }
    }

    private static boolean loadGlobalEnchantingTableFlag(Path dataDirectory, ClassLoader classLoader) {
        Path external = dataDirectory.resolve("config.yml");
        if (Files.isRegularFile(external)) {
            return YamlConfiguration.loadConfiguration(external.toFile())
                    .getBoolean("obtain.enchanting-table", true);
        }

        try (InputStream input = classLoader.getResourceAsStream("config.yml")) {
            if (input == null) {
                return true;
            }
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(input, StandardCharsets.UTF_8));
            return yaml.getBoolean("obtain.enchanting-table", true);
        } catch (Exception ignored) {
            return true;
        }
    }

    private static Map<String, String> loadNames(Path dataDirectory, ClassLoader classLoader) {
        Map<String, String> names = new HashMap<>();
        loadNamesFromResource(classLoader, names);

        Path external = dataDirectory.resolve("lang").resolve("zh_cn").resolve("enchantments.yml");
        if (Files.isRegularFile(external)) {
            loadNamesFromYaml(YamlConfiguration.loadConfiguration(external.toFile()), names);
        }
        return names;
    }

    private static void loadNamesFromResource(ClassLoader classLoader, Map<String, String> names) {
        try (InputStream input = classLoader.getResourceAsStream("lang/zh_cn/enchantments.yml")) {
            if (input == null) {
                return;
            }
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(input, StandardCharsets.UTF_8));
            loadNamesFromYaml(yaml, names);
        } catch (Exception ignored) {
            // Name fallback is the enchantment id.
        }
    }

    private static void loadNamesFromYaml(YamlConfiguration yaml, Map<String, String> names) {
        for (String key : yaml.getKeys(false)) {
            String name = yaml.getString(key + ".name");
            if (name != null && !name.isBlank()) {
                names.put(key.toLowerCase(Locale.ROOT), name);
            }
        }
    }

    private static BootstrapEnchantment parseEnchantment(YamlConfiguration yaml,
                                                         String sourceName,
                                                         Map<String, String> names) {
        String id = yaml.getString("id");
        if (id == null || id.isBlank()) {
            String fileName = new File(sourceName).getName();
            int dot = fileName.lastIndexOf('.');
            id = dot > 0 ? fileName.substring(0, dot) : fileName;
        }
        id = id.toLowerCase(Locale.ROOT);

        int maxLevel = Math.max(1, yaml.getInt("max-level", 1));
        int weight = Math.max(0, yaml.getInt("enchanting-table-weight",
                yaml.getInt("obtain.enchanting-table-weight", 10)));
        int minBase = Math.max(1, yaml.getInt("enchanting-table-min-cost.base", 1));
        int minPerLevel = Math.max(0, yaml.getInt("enchanting-table-min-cost.per-level", 10));
        int maxBase = Math.max(minBase, yaml.getInt("enchanting-table-max-cost.base", minBase + 20));
        int maxPerLevel = Math.max(minPerLevel, yaml.getInt("enchanting-table-max-cost.per-level", 10));

        List<Material> materials = parseMaterials(yaml.getStringList("applicable-items"));
        List<String> conflicts = yaml.getStringList("conflicts").stream()
                .filter(conflict -> conflict != null && !conflict.isBlank())
                .map(conflict -> conflict.toLowerCase(Locale.ROOT))
                .toList();

        Component displayName = displayName(names.getOrDefault(id, humanize(id)));
        return new BootstrapEnchantment(
                id,
                yaml.getBoolean("enabled", true),
                yaml.getBoolean("curse", false),
                maxLevel,
                weight,
                minBase,
                minPerLevel,
                maxBase,
                maxPerLevel,
                Math.max(1, yaml.getInt("anvil-cost", Math.min(8, maxLevel * 2))),
                yaml.getBoolean("obtain.enchanting-table", true),
                yaml.getBoolean("obtain.villager-trade", true),
                materials,
                conflicts,
                displayName,
                activeSlots(materials)
        );
    }

    private static Component displayName(String rawName) {
        String text = LegacyColorConverter.convert("<!i>" + rawName);
        return MINI_MESSAGE.deserialize(text);
    }

    private static String humanize(String id) {
        String[] parts = id.split("_");
        List<String> words = new ArrayList<>(parts.length);
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            words.add(part.substring(0, 1).toUpperCase(Locale.ROOT) + part.substring(1));
        }
        return words.isEmpty() ? id : String.join(" ", words);
    }

    private static List<Material> parseMaterials(List<String> tokens) {
        Set<Material> result = new LinkedHashSet<>();
        for (String token : tokens) {
            if (token == null || token.isBlank()) {
                continue;
            }
            String upper = token.trim().toUpperCase(Locale.ROOT);
            List<Material> category = CATEGORY_MATERIALS.get(upper);
            if (category != null) {
                result.addAll(category);
                continue;
            }
            try {
                result.add(Material.valueOf(upper));
            } catch (IllegalArgumentException ignored) {
                // Runtime config loading reports unknown material names.
            }
        }
        return new ArrayList<>(result);
    }

    private static List<EquipmentSlotGroup> activeSlots(List<Material> materials) {
        boolean head = false;
        boolean chest = false;
        boolean legs = false;
        boolean feet = false;
        boolean hand = false;

        for (Material material : materials) {
            String name = material.name();
            if (name.endsWith("_HELMET") || material == Material.TURTLE_HELMET) {
                head = true;
            } else if (name.endsWith("_CHESTPLATE")) {
                chest = true;
            } else if (name.endsWith("_LEGGINGS")) {
                legs = true;
            } else if (name.endsWith("_BOOTS")) {
                feet = true;
            } else {
                hand = true;
            }
        }

        List<EquipmentSlotGroup> slots = new ArrayList<>();
        if (hand || materials.isEmpty()) {
            slots.add(EquipmentSlotGroup.MAINHAND);
        }
        if (head) {
            slots.add(EquipmentSlotGroup.HEAD);
        }
        if (chest) {
            slots.add(EquipmentSlotGroup.CHEST);
        }
        if (legs) {
            slots.add(EquipmentSlotGroup.LEGS);
        }
        if (feet) {
            slots.add(EquipmentSlotGroup.FEET);
        }
        return slots;
    }

    private static Map<String, List<Material>> buildCategoryMaterials() {
        Map<String, List<Material>> map = new HashMap<>();
        map.put("SWORD", Arrays.asList(
                Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD,
                Material.GOLDEN_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD));
        map.put("AXE", Arrays.asList(
                Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE,
                Material.GOLDEN_AXE, Material.DIAMOND_AXE, Material.NETHERITE_AXE));
        map.put("PICKAXE", Arrays.asList(
                Material.WOODEN_PICKAXE, Material.STONE_PICKAXE, Material.IRON_PICKAXE,
                Material.GOLDEN_PICKAXE, Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE));
        map.put("SHOVEL", Arrays.asList(
                Material.WOODEN_SHOVEL, Material.STONE_SHOVEL, Material.IRON_SHOVEL,
                Material.GOLDEN_SHOVEL, Material.DIAMOND_SHOVEL, Material.NETHERITE_SHOVEL));
        map.put("HOE", Arrays.asList(
                Material.WOODEN_HOE, Material.STONE_HOE, Material.IRON_HOE,
                Material.GOLDEN_HOE, Material.DIAMOND_HOE, Material.NETHERITE_HOE));
        map.put("BOW", Collections.singletonList(Material.BOW));
        map.put("CROSSBOW", Collections.singletonList(Material.CROSSBOW));
        map.put("TRIDENT", Collections.singletonList(Material.TRIDENT));
        map.put("FISHING_ROD", Collections.singletonList(Material.FISHING_ROD));
        map.put("SHIELD", Collections.singletonList(Material.SHIELD));
        map.put("ELYTRA", Collections.singletonList(Material.ELYTRA));
        map.put("HELMET", Arrays.asList(
                Material.LEATHER_HELMET, Material.CHAINMAIL_HELMET, Material.IRON_HELMET,
                Material.GOLDEN_HELMET, Material.DIAMOND_HELMET, Material.NETHERITE_HELMET,
                Material.TURTLE_HELMET));
        map.put("CHESTPLATE", Arrays.asList(
                Material.LEATHER_CHESTPLATE, Material.CHAINMAIL_CHESTPLATE,
                Material.IRON_CHESTPLATE, Material.GOLDEN_CHESTPLATE,
                Material.DIAMOND_CHESTPLATE, Material.NETHERITE_CHESTPLATE));
        map.put("LEGGINGS", Arrays.asList(
                Material.LEATHER_LEGGINGS, Material.CHAINMAIL_LEGGINGS,
                Material.IRON_LEGGINGS, Material.GOLDEN_LEGGINGS,
                Material.DIAMOND_LEGGINGS, Material.NETHERITE_LEGGINGS));
        map.put("BOOTS", Arrays.asList(
                Material.LEATHER_BOOTS, Material.CHAINMAIL_BOOTS, Material.IRON_BOOTS,
                Material.GOLDEN_BOOTS, Material.DIAMOND_BOOTS, Material.NETHERITE_BOOTS));
        return map;
    }

    private record BootstrapData(boolean globalEnchantingTableEnabled,
                                 List<BootstrapEnchantment> enchantments) {
    }

    private record BootstrapEnchantment(String id,
                                        boolean enabled,
                                        boolean curse,
                                        int maxLevel,
                                        int weight,
                                        int minCostBase,
                                        int minCostPerLevel,
                                        int maxCostBase,
                                        int maxCostPerLevel,
                                        int anvilCost,
                                        boolean enchantingTable,
                                        boolean villagerTrade,
                                        List<Material> applicableItems,
                                        List<String> conflicts,
                                        Component displayName,
                                        List<EquipmentSlotGroup> slots) {

        private TypedKey<Enchantment> typedKey() {
            return TypedKey.create(RegistryKey.ENCHANTMENT, Key.key(EnchantmentRegistry.getNamespace(), id));
        }

        private List<TypedKey<Enchantment>> conflictKeys(Set<String> enabledIds) {
            return conflicts.stream()
                    .filter(conflict -> shouldRegisterConflict(conflict, enabledIds))
                    .map(BootstrapEnchantment::toEnchantmentKey)
                    .toList();
        }

        private static boolean shouldRegisterConflict(String id, Set<String> enabledIds) {
            int separator = id.indexOf(':');
            if (separator < 0) {
                return enabledIds.contains(id);
            }

            String namespace = id.substring(0, separator);
            if (EnchantmentRegistry.getNamespace().equals(namespace)) {
                return enabledIds.contains(id.substring(separator + 1));
            }
            return true;
        }

        private static TypedKey<Enchantment> toEnchantmentKey(String id) {
            Key key = id.contains(":")
                    ? Key.key(id)
                    : Key.key(EnchantmentRegistry.getNamespace(), id);
            return TypedKey.create(RegistryKey.ENCHANTMENT, key);
        }
    }
}
