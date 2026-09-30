package com.linweiyun.elementlib.api;

/**
 * 示范内容的运行期开关：星扩散 / 星超导 / 月感电三个变体分支的开关。
 *
 * <p>默认全部为关，反应走普通分支（普通超导 / 普通感电 / 普通扩散）；
 * 命令 {@code /elementlib demo <name> <on|off>} 打开对应变体。
 * 默认的 {@link ReactionVariantGate} 直接读这里。
 */
public final class DemoContentToggles {

    private static volatile boolean stellarSwirl = false;
    private static volatile boolean stellarConduce = false;
    private static volatile boolean lunarCharged = false;

    private DemoContentToggles() {
    }

    /** 设置星扩散开关。 */
    public static void setStellarSwirl(boolean v) {
        stellarSwirl = v;
    }

    /** 星扩散开关。 */
    public static boolean stellarSwirl() {
        return stellarSwirl;
    }

    /** 设置星超导开关。 */
    public static void setStellarConduce(boolean v) {
        stellarConduce = v;
    }

    /** 星超导开关。 */
    public static boolean stellarConduce() {
        return stellarConduce;
    }

    /** 设置月感电开关。 */
    public static void setLunarCharged(boolean v) {
        lunarCharged = v;
    }

    /** 月感电开关。 */
    public static boolean lunarCharged() {
        return lunarCharged;
    }

    /** 当前三个开关的回显文本；默认是 {@code stellar_swirl=off stellar_conduce=off lunar_charged=off}。 */
    public static String describe() {
        return "stellar_swirl=" + (stellarSwirl ? "on" : "off")
                + " stellar_conduce=" + (stellarConduce ? "on" : "off")
                + " lunar_charged=" + (lunarCharged ? "on" : "off");
    }
}
