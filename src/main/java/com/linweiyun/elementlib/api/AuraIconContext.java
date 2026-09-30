package com.linweiyun.elementlib.api;

import com.linweiyun.elementlib.core.element.GenshinElement;
import com.linweiyun.elementlib.core.system.about.AttachmentProfile;
import com.linweiyun.elementlib.core.system.about.AttachmentSource;
import com.linweiyun.elementlib.core.system.about.host.ElementalHost;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

/**
 * 判定一条元素附着要不要显示图标时能看到的全部信息。
 *
 * <p>{@code host} 是附着所在宿主；{@code entity} 只在生物宿主上非空；{@code profile} 可能为空
 * （反序列化出来、或调用方没给）。
 */
public record AuraIconContext(ElementalHost host, @Nullable LivingEntity entity,
                              GenshinElement element, AttachmentSource source,
                              @Nullable AttachmentProfile profile) {
}
