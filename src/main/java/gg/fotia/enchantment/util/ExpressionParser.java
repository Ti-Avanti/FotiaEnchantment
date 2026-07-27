package gg.fotia.enchantment.util;

import java.util.Map;

/**
 * 数学表达式解析器
 * 支持变量替换和四则运算，使用递归下降解析器实现。
 * 不使用反射，不使用 ScriptEngine。
 */
public class ExpressionParser {

    /**
     * 计算包含变量的数学表达式
     *
     * @param expression 表达式字符串，可包含 {level}, {value}, {alt_value} 等变量
     * @param variables  变量映射表
     * @return 计算结果
     */
    public static double evaluate(String expression, Map<String, Double> variables) {
        if (expression == null || expression.trim().isEmpty()) {
            return 0;
        }

        // 替换变量
        String expr = replaceVariables(expression.trim(), variables);

        // 纯数字快速路径 (无异常控制流; 支持科学计数法, 变量替换可能产出 1.0E-7 类字符串)
        if (isPlainNumber(expr)) {
            return Double.parseDouble(expr);
        }

        // 使用递归下降解析器计算
        Parser parser = new Parser(expr);
        double result = parser.parseExpression();

        if (parser.pos < parser.input.length()) {
            throw new IllegalArgumentException("表达式解析错误，未预期的字符: '"
                    + parser.input.charAt(parser.pos) + "' 在位置 " + parser.pos);
        }

        return result;
    }

    /**
     * 无异常地判断字符串是否为纯数字 (含正负号、小数点与科学计数法)
     */
    private static boolean isPlainNumber(String s) {
        int length = s.length();
        if (length == 0) {
            return false;
        }
        int i = 0;
        char c = s.charAt(0);
        if (c == '+' || c == '-') {
            i++;
        }
        boolean digitSeen = false;
        boolean dotSeen = false;
        boolean expSeen = false;
        for (; i < length; i++) {
            c = s.charAt(i);
            if (c >= '0' && c <= '9') {
                digitSeen = true;
                continue;
            }
            if (c == '.' && !dotSeen && !expSeen) {
                dotSeen = true;
                continue;
            }
            if ((c == 'e' || c == 'E') && digitSeen && !expSeen) {
                expSeen = true;
                if (i + 1 < length && (s.charAt(i + 1) == '+' || s.charAt(i + 1) == '-')) {
                    i++;
                }
                digitSeen = false;
                continue;
            }
            return false;
        }
        return digitSeen;
    }

    /**
     * 替换表达式中的变量
     */
    private static String replaceVariables(String expression, Map<String, Double> variables) {
        if (variables == null || variables.isEmpty()) {
            return expression;
        }

        String result = expression;
        for (Map.Entry<String, Double> entry : variables.entrySet()) {
            String key = entry.getKey();
            // 支持 {variable} 格式
            String placeholder = "{" + key + "}";
            if (result.contains(placeholder)) {
                // 将变量值格式化，避免不必要的小数点
                double value = entry.getValue();
                String valueStr;
                if (value == Math.floor(value) && !Double.isInfinite(value)) {
                    valueStr = String.valueOf((long) value);
                } else {
                    valueStr = String.valueOf(value);
                }
                result = result.replace(placeholder, valueStr);
            }
        }
        return result;
    }

    /**
     * 递归下降解析器内部类
     */
    private static class Parser {
        final String input;
        int pos;

        Parser(String input) {
            this.input = input;
            this.pos = 0;
        }

        /**
         * 解析表达式（加减法）
         * expression = term (('+' | '-') term)*
         */
        double parseExpression() {
            double result = parseTerm();
            while (pos < input.length()) {
                skipWhitespace();
                if (pos >= input.length()) break;
                char op = input.charAt(pos);
                if (op == '+' || op == '-') {
                    pos++;
                    double term = parseTerm();
                    if (op == '+') {
                        result += term;
                    } else {
                        result -= term;
                    }
                } else {
                    break;
                }
            }
            return result;
        }

        /**
         * 解析项（乘除法）
         * term = factor (('*' | '/') factor)*
         */
        double parseTerm() {
            double result = parseFactor();
            while (pos < input.length()) {
                skipWhitespace();
                if (pos >= input.length()) break;
                char op = input.charAt(pos);
                if (op == '*' || op == '/') {
                    pos++;
                    double factor = parseFactor();
                    if (op == '*') {
                        result *= factor;
                    } else {
                        if (factor == 0) {
                            throw new ArithmeticException("除以零");
                        }
                        result /= factor;
                    }
                } else {
                    break;
                }
            }
            return result;
        }

        /**
         * 解析因子（数字或括号表达式）
         * factor = ['-'] (NUMBER | '(' expression ')')
         */
        double parseFactor() {
            skipWhitespace();

            // 处理负号
            if (pos < input.length() && input.charAt(pos) == '-') {
                pos++;
                return -parseFactor();
            }

            // 处理正号
            if (pos < input.length() && input.charAt(pos) == '+') {
                pos++;
                return parseFactor();
            }

            // 处理括号
            if (pos < input.length() && input.charAt(pos) == '(') {
                pos++; // 跳过 '('
                double result = parseExpression();
                skipWhitespace();
                if (pos < input.length() && input.charAt(pos) == ')') {
                    pos++; // 跳过 ')'
                } else {
                    throw new IllegalArgumentException("缺少右括号");
                }
                return result;
            }

            // 解析数字
            return parseNumber();
        }

        /**
         * 解析数字（整数、浮点数或科学计数法）
         */
        double parseNumber() {
            skipWhitespace();
            int start = pos;
            while (pos < input.length() && (Character.isDigit(input.charAt(pos)) || input.charAt(pos) == '.')) {
                pos++;
            }
            // 科学计数法后缀: 1.0E-7 / 2e3 (变量替换可能产出此类字符串)
            if (pos > start && pos < input.length()
                    && (input.charAt(pos) == 'e' || input.charAt(pos) == 'E')) {
                int probe = pos + 1;
                if (probe < input.length() && (input.charAt(probe) == '+' || input.charAt(probe) == '-')) {
                    probe++;
                }
                int digits = probe;
                while (digits < input.length() && Character.isDigit(input.charAt(digits))) {
                    digits++;
                }
                if (digits > probe) {
                    pos = digits;
                }
            }
            if (start == pos) {
                throw new IllegalArgumentException("表达式解析错误，预期数字在位置 " + pos);
            }
            return Double.parseDouble(input.substring(start, pos));
        }

        /**
         * 跳过空白字符
         */
        void skipWhitespace() {
            while (pos < input.length() && Character.isWhitespace(input.charAt(pos))) {
                pos++;
            }
        }
    }
}
