package gg.fotia.enchantment.util;

import java.util.Map;

public final class ExpressionPredicate {

    private static final double EPSILON = 0.0000001D;

    private ExpressionPredicate() {
    }

    public static boolean evaluate(String expression, Map<String, Double> variables) {
        if (expression == null || expression.isBlank()) {
            return false;
        }

        String trimmed = expression.trim();
        OperatorMatch match = findOperator(trimmed);
        if (match == null) {
            return ExpressionParser.evaluate(trimmed, variables) > 0.0D;
        }

        double left = ExpressionParser.evaluate(trimmed.substring(0, match.index()).trim(), variables);
        double right = ExpressionParser.evaluate(trimmed.substring(match.index() + match.operator().length()).trim(), variables);
        return switch (match.operator()) {
            case ">=" -> left >= right || nearlyEqual(left, right);
            case "<=" -> left <= right || nearlyEqual(left, right);
            case ">" -> left > right;
            case "<" -> left < right;
            case "==" -> nearlyEqual(left, right);
            case "!=" -> !nearlyEqual(left, right);
            default -> false;
        };
    }

    private static OperatorMatch findOperator(String expression) {
        int depth = 0;
        for (int i = 0; i < expression.length(); i++) {
            char current = expression.charAt(i);
            if (current == '(') {
                depth++;
                continue;
            }
            if (current == ')') {
                depth = Math.max(0, depth - 1);
                continue;
            }
            if (depth != 0) {
                continue;
            }

            String two = i + 1 < expression.length() ? expression.substring(i, i + 2) : "";
            if (two.equals(">=") || two.equals("<=") || two.equals("==") || two.equals("!=")) {
                return new OperatorMatch(i, two);
            }
            if (current == '>' || current == '<') {
                return new OperatorMatch(i, String.valueOf(current));
            }
        }
        return null;
    }

    private static boolean nearlyEqual(double left, double right) {
        return Math.abs(left - right) < EPSILON;
    }

    private record OperatorMatch(int index, String operator) {
    }
}
