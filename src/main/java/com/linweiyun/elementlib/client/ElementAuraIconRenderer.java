package com.linweiyun.elementlib.client;

import com.linweiyun.elementlib.ElementLib;
import com.linweiyun.elementlib.api.AuraIconContext;
import com.linweiyun.elementlib.api.AuraIconRules;
import com.linweiyun.elementlib.config.ElementLibConfig;
import com.linweiyun.elementlib.core.attachment.ElementalAttachments;
import com.linweiyun.elementlib.core.attachment.StatusContainer;
import com.linweiyun.elementlib.core.element.GenshinElement;
import com.linweiyun.elementlib.api.ElementRoles;
import com.linweiyun.elementlib.core.status.StatusInstance;
import com.linweiyun.elementlib.core.system.about.ElementalAttachmentInstance;
import com.linweiyun.elementlib.core.system.about.host.ElementalHost;
import com.linweiyun.elementlib.core.system.about.host.EntityHost;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.linweiyun.elementlib.util.log.LogGroup;
import com.linweiyun.elementlib.util.log.ModLog;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 在生物头顶绘制元素附着图标（相机对齐 billboard，只画主元素）。
 *
 * <p>只处理客户端能读到容器的实体：状态容器是同步附件，普通生物身上就能直接读。
 */
@EventBusSubscriber(value = Dist.CLIENT, modid = ElementLib.MOD_ID)
public final class ElementAuraIconRenderer {

    private static final Logger LOGGER = ModLog.getLogger(LogGroup.RENDER);

    /** 超过这个距离的实体不画（格）。 */
    private static final double MAX_DISTANCE = 24.0;

    /** 图标中心相对实体包围盒顶部的高度。 */
    private static final float Y_OFFSET = 0.5f;

    private static final float ICON_SIZE = 0.30f;
    private static final float ICON_SPACING = 0.04f;

    /** 元素 → 图标 {@link RenderType}；元素是注册表里的单例，按实例建表。 */
    private static final Map<GenshinElement, RenderType> ICON_TYPES = new IdentityHashMap<>();

    /** 没有对应贴图的元素，记下来避免每帧重查资源包。 */
    private static final Set<GenshinElement> MISSING_ICONS =
            Collections.newSetFromMap(new IdentityHashMap<>());

    /** 低量判定：剩余衰减时间 ≤ 此值（秒）的元素才算“低量”，低量才参与闪烁。 */
    private static final float BLINK_THRESHOLD_SECONDS = 2.0f;

    /** 闪烁周期（tick）；方波：半个周期可见、半个周期隐去。 */
    private static final long BLINK_PERIOD = 10L;

    /** 待绘图标：主元素 + 最大元素量 + 是否低量（同主元素任一附着低量则低量）。 */
    private record AuraIcon(GenshinElement element, float unit, boolean isLow) {
    }

    /** 每实体复用的待绘图标列表，避免每实体每帧新建集合。 */
    private static final List<AuraIcon> MAIN_ELEMENTS = new ArrayList<>();

    /** 临时调试：只打前 N 帧，避免刷屏。 */
    private static int debugFrames = 0;

    private ElementAuraIconRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (!ElementLibConfig.showAuraIcon()) {
            return;
        }
        if (event.getStage() != Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }

        if (debugFrames < 60 && (debugFrames % 20 == 0)) {
            LOGGER.info("[aura-debug] onRenderLevelStage AFTER_TRANSLUCENT_BLOCKS fired");
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();

        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(true);

        // 相机在本帧的实体循环里是常量：位置、朝向各取一次就够
        Camera camera = event.getCamera();
        Vec3 camPos = camera.getPosition();
        Quaternionf camRot = camera.rotation();
        double camX = camPos.x;
        double camY = camPos.y;
        double camZ = camPos.z;

        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living)) continue;
            // 第一人称下自己的头就在相机上，画出来会糊满视口
            if (living == mc.player) continue;
            if (!living.isAlive()) continue;

            double relX = Mth.lerp(partialTick, living.xo, living.getX()) - camX;
            double relY = Mth.lerp(partialTick, living.yo, living.getY()) - camY;
            double relZ = Mth.lerp(partialTick, living.zo, living.getZ()) - camZ;
            double distance = Math.sqrt(relX * relX + relY * relY + relZ * relZ);
            if (distance > MAX_DISTANCE) continue;

            StatusContainer container = ElementalAttachments.peekContainer(living);
            if (container == null) continue;
            if (!collectMainElements(container, living)) continue;

            if (debugFrames < 60 && (debugFrames % 20 == 0)) {
                LOGGER.info("[aura-debug] entity {} collected {} icons: {}", living.getDisplayName().getString(),
                        MAIN_ELEMENTS.size(), MAIN_ELEMENTS.stream().map(i -> i.element().getId()).toList());
            }

            poseStack.pushPose();
            poseStack.translate((float) relX, (float) (relY + living.getBbHeight() + Y_OFFSET), (float) relZ);
            poseStack.mulPose(camRot);
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0f));
            renderIcons(poseStack, buffers, mc.level.getGameTime() + (double) partialTick);
            poseStack.popPose();
        }
        if (debugFrames < 60) debugFrames++;
    }

    /**
     * 收集要画的主元素，顺手用 {@link AuraIconRules} 过滤。
     *
     * <p>一个元素是否画图标按它<b>自己</b>判定，画出来的是归并后的主元素。
     */
    private static boolean collectMainElements(StatusContainer container, LivingEntity living) {
        MAIN_ELEMENTS.clear();

        // 临时调试：打印客户端容器实际内容，确认数据层
        if (debugFrames < 30) {
            List<String> desc = new ArrayList<>();
            for (StatusInstance inst : container.getAll()) desc.add(inst.getClass().getSimpleName());
            LOGGER.info("[aura-debug] entity={} container.size={} contents={}",
                    living.getType().getDescription().getString(), container.getAll().size(), desc);
        }

        ElementalHost host = EntityHost.of(living);
        for (StatusInstance inst : container.getAll()) {
            if (inst.isFinished()) continue;
            if (!(inst instanceof ElementalAttachmentInstance ea)) continue;

            GenshinElement element = ea.getElement();
            if (element == null) continue;
            // 效果载体（寒）不占图标：它伴随冰/冻存在，显示的应该是冰/冻本身
            if (element.isEffectCarrier()) continue;
            if (ElementRoles.is(element, ElementRoles.FYSIKOS)) continue;

            AuraIconContext ctx = new AuraIconContext(host, living, element, ea.getSource(), ea.getProfile());
            if (!AuraIconRules.shouldShow(ctx)) continue;

            GenshinElement main = element.getMainElement();
            if (main == null) continue;

            boolean isLow = isDecayLow(ea);
            int index = indexOfIcon(main);
            if (index < 0) {
                MAIN_ELEMENTS.add(new AuraIcon(main, ea.getUnit(), isLow));
            } else if (ea.getUnit() > MAIN_ELEMENTS.get(index).unit()) {
                // 类元素并入主元素：同名主元素只留元素量最大的那一条；low 标记取或
                AuraIcon prev = MAIN_ELEMENTS.get(index);
                MAIN_ELEMENTS.set(index, new AuraIcon(main, ea.getUnit(), prev.isLow() || isLow));
            } else if (isLow && !MAIN_ELEMENTS.get(index).isLow()) {
                // 量没更大但这条低量：只补上低量标记
                AuraIcon prev = MAIN_ELEMENTS.get(index);
                MAIN_ELEMENTS.set(index, new AuraIcon(main, prev.unit(), true));
            }
        }
        return !MAIN_ELEMENTS.isEmpty();
    }

    /**
     * 低量判定（完全照搬原项目）：衰减周期内剩余时间 ≤ {@link #BLINK_THRESHOLD_SECONDS} 秒才算低量；
     * 恒定附着不衰减，永远不算低量。
     */
    private static boolean isDecayLow(ElementalAttachmentInstance ea) {
        if (ea.isPermanent()) {
            return false;
        }
        float rate = ea.getCurrentDecayPerSecond();
        if (rate <= 0.0001f) {
            return false;
        }
        return ea.getUnit() / rate <= BLINK_THRESHOLD_SECONDS;
    }

    /** 已在列表里的同名主元素下标，没有返回 {@code -1}。 */
    private static int indexOfIcon(GenshinElement main) {
        for (int i = 0; i < MAIN_ELEMENTS.size(); i++) {
            if (MAIN_ELEMENTS.get(i).element() == main) {
                return i;
            }
        }
        return -1;
    }

    private static void renderIcons(PoseStack poseStack, MultiBufferSource.BufferSource buffers, double time) {
        int count = MAIN_ELEMENTS.size();
        float totalWidth = count * ICON_SIZE + (count - 1) * ICON_SPACING;
        float startX = -totalWidth / 2.0f;

        // 方波：亮半周期、灭半周期（完全照搬原项目）
        boolean blinkVisible = ((long) time % BLINK_PERIOD) < (BLINK_PERIOD / 2L);

        for (int i = 0; i < count; i++) {
            AuraIcon icon = MAIN_ELEMENTS.get(i);
            RenderType type = iconType(icon.element());
            if (type == null) {
                if (debugFrames < 60) LOGGER.warn("[aura-debug] no RenderType for icon {}", icon.element().getId());
                continue;
            }

            // 闪烁：低量且当前相位不可见时整帧跳过（保持占位）；可见时恒全亮，不再带 alpha 渐暗
            if (icon.isLow() && !blinkVisible) continue;

            float x1 = startX + i * (ICON_SIZE + ICON_SPACING);
            float x2 = x1 + ICON_SIZE;
            float yTop = ICON_SIZE / 2.0f;
            float yBottom = -ICON_SIZE / 2.0f;

            VertexConsumer consumer = buffers.getBuffer(type);
            drawTexturedQuad(consumer, poseStack.last().pose(), 0.0f,
                    x1, yBottom, x2, yTop,
                    1.0f, 0.0f, 0.0f, 1.0f,
                    1.0f, 1.0f, 1.0f, 1.0f);
        }

        // 把这次画的 batch 一次性提交 GPU
        buffers.endBatch();
    }

    /** 取元素的图标 {@link RenderType}；没有贴图返回 {@code null} 并记进缺失表。 */
    @Nullable
    private static RenderType iconType(GenshinElement element) {
        RenderType cached = ICON_TYPES.get(element);
        if (cached != null) {
            return cached;
        }
        if (MISSING_ICONS.contains(element)) {
            return null;
        }

        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(
                ElementLib.MOD_ID, "icon/elemental/" + element.getId() + ".png");

        ResourceManager resources = Minecraft.getInstance().getResourceManager();
        if (resources.getResource(texture).isEmpty()) {
            MISSING_ICONS.add(element);
            return null;
        }

        RenderType built = RenderType.entityTranslucent(texture);
        ICON_TYPES.put(element, built);
        return built;
    }

    private static void drawTexturedQuad(VertexConsumer consumer, Matrix4f matrix, float z,
                                         float x1, float y1, float x2, float y2,
                                         float u1, float v1, float u2, float v2,
                                         float r, float g, float b, float a) {
        consumer.addVertex(matrix, x1, y1, z).setColor(r, g, b, a).setUv(u1, v2)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(0, 0, 1);
        consumer.addVertex(matrix, x2, y1, z).setColor(r, g, b, a).setUv(u2, v2)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(0, 0, 1);
        consumer.addVertex(matrix, x2, y2, z).setColor(r, g, b, a).setUv(u2, v1)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(0, 0, 1);
        consumer.addVertex(matrix, x1, y2, z).setColor(r, g, b, a).setUv(u1, v1)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(0, 0, 1);
    }
}
