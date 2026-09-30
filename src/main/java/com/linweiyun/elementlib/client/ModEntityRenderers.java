package com.linweiyun.elementlib.client;

import com.linweiyun.elementlib.ElementLib;
import com.linweiyun.elementlib.core.entity.ModEntities;
import com.linweiyun.elementlib.core.entity.StellarVortexEntity;
import com.linweiyun.elementlib.core.entity.ThunderCloudEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers;

/** 示范实体的客户端渲染注册：两者共用一个不画几何体的渲染器。 */
@EventBusSubscriber(value = Dist.CLIENT, modid = ElementLib.MOD_ID)
public final class ModEntityRenderers {

    /** 什么都没有时兜底用的贴图路径（不含扩展名）。 */
    private static final ResourceLocation BLANK_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ElementLib.MOD_ID, "textures/misc/blank");

    private ModEntityRenderers() {
    }

    @SubscribeEvent
    public static void onRegisterRenderers(RegisterRenderers event) {
        if (ModEntities.THUNDER_CLOUD != null) {
            event.registerEntityRenderer(ModEntities.THUNDER_CLOUD.get(),
                    context -> new NoopRenderer<ThunderCloudEntity>(context));
        }
        if (ModEntities.STELLAR_VORTEX != null) {
            event.registerEntityRenderer(ModEntities.STELLAR_VORTEX.get(),
                    context -> new NoopRenderer<StellarVortexEntity>(context));
        }
    }

    /** 什么都不画：实体可见性完全交给服务端粒子。 */
    private static final class NoopRenderer<T extends Entity> extends EntityRenderer<T> {

        private NoopRenderer(EntityRendererProvider.Context context) {
            super(context);
            this.shadowRadius = 0.0F;
            this.shadowStrength = 0.0F;
        }

        @Override
        public void render(T entity, float entityYaw, float partialTick, PoseStack poseStack,
                           MultiBufferSource bufferSource, int packedLight) {
        }

        @Override
        public ResourceLocation getTextureLocation(T entity) {
            return BLANK_TEXTURE;
        }
    }
}