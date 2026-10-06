package com.linweiyun.elementlib.command;

import com.linweiyun.elementlib.ElementLib;
import com.linweiyun.elementlib.api.DemoContentToggles;
import com.linweiyun.elementlib.api.ElementLibApi;
import com.linweiyun.elementlib.config.ElementLibConfig;
import com.linweiyun.elementlib.content.items.ElementSwordItem;
import com.linweiyun.elementlib.content.items.ModItems;
import com.linweiyun.elementlib.core.attachment.ElementalAttachments;
import com.linweiyun.elementlib.core.attachment.StatusContainer;
import com.linweiyun.elementlib.core.element.GenshinElement;
import com.linweiyun.elementlib.core.system.about.AttachResult;
import com.linweiyun.elementlib.core.system.about.AttachmentProfile;
import com.linweiyun.elementlib.core.system.about.AttachmentSource;
import com.linweiyun.elementlib.core.system.about.ElementalAttachmentHelper;
import com.linweiyun.elementlib.core.system.about.ElementalAttachmentInstance;
import com.linweiyun.elementlib.core.system.about.host.BlockHost;
import com.linweiyun.elementlib.core.system.about.host.EntityHost;
import com.linweiyun.elementlib.core.system.combat.decay.DecayCounterManager;
import com.linweiyun.elementlib.core.system.combat.decay.DecayGroup;
import com.linweiyun.elementlib.core.system.combat.decay.DecayGroups;
import com.linweiyun.elementlib.core.system.combat.decay.DecayResult;
import com.linweiyun.elementlib.core.system.combat.decay.DecaySpec;
import com.linweiyun.elementlib.core.system.registry.ModRegistries;
import com.linweiyun.elementlib.core.system.reaction.ElementalReaction;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * <b>ElementLib 的游戏内测试入口</b> —— {@code /elementlib ...}。
 */
@EventBusSubscriber(modid = ElementLib.MOD_ID)
public final class ElementLibCommand {

    private ElementLibCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("elementlib")
                .then(Commands.literal("attach")
                        .then(Commands.argument("element", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    for (Identifier id : ModRegistries.ELEMENT_REGISTRY.keySet()) {
                                        builder.suggest(id.getPath());
                                        builder.suggest(id.toString());
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> attach(ctx, 1.0f, AttachmentSource.NORMAL_ATTACK))
                                .then(Commands.argument("amount", FloatArgumentType.floatArg(0.0f))
                                        .executes(ctx -> attach(ctx, FloatArgumentType.getFloat(ctx, "amount"),
                                                AttachmentSource.NORMAL_ATTACK))
                                        .then(Commands.argument("source", StringArgumentType.word())
                                                .suggests((ctx, builder) -> {
                                                    for (AttachmentSource s : AttachmentSource.values()) {
                                                        builder.suggest(s.name());
                                                    }
                                                    return builder.buildFuture();
                                                })
                                                .executes(ctx -> attach(ctx,
                                                        FloatArgumentType.getFloat(ctx, "amount"),
                                                        parseSource(StringArgumentType.getString(ctx, "source"))))
                                                .then(Commands.argument("targets", EntityArgument.entities())
                                                        .executes(ctx -> {
                                                            Collection<? extends Entity> targets =
                                                                    EntityArgument.getEntities(ctx, "targets");
                                                            return attachTo(ctx,
                                                                    FloatArgumentType.getFloat(ctx, "amount"),
                                                                    parseSource(StringArgumentType.getString(ctx, "source")),
                                                                    targets);
                                                        }))))))

                .then(Commands.literal("clear")
                        .executes(ctx -> clear(ctx, selfTargets(ctx)))
                        .then(Commands.argument("targets", EntityArgument.entities())
                                .executes(ctx -> clear(ctx, EntityArgument.getEntities(ctx, "targets")))))

                .then(Commands.literal("info")
                        .executes(ctx -> info(ctx, selfTargets(ctx)))
                        .then(Commands.argument("targets", EntityArgument.entities())
                                .executes(ctx -> info(ctx, EntityArgument.getEntities(ctx, "targets")))))

                .then(Commands.literal("decay")
                        .then(Commands.argument("tag", StringArgumentType.word())
                                .executes(ctx -> decay(ctx, StringArgumentType.getString(ctx, "tag"), 8))
                                .then(Commands.argument("hits", IntegerArgumentType.integer(1, 64))
                                        .executes(ctx -> decay(ctx,
                                                StringArgumentType.getString(ctx, "tag"),
                                                IntegerArgumentType.getInteger(ctx, "hits"))))))

                .then(Commands.literal("block")
                        .then(Commands.argument("element", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    for (Identifier id : ModRegistries.ELEMENT_REGISTRY.keySet()) {
                                        builder.suggest(id.getPath());
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> block(ctx, 1.0f))
                                .then(Commands.argument("amount", FloatArgumentType.floatArg(0.0f))
                                        .executes(ctx -> block(ctx, FloatArgumentType.getFloat(ctx, "amount"))))))

                .then(Commands.literal("reactions")
                        .executes(ElementLibCommand::reactions))

                .then(Commands.literal("demo")
                        .then(Commands.literal("stellar_swirl")
                                .then(Commands.literal("on").executes(ctx -> demo(ctx, "stellar_swirl", true)))
                                .then(Commands.literal("off").executes(ctx -> demo(ctx, "stellar_swirl", false))))
                        .then(Commands.literal("stellar_conduce")
                                .then(Commands.literal("on").executes(ctx -> demo(ctx, "stellar_conduce", true)))
                                .then(Commands.literal("off").executes(ctx -> demo(ctx, "stellar_conduce", false))))
                        .then(Commands.literal("lunar")
                                .then(Commands.literal("on").executes(ctx -> demo(ctx, "lunar", true)))
                                .then(Commands.literal("off").executes(ctx -> demo(ctx, "lunar", false))))
                        .then(Commands.literal("status").executes(ElementLibCommand::demoStatus)))

                .then(Commands.literal("icon")
                        .then(Commands.literal("on").executes(ctx -> icon(ctx, true)))
                        .then(Commands.literal("off").executes(ctx -> icon(ctx, false))))

                .then(Commands.literal("wand").executes(ElementLibCommand::wand))

                .then(Commands.literal("swords").executes(ElementLibCommand::swords)));
    }

    // ==================== 子命令实现 ====================

    private static int attach(CommandContext<CommandSourceStack> ctx, float amount, AttachmentSource source) {
        return attachTo(ctx, amount, source, selfTargets(ctx));
    }

    private static int attachTo(CommandContext<CommandSourceStack> ctx, float amount,
                                AttachmentSource source, Collection<? extends Entity> targets) {
        GenshinElement element = resolveElement(ctx, "element");
        if (element == null) {
            return 0;
        }
        AttachmentProfile profile = AttachmentProfile.forAmount(amount);
        int attached = 0;
        int rejected = 0;
        for (Entity entity : targets) {
            if (!(entity instanceof LivingEntity living)) {
                continue;
            }
            EntityHost host = EntityHost.of(living);
            if (host == null) {
                continue;
            }
            AttachResult result = ElementalAttachmentHelper.attach(host, element, source, profile,
                    source.name().toLowerCase(java.util.Locale.ROOT), living.level().getGameTime());
            if (result.attached()) {
                attached++;
            } else {
                rejected++;
            }
        }
        final int ok = attached;
        final int denied = rejected;
        String text = "[ElementLib] 附着 " + element.getId() + " " + (ok + denied)
                + " 个目标：成功 " + ok + "，被拒 " + denied;
        if (denied > 0) {
            text += "（类元素只能由反应生成 / 瞬发元素无可反应对象）";
        }
        final String message = text;
        if (ok == 0 && denied > 0) {
            ctx.getSource().sendFailure(Component.literal(message));
        } else {
            ctx.getSource().sendSuccess(() -> Component.literal(message), true);
        }
        return ok;
    }

    private static int clear(CommandContext<CommandSourceStack> ctx, Collection<? extends Entity> targets) {
        int done = 0;
        for (Entity entity : targets) {
            if (!(entity instanceof LivingEntity living)) {
                continue;
            }
            StatusContainer container = ElementalAttachments.container(living);
            if (container == null) {
                continue;
            }
            container.clear();
            ElementalAttachments.commit(living, container);
            done++;
        }
        final int count = done;
        ctx.getSource().sendSuccess(() -> Component.literal("[ElementLib] 已清空 " + count + " 个目标的元素容器"), false);
        return count;
    }

    private static int info(CommandContext<CommandSourceStack> ctx, Collection<? extends Entity> targets) {
        int lines = 0;
        for (Entity entity : targets) {
            if (!(entity instanceof LivingEntity living)) {
                continue;
            }
            StatusContainer container = ElementalAttachments.container(living);
            if (container == null) {
                continue;
            }
            List<String> parts = new ArrayList<>();
            for (var inst : container.getAll()) {
                if (!(inst instanceof ElementalAttachmentInstance ea) || ea.isFinished()) {
                    continue;
                }
                parts.add(ea.getElement() == null ? "?" : ea.getElement().getId()
                        + "{unit=" + String.format(java.util.Locale.ROOT, "%.3f", ea.getUnit())
                        + ", decay/s=" + String.format(java.util.Locale.ROOT, "%.3f", ea.getCurrentDecayPerSecond())
                        + ", source=" + ea.getSource()
                        + ", permanent=" + ea.isPermanent() + "}");
            }
            final String text = "[ElementLib] " + living.getName().getString()
                    + " 元素容器（" + parts.size() + " 条）：" + (parts.isEmpty() ? "空" : String.join(" | ", parts));
            ctx.getSource().sendSuccess(() -> Component.literal(text), false);
            lines++;
        }
        if (lines == 0) {
            ctx.getSource().sendSuccess(() -> Component.literal("[ElementLib] 没有可查询的生物目标"), false);
        }
        return lines;
    }

    /**
     * 跑一次计时计数器 —— 真的走 {@link DecayCounterManager#processHit}，
     * 打印每一次命中的元素量系数。
     */
    private static int decay(CommandContext<CommandSourceStack> ctx, String tag, int hits) {
        DecayGroup group = groupOf(tag);
        if (group == null) {
            ctx.getSource().sendFailure(Component.literal(
                    "[ElementLib] 未知衰减组 '" + tag + "'，可用：normal / skill / burst"));
            return 0;
        }
        LivingEntity self = selfLiving(ctx);
        if (self == null) {
            ctx.getSource().sendFailure(Component.literal("[ElementLib] 该命令需要由一个生物实体执行"));
            return 0;
        }
        long now = self.level().getGameTime();
        DecayCounterManager manager = ElementalAttachments.decayCounter(self);
        DecaySpec spec = new DecaySpec(tag, group);
        StringBuilder seq = new StringBuilder();
        for (int i = 0; i < hits; i++) {
            DecayResult r = manager.processHit(self, "cmd", spec, now);
            if (i > 0) {
                seq.append(',');
            }
            seq.append(String.format(java.util.Locale.ROOT, "%.2f", r.getElementCoefficient()));
        }
        final DecayGroup g = group;
        ctx.getSource().sendSuccess(() -> Component.literal(
                "[ElementLib] 衰减组 " + tag + "（clearTime=" + g.clearTimeTicks() + " tick）元素量系数序列："
                        + seq + "  计数器数=" + manager.getCounterCount()), false);
        return hits;
    }

    private static int block(CommandContext<CommandSourceStack> ctx, float amount) {
        ServerLevel level = ctx.getSource().getLevel();
        Entity source = ctx.getSource().getEntity();
        Vec3 p = source != null ? source.position() : ctx.getSource().getPosition();
        // 脚下方块：实体位置下移一格
        BlockPos pos = BlockPos.containing(p.x, p.y - 1.0d, p.z);
        GenshinElement element = resolveElement(ctx, "element");
        if (element == null) {
            return 0;
        }
        BlockHost host = BlockHost.of(level, pos);
        if (host == null) {
            ctx.getSource().sendFailure(Component.literal("[ElementLib] 目标方块坐标无效"));
            return 0;
        }
        var result = ElementalAttachmentHelper.attach(host, element,
                AttachmentSource.ENVIRONMENTAL, AttachmentProfile.forAmount(amount));
        StatusContainer container = host.container();
        if (container != null) {
            host.commit(container);
        }
        final String msg = "[ElementLib] 方块 " + host.state().getBlock()
                + " @ " + pos.toShortString() + " 附着 " + element.getId()
                + "（" + amount + "U）→ attached=" + result.attached()
                + ", reacted=" + result.reacted();
        ctx.getSource().sendSuccess(() -> Component.literal(msg), true);
        return 1;
    }

    private static int reactions(CommandContext<CommandSourceStack> ctx) {
        List<String> list = new ArrayList<>();
        for (ElementalReaction reaction : ModRegistries.ELEMENTAL_REACTIONS_REGISTRY) {
            String a = reaction.getElementA() == null ? reaction.toString() : reaction.getElementA().getId();
            String b = reaction.getElementB() == null ? "?" : reaction.getElementB().getId();
            list.add(reaction.getReactionType() + "(" + a + ":" + reaction.getRatioA()
                    + " + " + b + ":" + reaction.getRatioB() + ", prio=" + reaction.getBasePriority() + ")");
        }
        final String text = "[ElementLib] 已注册反应 " + list.size() + " 条：" + String.join(" | ", list);
        ctx.getSource().sendSuccess(() -> Component.literal(text), false);
        return list.size();
    }

    /** 切换星扩散 / 星超导 / 月感电三个示范变体。 */
    private static int demo(CommandContext<CommandSourceStack> ctx, String key, boolean on) {
        switch (key) {
            case "stellar_swirl" -> DemoContentToggles.setStellarSwirl(on);
            case "stellar_conduce" -> DemoContentToggles.setStellarConduce(on);
            case "lunar" -> DemoContentToggles.setLunarCharged(on);
            default -> {
                return 0;
            }
        }
        ctx.getSource().sendSuccess(() -> Component.literal(
                "[ElementLib] 示范变体 " + key + " = " + (on ? "on" : "off")), false);
        return 1;
    }

    private static int demoStatus(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSuccess(() -> Component.literal(
                "[ElementLib] " + DemoContentToggles.describe()), false);
        return 1;
    }

    /** 元素附着图标总闸：运行期开关与配置同时写。 */
    private static int icon(CommandContext<CommandSourceStack> ctx, boolean visible) {
        ElementLibApi.setAuraIconVisible(visible);
        try {
            ElementLibConfig.SHOW_AURA_ICON.set(visible);
        } catch (RuntimeException ignored) {
            // 配置尚未加载：只保留运行期开关
        }
        ctx.getSource().sendSuccess(() -> Component.literal(
                "[ElementLib] 元素图标: " + (visible ? "on" : "off")), false);
        return 1;
    }

    private static int wand(CommandContext<CommandSourceStack> ctx) {
        Player player = demoItemsPlayer(ctx);
        if (player == null) {
            return 0;
        }
        if (!ModItems.ELEMENT_WAND.isBound()) {
            ctx.getSource().sendFailure(Component.literal("[ElementLib] 元素法杖未注册"));
            return 0;
        }
        give(player, new ItemStack(ModItems.ELEMENT_WAND.get()));
        ctx.getSource().sendSuccess(() -> Component.literal("[ElementLib] 已获得元素法杖"), false);
        return 1;
    }

    private static int swords(CommandContext<CommandSourceStack> ctx) {
        Player player = demoItemsPlayer(ctx);
        if (player == null) {
            return 0;
        }
        int given = 0;
        for (DeferredItem<ElementSwordItem> holder : ModItems.ELEMENT_SWORDS) {
            if (!holder.isBound()) {
                continue;
            }
            give(player, new ItemStack(holder.get()));
            given++;
        }
        final int count = given;
        ctx.getSource().sendSuccess(() -> Component.literal(
                "[ElementLib] 已获得 " + count + " 把元素剑"), false);
        return count;
    }

    /** 示范物品命令的公共守卫：开关打开且执行者是玩家。 */
    @Nullable
    private static Player demoItemsPlayer(CommandContext<CommandSourceStack> ctx) {
        if (!ElementLibConfig.demoItemsEnabled()) {
            ctx.getSource().sendFailure(Component.literal(
                    "[ElementLib] 示范物品未启用（items.toml: register-demo-items = false）"));
            return null;
        }
        if (!(ctx.getSource().getEntity() instanceof Player player)) {
            ctx.getSource().sendFailure(Component.literal("[ElementLib] 该子命令需要由玩家执行"));
            return null;
        }
        return player;
    }

    private static void give(Player player, ItemStack stack) {
        if (!player.addItem(stack) && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.drop(stack, false, false);
        }
    }

    // ==================== 工具 ====================

    private static Collection<? extends Entity> selfTargets(CommandContext<CommandSourceStack> ctx) {
        Entity self = ctx.getSource().getEntity();
        return self == null ? List.of() : List.of(self);
    }

    private static LivingEntity selfLiving(CommandContext<CommandSourceStack> ctx) {
        return ctx.getSource().getEntity() instanceof LivingEntity living ? living : null;
    }

    private static GenshinElement resolveElement(CommandContext<CommandSourceStack> ctx, String arg) {
        String raw = StringArgumentType.getString(ctx, arg);
        Identifier id = raw.contains(":") ? Identifier.parse(raw) : ElementLib.id(raw);
        GenshinElement element = ModRegistries.ELEMENT_REGISTRY.get(id).map(r -> r.value()).orElse(null);
        if (element == null) {
            ctx.getSource().sendFailure(Component.literal("[ElementLib] 未找到元素 '" + raw + "'"));
        }
        return element;
    }

    private static AttachmentSource parseSource(String raw) {
        for (AttachmentSource s : AttachmentSource.values()) {
            if (s.name().equalsIgnoreCase(raw)) {
                return s;
            }
        }
        return AttachmentSource.NORMAL_ATTACK;
    }

    private static DecayGroup groupOf(String tag) {
        return switch (tag.toLowerCase(java.util.Locale.ROOT)) {
            case "normal", "default", "attack" -> DecayGroups.DEFAULT_NORMAL_ATTACK;
            case "skill", "elemental_skill" -> DecayGroups.DEFAULT_ELEMENTAL_SKILL;
            case "burst", "elemental_burst" -> DecayGroups.DEFAULT_ELEMENTAL_BURST;
            default -> null;
        };
    }
}
