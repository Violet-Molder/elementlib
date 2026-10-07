package com.linweiyun.elementlib.api.event;

import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.Nullable;

/**
 * 带事件 id 与中文名的事件。
 *
 * <p>id 供日志与调试使用；NeoForge 事件总线按事件类派发，id 不参与派发。
 */
public interface ElibIdentifiedEvent {

    Identifier eventId();

    /** 这个事件的中文名（日志用），例如「攻击行为」「造成伤害」。 */
    String displayName();

    /** 日志用的完整描述：「中文名 id | 事件内容」；不是本接口的实现时退回 {@code toString()}。 */
    static String describe(@Nullable Event event) {
        if (event == null) {
            return "-";
        }
        if (event instanceof ElibIdentifiedEvent identified) {
            return identified.displayName() + " " + identified.eventId() + " | " + event;
        }
        return String.valueOf(event);
    }
}