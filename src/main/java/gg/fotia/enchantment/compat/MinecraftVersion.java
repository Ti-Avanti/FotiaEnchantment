package gg.fotia.enchantment.compat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record MinecraftVersion(int major, int minor, int patch) implements Comparable<MinecraftVersion> {

    private static final Pattern VERSION_PATTERN = Pattern.compile("^(\\d+)\\.(\\d+)(?:\\.(\\d+))?.*");
    private static final MinecraftVersion UNKNOWN = new MinecraftVersion(0, 0, 0);

    public static MinecraftVersion parse(String raw) {
        if (raw == null) {
            return UNKNOWN;
        }
        Matcher matcher = VERSION_PATTERN.matcher(raw.trim());
        if (!matcher.matches()) {
            return UNKNOWN;
        }
        try {
            return new MinecraftVersion(Integer.parseInt(matcher.group(1)),
                    Integer.parseInt(matcher.group(2)),
                    matcher.group(3) == null ? 0 : Integer.parseInt(matcher.group(3)));
        } catch (NumberFormatException ignored) {
            return UNKNOWN;
        }
    }

    @Override
    public int compareTo(MinecraftVersion other) {
        int result = Integer.compare(major, other.major);
        if (result == 0) result = Integer.compare(minor, other.minor);
        return result == 0 ? Integer.compare(patch, other.patch) : result;
    }
}
