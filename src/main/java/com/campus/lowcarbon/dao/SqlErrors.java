package com.campus.lowcarbon.dao;

/** SQL 异常识别工具：把底层驱动错误翻译成业务可理解的异常类型。 */
public final class SqlErrors {

    private SqlErrors() {
    }

    /** 是否为唯一约束冲突（如用户名、分类名重复）。 */
    public static boolean isUniqueViolation(Throwable error) {
        Throwable current = error;
        while (current != null) {
            String message = current.getMessage();
            if (message != null) {
                String lower = message.toLowerCase();
                if (lower.contains("unique constraint failed")
                        || lower.contains("constraint failed: unique")
                        || lower.contains("sqlite_constraint_unique")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    /** 是否为外键约束失败。 */
    public static boolean isForeignKeyViolation(Throwable error) {
        Throwable current = error;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.toLowerCase().contains("foreign key constraint failed")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
