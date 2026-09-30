package com.linweiyun.elementlib;

import com.linweiyun.elementlib.config.ElementLibConfig;
import com.linweiyun.elementlib.content.items.ModItems;
import com.linweiyun.elementlib.core.attachment.ElementalAttachments;
import com.linweiyun.elementlib.core.element.ModElements;
import com.linweiyun.elementlib.core.entity.ModEntities;
import com.linweiyun.elementlib.core.system.about.block.BlockElementHelper;
import com.linweiyun.elementlib.core.system.combat.decay.DecayCounterService;
import com.linweiyun.elementlib.core.system.registry.ModRegistries;
import com.linweiyun.elementlib.core.system.registry.register.ModElementalReactions;
import com.linweiyun.elementlib.core.system.registry.register.ModReactionTypes;
import com.linweiyun.elementlib.core.system.registry.register.ModStatusInstanceTypes;
import com.linweiyun.elementlib.util.log.LogGroup;
import com.linweiyun.elementlib.util.log.ModLog;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

/**
 * <b>ElementLib</b> —— 元素体系库主类。
 *
 * <p>元素注册、附着、反应、反应类型、衰减序列（只含元素附着量）、计时计数器、环境（方块）元素附着都在这里自洽运行。
 *
 * <p>验收入口：{@code /elementlib} 命令挂元素、看反应。
 */
@Mod(ElementLib.MOD_ID)
public class ElementLib {

    public static final String MOD_ID = "elementlib";

    /** 本模组所有日志的总开关（运行期可改）。 */
    public static volatile boolean LOG_ENABLED = true;

    public static final Logger LOGGER = ModLog.getLogger(LogGroup.CORE);

    public ElementLib(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::onConfigLoading);
        NeoForge.EVENT_BUS.register(this);

        modContainer.registerConfig(ModConfig.Type.COMMON, ElementLibConfig.ELEMENTS_SPEC, "elementlib/elements.toml");
        modContainer.registerConfig(ModConfig.Type.COMMON, ElementLibConfig.REACTIONS_SPEC, "elementlib/reactions.toml");
        modContainer.registerConfig(ModConfig.Type.COMMON, ElementLibConfig.ITEMS_SPEC, "elementlib/items.toml");

        boolean elements = ElementLibConfig.demoElementsEnabled();
        boolean reactions = ElementLibConfig.demoReactionsEnabled();

        ModElements.register(modEventBus);
        ElementalAttachments.register(modEventBus);
        ModStatusInstanceTypes.register(modEventBus);

        if (reactions && !elements) {
            LOGGER.warn("[ElementLib] 示范元素未启用，跳过示范反应类型与示范反应注册");
        }
        if (elements && reactions) {
            ModReactionTypes.register(modEventBus);
            ModElementalReactions.register(modEventBus);
            ModEntities.register(modEventBus);
        }
        if (ElementLibConfig.demoItemsEnabled()) {
            ModItems.register(modEventBus);
        }
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // 类元素（冻/激/燃/木）的主元素关联必须等所有元素注册完才能设
        event.enqueueWork(() -> {
            ModElements.setupSubElements();
            LOGGER.info("[ElementLib] 已注册元素 {} 个 / 反应 {} 条 / 反应类型 {} 个",
                    ModRegistries.ELEMENT_REGISTRY.size(),
                    ModRegistries.ELEMENTAL_REACTIONS_REGISTRY.size(),
                    ModRegistries.REACTION_TYPE_REGISTRY.size());
        });
        // 自定义注册表由 ModRegistries 上的 @EventBusSubscriber 监听 NewRegistryEvent 自行注册
    }

    /** 配置加载完成后，用 Spec 的真实值覆盖早期注册开关。 */
    private void onConfigLoading(ModConfigEvent.Loading event) {
        ElementLibConfig.syncEarlyFlags();
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        DecayCounterService.initOnServer(event.getServer().getLevel(Level.OVERWORLD));
        LOGGER.info("[ElementLib] DecayCounter Worker started");
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        for (ServerLevel level : event.getServer().getAllLevels()) {
            BlockElementHelper.onServerTick(level);
        }
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        DecayCounterService.shutdown();
        LOGGER.info("[ElementLib] DecayCounter Worker stopped");
    }
}
