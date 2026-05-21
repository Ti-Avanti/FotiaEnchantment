package gg.fotia.enchantment.pipeline;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.core.EnchantmentData;
import gg.fotia.enchantment.core.EnchantmentManager;
import gg.fotia.enchantment.core.PDCManager;
import gg.fotia.enchantment.integration.WorldGuardHook;
import gg.fotia.enchantment.pipeline.condition.Condition;
import gg.fotia.enchantment.pipeline.condition.ConditionContext;
import gg.fotia.enchantment.pipeline.condition.ConditionRegistry;
import gg.fotia.enchantment.pipeline.condition.impl.*;
import gg.fotia.enchantment.pipeline.effect.Effect;
import gg.fotia.enchantment.pipeline.effect.EffectContext;
import gg.fotia.enchantment.pipeline.effect.EffectRegistry;
import gg.fotia.enchantment.pipeline.effect.impl.*;
import gg.fotia.enchantment.pipeline.trigger.TriggerContext;
import gg.fotia.enchantment.pipeline.trigger.TriggerRegistry;
import gg.fotia.enchantment.pipeline.trigger.impl.*;
import gg.fotia.enchantment.util.SchedulerUtils;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 效果管道 - 核心执行引擎
 *
 * <p>流程：触发器触发 → 扫描玩家装备上的附魔 →
 * 找到匹配该触发器的效果配置 → 检查条件 → 检查冷却 → 执行动作。
 *
 * <p>本类同时承担内置触发器/条件/效果的注册入口，
 * 以及对单 tick 内总执行次数的限流。
 */
public class EffectPipeline {

    private final FotiaEnchantment plugin;
    private final TriggerRegistry triggerRegistry;
    private final ConditionRegistry conditionRegistry;
    private final EffectRegistry effectRegistry;
    private final CooldownManager cooldownManager;

    private int maxEffectsPerTick;
    private int currentTickEffects = 0;
    private Object tickResetTask;
    private Object cooldownPurgeTask;
    private final ThreadLocal<List<String>> executionStack = ThreadLocal.withInitial(ArrayList::new);

    public EffectPipeline(FotiaEnchantment plugin) {
        this.plugin = plugin;
        this.triggerRegistry = new TriggerRegistry();
        this.conditionRegistry = new ConditionRegistry();
        this.effectRegistry = new EffectRegistry();
        this.cooldownManager = new CooldownManager();
        this.maxEffectsPerTick = plugin.getConfigManager().getMainConfig()
                .getInt("performance.max-effects-per-tick", 50);
    }

    /**
     * 初始化管道：注册所有内置组件，激活触发器
     */
    public void init() {
        registerBuiltinTriggers();
        registerBuiltinConditions();
        registerBuiltinEffects();
        startMaintenanceTasks();
        triggerRegistry.activateAll(this);
        plugin.getLogger().info("效果管道已初始化");
    }

    /**
     * 关闭管道
     */
    public void shutdown() {
        triggerRegistry.deactivateAll();
        stopMaintenanceTasks();
        cooldownManager.clearAll();
    }

    /**
     * 重载配置
     */
    public void reload() {
        triggerRegistry.deactivateAll();
        maxEffectsPerTick = plugin.getConfigManager().getMainConfig()
                .getInt("performance.max-effects-per-tick", 50);
        currentTickEffects = 0;
        triggerRegistry.activateAll(this);
    }

    /**
     * 触发器调用此方法来执行效果管道
     *
     * @param context 触发上下文
     */
    public void execute(TriggerContext context) {
        if (context == null) {
            return;
        }
        Player player = context.getPlayer();
        if (player == null) {
            return;
        }

        String triggerId = context.getTriggerId();
        if (triggerId == null || triggerId.isEmpty()) {
            return;
        }

        String executionKey = player.getUniqueId() + ":" + triggerId.toUpperCase(Locale.ROOT);
        List<String> stack = executionStack.get();
        if (stack.contains(executionKey)) {
            return;
        }
        stack.add(executionKey);
        try {
            WorldGuardHook worldGuardHook = plugin.getIntegrationManager() != null
                    ? plugin.getIntegrationManager().getWorldGuardHook()
                    : null;
            if (worldGuardHook != null
                    && !worldGuardHook.isEnchantAllowed(player, player.getLocation())) {
                return;
            }

            // 获取玩家所有装备上匹配该触发器的活跃附魔
            List<ActiveEnchantment> activeEnchantments = getActiveEnchantments(player, triggerId);
            if (activeEnchantments.isEmpty()) {
                return;
            }

            for (int idx = 0; idx < activeEnchantments.size(); idx++) {
                // 性能限制检查
                if (currentTickEffects >= maxEffectsPerTick) {
                    break;
                }

                ActiveEnchantment active = activeEnchantments.get(idx);
                EnchantmentData.EffectBlock effectBlock = active.getEffectBlock();
                if (effectBlock == null) {
                    continue;
                }
                int level = active.getLevel();

                // 构建变量 Map
                Map<String, Double> variables = new HashMap<>();
                variables.put("level", (double) level);
                variables.put("value", context.getValue());
                variables.put("alt_value", context.getAltValue());

                // 检查所有条件
                boolean allConditionsMet = true;
                if (effectBlock.getConditions() != null) {
                    for (EnchantmentData.ConditionConfig condConfig : effectBlock.getConditions()) {
                        if (condConfig == null) {
                            continue;
                        }
                        Condition condition = conditionRegistry.get(condConfig.getType());
                        if (condition == null) {
                            continue;
                        }
                        ConditionContext condContext = new ConditionContext(
                                plugin, context, condConfig, level, variables
                        );
                        boolean passed;
                        try {
                            passed = condition.check(condContext);
                        } catch (Exception ex) {
                            plugin.getLogger().warning("检查条件 "
                                    + condConfig.getType() + " 时出错: " + ex.getMessage());
                            passed = false;
                        }
                        if (!passed) {
                            allConditionsMet = false;
                            break;
                        }
                    }
                }

                if (!allConditionsMet) {
                    continue;
                }

                // 检查冷却
                String cooldownKey = active.getData().getId() + ":" + active.getEffectIndex();
                if (effectBlock.getCooldown() > 0
                        && cooldownManager.isOnCooldown(player.getUniqueId(), cooldownKey)) {
                    continue;
                }

                // 执行所有动作
                if (effectBlock.getActions() != null) {
                    for (EnchantmentData.ActionConfig actionConfig : effectBlock.getActions()) {
                        if (actionConfig == null) {
                            continue;
                        }
                        Effect effect = effectRegistry.get(actionConfig.getType());
                        if (effect == null) {
                            continue;
                        }
                        EffectContext effectContext = new EffectContext(
                                plugin, context, actionConfig, level, variables
                        );
                        try {
                            effect.execute(effectContext);
                        } catch (Exception ex) {
                            plugin.getLogger().warning("执行效果 "
                                    + actionConfig.getType() + " 时出错: " + ex.getMessage());
                        }
                        currentTickEffects++;
                        if (effectContext.isStopChain()) {
                            break;
                        }
                    }
                }

                // 设置冷却
                if (effectBlock.getCooldown() > 0) {
                    cooldownManager.setCooldown(
                            player.getUniqueId(), cooldownKey, effectBlock.getCooldown());
                }
            }
        } finally {
            stack.remove(stack.size() - 1);
            if (stack.isEmpty()) {
                executionStack.remove();
            }
        }
    }

    /**
     * 获取玩家装备上匹配指定触发器的活跃附魔
     */
    private List<ActiveEnchantment> getActiveEnchantments(Player player, String triggerId) {
        List<ActiveEnchantment> result = new ArrayList<>();
        PlayerInventory inv = player.getInventory();

        // 检查所有可能携带附魔的槽位
        ItemStack[] itemsToCheck = {
                inv.getItemInMainHand(),
                inv.getItemInOffHand(),
                inv.getHelmet(),
                inv.getChestplate(),
                inv.getLeggings(),
                inv.getBoots()
        };

        // 从 EnchantmentManager 获取所有已注册附魔
        // 注意：EnchantmentManager 尚未完全实现，这里预留接口
        Map<String, EnchantmentData> allEnchantments = getRegisteredEnchantments();
        if (allEnchantments == null || allEnchantments.isEmpty()) {
            return result;
        }

        for (ItemStack item : itemsToCheck) {
            if (item == null || item.getType().isAir()) {
                continue;
            }

            for (Map.Entry<String, EnchantmentData> entry : allEnchantments.entrySet()) {
                EnchantmentData data = entry.getValue();
                if (data == null || !data.isEnabled()) {
                    continue;
                }

                int level = getEnchantLevel(item, data);
                if (level <= 0) {
                    continue;
                }

                List<EnchantmentData.EffectBlock> effects = data.getEffects();
                if (effects == null || effects.isEmpty()) {
                    continue;
                }
                for (int effectIndex = 0; effectIndex < effects.size(); effectIndex++) {
                    EnchantmentData.EffectBlock block = effects.get(effectIndex);
                    if (block == null) {
                        continue;
                    }
                    if (triggerId.equalsIgnoreCase(block.getTrigger())) {
                        result.add(new ActiveEnchantment(data, level, item, block, effectIndex));
                    }
                }
            }
        }

        return result;
    }

    /**
     * 获取物品上某附魔的等级。
     */
    private int getEnchantLevel(ItemStack item, EnchantmentData data) {
        if (item == null || data == null || data.getId() == null) {
            return 0;
        }
        EnchantmentManager enchantmentManager = plugin.getEnchantmentManager();
        if (enchantmentManager == null) {
            return 0;
        }
        PDCManager pdcManager = enchantmentManager.getPdcManager();
        return pdcManager.getEnchantmentLevel(item, data.getId());
    }

    /**
     * 获取所有已注册的附魔数据（从 EnchantmentManager）
     */
    private Map<String, EnchantmentData> getRegisteredEnchantments() {
        EnchantmentManager enchantmentManager = plugin.getEnchantmentManager();
        if (enchantmentManager == null) {
            return Collections.emptyMap();
        }
        return enchantmentManager.getRegistry().getAllEnchantments();
    }

    private void startMaintenanceTasks() {
        stopMaintenanceTasks();
        tickResetTask = SchedulerUtils.runTaskTimer(plugin, this::resetTickCounter, 1L, 1L);
        cooldownPurgeTask = SchedulerUtils.runTaskTimer(plugin, cooldownManager::purgeExpired, 20L * 60L, 20L * 60L);
    }

    private void stopMaintenanceTasks() {
        SchedulerUtils.cancelTask(tickResetTask);
        SchedulerUtils.cancelTask(cooldownPurgeTask);
        tickResetTask = null;
        cooldownPurgeTask = null;
    }

    // ==================== 内置组件注册（待实现） ====================

    private void registerBuiltinTriggers() {
        // 近战
        triggerRegistry.register("MELEE_ATTACK", MeleeAttackTrigger::new);
        triggerRegistry.register("MELEE_ATTACK_CRITICAL", MeleeAttackCriticalTrigger::new);
        triggerRegistry.register("MELEE_ATTACK_BEHIND", MeleeAttackBehindTrigger::new);
        triggerRegistry.register("MELEE_ATTACK_WHILE_AIRBORNE", MeleeAttackWhileAirborneTrigger::new);
        triggerRegistry.register("MELEE_ATTACK_SWEEP", MeleeAttackSweepTrigger::new);
        triggerRegistry.register("MELEE_ATTACK_COMBO", MeleeAttackComboTrigger::new);

        // 远程
        triggerRegistry.register("BOW_ATTACK", BowAttackTrigger::new);
        triggerRegistry.register("CROSSBOW_ATTACK", CrossbowAttackTrigger::new);
        triggerRegistry.register("TRIDENT_ATTACK", TridentAttackTrigger::new);
        triggerRegistry.register("BOW_SHOOT", BowShootTrigger::new);
        triggerRegistry.register("CROSSBOW_SHOOT", CrossbowShootTrigger::new);
        triggerRegistry.register("TRIDENT_THROW", TridentThrowTrigger::new);
        triggerRegistry.register("PROJECTILE_HIT_ENTITY", ProjectileHitEntityTrigger::new);
        triggerRegistry.register("PROJECTILE_HIT_BLOCK", ProjectileHitBlockTrigger::new);
        triggerRegistry.register("HEADSHOT", HeadshotTrigger::new);
        triggerRegistry.register("ARROW_BOUNCE", ArrowBounceTrigger::new);

        // 击杀
        triggerRegistry.register("KILL", KillTrigger::new);
        triggerRegistry.register("KILL_PLAYER", KillPlayerTrigger::new);
        triggerRegistry.register("KILL_BOSS", KillBossTrigger::new);
        triggerRegistry.register("KILL_STREAK", KillStreakTrigger::new);
        triggerRegistry.register("ASSIST", AssistTrigger::new);
        triggerRegistry.register("FIRST_BLOOD", FirstBloodTrigger::new);

        // 受伤
        triggerRegistry.register("TAKE_DAMAGE", TakeDamageTrigger::new);
        triggerRegistry.register("TAKE_ENTITY_DAMAGE", TakeEntityDamageTrigger::new);
        triggerRegistry.register("TAKE_PLAYER_DAMAGE", TakePlayerDamageTrigger::new);
        triggerRegistry.register("TAKE_PROJECTILE_DAMAGE", TakeProjectileDamageTrigger::new);
        triggerRegistry.register("FALL_DAMAGE", FallDamageTrigger::new);
        triggerRegistry.register("FIRE_DAMAGE", FireDamageTrigger::new);
        triggerRegistry.register("EXPLOSION_DAMAGE", ExplosionDamageTrigger::new);
        triggerRegistry.register("POISON_DAMAGE", PoisonDamageTrigger::new);
        triggerRegistry.register("WITHER_DAMAGE", WitherDamageTrigger::new);
        triggerRegistry.register("DROWNING_DAMAGE", DrowningDamageTrigger::new);
        triggerRegistry.register("VOID_DAMAGE", VoidDamageTrigger::new);
        triggerRegistry.register("MAGIC_DAMAGE", MagicDamageTrigger::new);
        triggerRegistry.register("SHIELD_BLOCK", ShieldBlockTrigger::new);
        triggerRegistry.register("ARMOR_ABSORB", ArmorAbsorbTrigger::new);
        triggerRegistry.register("DODGE", DodgeTrigger::new);
        triggerRegistry.register("RESURRECT", ResurrectTrigger::new);
        triggerRegistry.register("NEAR_DEATH", NearDeathTrigger::new);

        // 生命
        triggerRegistry.register("DEATH", DeathTrigger::new);
        triggerRegistry.register("RESPAWN", RespawnTrigger::new);
        triggerRegistry.register("HEAL", HealTrigger::new);
        triggerRegistry.register("NATURAL_REGEN", NaturalRegenTrigger::new);
        triggerRegistry.register("GAIN_ABSORPTION", GainAbsorptionTrigger::new);

        // 挖掘
        triggerRegistry.register("MINE_BLOCK", MineBlockTrigger::new);
        triggerRegistry.register("MINE_BLOCK_PROGRESS", MineBlockProgressTrigger::new);
        triggerRegistry.register("MINE_ORE", MineOreTrigger::new);
        triggerRegistry.register("MINE_DEEPSLATE", MineDeepslateTrigger::new);
        triggerRegistry.register("BLOCK_ITEM_DROP", BlockItemDropTrigger::new);
        triggerRegistry.register("PLACE_BLOCK", PlaceBlockTrigger::new);
        triggerRegistry.register("BREAK_SPAWNER", BreakSpawnerTrigger::new);
        triggerRegistry.register("INTERACT_BLOCK", InteractBlockTrigger::new);

        // 农业
        triggerRegistry.register("HARVEST", HarvestTrigger::new);
        triggerRegistry.register("HARVEST_TREE", HarvestTreeTrigger::new);
        triggerRegistry.register("BONEMEAL_CROP", BonemealCropTrigger::new);
        triggerRegistry.register("PLANT_SEED", PlantSeedTrigger::new);
        triggerRegistry.register("SHEAR_ENTITY", ShearEntityTrigger::new);
        triggerRegistry.register("SHEAR_BLOCK", ShearBlockTrigger::new);
        triggerRegistry.register("BREED_ANIMAL", BreedAnimalTrigger::new);
        triggerRegistry.register("TAME_ANIMAL", TameAnimalTrigger::new);
        triggerRegistry.register("MILK_COW", MilkCowTrigger::new);
        triggerRegistry.register("COMPOST_ITEM", CompostItemTrigger::new);

        // 制作
        triggerRegistry.register("CRAFT", CraftTrigger::new);
        triggerRegistry.register("SMELT", SmeltTrigger::new);
        triggerRegistry.register("BREW", BrewTrigger::new);
        triggerRegistry.register("ENCHANT_ITEM", EnchantItemTrigger::new);
        triggerRegistry.register("SMITH_ITEM", SmithItemTrigger::new);
        triggerRegistry.register("GRIND_ITEM", GrindItemTrigger::new);
        triggerRegistry.register("ANVIL_USE", AnvilUseTrigger::new);
        triggerRegistry.register("LOOM_USE", LoomUseTrigger::new);
        triggerRegistry.register("CARTOGRAPHY_USE", CartographyUseTrigger::new);
        triggerRegistry.register("STONECUTTER_USE", StonecutterUseTrigger::new);
        triggerRegistry.register("REPAIR_ITEM", RepairItemTrigger::new);

        // 交易
        triggerRegistry.register("VILLAGER_TRADE", VillagerTradeTrigger::new);
        triggerRegistry.register("SELL_ITEM", SellItemTrigger::new);
        triggerRegistry.register("BUY_ITEM", BuyItemTrigger::new);

        // 物品
        triggerRegistry.register("CONSUME", ConsumeTrigger::new);
        triggerRegistry.register("PICK_UP_ITEM", PickUpItemTrigger::new);
        triggerRegistry.register("DROP_ITEM", DropItemTrigger::new);
        triggerRegistry.register("DAMAGE_ITEM", DamageItemTrigger::new);
        triggerRegistry.register("ITEM_BREAK", ItemBreakTrigger::new);
        triggerRegistry.register("HOLD_ITEM_CHANGE", HoldItemChangeTrigger::new);
        triggerRegistry.register("CHANGE_ARMOR", ChangeArmorTrigger::new);
        triggerRegistry.register("THROW_EGG", ThrowEggTrigger::new);
        triggerRegistry.register("THROW_SNOWBALL", ThrowSnowballTrigger::new);
        triggerRegistry.register("THROW_ENDER_PEARL", ThrowEnderPearlTrigger::new);
        triggerRegistry.register("USE_FIREWORK", UseFireworkTrigger::new);
        triggerRegistry.register("FILL_BUCKET", FillBucketTrigger::new);
        triggerRegistry.register("EMPTY_BUCKET", EmptyBucketTrigger::new);

        // 移动
        triggerRegistry.register("JUMP", JumpTrigger::new);
        triggerRegistry.register("DOUBLE_JUMP", DoubleJumpTrigger::new);
        triggerRegistry.register("SPRINT_START", SprintStartTrigger::new);
        triggerRegistry.register("SPRINT_STOP", SprintStopTrigger::new);
        triggerRegistry.register("SNEAK_START", SneakStartTrigger::new);
        triggerRegistry.register("SNEAK_STOP", SneakStopTrigger::new);
        triggerRegistry.register("DEPLOY_ELYTRA", DeployElytraTrigger::new);
        triggerRegistry.register("ELYTRA_BOOST", ElytraBoostTrigger::new);
        triggerRegistry.register("ELYTRA_GLIDE", ElytraGlideTrigger::new);
        triggerRegistry.register("RIPTIDE", RiptideTrigger::new);
        triggerRegistry.register("ENTER_WATER", EnterWaterTrigger::new);
        triggerRegistry.register("EXIT_WATER", ExitWaterTrigger::new);
        triggerRegistry.register("SWIM", SwimTrigger::new);
        triggerRegistry.register("ENTER_LAVA", EnterLavaTrigger::new);
        triggerRegistry.register("TELEPORT", TeleportTrigger::new);
        triggerRegistry.register("ENTER_VEHICLE", EnterVehicleTrigger::new);
        triggerRegistry.register("EXIT_VEHICLE", ExitVehicleTrigger::new);
        triggerRegistry.register("TOGGLE_FLIGHT", ToggleFlightTrigger::new);
        triggerRegistry.register("LAND", LandTrigger::new);

        // 状态
        triggerRegistry.register("POTION_EFFECT", PotionEffectTrigger::new);
        triggerRegistry.register("LOSE_POTION_EFFECT", LosePotionEffectTrigger::new);
        triggerRegistry.register("GAIN_XP", GainXpTrigger::new);
        triggerRegistry.register("LEVEL_UP", LevelUpTrigger::new);
        triggerRegistry.register("GAIN_HUNGER", GainHungerTrigger::new);
        triggerRegistry.register("LOSE_HUNGER", LoseHungerTrigger::new);
        triggerRegistry.register("CATCH_FIRE", CatchFireTrigger::new);
        triggerRegistry.register("EXTINGUISH", ExtinguishTrigger::new);
        triggerRegistry.register("FREEZE", FreezeTrigger::new);

        // 钓鱼
        triggerRegistry.register("CAST_ROD", CastRodTrigger::new);
        triggerRegistry.register("BITE", BiteTrigger::new);
        triggerRegistry.register("CATCH_FISH", CatchFishTrigger::new);
        triggerRegistry.register("CATCH_TREASURE", CatchTreasureTrigger::new);
        triggerRegistry.register("CATCH_JUNK", CatchJunkTrigger::new);
        triggerRegistry.register("CATCH_ENTITY", CatchEntityTrigger::new);
        triggerRegistry.register("REEL_IN", ReelInTrigger::new);
        triggerRegistry.register("HOOK_IN_GROUND", HookInGroundTrigger::new);

        // 实体
        triggerRegistry.register("ENTITY_TARGET_ME", EntityTargetMeTrigger::new);
        triggerRegistry.register("ENTITY_SPAWN_NEAR", EntitySpawnNearTrigger::new);
        triggerRegistry.register("COLLIDE_WITH_ENTITY", CollideWithEntityTrigger::new);
        triggerRegistry.register("LEASH_ENTITY", LeashEntityTrigger::new);
        triggerRegistry.register("UNLEASH_ENTITY", UnleashEntityTrigger::new);
        triggerRegistry.register("INTERACT_ENTITY", InteractEntityTrigger::new);
        triggerRegistry.register("MOUNT_ENTITY", MountEntityTrigger::new);
        triggerRegistry.register("DISMOUNT_ENTITY", DismountEntityTrigger::new);

        // 环境
        triggerRegistry.register("CHANGE_BIOME", ChangeBiomeTrigger::new);
        triggerRegistry.register("CHANGE_WORLD", ChangeWorldTrigger::new);
        triggerRegistry.register("LIGHTNING_STRIKE_NEAR", LightningStrikeNearTrigger::new);
        triggerRegistry.register("ENTER_REGION", EnterRegionTrigger::new);
        triggerRegistry.register("EXIT_REGION", ExitRegionTrigger::new);
        triggerRegistry.register("WIN_RAID", WinRaidTrigger::new);

        // 社交
        triggerRegistry.register("SEND_CHAT", SendChatTrigger::new);
        triggerRegistry.register("RUN_COMMAND", RunCommandTrigger::new);
        triggerRegistry.register("JOIN_SERVER", JoinServerTrigger::new);
        triggerRegistry.register("LEAVE_SERVER", LeaveServerTrigger::new);
        triggerRegistry.register("COMPLETE_ADVANCEMENT", CompleteAdvancementTrigger::new);

        // 睡眠
        triggerRegistry.register("ENTER_BED", EnterBedTrigger::new);
        triggerRegistry.register("LEAVE_BED", LeaveBedTrigger::new);
        triggerRegistry.register("WAKE_UP", WakeUpTrigger::new);

        // 方块
        triggerRegistry.register("BELL_RING", BellRingTrigger::new);
        triggerRegistry.register("NOTE_BLOCK_PLAY", NoteBlockPlayTrigger::new);
        triggerRegistry.register("CAULDRON_LEVEL_CHANGE", CauldronLevelChangeTrigger::new);
        triggerRegistry.register("PRESSURE_PLATE", PressurePlateTrigger::new);
        triggerRegistry.register("TRIPWIRE", TripwireTrigger::new);
        triggerRegistry.register("OPEN_CONTAINER", OpenContainerTrigger::new);
        triggerRegistry.register("CLOSE_CONTAINER", CloseContainerTrigger::new);

        // 被动
        triggerRegistry.register("HOLD", HoldTrigger::new);
        triggerRegistry.register("WEAR", WearTrigger::new);
        triggerRegistry.register("TIMER_1S", Timer1sTrigger::new);
        triggerRegistry.register("TIMER_5S", Timer5sTrigger::new);
        triggerRegistry.register("TIMER_10S", Timer10sTrigger::new);
        triggerRegistry.register("TIMER_30S", Timer30sTrigger::new);
        triggerRegistry.register("TIMER_60S", Timer60sTrigger::new);
        triggerRegistry.register("TIMER_CUSTOM", TimerCustomTrigger::new);
    }

    private void registerBuiltinConditions() {
        registerBuiltinConditions(conditionRegistry);
    }

    public static void registerBuiltinConditions(ConditionRegistry conditionRegistry) {
        conditionRegistry.register("chance", ChanceCondition::new);
        conditionRegistry.register("health_below", HealthBelowCondition::new);
        conditionRegistry.register("in_biome", InBiomeCondition::new);
        conditionRegistry.register("time", TimeCondition::new);
        conditionRegistry.register("weather", WeatherCondition::new);
        conditionRegistry.register("altitude", AltitudeCondition::new);
        conditionRegistry.register("in_water", InWaterCondition::new);
        conditionRegistry.register("in_world", InWorldCondition::new);
        conditionRegistry.register("exposure_to_sky", ExposureToSkyCondition::new);
        conditionRegistry.register("permission", PermissionCondition::new);
        conditionRegistry.register("target_is_player", TargetIsPlayerCondition::new);
        conditionRegistry.register("target_armor_points", TargetArmorPointsCondition::new);
        conditionRegistry.register("target_has_enchant", TargetHasEnchantCondition::new);
        conditionRegistry.register("target_is_blocking", TargetIsBlockingCondition::new);
        conditionRegistry.register("target_is_sprinting", TargetIsSprintingCondition::new);
        conditionRegistry.register("target_armor_type", TargetArmorTypeCondition::new);
        conditionRegistry.register("target_potion_effect", TargetPotionEffectCondition::new);
        conditionRegistry.register("target_health", TargetHealthCondition::new);
        conditionRegistry.register("kill_streak", KillStreakCondition::new);
        conditionRegistry.register("distance_to_target", DistanceToTargetCondition::new);
        conditionRegistry.register("players_nearby", PlayersNearbyCondition::new);
        conditionRegistry.register("in_combat", InCombatCondition::new);
        conditionRegistry.register("last_damage_interval", LastDamageIntervalCondition::new);
        conditionRegistry.register("consecutive_hits", ConsecutiveHitsCondition::new);
        conditionRegistry.register("not_attacked_for", NotAttackedForCondition::new);
        conditionRegistry.register("behind_target", BehindTargetCondition::new);
        conditionRegistry.register("velocity_above", VelocityAboveCondition::new);
        conditionRegistry.register("on_fire", OnFireCondition::new);
        conditionRegistry.register("food_level", FoodLevelCondition::new);
        conditionRegistry.register("cooldown_check", CooldownCheckCondition::new);

        String[] expandedIds = {
                "health_percent_above", "health_percent_below", "health_percent_between", "xp_level_at_least",
                "xp_level_below", "food_percent_above", "food_percent_below", "saturation_above",
                "oxygen_above", "oxygen_below", "absorption_above", "armor_points_above",
                "armor_toughness_above", "luck_above", "stat_value_above",
                "is_on_ground", "is_in_air", "is_falling", "is_flying", "is_gliding", "is_swimming",
                "is_sneaking", "is_sprinting", "is_riding", "is_climbing", "is_submerged", "is_frozen",
                "velocity_below", "fall_distance_above", "movement_speed_above", "looking_at_block",
                "mainhand_is", "offhand_is", "wearing_helmet", "wearing_chestplate", "wearing_leggings",
                "wearing_boots", "wearing_full_set", "item_has_lore", "item_has_name", "item_has_model_data",
                "item_has_custom_data", "item_durability_above", "item_durability_below",
                "item_has_vanilla_enchant", "item_has_custom_enchant", "inventory_contains",
                "inventory_has_space", "slot_empty",
                "target_exists", "target_is_living", "target_is_monster", "target_is_boss", "target_is_tamed",
                "target_is_named", "target_on_fire", "target_in_water", "target_health_percent_above",
                "target_health_percent_below", "target_distance_above", "target_distance_below",
                "target_has_permission", "target_in_region", "target_is_same_world", "target_line_of_sight",
                "damage_above", "damage_below",
                "in_region", "not_in_region", "in_claim", "in_safe_zone", "in_pvp_zone",
                "in_light_level_above", "in_light_level_below", "standing_on_block", "inside_block",
                "near_block", "near_entity_type", "near_player_count", "moon_phase", "season_is",
                "balance_above", "balance_below", "points_above", "points_below", "has_job",
                "job_level_above", "skill_level_above", "mcmmo_level_above", "aura_skill_level_above",
                "placeholder_equals", "placeholder_contains", "placeholder_greater_than", "quest_active",
                "quest_completed", "town_role_is", "lands_role_is",
                "any_of", "all_of", "none_of", "at_least_of", "expression_true", "expression_false",
                "cooldown_ready", "cooldown_active", "random_weight_passed", "trigger_value_above",
                "trigger_value_below", "alt_value_present", "context_has_block", "context_has_projectile",
                "is_op", "is_online_longer_than", "joined_before", "has_playtime_above",
                "in_permission_group", "has_scoreboard_tag", "has_advancement", "has_recipe",
                "language_is", "client_brand_is", "ping_below", "ping_above", "worldguard_flag_allowed",
                "blacklist_exempt"
        };
        for (String id : expandedIds) {
            conditionRegistry.register(id, () -> new CatalogCondition(id));
        }
    }

    private void registerBuiltinEffects() {
        registerBuiltinEffects(effectRegistry);
    }

    public static void registerBuiltinEffects(EffectRegistry effectRegistry) {
        effectRegistry.register("DAMAGE_MULTIPLY", DamageMultiplyEffect::new);
        effectRegistry.register("DAMAGE_ADD", DamageAddEffect::new);
        effectRegistry.register("DAMAGE_REDUCE", DamageReduceEffect::new);
        effectRegistry.register("TRUE_DAMAGE", TrueDamageEffect::new);
        effectRegistry.register("DODGE", DodgeEffect::new);
        effectRegistry.register("THORNS", ThornsEffect::new);
        effectRegistry.register("ADD_POTION_SELF", AddPotionSelfEffect::new);
        effectRegistry.register("ADD_POTION_TARGET", AddPotionTargetEffect::new);
        effectRegistry.register("REMOVE_POTION", RemovePotionEffect::new);
        effectRegistry.register("HEAL", HealEffect::new);
        effectRegistry.register("LIFESTEAL", LifestealEffect::new);
        effectRegistry.register("SPEED_BOOST", SpeedBoostEffect::new);
        effectRegistry.register("LAUNCH", LaunchEffect::new);
        effectRegistry.register("BONUS_DROP", BonusDropEffect::new);
        effectRegistry.register("SMELT", SmeltEffect::new);
        effectRegistry.register("VEIN_MINE", VeinMineEffect::new);
        effectRegistry.register("PARTICLE", ParticleEffect::new);
        effectRegistry.register("SOUND", SoundEffect::new);
        effectRegistry.register("EXPLODE", ExplodeEffect::new);
        effectRegistry.register("LIGHTNING", LightningEffect::new);
        effectRegistry.register("IGNITE_TARGET", IgniteTargetEffect::new);

        registerExpandedEffects(effectRegistry);
    }

    private static void registerExpandedEffects(EffectRegistry effectRegistry) {
        String[][] families = {
                {"SELF,TARGET,NEARBY_ENEMY,NEARBY_ALLY,PROJECTILE",
                        "DAMAGE_ADD,DAMAGE_MULTIPLY,DAMAGE_REDUCE,TRUE_DAMAGE,HEAL,LIFESTEAL,ABSORB,BLEED,POISON,WITHER,BURN,FREEZE,EXECUTE,REFLECT,SHIELD,ARMOR_PIERCE,CRIT_BOOST,DAMAGE_CAP"},
                {"SELF,TARGET,PROJECTILE,NEARBY_ENEMY",
                        "PUSH,PULL,KNOCKUP,TELEPORT,DASH,BLINK,HOMING,ROOT,STUN,SILENCE,SLOW,SPEED,GRAVITY,GLIDE_BOOST,SAFE_FALL,SWAP_POSITION,ROTATE,VORTEX"},
                {"HELD_ITEM,TARGET_ITEM,DROP,INVENTORY",
                        "REPAIR,DAMAGE,DUPLICATE,CONSUME,TRANSFORM,ADD_LORE,SET_NAME,SET_MODEL,ADD_ENCHANT,REMOVE_ENCHANT,TRANSFER_ENCHANT,AUTO_SMELT,AUTO_PICKUP,MULTIPLY_DROPS,FILTER_DROPS,TELEKINESIS"},
                {"BLOCK,AREA,LINE,SPHERE",
                        "BREAK,PLACE,REPLACE,AGE_CROP,REPLANT,TILL,MELT,FREEZE_WATER,LIGHTNING,EXPLOSION,SHOCKWAVE,PARTICLE_FIELD,SOUND_FIELD,GLOW_ORE,VEIN_MINE,MINE_RADIUS,DROP_BLOCK_LOOT,CANCEL_BLOCK_DROP"}
        };
        Set<String> existing = new HashSet<>(effectRegistry.getRegisteredIds());
        int added = 0;
        for (String[] family : families) {
            String[] targets = family[0].split(",");
            String[] operations = family[1].split(",");
            for (String operation : operations) {
                for (String target : targets) {
                    String id = target + "_" + operation;
                    if (!existing.contains(id)) {
                        effectRegistry.register(id, () -> new CatalogEffect(id));
                        existing.add(id);
                        added++;
                    }
                    if (added >= 270) {
                        return;
                    }
                }
            }
        }
    }

    // ==================== Getter ====================

    public TriggerRegistry getTriggerRegistry() {
        return triggerRegistry;
    }

    public ConditionRegistry getConditionRegistry() {
        return conditionRegistry;
    }

    public EffectRegistry getEffectRegistry() {
        return effectRegistry;
    }

    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }

    public FotiaEnchantment getPlugin() {
        return plugin;
    }

    public int getMaxEffectsPerTick() {
        return maxEffectsPerTick;
    }

    public int getCurrentTickEffects() {
        return currentTickEffects;
    }

    /**
     * 每tick重置计数器（由定时器调用）
     */
    public void resetTickCounter() {
        currentTickEffects = 0;
    }

    /**
     * 活跃附魔数据包装
     */
    public static class ActiveEnchantment {
        private final EnchantmentData data;
        private final int level;
        private final ItemStack item;
        private final EnchantmentData.EffectBlock effectBlock;
        private final int effectIndex;

        public ActiveEnchantment(EnchantmentData data,
                                 int level,
                                 ItemStack item,
                                 EnchantmentData.EffectBlock effectBlock,
                                 int effectIndex) {
            this.data = data;
            this.level = level;
            this.item = item;
            this.effectBlock = effectBlock;
            this.effectIndex = effectIndex;
        }

        public EnchantmentData getData() {
            return data;
        }

        public int getLevel() {
            return level;
        }

        public ItemStack getItem() {
            return item;
        }

        public EnchantmentData.EffectBlock getEffectBlock() {
            return effectBlock;
        }

        public int getEffectIndex() {
            return effectIndex;
        }
    }
}
