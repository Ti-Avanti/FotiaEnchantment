package gg.fotia.enchantment.gui.menu;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MenuLayout {

    private final Map<Character, List<Integer>> slotsByChar;
    private final int rows;

    private MenuLayout(Map<Character, List<Integer>> slotsByChar, int rows) {
        this.slotsByChar = slotsByChar;
        this.rows = rows;
    }

    public static MenuLayout from(ConfigurationSection root) {
        List<String> lines = root == null ? Collections.emptyList() : root.getStringList("layout");
        Map<Character, List<Integer>> slotsByChar = new HashMap<>();
        int row = 0;
        for (String line : lines) {
            if (line == null) {
                row++;
                continue;
            }
            int max = Math.min(9, line.length());
            for (int column = 0; column < max; column++) {
                char symbol = line.charAt(column);
                if (symbol == ' ') {
                    continue;
                }
                slotsByChar.computeIfAbsent(symbol, ignored -> new ArrayList<>()).add(row * 9 + column);
            }
            row++;
        }

        Map<Character, List<Integer>> immutable = new HashMap<>();
        for (Map.Entry<Character, List<Integer>> entry : slotsByChar.entrySet()) {
            immutable.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return new MenuLayout(Map.copyOf(immutable), Math.max(0, lines.size()));
    }

    public int rows() {
        return rows;
    }

    public List<Integer> slots(String symbols) {
        if (symbols == null || symbols.isBlank()) {
            return Collections.emptyList();
        }
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < symbols.length(); i++) {
            slots.addAll(slotsByChar.getOrDefault(symbols.charAt(i), Collections.emptyList()));
        }
        return List.copyOf(slots);
    }
}
