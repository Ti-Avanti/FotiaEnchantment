# FotiaEnchantment AI Enchantment Writing Guide

This file is for AI agents that generate FotiaEnchantment configuration. The goal is to produce YAML that the current plugin can load, not to describe hypothetical features.

`VIBEDOC.md` is the canonical full guide. This English file must follow the same schema and must not reintroduce legacy keys.

## Output Target

When a user asks for a new enchantment, usually output these files or snippets:

1. Enchantment config: `enchantments/<category>/<enchantment_id>.yml`
2. Simplified Chinese language entry: `lang/zh_cn/enchantments.yml`
3. English language entry: `lang/en_us/enchantments.yml`

Output YAML snippets by file path. Keep YAML as UTF-8 without BOM.

## Hard Failure Rules

- Do not invent config keys, triggers, conditions, actions, item categories, rarity IDs, group IDs, or enchantment IDs.
- Do not use legacy keys: `item-groups`, `materials`, `enchanting-table.weight`, `codex.enabled`, `codex.weight`, or effect-level `chance`.
- Do not wrap the file under a top-level `enchantment_id:` key. The file root is the enchantment object.
- Use `applicable-items`, not `item-groups` or `materials`.
- Use `codex-pools`, not `codex.enabled` or `codex.weight`.
- Put probability in `effects[].conditions` with `type: chance`; do not put `chance` directly in the effect block.
- Prefer root-level `enchanting-table-weight` and `villager-trade-price-range` in new configs. The loader accepts legacy nested `obtain.enchanting-table-weight` and `obtain.villager-trade-price-range`, but do not generate that style.
- If `obtain.villager-trade: false`, `villager-trade-price-range` is ignored by the plugin. If villager trading is enabled or not explicitly disabled, `villager-trade-price-range` must be a two-integer list such as `[16, 40]` or a YAML block list. Do not write `min` / `max`.
- `conflicts` may only reference existing enchantment IDs, IDs generated in the same answer, or IDs the user explicitly told you to keep. If unsure, write `conflicts: []`.

## Log Triage Boundary

When a user provides a server log and asks whether it is an enchantment config issue, separate config errors from plugin jar or Paper remap cache errors first.

- If the log contains `[FotiaEnchantment] 配置错误`, `位置: <config path>`, `未定义附魔`, or `conflicts 引用了未定义附魔`, inspect the enchantment YAML using this guide.
- If the log contains `NoClassDefFoundError` or `ClassNotFoundException` for a class under `gg.fotia.enchantment.`, such as `TriggerContext`, `MenuConfig`, `PacketEventsHook`, or `FePaperCommand`, do not tell the user to edit enchantment YAML. This usually means the server loaded an incomplete plugin jar, the wrong jar, an abnormal filename, or a stale/broken Paper `.paper-remapped` cache.
- If the jar name in the log looks like `FotiaEnchantment-1.0.6 .jar` with an extra space after the version, tell the user to stop the server, delete the bad jar and `plugins/.paper-remapped/FotiaEnchantment*.jar`, then install the official `FotiaEnchantment-<version>.jar`. The extra space is not itself a YAML config error.
- Do not explain missing internal plugin classes as bad triggers, conditions, actions, language entries, or enchantment config.

## Naming Rules

- Enchantment IDs must be lowercase snake_case, for example `resilience` or `chain_miner`.
- File name should match the enchantment ID, for example `resilience.yml`.
- Do not create two enchantments with the same ID.
- Avoid spaces, Chinese characters, punctuation, and uppercase letters in IDs.
- Language text may use MiniMessage. The plugin accepts legacy `&` and `§` codes, but prefer plain text unless colors are requested.

## Current YAML Template

```yaml
id: example_enchant
enabled: true
curse: false
max-level: 5
rarity: radiant
group: offensive
category: melee

applicable-items:
  - SWORD
  - AXE

conflicts: []

obtain:
  enchanting-table: true
  anvil: true
  villager-trade: true

enchanting-table-weight: 10
villager-trade-price-range:
  - 16
  - 40

codex-pools:
  radiant: 10
  aureate: 4

effects:
  - trigger: MELEE_ATTACK
    cooldown: 60
    cooldown-levels:
      1: 100
      3: 60
      5: 40
    conditions:
      - type: chance
        value: "{level} * 8"
    actions:
      - type: DAMAGE_ADD
        value: "{level} * 1.5"
```

## Required And Common Fields

`id`: unique lowercase enchantment ID. Prefer matching the file name.

`enabled`: `true` or `false`.

`curse`: `true` only for curse enchantments. Use `false` for normal enchantments.

`max-level`: maximum enchantment level, at least `1`.

`rarity`: prefer one of `dustlight`, `moonlit`, `radiant`, `aureate`, `divine`.

`group`: prefer one of `fire`, `ice`, `lightning`, `defensive`, `offensive`, `utility`, `movement`, `mining`.

`category`: one of `melee`, `ranged`, `armor`, `tools`, `universal`. The file path should match this category.

`applicable-items`: item categories or Bukkit material names. Prefer categories unless the user asks for exact materials.

Common categories:

```text
SWORD
AXE
PICKAXE
SHOVEL
HOE
BOW
CROSSBOW
TRIDENT
FISHING_ROD
SHIELD
ELYTRA
HELMET
CHESTPLATE
LEGGINGS
BOOTS
```

`conflicts`: list of real conflicting enchantment IDs. Empty list is valid.

`obtain`: acquisition switches. Use booleans for `enchanting-table`, `anvil`, and `villager-trade`.

`enchanting-table-weight`: integer weight. Higher means more common.

`villager-trade-price-range`: two integers: minimum and maximum emerald cost. It must satisfy `0 <= min <= max` when villager trading is enabled or not explicitly disabled. When `obtain.villager-trade: false`, omit this field when possible; an empty list is tolerated because the plugin ignores it.

Keep `enchanting-table-weight` and `villager-trade-price-range` at the file root in new examples. Nested `obtain.*` versions are only compatibility input.

`codex-pools`: map from rarity ID to integer weight.

## Effect Pipeline

Each effect block must have a `trigger` and at least one action:

```yaml
effects:
  - trigger: TRIGGER_ID
    cooldown: 100
    conditions:
      - type: chance
        value: "{level} * 5"
    actions:
      - type: ACTION_ID
        value: "{level} * 2"
```

`cooldown`, `cooldown-levels`, `cooldown-formula`, and `duration` use ticks. `20` ticks = `1` second. Use cooldowns for frequent triggers, area damage, explosions, repeated potion effects, and repeated particle/sound effects.

Use `cooldown-levels` when exact levels need different cooldowns:

```yaml
cooldown: 100
cooldown-levels:
  1: 100
  3: 60
  5: 40
```

Use `cooldown-formula` for formula-based scaling:

```yaml
cooldown: 100
cooldown-formula: "{level} * 20"
```

Runtime priority is exact current level in `cooldown-levels`, then `cooldown-formula`, then fixed `cooldown`.

Action params must be siblings of `type` and `value`, such as `potion`, `duration`, or `multiplier`. Do not wrap them under `extra-params`; the loader does not expand that field.

Conditions use lowercase IDs such as:

```text
chance
health_below
health_percent_below
health_percent_above
in_biome
time
weather
altitude
in_water
in_world
permission
target_is_player
target_is_living
target_health_percent_below
target_distance_below
players_nearby
cooldown_ready
is_sneaking
is_sprinting
is_gliding
mainhand_is
item_durability_below
```

Actions use uppercase IDs such as:

```text
DAMAGE_ADD
DAMAGE_MULTIPLY
DAMAGE_REDUCE
TRUE_DAMAGE
HEAL
LIFESTEAL
DODGE
THORNS
ADD_POTION_SELF
ADD_POTION_TARGET
REMOVE_POTION
BONUS_DROP
SMELT
VEIN_MINE
PARTICLE
SOUND
EXPLODE
LIGHTNING
IGNITE_TARGET
HELD_ITEM_REPAIR
BLOCK_EXPLOSION
BLOCK_LIGHTNING
NEARBY_ENEMY_PUSH
```

If the desired trigger, condition, or action is not listed in this guide or in `VIBEDOC.md`, do not invent it. Choose the closest supported option and explain the compromise.

Important runtime constraints:

- `BONUS_DROP` requires `multiplier` and only modifies real dropped item entities from `BLOCK_ITEM_DROP`; do not place it under `MINE_BLOCK` or `MINE_ORE`.
- `EXPLODE` uses the target location, or the player location when there is no target. Use `BLOCK_EXPLOSION` for a block-context explosion.
- `LIGHTNING` requires a target. Use `BLOCK_LIGHTNING` for block-context lightning.
- `PARTICLE` and `SOUND` only distinguish `at: SELF` and `at: TARGET`; `at: BLOCK` is not supported.

## Language Entries

At minimum add `zh_cn` and `en_us` entries. If the user requests full built-in language coverage, also add `zh_tw`, `ja_jp`, and `ko_kr`.

```yaml
example_enchant:
  name: "Example Enchant"
  description:
    - "Melee attacks have a chance to deal bonus damage."
```

Descriptions should explain the player-visible result. Do not describe config keys. If the effect has chance, duration, range, damage, healing, drop multiplier, potion amplifier, or cooldown gameplay impact, include that information in a player-friendly way.

Descriptions are rendered dynamically. The plugin prefers the language file `description` for item lore and guide text, and only falls back to generated effect text when the language description is empty.

Use placeholders instead of hardcoding level-scaled values:

```yaml
example_enchant:
  name: "Example Enchant"
  description:
    - "Attacks have a {chance}% chance to deal {amount} bonus damage."
    - "Cooldown: {cooldown_seconds}s, range: {range}, duration: {seconds}s."
```

Description placeholders:

| Placeholder | Source |
| --- | --- |
| `{level}` | Current enchantment level. |
| `{cooldown}` / `{cooldown_ticks}` / `{cooldown-ticks}` | Raw root-level effect-block `cooldown` ticks. |
| `{cooldown_seconds}` / `{cooldown-seconds}` | Root-level effect-block `cooldown / 20` in seconds. |
| `{chance}` | `value` from a `chance` condition, interpreted as percent. |
| `{value}` | Root action `value`. |
| `{duration}` | Raw action param `duration` ticks. |
| `{seconds}` | Action param `duration / 20`; `IGNITE_TARGET` derives this from action `value`. |
| `{amount}` | Damage, healing, repair, or similar amount; generated from action `value` for `DAMAGE_ADD`, `TRUE_DAMAGE`, `HEAL`, `HELD_ITEM_REPAIR`, and unknown actions. |
| `{damage}` | Damage amount; generated from action `value` for `DAMAGE_ADD`, `TRUE_DAMAGE`, and unknown actions. |
| `{percent}` | Percent value; generated from action `value` for `DAMAGE_REDUCE`, `LIFESTEAL`, and `THORNS`; `HEAL` converts `value * 5` to a health percent. |
| `{multiplier}` | Multiplier value; can come from a `multiplier` param, or from action `value` for `DAMAGE_MULTIPLY` / `BONUS_DROP`. |
| `{power}` | Strength value; can come from a `power` param, or from action `value` for `LAUNCH`. |
| `{amplifier}` | Displayed potion or speed level; `ADD_POTION_SELF` / `ADD_POTION_TARGET` display config `amplifier: 0` as level 1, and `SPEED_BOOST` derives `value + 1`. |
| `{potion}` | Raw potion param. |
| `{radius}` | Action or condition param `radius`; `max-blocks` also generates the same value as `radius`. |
| `{range}` | Action or condition param `range`, commonly used for line distance, search radius, or custom area size. |
| `{max-blocks}` / `{max_blocks}` / `{blocks}` | Vein mining block count. |
| `{<param_name>}` | Any action param or condition extra param can be referenced by the same name, such as `distance`, `key`, `weight`, `total`, `required`, `material`, `permission`, or `range`. |

When one enchantment has multiple values of the same kind, the plugin numbers them by encounter order in `effects`, from top to bottom and from the first action to the last action in each block. The unnumbered placeholder always means the first value:

| Placeholder | Meaning |
| --- | --- |
| `{chance}` / `{chance1}` | First `chance` condition. |
| `{chance2}` | Second `chance` condition. |
| `{chance3}` | Third `chance` condition. |
| `{value}` / `{value1}` | First action `value`. |
| `{value2}` | Second action `value`. |
| `{seconds}` / `{seconds1}` | First calculated `duration / 20` value. |
| `{seconds2}` | Second calculated `duration / 20` value. |
| `{cooldown_seconds}` / `{cooldown_seconds1}` | First cooldown value converted to seconds. |
| `{cooldown_seconds2}` | Second cooldown value converted to seconds. |
| `{amount2}`, `{damage2}`, `{radius2}`, `{range2}`, `{multiplier2}`, `{amplifier2}`, `{potion2}` | Second value of the same kind; later values follow the same rule. |

For an enchantment with three independent probabilities, write each description line against the matching numbered placeholder instead of reusing `{chance}` for every line:

```yaml
sacred_blessing:
  name: "Sacred Blessing"
  description:
    - "Jumping has a {chance}% chance to grant Regeneration I for {seconds}s."
    - "Taking damage has a {chance2}% chance to grant Instant Health II."
    - "Taking damage has a {chance3}% chance to grant Resistance I for {seconds2}s."
```

If one effect has no `duration`, it does not consume a `{secondsN}` slot. Numbering only counts values that actually exist and can be calculated. Keep description numbering aligned with the order of the real `effects` entries.

Effect cooldown can also be used in descriptions: `{cooldown}` renders the current level's resolved ticks and `{cooldown_seconds}` renders the resolved seconds. If `cooldown-levels` or `cooldown-formula` is configured, placeholders use the current level's resolved cooldown. Extra action or condition params can also be referenced by the same key. For example, `range: "{level} + 3"` can be displayed with `{range}`. Hyphen and underscore forms are both accepted for the same key, for example `{max-blocks}` / `{max_blocks}` and `{cooldown-seconds}` / `{cooldown_seconds}`. Unknown placeholders stay visible, so do not invent placeholders that are not listed here and not present in the effect params.

Formulas support `{level}`, numbers, spaces, parentheses, and `+ - * /` only. Do not use functions such as `floor()`, `ceil()`, `min()`, or `max()`.

## Scope Boundary

This guide is only for generating enchantment YAML and language entries. Do not output Java source changes, engineering workflow steps, operations scripts, or unrelated project procedures unless the user explicitly asks for a task outside enchantment config writing.

The anvil breakthrough stone and the "too expensive" bypass GUI are system custom-item / GUI configs, not enchantment YAML fields. They are controlled by `items/custom-items.yml` entry `anvil-breakthrough-stone` and `gui/anvil-breakthrough.yml`. Unless the user explicitly asks to change system items or GUI layout, do not invent enchantment fields such as `breakthrough`, `too-expensive`, or `anvil-gui`.

## AI Self-Check

Before returning or writing an enchantment, check:

- File path category, `id`, file name, and language key match.
- No legacy keys are present.
- `rarity`, `group`, `category`, and `applicable-items` are valid.
- Every `conflicts` entry is a known enchantment ID; otherwise use `conflicts: []`.
- If villager trading is enabled or not explicitly disabled, `villager-trade-price-range` is a two-integer list, not a `min/max` map. If `obtain.villager-trade: false`, the field is omitted or known to be ignored.
- Every effect block has `trigger`.
- Every effect block has at least one `actions` entry.
- Trigger, condition, and action IDs are supported by this guide or `VIBEDOC.md`.
- Chance is written as a `chance` condition when probability is part of the design.
- Formulas use `{level}` where scaling is expected.
- Language files include both `name` and `description`.
- YAML indentation is valid.
- The answer stays focused on enchantment config and language entries, with no unrelated engineering workflow mixed in.
