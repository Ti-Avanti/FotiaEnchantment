package gg.fotia.enchantment.item;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class DisenchantTargetSelector {

    private DisenchantTargetSelector() {
    }

    static List<DisenchantTarget> select(List<DisenchantTarget> available,
                                         boolean selectable,
                                         Collection<String> selectedKeys,
                                         int maxRemove,
                                         boolean shuffle) {
        if (available == null || available.isEmpty() || maxRemove <= 0) {
            return List.of();
        }

        List<DisenchantTarget> selected;
        if (!selectable) {
            selected = new ArrayList<>(available);
            if (shuffle) {
                Collections.shuffle(selected);
            }
        } else {
            selected = selectedTargets(available, selectedKeys);
        }

        if (selected.size() > maxRemove) {
            return new ArrayList<>(selected.subList(0, maxRemove));
        }
        return selected;
    }

    private static List<DisenchantTarget> selectedTargets(List<DisenchantTarget> available,
                                                         Collection<String> selectedKeys) {
        if (selectedKeys == null || selectedKeys.isEmpty()) {
            return List.of();
        }

        Map<String, DisenchantTarget> bySelectionKey = new LinkedHashMap<>();
        for (DisenchantTarget target : available) {
            bySelectionKey.put(target.selectionKey(), target);
            if (target.type() == DisenchantTargetType.FOTIA) {
                bySelectionKey.putIfAbsent(target.id(), target);
            }
        }

        List<DisenchantTarget> selected = new ArrayList<>();
        for (String selectionKey : selectedKeys) {
            DisenchantTarget target = bySelectionKey.get(selectionKey);
            if (target != null) {
                selected.add(target);
            }
        }
        return selected;
    }
}
