package gg.fotia.enchantment.core;

import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

public final class EnchantmentConflictPolicy {

    private EnchantmentConflictPolicy() {
    }

    public static boolean hasCustomConflict(String candidateId,
                                            EnchantmentData candidate,
                                            Collection<String> existingIds,
                                            Function<String, EnchantmentData> dataResolver) {
        if (candidate == null || existingIds == null || existingIds.isEmpty()) {
            return false;
        }
        String normalizedCandidateId = normalizeCustomId(candidateId == null ? candidate.getId() : candidateId);
        if (normalizedCandidateId.isEmpty()) {
            return false;
        }

        for (String existingId : existingIds) {
            String normalizedExistingId = normalizeCustomId(existingId);
            if (normalizedExistingId.isEmpty() || normalizedCandidateId.equals(normalizedExistingId)) {
                continue;
            }
            if (referencesCustom(candidate.getConflicts(), normalizedExistingId)) {
                return true;
            }
            EnchantmentData existing = dataResolver == null ? null : dataResolver.apply(normalizedExistingId);
            if (existing != null && referencesCustom(existing.getConflicts(), normalizedCandidateId)) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasCustomConflict(String candidateId,
                                            EnchantmentData candidate,
                                            Map<String, Integer> existing,
                                            Function<String, EnchantmentData> dataResolver) {
        return existing != null
                && hasCustomConflict(candidateId, candidate, existing.keySet(), dataResolver);
    }

    public static boolean referencesCustom(Collection<String> references, String customId) {
        if (references == null || references.isEmpty()) {
            return false;
        }
        String normalizedTarget = normalizeCustomId(customId);
        if (normalizedTarget.isEmpty()) {
            return false;
        }
        for (String reference : references) {
            if (normalizedTarget.equals(normalizeCustomId(reference))) {
                return true;
            }
        }
        return false;
    }

    public static boolean referencesBukkit(Collection<String> references, Enchantment enchantment) {
        if (references == null || references.isEmpty() || enchantment == null || enchantment.getKey() == null) {
            return false;
        }
        NamespacedKey key = enchantment.getKey();
        String fullKey = key.toString().toLowerCase(Locale.ROOT);
        String simpleKey = key.getKey().toLowerCase(Locale.ROOT);
        for (String reference : references) {
            String normalized = normalizeRaw(reference);
            if (normalized.isEmpty()) {
                continue;
            }
            if (normalized.contains(":")) {
                if (normalized.equals(fullKey)) {
                    return true;
                }
            } else if (normalized.equals(simpleKey)) {
                return true;
            }
        }
        return false;
    }

    public static String normalizeCustomId(String id) {
        String normalized = normalizeRaw(id);
        int separator = normalized.indexOf(':');
        if (separator > 0 && separator < normalized.length() - 1
                && EnchantmentRegistry.getNamespace().equals(normalized.substring(0, separator))) {
            return normalized.substring(separator + 1);
        }
        return normalized;
    }

    private static String normalizeRaw(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
