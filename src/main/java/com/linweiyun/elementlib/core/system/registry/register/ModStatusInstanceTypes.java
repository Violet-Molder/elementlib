package com.linweiyun.elementlib.core.system.registry.register;

import com.linweiyun.elementlib.core.status.StatusInstanceType;
import com.linweiyun.elementlib.core.system.about.ElementalAttachmentInstance;
import com.linweiyun.elementlib.core.system.registry.ModRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModStatusInstanceTypes {

    public static final DeferredRegister<StatusInstanceType<?>> STATUS_INSTANCE_TYPES =
            ModRegistries.STATUS_INSTANCE_TYPES;

    public static final DeferredHolder<StatusInstanceType<?>, StatusInstanceType<ElementalAttachmentInstance>> ELEMENTAL_ATTACHMENT =
            STATUS_INSTANCE_TYPES.register("elemental_attachment",
                    () -> new StatusInstanceType<>("elemental_attachment", ElementalAttachmentInstance::new));


    public static void register(IEventBus bus) {
        STATUS_INSTANCE_TYPES.register(bus);
    }
}
