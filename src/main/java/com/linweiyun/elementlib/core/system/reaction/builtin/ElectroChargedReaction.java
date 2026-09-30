package com.linweiyun.elementlib.core.system.reaction.builtin;

import com.linweiyun.elementlib.api.ElementalReactionType;
import com.linweiyun.elementlib.core.attachment.StatusContainer;
import com.linweiyun.elementlib.core.element.GenshinElement;
import com.linweiyun.elementlib.core.element.ModElements;
import com.linweiyun.elementlib.core.status.StatusInstance;
import com.linweiyun.elementlib.core.system.about.ElementalAttachmentInstance;
import com.linweiyun.elementlib.core.system.reaction.ElectroChargedTickState;
import com.linweiyun.elementlib.core.system.reaction.ElementalReaction;
import com.linweiyun.elementlib.core.system.reaction.ReactionContext;
import com.linweiyun.elementlib.core.system.reaction.ReactionPriorityCalculator;
import com.linweiyun.elementlib.core.system.reaction.ReactionResult;
import com.linweiyun.elementlib.util.log.LogGroup;
import com.linweiyun.elementlib.util.log.ModLog;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.function.Supplier;

/**
 * 感电反应 —— 水 + 雷，共存反应。
 */
public class ElectroChargedReaction extends ElementalReaction {
    public static final Logger LOGGER = ModLog.getLogger(LogGroup.ELEMENT);
    public static final float CONSUME_PER_TRIGGER = 0.4f;

    public ElectroChargedReaction(Supplier<ElementalReactionType> type,
                                  String elementAId, String elementBId,
                                  float ratioA, float ratioB, int basePriority) {
        super(type, elementAId, elementBId, ratioA, ratioB, basePriority);
    }

    @Override
    public boolean isBlocked(ReactionContext context) {
        return ReactionPriorityCalculator.hasFrozen(context.targetContainer());
    }

    @Override
    public ReactionResult execute(ReactionContext ctx) {
        GenshinElement hydro = ModElements.of(ModElements.HYDRO);
        GenshinElement electro = ModElements.of(ModElements.ELECTRO);
        if (hydro == null || electro == null) {
            return ReactionResult.builder(type()).build();
        }

        ElementalAttachmentInstance hydroInst = findElement(ctx.targetContainer(), hydro);
        ElementalAttachmentInstance electroInst = findElement(ctx.targetContainer(), electro);

        if (hydroInst == null || electroInst == null) {
            return ReactionResult.builder(type()).build();
        }

        float hydroBefore = hydroInst.getUnit();
        float electroBefore = electroInst.getUnit();

        if (hydroBefore <= 0 || electroBefore <= 0) {
            return ReactionResult.builder(type()).build();
        }

        hydroInst.consume(Math.min(CONSUME_PER_TRIGGER, hydroBefore));
        electroInst.consume(Math.min(CONSUME_PER_TRIGGER, electroBefore));

        ElectroChargedTickState state = ctx.targetContainer().getElectroChargedTickState();
        state.setContainer(ctx.targetContainer());
        state.onActiveTrigger(ctx.attackerEntity(), ctx.targetEntity());

        return ReactionResult.builder(type())
                .reacted()
                .consumedAttacker(ctx.attackerUnit())
                .consumedDefender(0)
                .build();
    }

    /** 找容器里第一个该元素的附着实例。 */
    @Nullable
    public static ElementalAttachmentInstance findElement(StatusContainer container,
                                                          @Nullable GenshinElement element) {
        if (element == null) return null;
        for (StatusInstance inst : container.getAll()) {
            if (inst.isFinished()) continue;
            if (!(inst instanceof ElementalAttachmentInstance ea)) continue;
            if (ea.getElement() == element) return ea;
        }
        return null;
    }
}
