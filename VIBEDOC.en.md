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
- If `obtain.villager-trade: false`, `villager-trade-price-range` is ignored by the plugin. If villager trading is enabled or not explicitly disabled, `villager-trade-price-range` must be a two-integer list such as `[16, 40]` or a block list. Do not write `min` / `max`.
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

`cooldown` uses ticks. `20` ticks = `1` second. Use cooldowns for frequent triggers, area damage, explosions, repeated potion effects, and repeated particle/sound effects.

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
BLOCK_MINE_RADIUS
DROP_MULTIPLY_DROPS
```

If the desired trigger, condition, or action is not listed in this guide or in `VIBEDOC.md`, do not invent it. Choose the closest supported option and explain the compromise.

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
    - "Range: {radius}, duration: {seconds}s, level: {level}."
```

Common description placeholders:

| Placeholder | Source |
| --- | --- |
| `{level}` | Current enchantment level. |
| `{chance}` | First active `chance` condition at the current level. |
| `{value}` | First action `value` at the current level. |
| `{amount}` | Damage, healing, repair, or similar amount; falls back to action `value` when no `amount` param exists. |
| `{damage}` | Damage action value. |
| `{percent}` | Percent action value such as reduction, lifesteal, or thorns. |
| `{multiplier}` | Multiplier param or action value. |
| `{radius}` | `radius` param, or `max-blocks` for vein mining when no radius is set. |
| `{range}` | `range` param. |
| `{duration}` | Raw `duration` ticks. |
| `{seconds}` | `duration / 20`. |
| `{power}` | `power` param or launch strength. |
| `{max-blocks}` / `{max_blocks}` / `{blocks}` | Vein mining block count. |
| `{amplifier}` | Displayed potion level; config `amplifier: 0` displays as level 1. |
| `{potion}` | Raw potion type. |

When one enchantment has multiple values of the same kind, the plugin numbers them by encounter order in `effects`, from top to bottom and from the first action to the last action in each block. The unnumbered placeholder always means the first value:

| Placeholder | Meaning |
| --- | --- |
| `{chance}` / `{chance1}` | First `chance` condition. |
| `{chance2}` | Second `chance` condition. |
| `{chance3}` | Third `chance` condition. |
| `{seconds}` / `{seconds1}` | First calculated `duration / 20` value. |
| `{seconds2}` | Second calculated `duration / 20` value. |
| `{amount2}`, `{damage2}`, `{radius2}`, `{amplifier2}` | Second value of the same kind; later values follow the same rule. |

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

Extra action or condition params can also be referenced by the same key. For example, `range: "{level} + 3"` can be displayed with `{range}`. Hyphen and underscore forms are both accepted for the same key. Unknown placeholders stay visible, so do not invent placeholders that are not listed here and not present in the effect params.

Formulas support `{level}`, numbers, spaces, parentheses, and `+ - * /` only. Do not use functions such as `floor()`, `ceil()`, `min()`, or `max()`.

## Scope Boundary

This guide is only for generating enchantment YAML and language entries. Do not output Java source changes, engineering workflow steps, operations scripts, or unrelated project procedures unless the user explicitly asks for a task outside enchantment config writing.

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
