# FotiaEnchantment AI Enchantment Writing Guide

This file is written for AI agents. It teaches an AI how to generate valid FotiaEnchantment configuration without reading the plugin source code.

The goal is not to explain the plugin internals. The goal is to produce usable enchantment YAML and language entries that a server owner can paste into the plugin.

## Output Target

When a user asks for a new enchantment, usually output these files or snippets:

1. Enchantment config: `enchantments/<category>/<enchantment_id>.yml`
2. Chinese language entry: `lang/zh_cn/enchantments.yml`
3. English language entry: `lang/en_us/enchantments.yml`

If the user asks for another language, also output that language entry. Keep all YAML as UTF-8 without BOM.

## Naming Rules

- Enchantment IDs must be lowercase snake_case, for example `resilience` or `chain_miner`.
- File name should match the enchantment ID.
- Do not create two enchantments with the same ID.
- Use short, clear IDs. Avoid spaces, Chinese characters, punctuation, and uppercase letters.
- Use MiniMessage-compatible text in language files. Legacy color codes `&` and `§` are allowed by the plugin, but prefer clean text unless color is requested.

## Enchantment YAML Template

```yaml
enchantment_id:
  enabled: true
  max-level: 5
  rarity: dustlight
  group: defensive
  item-groups:
    - chestplates
    - leggings
  materials: []
  conflicts: []

  obtain:
    enchanting-table: true
    anvil: true
    villager-trade: true

  enchanting-table:
    weight: 20

  codex:
    enabled: true
    weight: 20

  effects:
    - trigger: TAKE_ENTITY_DAMAGE
      chance: "{level} * 3 + 7"
      cooldown: 120
      conditions:
        - type: HEALTH_PERCENT_BELOW
          value: 80
      actions:
        - type: HEAL
          value: "{level} * 0.08"
        - type: ADD_POTION_SELF
          potion: DAMAGE_RESISTANCE
          amplifier: "min(1, floor(({level} - 1) / 3))"
          duration: "{level} * 30 + 10"
```

The top-level key must be the same as the file name and enchantment ID.

## Required Fields

`enabled`: `true` or `false`.

`max-level`: maximum enchantment level.

`rarity`: one of:

- `dustlight`: common
- `moonlit`: uncommon
- `radiant`: medium
- `aureate`: rare
- `divine`: very rare

`group`: logical conflict/limit group. Use existing groups when possible:

- `fire`
- `ice`
- `lightning`
- `defensive`
- `offensive`
- `utility`
- `movement`
- `mining`

`item-groups`: item category list. Prefer these when the enchantment applies to many items:

- `helmets`
- `chestplates`
- `leggings`
- `boots`
- `swords`
- `axes`
- `pickaxes`
- `shovels`
- `hoes`
- `bows`
- `crossbows`
- `tridents`
- `fishing-rods`
- `shields`
- `books`

`materials`: explicit Bukkit material overrides. Use uppercase material names such as `DIAMOND_CHESTPLATE` or `NETHERITE_PICKAXE`.

`conflicts`: enchantment IDs that cannot coexist with this enchantment.

## Acquisition Fields

`obtain.enchanting-table`: whether the enchantment can appear in an enchanting table.

`obtain.anvil`: whether it can be combined on an anvil.

`obtain.villager-trade`: whether villagers may sell it.

`enchanting-table.weight`: higher values appear more often.

`codex.enabled`: whether it can appear in Stellaris Codex rolls.

`codex.weight`: higher values are more likely in codex rolls.

## Effect Pipeline

Every effect entry follows this shape:

```yaml
effects:
  - trigger: TRIGGER_ID
    chance: "{level} * 5"
    cooldown: 100
    conditions: []
    actions: []
```

`trigger` decides when the effect runs.

`chance` is optional. It is a percent value. Use expressions with `{level}` for scaling.

`cooldown` is optional and uses ticks. 20 ticks = 1 second.

`conditions` are checks. If any required condition fails, actions do not run.

`actions` are what actually happens.

## Common Triggers

Use the trigger that matches the desired gameplay moment:

- `MELEE_ATTACK`: player hits an entity with a melee item.
- `BOW_ATTACK`: player shoots with a bow.
- `PROJECTILE_HIT`: projectile hits something.
- `TAKE_ENTITY_DAMAGE`: player is damaged by an entity.
- `TAKE_PLAYER_DAMAGE`: player takes damage.
- `FIRE_DAMAGE`: player takes fire damage.
- `KILL_ENTITY`: player kills an entity.
- `KILL_STREAK`: player reaches a kill streak event.
- `MINE_BLOCK`: player mines a block.
- `MINE_ORE`: player mines ore.
- `BREAK_BLOCK`: player breaks a block.
- `HARVEST_CROP`: player harvests crops.
- `FISH_CAUGHT`: player catches fish or loot.
- `HOLD`: player is holding the item.
- `WEAR`: player is wearing armor.
- `TIMER_1S`, `TIMER_5S`, `TIMER_30S`: periodic checks.
- `SPRINT_START`, `SNEAK_START`, `JUMP`: movement events.
- `ANVIL_USE`: player uses an anvil.
- `ENCHANT_ITEM`: player enchants an item.

If a desired trigger does not exist, do not invent it. Choose the closest supported trigger and explain the compromise.

## Common Conditions

Use conditions only when they are needed:

- `CHANCE`: probability check.
- `HEALTH_PERCENT_BELOW`: player health percent is below a value.
- `HEALTH_PERCENT_ABOVE`: player health percent is above a value.
- `TARGET_HEALTH_BELOW`: target health is below a value.
- `TARGET_IS_PLAYER`: target is a player.
- `TARGET_HAS_POTION`: target has a potion effect.
- `PLAYER_IN_WATER`: player is in water.
- `PLAYER_ON_FIRE`: player is burning.
- `HAS_PERMISSION`: player has a permission.
- `WORLD`: player is in a specific world.
- `BIOME`: player is in a specific biome.
- `HEIGHT_ABOVE`, `HEIGHT_BELOW`: Y-level checks.
- `SPEED_ABOVE`: player speed is above a value.
- `NEARBY_PLAYERS`: nearby player count.
- `COOLDOWN_READY`: cooldown is ready.
- `ITEM_MATERIAL`: item material matches.
- `HAS_ENCHANTMENT`: item has a specific enchantment.

Do not add conditions that do not affect the requested design.

## Common Actions

Damage and healing:

- `DAMAGE`: add or modify damage.
- `TRUE_DAMAGE`: deal true damage.
- `REDUCE_DAMAGE`: reduce incoming damage.
- `HEAL`: restore health.
- `LIFE_STEAL`: steal health from the target.
- `DODGE`: avoid incoming damage.
- `REFLECT_DAMAGE`: reflect damage back.

Potion and status:

- `ADD_POTION_SELF`: add a potion effect to the player.
- `ADD_POTION_TARGET`: add a potion effect to the target.
- `REMOVE_POTION`: remove a potion effect.
- `SET_FIRE`: ignite target.
- `KNOCKBACK`: knock back target.

World and feedback:

- `PARTICLE`: play particles.
- `SOUND`: play a sound.
- `LIGHTNING`: summon lightning.
- `EXPLOSION`: create an explosion.

Items and drops:

- `ADD_DROP`: add drops.
- `AUTO_SMELT`: smelt drops.
- `CHAIN_MINE`: mine connected blocks.
- `ADD_EXP`: add experience.
- `REPAIR_ITEM`: repair item durability.

If an action needs a value, duration, amplifier, radius, material, potion, particle, or sound, include the relevant field.

## Expressions And Placeholders

The most important placeholder is `{level}`.

Examples:

- `"{level} * 5"` means level 1 = 5, level 5 = 25.
- `"{level} * 0.1"` means level 1 = 0.1, level 5 = 0.5.
- `"min(80, {level} * 12)"` caps a value at 80.
- `"max(5, {level} * 3)"` keeps a minimum value.

Use expressions for level scaling instead of writing separate configs per level.

## Descriptions

The language description should describe the gameplay effect clearly.

If the enchantment has level scaling or chance, write it with placeholders so the plugin can calculate the current level display later.

Good:

```yaml
resilience:
  name: "Resilience"
  description:
    - "When damaged, has a {chance}% chance to restore {heal}% health and gain Resistance {amplifier} for {duration}s."
```

Good Chinese equivalent:

```yaml
resilience:
  name: "韧性"
  description:
    - "受到攻击时有 {chance}% 概率恢复 {heal}% 生命，并获得抗性提升 {amplifier}，持续 {duration} 秒。"
```

Avoid vague descriptions such as "levels increase the effect" when the actual chance, heal, duration, or range can be shown.

## Language Entry Template

Chinese:

```yaml
resilience:
  name: "韧性"
  description:
    - "受到攻击时有 {chance}% 概率恢复 {heal}% 生命，并获得抗性提升 {amplifier}，持续 {duration} 秒。"
```

English:

```yaml
resilience:
  name: "Resilience"
  description:
    - "When damaged, has a {chance}% chance to restore {heal}% health and gain Resistance {amplifier} for {duration}s."
```

Use short descriptions for simple enchantments and detailed descriptions for complex ones.

## Example: Defensive Enchantment

```yaml
resilience:
  enabled: true
  max-level: 5
  rarity: moonlit
  group: defensive
  item-groups:
    - chestplates
    - leggings
  materials: []
  conflicts: []
  obtain:
    enchanting-table: true
    anvil: true
    villager-trade: true
  enchanting-table:
    weight: 16
  codex:
    enabled: true
    weight: 16
  effects:
    - trigger: TAKE_ENTITY_DAMAGE
      chance: "{level} * 2 + 8"
      cooldown: 160
      actions:
        - type: HEAL
          value: "{level} * 8"
        - type: ADD_POTION_SELF
          potion: DAMAGE_RESISTANCE
          amplifier: "min(1, floor(({level} - 1) / 3))"
          duration: "{level} * 30 + 10"
```

## AI Self-Check

Before returning a generated enchantment, check:

- The enchantment ID is lowercase snake_case.
- The file path category matches the item type.
- `enabled`, `max-level`, `rarity`, `group`, `item-groups` or `materials`, `obtain`, and `effects` exist.
- All trigger IDs are supported.
- All condition and action IDs are supported.
- Chance and value formulas use `{level}` where scaling is expected.
- The language files include both `name` and `description`.
- Descriptions mention chance, healing, duration, range, damage, drops, or potion levels when relevant.
- Do not invent fake config keys.
- Do not invent fake triggers, conditions, or actions.
- YAML indentation is valid.
