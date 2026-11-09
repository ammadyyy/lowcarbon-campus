package com.campus.lowcarbon.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * 应用配置读取器。
 * <p>优先级：系统属性（-Dkey=value） &gt; classpath 下的 application.properties &gt; 代码默认值。</p>
 */
public final class AppConfig {

    private static final String CONFIG_FILE = "/application.properties";
    private static final Properties PROPERTIES = load();

    private AppConfig() {
    }

    public static String get(String key, String defaultValue) {
        String override = System.getProperty(key);
        if (override != null && !override.trim().isEmpty()) {
            return override.trim();
        }
        String value = PROPERTIES.getProperty(key);
        return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
    }

    public static int getInt(String key, int defaultValue) {
        String raw = get(key, null);
        if (raw == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("配置项 " + key + " 必须是整数，当前值：" + raw, ex);
        }
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        String raw = get(key, null);
        return raw == null ? defaultValue : Boolean.parseBoolean(raw);
    }

    private static Properties load() {
        Properties properties = new Properties();
        try (InputStream input = AppConfig.class.getResourceAsStream(CONFIG_FILE)) {
            if (input == null) {
                return properties;
            }
            properties.load(new InputStreamReader(input, StandardCharsets.UTF_8));
        } catch (IOException ex) {
            throw new IllegalStateException("读取配置文件失败：" + CONFIG_FILE, ex);
        }
        return properties;
    }
}
