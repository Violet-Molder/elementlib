package com.linweiyun.elementlib.config;

import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 早期注册开关 —— 注册决策必须发生在配置真正加载之前，所以在类初始化时直接读配置文件。
 *
 * <p>按行匹配 {@code key = true|false}，容忍空格、注释与缺文件；缺文件或读失败一律取默认 {@code true}。
 * 配置加载完成后由 {@code ElementLibConfig.syncEarlyFlags()} 用 Spec 的真实值覆盖一次。
 */
public final class ElementLibEarlyFlags {

    private static final String ELEMENTS_FILE = "elementlib/elements.toml";
    private static final String REACTIONS_FILE = "elementlib/reactions.toml";
    private static final String ITEMS_FILE = "elementlib/items.toml";

    private static volatile boolean demoElements = readBoolean(ELEMENTS_FILE, "register-demo-elements", true);
    private static volatile boolean demoReactions = readBoolean(REACTIONS_FILE, "register-demo-reactions", true);
    private static volatile boolean demoItems = readBoolean(ITEMS_FILE, "register-demo-items", true);
    private static volatile boolean auraIcon = readBoolean(ELEMENTS_FILE, "show-aura-icon", true);

    private ElementLibEarlyFlags() {
    }

    public static boolean demoElements() {
        return demoElements;
    }

    public static boolean demoReactions() {
        return demoReactions;
    }

    public static boolean demoItems() {
        return demoItems;
    }

    public static boolean auraIcon() {
        return auraIcon;
    }

    /** 用配置的真实值覆盖早期读到的值，保持运行期一致。 */
    public static void resync(boolean elements, boolean reactions, boolean items, boolean showAuraIcon) {
        demoElements = elements;
        demoReactions = reactions;
        demoItems = items;
        auraIcon = showAuraIcon;
    }

    /**
     * 从配置文件里读一个布尔项。
     *
     * <p>只做「一行一项」的宽松匹配：忽略空行与 {@code #} 开头的行，取第一个 {@code key} 匹配的行，
     * 去掉行尾注释后按 {@code true}/{@code false} 解析；其余情况返回 {@code fallback}。
     */
    private static boolean readBoolean(String relativePath, String key, boolean fallback) {
        try {
            Path path = FMLPaths.CONFIGDIR.get().resolve(relativePath);
            if (!Files.isRegularFile(path)) {
                return fallback;
            }
            List<String> lines = Files.readAllLines(path);
            for (String raw : lines) {
                if (raw == null) continue;
                String line = raw.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int eq = line.indexOf('=');
                if (eq < 0) continue;
                if (!line.substring(0, eq).trim().equals(key)) continue;
                String value = line.substring(eq + 1).trim();
                int hash = value.indexOf('#');
                if (hash >= 0) value = value.substring(0, hash).trim();
                if (value.equalsIgnoreCase("true")) return true;
                if (value.equalsIgnoreCase("false")) return false;
            }
        } catch (Exception ignored) {
            // 配置不可读时按默认值处理
        }
        return fallback;
    }
}
