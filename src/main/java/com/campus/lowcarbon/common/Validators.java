package com.campus.lowcarbon.common;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 输入校验工具（防御性编程的核心工具类）。
 * <p>所有对外入口（Service 层）在写库前都必须经过这里，保证非法数据不会进入数据库。</p>
 */
public final class Validators {

    /** 用户名：3-20 位字母、数字或下划线。 */
    public static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{3,20}$");

    /** 物品标签分隔符。 */
    public static final String TAG_SEPARATOR = ",";

    private static final BigDecimal MAX_PRICE = new BigDecimal("99999.99");

    private Validators() {
        // 工具类禁止实例化
    }

    /**
     * 必填文本校验：去首尾空白后检查长度。
     *
     * @param value 原始文本
     * @param field 字段中文名，用于错误提示
     * @param min   最小长度（含）
     * @param max   最大长度（含）
     * @return 去空白后的文本
     */
    public static String requireText(String value, String field, int min, int max) {
        if (value == null) {
            throw new ValidationException(field + "不能为空");
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new ValidationException(field + "不能为空");
        }
        if (trimmed.length() < min || trimmed.length() > max) {
            throw new ValidationException(
                    field + "长度必须在 " + min + " 到 " + max + " 个字符之间（当前 " + trimmed.length() + "）");
        }
        return trimmed;
    }

    /**
     * 选填文本校验：允许为空，非空时检查最大长度。
     *
     * @return 去空白后的文本，null 转为空串
     */
    public static String optionalText(String value, String field, int max) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.length() > max) {
            throw new ValidationException(field + "长度不能超过 " + max + " 个字符");
        }
        return trimmed;
    }

    /** 用户名格式校验，返回去空白后的用户名。 */
    public static String requireUsername(String username) {
        String trimmed = requireText(username, "用户名", 3, 20);
        if (!USERNAME_PATTERN.matcher(trimmed).matches()) {
            throw new ValidationException("用户名只能由 3-20 位字母、数字或下划线组成");
        }
        return trimmed;
    }

    /** 口令强度校验：8-64 位，且同时包含字母与数字。 */
    public static String requirePassword(String rawPassword) {
        if (rawPassword == null || rawPassword.isEmpty()) {
            throw new ValidationException("密码不能为空");
        }
        if (rawPassword.length() < 8 || rawPassword.length() > 64) {
            throw new ValidationException("密码长度必须在 8 到 64 位之间");
        }
        boolean hasLetter = false;
        boolean hasDigit = false;
        for (int i = 0; i < rawPassword.length(); i++) {
            char c = rawPassword.charAt(i);
            if (Character.isLetter(c)) {
                hasLetter = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            } else if (Character.isWhitespace(c)) {
                throw new ValidationException("密码不能包含空格");
            }
        }
        if (!hasLetter || !hasDigit) {
            throw new ValidationException("密码必须同时包含字母和数字");
        }
        return rawPassword;
    }

    /** 价格校验：0 表示免费/赠送，最大 99999.99，最多两位小数。 */
    public static BigDecimal requirePrice(BigDecimal price) {
        if (price == null) {
            throw new ValidationException("价格不能为空");
        }
        if (price.signum() < 0) {
            throw new ValidationException("价格不能为负数");
        }
        if (price.compareTo(MAX_PRICE) > 0) {
            throw new ValidationException("价格不能超过 " + MAX_PRICE.toPlainString() + " 元");
        }
        if (price.scale() > 2) {
            throw new ValidationException("价格最多保留两位小数");
        }
        return price;
    }

    /** 把用户输入的文本解析为价格，解析失败抛出可读异常。 */
    public static BigDecimal parsePrice(String text) {
        String trimmed = requireText(text, "价格", 1, 12);
        BigDecimal value;
        try {
            value = new BigDecimal(trimmed);
        } catch (NumberFormatException ex) {
            throw new ValidationException("价格必须是数字，例如 12.50");
        }
        return requirePrice(value);
    }

    /** 主键校验：必须为正数。 */
    public static long requireId(long id, String field) {
        if (id <= 0) {
            throw new ValidationException(field + "非法：" + id);
        }
        return id;
    }

    /** 集合索引/分页参数校验。 */
    public static int requireRange(int value, String field, int min, int max) {
        if (value < min || value > max) {
            throw new ValidationException(field + "必须在 " + min + " 到 " + max + " 之间");
        }
        return value;
    }

    /** 标签规范化：去空白、去空项、去重、限制数量。 */
    public static String normalizeTags(String rawTags) {
        if (rawTags == null) {
            return "";
        }
        Set<String> tags = new LinkedHashSet<String>();
        for (String piece : rawTags.split("[,，;；\\s]+")) {
            String tag = piece.trim();
            if (tag.isEmpty()) {
                continue;
            }
            if (tag.length() > 12) {
                throw new ValidationException("单个标签不能超过 12 个字符：" + tag);
            }
            tags.add(tag);
        }
        if (tags.size() > 5) {
            throw new ValidationException("标签最多 5 个");
        }
        return String.join(TAG_SEPARATOR, tags);
    }

    /** 把入库的逗号分隔标签拆成列表。 */
    public static List<String> splitTags(String stored) {
        List<String> result = new ArrayList<String>();
        if (stored == null || stored.trim().isEmpty()) {
            return result;
        }
        for (String piece : stored.split(TAG_SEPARATOR)) {
            String tag = piece.trim();
            if (!tag.isEmpty()) {
                result.add(tag);
            }
        }
        return result;
    }
}
