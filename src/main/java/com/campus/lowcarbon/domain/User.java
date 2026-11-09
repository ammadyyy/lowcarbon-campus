package com.campus.lowcarbon.domain;

import java.time.LocalDateTime;
import java.util.Objects;

import com.campus.lowcarbon.common.AuthException;
import com.campus.lowcarbon.common.ValidationException;

/**
 * 用户实体（不可变对象，所有"修改"都返回新实例，避免并发下的脏写）。
 * <p>ADT 不变量：用户名符合 3-20 位字母数字下划线；口令只以散列形式存在；信用分 0-100。</p>
 */
public final class User {

    /** 新注册用户的初始信用分。 */
    public static final int INITIAL_CREDIT = 100;

    private final long id;
    private final String username;
    private final String passwordHash;
    private final Role role;
    private final int creditScore;
    private final String contact;
    private final boolean banned;
    private final LocalDateTime createdAt;

    private User(Builder builder) {
        this.id = builder.id;
        this.username = builder.username;
        this.passwordHash = builder.passwordHash;
        this.role = builder.role;
        this.creditScore = builder.creditScore;
        this.contact = builder.contact;
        this.banned = builder.banned;
        this.createdAt = builder.createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public int getCreditScore() {
        return creditScore;
    }

    public String getContact() {
        return contact;
    }

    public boolean isBanned() {
        return banned;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isAdmin() {
        return role.isAdmin();
    }

    /** 已持久化的用户才允许执行写操作。 */
    public boolean isPersisted() {
        return id > 0;
    }

    /**
     * 校验账号可用性：被封禁的账号不能登录，也不能发布/修改物品。
     *
     * @throws AuthException 账号被封禁
     */
    public User ensureActive() {
        if (banned) {
            throw new AuthException("账号 " + username + " 已被封禁，请联系管理员");
        }
        return this;
    }

    /** 信用分增减，结果裁剪到 [0, 100]。 */
    public User adjustCredit(int delta) {
        int next = Math.max(0, Math.min(100, creditScore + delta));
        return toBuilder().creditScore(next).build();
    }

    public User withContact(String newContact) {
        return toBuilder().contact(newContact).build();
    }

    public User withRole(Role newRole) {
        return toBuilder().role(newRole).build();
    }

    public User withBanned(boolean value) {
        return toBuilder().banned(value).build();
    }

    public User withPasswordHash(String newHash) {
        return toBuilder().passwordHash(newHash).build();
    }

    public Builder toBuilder() {
        return new Builder()
                .id(id)
                .username(username)
                .passwordHash(passwordHash)
                .role(role)
                .creditScore(creditScore)
                .contact(contact)
                .banned(banned)
                .createdAt(createdAt);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof User)) {
            return false;
        }
        User that = (User) other;
        return username.equals(that.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username);
    }

    @Override
    public String toString() {
        return "User{id=" + id + ", username='" + username + "', role=" + role
                + ", credit=" + creditScore + ", banned=" + banned + '}';
    }

    /** 用户构造器（Builder 模式）：集中校验，避免出现非法实体。 */
    public static final class Builder {

        private long id;
        private String username;
        private String passwordHash;
        private Role role = Role.STUDENT;
        private int creditScore = INITIAL_CREDIT;
        private String contact = "";
        private boolean banned;
        private LocalDateTime createdAt = LocalDateTime.now();

        public Builder id(long id) {
            this.id = id;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder passwordHash(String passwordHash) {
            this.passwordHash = passwordHash;
            return this;
        }

        public Builder role(Role role) {
            this.role = role;
            return this;
        }

        public Builder creditScore(int creditScore) {
            this.creditScore = creditScore;
            return this;
        }

        public Builder contact(String contact) {
            this.contact = contact;
            return this;
        }

        public Builder banned(boolean banned) {
            this.banned = banned;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public User build() {
            if (username == null || !username.matches("^[A-Za-z0-9_]{3,20}$")) {
                throw new ValidationException("用户名只能由 3-20 位字母、数字或下划线组成");
            }
            if (passwordHash == null || passwordHash.trim().isEmpty()) {
                throw new ValidationException("用户口令散列不能为空");
            }
            if (role == null) {
                throw new ValidationException("用户角色不能为空");
            }
            if (creditScore < 0 || creditScore > 100) {
                throw new ValidationException("信用分必须在 0 到 100 之间");
            }
            if (createdAt == null) {
                throw new ValidationException("创建时间不能为空");
            }
            return new User(this);
        }
    }
}
