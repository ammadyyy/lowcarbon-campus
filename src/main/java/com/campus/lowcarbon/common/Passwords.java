package com.campus.lowcarbon.common;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * 口令安全工具：PBKDF2-HMAC-SHA256 加盐散列。
 * <p>
 * 数据库中只保存 {@code pbkdf2$迭代次数$盐$摘要} 形式的编码串，
 * 不保存任何明文或可逆密文；校验时使用常量时间比较，避免时序攻击。
 * </p>
 */
public final class Passwords {

    /** 编码串前缀，便于将来平滑升级算法。 */
    public static final String PREFIX = "pbkdf2";

    /** 默认迭代次数（OWASP 2023 对 PBKDF2-HMAC-SHA256 的建议区间下限）。 */
    public static final int DEFAULT_ITERATIONS = 120_000;

    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final int iterations;

    public Passwords() {
        this(DEFAULT_ITERATIONS);
    }

    /**
     * @param iterations 迭代次数，测试中可传入较小值以缩短耗时，生产环境不应低于 10000
     */
    public Passwords(int iterations) {
        if (iterations < 1) {
            throw new ValidationException("口令散列迭代次数必须为正数");
        }
        this.iterations = iterations;
    }

    /**
     * 生成口令散列。
     *
     * @param rawPassword 明文口令
     * @return 散列结果（含盐与迭代次数）
     */
    public HashedPassword hash(String rawPassword) {
        if (rawPassword == null || rawPassword.isEmpty()) {
            throw new ValidationException("口令不能为空");
        }
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] derived = derive(rawPassword.toCharArray(), salt, iterations);
        Base64.Encoder encoder = Base64.getEncoder();
        return new HashedPassword(
                PREFIX + "$" + iterations
                        + "$" + encoder.encodeToString(salt)
                        + "$" + encoder.encodeToString(derived));
    }

    /**
     * 校验明文口令与数据库编码串是否匹配。
     *
     * @param rawPassword 明文口令
     * @param encoded     数据库中的编码串，允许为 null 或格式非法（返回 false）
     * @return 是否匹配
     */
    public boolean matches(String rawPassword, String encoded) {
        if (rawPassword == null || encoded == null) {
            return false;
        }
        String[] parts = encoded.split("\\$");
        if (parts.length != 4 || !PREFIX.equals(parts[0])) {
            return false;
        }
        try {
            int storedIterations = Integer.parseInt(parts[1]);
            Base64.Decoder decoder = Base64.getDecoder();
            byte[] salt = decoder.decode(parts[2]);
            byte[] expected = decoder.decode(parts[3]);
            byte[] actual = derive(rawPassword.toCharArray(), salt, storedIterations);
            return MessageDigest.isEqual(expected, actual);
        } catch (IllegalArgumentException ex) {
            // 编码串非法（Base64 解析失败或迭代次数不是数字）：视为不匹配，不抛异常
            return false;
        }
    }

    /** 判断字符串是否为本类生成的合法编码串（用于数据迁移与自检）。 */
    public static boolean isEncoded(String value) {
        if (value == null) {
            return false;
        }
        String[] parts = value.split("\\$");
        if (parts.length != 4 || !PREFIX.equals(parts[0])) {
            return false;
        }
        try {
            Integer.parseInt(parts[1]);
            Base64.getDecoder().decode(parts[2]);
            Base64.getDecoder().decode(parts[3]);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private static byte[] derive(char[] password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, KEY_BITS);
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return factory.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
            throw new IllegalStateException("当前 JDK 不支持 PBKDF2WithHmacSHA256", ex);
        } finally {
            spec.clearPassword();
        }
    }

    /** 散列结果值对象。 */
    public static final class HashedPassword {

        private final String encoded;

        HashedPassword(String encoded) {
            this.encoded = encoded;
        }

        /** 可直接写入 users.password_hash 的编码串。 */
        public String encoded() {
            return encoded;
        }

        @Override
        public String toString() {
            return encoded;
        }
    }

    /** 便于独立调用：把任意明文转为可入库的编码串。 */
    public static String encode(String rawPassword) {
        return new Passwords().hash(rawPassword).encoded();
    }

    /**
     * 生成随机口令（用于初始化管理员账号后要求重置等场景）。
     *
     * @param length 口令长度，至少 8
     * @return 随机明文口令
     */
    public static String randomPassword(int length) {
        if (length < 8) {
            throw new ValidationException("随机口令长度不能小于 8");
        }
        String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(alphabet.charAt(RANDOM.nextInt(alphabet.length())));
        }
        return builder.toString();
    }
}
