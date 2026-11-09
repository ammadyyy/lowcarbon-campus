package com.campus.lowcarbon.domain;

import com.campus.lowcarbon.common.ValidationException;

/** 用户角色。 */
public enum Role {

    STUDENT("STUDENT", "普通学生用户"),
    ADMIN("ADMIN", "系统管理员");

    private final String code;
    private final String label;

    Role(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    public boolean isAdmin() {
        return this == ADMIN;
    }

    /** 由数据库存储的编码还原枚举，非法编码直接拒绝（防御性编程）。 */
    public static Role fromCode(String code) {
        for (Role role : values()) {
            if (role.code.equals(code)) {
                return role;
            }
        }
        throw new ValidationException("非法的用户角色：" + code);
    }

    @Override
    public String toString() {
        return label;
    }
}
