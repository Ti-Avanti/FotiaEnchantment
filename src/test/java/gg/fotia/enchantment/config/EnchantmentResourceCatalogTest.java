package gg.fotia.enchantment.config;

import gg.fotia.enchantment.pipeline.EffectPipeline;
import gg.fotia.enchantment.pipeline.condition.ConditionRegistry;
import gg.fotia.enchantment.pipeline.effect.EffectRegistry;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnchantmentResourceCatalogTest {

    private static final Path RESOURCES = Path.of("src", "main", "resources");
    private static final Path ENCHANTMENTS = RESOURCES.resolve("enchantments");
    private static final String GENERATED_MARKER = "# === FotiaEnchantment 100 enchantment expansion ===";

    private static final Set<String> CATEGORIES = Set.of("melee", "ranged", "armor", "tools", "universal");
    private static final Set<String> RARITIES = Set.of("dustlight", "moonlit", "radiant", "aureate", "divine");
    private static final Set<String> GROUPS = Set.of("fire", "ice", "lightning", "defensive", "offensive", "utility", "movement", "mining");
    private static final Set<String> ITEM_ALIASES = Set.of(
            "SWORD", "SPEAR", "AXE", "PICKAXE", "SHOVEL", "HOE", "BOW", "CROSSBOW", "TRIDENT",
            "FISHING_ROD", "SHIELD", "ELYTRA", "HELMET", "CHESTPLATE", "LEGGINGS", "BOOTS");
    private static final Set<String> TRIGGERS = Set.of(
            "ANVIL_USE", "ARMOR_ABSORB", "ARROW_BOUNCE", "ASSIST", "BELL_RING", "BITE",
            "BLOCK_ITEM_DROP", "BONEMEAL_CROP", "BOW_ATTACK", "BOW_SHOOT", "BREAK_SPAWNER",
            "BREED_ANIMAL", "BREW", "BUY_ITEM", "CARTOGRAPHY_USE", "CAST_ROD", "CATCH_ENTITY",
            "CATCH_FIRE", "CATCH_FISH", "CATCH_JUNK", "CATCH_TREASURE", "CAULDRON_LEVEL_CHANGE",
            "CHANGE_ARMOR", "CHANGE_BIOME", "CHANGE_WORLD", "CLOSE_CONTAINER", "COLLIDE_WITH_ENTITY",
            "COMPLETE_ADVANCEMENT", "COMPOST_ITEM", "CONSUME", "CRAFT", "CROSSBOW_ATTACK",
            "CROSSBOW_SHOOT", "DAMAGE_ITEM", "DEATH", "DEPLOY_ELYTRA", "DISMOUNT_ENTITY",
            "DODGE", "DOUBLE_JUMP", "DROP_ITEM", "DROWNING_DAMAGE", "ELYTRA_BOOST", "ELYTRA_GLIDE",
            "EMPTY_BUCKET", "ENCHANT_ITEM", "ENTER_BED", "ENTER_LAVA", "ENTER_REGION", "ENTER_VEHICLE",
            "ENTER_WATER", "ENTITY_SPAWN_NEAR", "ENTITY_TARGET_ME", "EXIT_REGION", "EXIT_VEHICLE",
            "EXIT_WATER", "EXPLOSION_DAMAGE", "EXTINGUISH", "FALL_DAMAGE", "FILL_BUCKET",
            "FIRE_DAMAGE", "FIRST_BLOOD", "FREEZE", "GAIN_ABSORPTION", "GAIN_HUNGER", "GAIN_XP",
            "GRIND_ITEM", "HARVEST", "HARVEST_TREE", "HEADSHOT", "HEAL", "HOLD", "HOLD_ITEM_CHANGE",
            "HOOK_IN_GROUND", "INTERACT_BLOCK", "INTERACT_ENTITY", "ITEM_BREAK", "JOIN_SERVER",
            "JUMP", "KILL", "KILL_BOSS", "KILL_PLAYER", "KILL_STREAK", "LAND", "LEASH_ENTITY",
            "LEAVE_BED", "LEAVE_SERVER", "LEVEL_UP", "LIGHTNING_STRIKE_NEAR", "LOOM_USE",
            "LOSE_HUNGER", "LOSE_POTION_EFFECT", "MAGIC_DAMAGE", "MELEE_ATTACK",
            "MELEE_ATTACK_BEHIND", "MELEE_ATTACK_COMBO", "MELEE_ATTACK_CRITICAL",
            "MELEE_ATTACK_SWEEP", "MELEE_ATTACK_WHILE_AIRBORNE", "MILK_COW", "MINE_BLOCK",
            "MINE_BLOCK_PROGRESS", "MINE_DEEPSLATE", "MINE_ORE", "MOUNT_ENTITY", "NATURAL_REGEN",
            "NEAR_DEATH", "NOTE_BLOCK_PLAY", "OPEN_CONTAINER", "PICK_UP_ITEM", "PLACE_BLOCK",
            "PLANT_SEED", "POISON_DAMAGE", "POTION_EFFECT", "PRESSURE_PLATE", "PROJECTILE_HIT_BLOCK",
            "PROJECTILE_HIT_ENTITY", "REEL_IN", "REPAIR_ITEM", "RESPAWN", "RESURRECT", "RIPTIDE",
            "RUN_COMMAND", "SELL_ITEM", "SEND_CHAT", "SHEAR_BLOCK", "SHEAR_ENTITY", "SHIELD_BLOCK",
            "SMELT", "SMITH_ITEM", "SNEAK_START", "SNEAK_STOP", "SPRINT_START", "SPRINT_STOP",
            "STONECUTTER_USE", "SWIM", "TAKE_DAMAGE", "TAKE_ENTITY_DAMAGE", "TAKE_PLAYER_DAMAGE",
            "TAKE_PROJECTILE_DAMAGE", "TAME_ANIMAL", "TELEPORT", "THROW_EGG", "THROW_ENDER_PEARL",
            "THROW_SNOWBALL", "TIMER_10S", "TIMER_1S", "TIMER_30S", "TIMER_5S", "TIMER_60S",
            "TIMER_CUSTOM", "TOGGLE_FLIGHT", "TRIDENT_ATTACK", "TRIDENT_THROW", "TRIPWIRE",
            "UNLEASH_ENTITY", "USE_FIREWORK", "VILLAGER_TRADE", "VOID_DAMAGE", "WAKE_UP", "WEAR",
            "WIN_RAID", "WITHER_DAMAGE");
    private static final Set<String> POTIONS = Set.of(
            "ABSORPTION", "FIRE_RESISTANCE", "GLOWING", "HASTE", "INVISIBILITY", "JUMP_BOOST",
            "LUCK", "MINING_FATIGUE", "POISON", "REGENERATION", "RESISTANCE", "SATURATION",
            "SLOW_FALLING", "SLOWNESS", "SPEED", "STRENGTH", "WATER_BREATHING", "WEAKNESS");

    @Test
    void generatedExpansionContainsExactlyOneHundredEnchantments() throws IOException {
        assertEquals(100, generatedFiles().size());
    }

    @Test
    void allEnchantmentIdsAreUniqueAndLocalized() throws IOException {
        Map<String, Object> zh = yamlMap(RESOURCES.resolve("lang").resolve("zh_cn").resolve("enchantments.yml"));
        Map<String, Object> en = yamlMap(RESOURCES.resolve("lang").resolve("en_us").resolve("enchantments.yml"));
        Set<String> ids = new HashSet<>();

        for (Path path : allEnchantments()) {
            Map<String, Object> root = yamlMap(path);
            String id = string(root, "id");

            assertTrue(ids.add(id), "Duplicate enchantment id: " + id);
            assertTrue(zh.containsKey(id), "Missing zh_cn enchantment language entry: " + id);
            assertTrue(en.containsKey(id), "Missing en_us enchantment language entry: " + id);
        }
    }

    @Test
    void vanillaOverrideDefaultsCoverSpearAndLungeLocalization() throws IOException {
        Map<String, Object> sharpness = yamlMap(RESOURCES.resolve("vanilla").resolve("sharpness.yml"));
        Map<String, Object> lunge = yamlMap(RESOURCES.resolve("vanilla").resolve("lunge.yml"));

        assertTrue(list(sharpness, "applicable-items").contains("SPEAR"),
                "Sharpness override must allow spear items");
        assertEquals("突进", string(lunge, "display-name"));
        assertFalse(list(lunge, "description").isEmpty(), "Lunge must have a configured description");
    }

    @Test
    void allUsedTriggersHaveGuiLocalization() throws IOException {
        Map<?, ?> zhGuide = map(yamlMap(RESOURCES.resolve("lang").resolve("zh_cn").resolve("gui.yml")), "guide-gui");
        Map<?, ?> enGuide = map(yamlMap(RESOURCES.resolve("lang").resolve("en_us").resolve("gui.yml")), "guide-gui");
        Map<?, ?> zhAdmin = map(yamlMap(RESOURCES.resolve("lang").resolve("zh_cn").resolve("gui.yml")), "admin-gui");
        Map<?, ?> enAdmin = map(yamlMap(RESOURCES.resolve("lang").resolve("en_us").resolve("gui.yml")), "admin-gui");

        for (String trigger : usedTriggers()) {
            String key = "trigger-" + trigger;
            assertTrue(zhGuide.containsKey(key), "Missing zh_cn guide GUI trigger language entry: " + key);
            assertTrue(enGuide.containsKey(key), "Missing en_us guide GUI trigger language entry: " + key);
            assertTrue(zhAdmin.containsKey(key), "Missing zh_cn admin GUI trigger language entry: " + key);
            assertTrue(enAdmin.containsKey(key), "Missing en_us admin GUI trigger language entry: " + key);
            assertFalse(String.valueOf(zhGuide.get(key)).isBlank(), "Blank zh_cn guide GUI trigger language entry: " + key);
            assertFalse(String.valueOf(enGuide.get(key)).isBlank(), "Blank en_us guide GUI trigger language entry: " + key);
            assertFalse(String.valueOf(zhAdmin.get(key)).isBlank(), "Blank zh_cn admin GUI trigger language entry: " + key);
            assertFalse(String.valueOf(enAdmin.get(key)).isBlank(), "Blank en_us admin GUI trigger language entry: " + key);
        }
    }

    @Test
    void allUsedGuideDetailsHaveLocalization() throws IOException {
        Map<?, ?> zhGuide = map(yamlMap(RESOURCES.resolve("lang").resolve("zh_cn").resolve("gui.yml")), "guide-gui");
        Map<?, ?> enGuide = map(yamlMap(RESOURCES.resolve("lang").resolve("en_us").resolve("gui.yml")), "guide-gui");

        for (String condition : usedConditionTypes()) {
            String key = "condition-" + condition.toLowerCase(Locale.ROOT);
            assertTrue(zhGuide.containsKey(key), "Missing zh_cn guide GUI condition language entry: " + key);
            assertTrue(enGuide.containsKey(key), "Missing en_us guide GUI condition language entry: " + key);
            assertFalse(String.valueOf(zhGuide.get(key)).isBlank(), "Blank zh_cn guide GUI condition language entry: " + key);
            assertFalse(String.valueOf(enGuide.get(key)).isBlank(), "Blank en_us guide GUI condition language entry: " + key);
        }

        for (String action : usedActionTypes()) {
            String key = "action-" + action.toUpperCase(Locale.ROOT);
            assertTrue(zhGuide.containsKey(key), "Missing zh_cn guide GUI action language entry: " + key);
            assertTrue(enGuide.containsKey(key), "Missing en_us guide GUI action language entry: " + key);
            assertFalse(String.valueOf(zhGuide.get(key)).isBlank(), "Blank zh_cn guide GUI action language entry: " + key);
            assertFalse(String.valueOf(enGuide.get(key)).isBlank(), "Blank en_us guide GUI action language entry: " + key);
        }
    }

    @TestFactory
    Collection<DynamicTest> generatedEnchantmentsAreValidIndividually() throws IOException {
        Set<String> conditions = conditionIds();
        Set<String> effects = effectIds();
        Map<String, Object> zh = yamlMap(RESOURCES.resolve("lang").resolve("zh_cn").resolve("enchantments.yml"));
        Map<String, Object> en = yamlMap(RESOURCES.resolve("lang").resolve("en_us").resolve("enchantments.yml"));
        List<DynamicTest> tests = new ArrayList<>();

        for (Path path : generatedFiles()) {
            tests.add(DynamicTest.dynamicTest(ENCHANTMENTS.relativize(path).toString(), () ->
                    validateGenerated(path, conditions, effects, zh, en)));
        }
        return tests;
    }

    private static void validateGenerated(Path path, Set<String> conditions, Set<String> effects,
                                          Map<String, Object> zh, Map<String, Object> en) throws IOException {
        Map<String, Object> root = yamlMap(path);
        String fileName = path.getFileName().toString().replaceFirst("\\.yml$", "");
        String category = path.getParent().getFileName().toString();
        String id = string(root, "id");

        assertEquals(fileName, id, path + " id must match file name");
        assertEquals(category, string(root, "category"), path + " category must match directory");
        assertTrue(CATEGORIES.contains(category), path + " category is invalid");
        assertTrue(RARITIES.contains(string(root, "rarity")), path + " rarity is invalid");
        assertTrue(GROUPS.contains(string(root, "group")), path + " group is invalid");
        assertTrue(number(root, "max-level").intValue() >= 1, path + " max-level must be positive");

        List<?> items = list(root, "applicable-items");
        assertFalse(items.isEmpty(), path + " applicable-items cannot be empty");
        for (Object item : items) {
            String token = String.valueOf(item).toUpperCase(Locale.ROOT);
            assertTrue(ITEM_ALIASES.contains(token) || isMaterial(token), path + " invalid item: " + token);
        }

        assertLanguageEntry(zh, id, path + " zh_cn");
        assertLanguageEntry(en, id, path + " en_us");

        Map<?, ?> obtain = map(root, "obtain");
        assertEquals(Boolean.TRUE, obtain.get("enchanting-table"), path + " enchanting-table must be enabled");
        assertEquals(Boolean.TRUE, obtain.get("anvil"), path + " anvil must be enabled");
        assertNotNull(obtain.get("enchanting-table-weight"), path + " missing enchanting-table-weight");

        List<?> priceRange = list(root, "villager-trade-price-range");
        assertEquals(2, priceRange.size(), path + " villager-trade-price-range must have min and max");

        Map<?, ?> pools = map(root, "codex-pools");
        assertFalse(pools.isEmpty(), path + " codex-pools cannot be empty");
        for (Object key : pools.keySet()) {
            assertTrue(RARITIES.contains(String.valueOf(key)), path + " invalid codex rarity: " + key);
        }

        List<?> blocks = list(root, "effects");
        assertFalse(blocks.isEmpty(), path + " effects cannot be empty");
        for (Object blockRaw : blocks) {
            Map<?, ?> block = assertInstanceOf(Map.class, blockRaw, path + " effect block must be a map");
            String trigger = String.valueOf(block.get("trigger"));
            assertTrue(TRIGGERS.contains(trigger), path + " invalid trigger: " + trigger);
            assertTrue(number(block, "cooldown").intValue() >= 0, path + " cooldown cannot be negative");

            for (Object conditionRaw : list(block, "conditions")) {
                Map<?, ?> condition = assertInstanceOf(Map.class, conditionRaw, path + " condition must be a map");
                String type = String.valueOf(condition.get("type")).toLowerCase(Locale.ROOT);
                assertTrue(conditions.contains(type), path + " invalid condition: " + type);
            }

            List<?> actions = list(block, "actions");
            assertFalse(actions.isEmpty(), path + " actions cannot be empty");
            for (Object actionRaw : actions) {
                Map<?, ?> action = assertInstanceOf(Map.class, actionRaw, path + " action must be a map");
                String type = String.valueOf(action.get("type")).toUpperCase(Locale.ROOT);
                assertTrue(effects.contains(type), path + " invalid action: " + type);
                validateActionParams(path, action, type);
            }
        }
    }

    private static void validateActionParams(Path path, Map<?, ?> action, String type) {
        Object particle = action.get("particle");
        if ("PARTICLE".equals(type) && particle != null) {
            Particle.valueOf(String.valueOf(particle).toUpperCase(Locale.ROOT));
        }

        Object potion = action.get("potion");
        if (potion != null) {
            String key = String.valueOf(potion).toUpperCase(Locale.ROOT);
            assertTrue(POTIONS.contains(key), path + " unsupported potion id in " + type + ": " + key);
        }
    }

    private static void assertLanguageEntry(Map<String, Object> lang, String id, String label) {
        Map<?, ?> entry = assertInstanceOf(Map.class, lang.get(id), label + " language entry must be a map");
        assertFalse(String.valueOf(entry.get("name")).isBlank(), label + " name cannot be blank");
        assertFalse(list(entry, "description").isEmpty(), label + " description cannot be empty");
    }

    private static Set<String> conditionIds() {
        ConditionRegistry registry = new ConditionRegistry();
        EffectPipeline.registerBuiltinConditions(registry);
        return new TreeSet<>(registry.getRegisteredIds());
    }

    private static Set<String> effectIds() {
        EffectRegistry registry = new EffectRegistry();
        EffectPipeline.registerBuiltinEffects(registry);
        return new TreeSet<>(registry.getRegisteredIds());
    }

    private static List<Path> generatedFiles() throws IOException {
        try (Stream<Path> stream = Files.walk(ENCHANTMENTS)) {
            return stream
                    .filter(path -> path.toString().endsWith(".yml"))
                    .filter(EnchantmentResourceCatalogTest::containsGeneratedMarker)
                    .sorted()
                    .toList();
        }
    }

    private static List<Path> allEnchantments() throws IOException {
        try (Stream<Path> stream = Files.walk(ENCHANTMENTS)) {
            return stream
                    .filter(path -> path.toString().endsWith(".yml"))
                    .sorted()
                    .toList();
        }
    }

    private static Set<String> usedTriggers() throws IOException {
        Set<String> triggers = new TreeSet<>();
        for (Path path : allEnchantments()) {
            Map<String, Object> root = yamlMap(path);
            for (Object blockRaw : list(root, "effects")) {
                Map<?, ?> block = assertInstanceOf(Map.class, blockRaw, path + " effect block must be a map");
                triggers.add(String.valueOf(block.get("trigger")));
            }
        }
        return triggers;
    }

    private static Set<String> usedConditionTypes() throws IOException {
        Set<String> conditions = new TreeSet<>();
        for (Path path : allEnchantments()) {
            Map<String, Object> root = yamlMap(path);
            for (Object blockRaw : list(root, "effects")) {
                Map<?, ?> block = assertInstanceOf(Map.class, blockRaw, path + " effect block must be a map");
                for (Object conditionRaw : list(block, "conditions")) {
                    Map<?, ?> condition = assertInstanceOf(Map.class, conditionRaw, path + " condition must be a map");
                    conditions.add(String.valueOf(condition.get("type")));
                }
            }
        }
        return conditions;
    }

    private static Set<String> usedActionTypes() throws IOException {
        Set<String> actions = new TreeSet<>();
        for (Path path : allEnchantments()) {
            Map<String, Object> root = yamlMap(path);
            for (Object blockRaw : list(root, "effects")) {
                Map<?, ?> block = assertInstanceOf(Map.class, blockRaw, path + " effect block must be a map");
                for (Object actionRaw : list(block, "actions")) {
                    Map<?, ?> action = assertInstanceOf(Map.class, actionRaw, path + " action must be a map");
                    actions.add(String.valueOf(action.get("type")));
                }
            }
        }
        return actions;
    }

    private static boolean containsGeneratedMarker(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8).contains(GENERATED_MARKER);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> yamlMap(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            Object loaded = new Yaml().load(reader);
            return assertInstanceOf(Map.class, loaded, path + " must be a YAML map");
        }
    }

    private static String string(Map<?, ?> map, String key) {
        Object value = map.get(key);
        assertNotNull(value, "Missing required key: " + key);
        return String.valueOf(value);
    }

    private static Number number(Map<?, ?> map, String key) {
        Object value = map.get(key);
        assertInstanceOf(Number.class, value, "Expected numeric key: " + key);
        return (Number) value;
    }

    private static Map<?, ?> map(Map<?, ?> map, String key) {
        return assertInstanceOf(Map.class, map.get(key), "Expected map key: " + key);
    }

    private static List<?> list(Map<?, ?> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return List.of();
        }
        return assertInstanceOf(List.class, value, "Expected list key: " + key);
    }

    private static boolean isMaterial(String token) {
        try {
            Material.valueOf(token);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
