package gg.fotia.enchantment.update;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class UpdateVersion {

    private UpdateVersion() {
    }

    public static boolean isNewer(String latestVersion, String currentVersion) {
        ParsedVersion latest = parse(latestVersion);
        ParsedVersion current = parse(currentVersion);

        int max = Math.max(latest.numbers().size(), current.numbers().size());
        for (int i = 0; i < max; i++) {
            int left = i < latest.numbers().size() ? latest.numbers().get(i) : 0;
            int right = i < current.numbers().size() ? current.numbers().get(i) : 0;
            if (left != right) {
                return left > right;
            }
        }

        if (latest.snapshot() != current.snapshot()) {
            return !latest.snapshot();
        }
        if (latest.prerelease() != current.prerelease()) {
            return !latest.prerelease();
        }
        return false;
    }

    private static ParsedVersion parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return new ParsedVersion(List.of(0), true, true);
        }

        String version = raw.trim().toLowerCase(Locale.ROOT);
        if (version.startsWith("v")) {
            version = version.substring(1);
        }

        boolean snapshot = version.contains("snapshot");
        int qualifierIndex = version.indexOf('-');
        boolean prerelease = qualifierIndex >= 0;
        String numericPart = qualifierIndex >= 0 ? version.substring(0, qualifierIndex) : version;

        List<Integer> numbers = new ArrayList<>();
        for (String part : numericPart.split("\\.")) {
            numbers.add(parseLeadingInt(part));
        }
        if (numbers.isEmpty()) {
            numbers.add(0);
        }
        return new ParsedVersion(List.copyOf(numbers), snapshot, prerelease);
    }

    private static int parseLeadingInt(String raw) {
        if (raw == null || raw.isBlank()) {
            return 0;
        }
        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (!Character.isDigit(c)) {
                break;
            }
            digits.append(c);
        }
        if (digits.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(digits.toString());
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private record ParsedVersion(List<Integer> numbers, boolean snapshot, boolean prerelease) {
    }
}
