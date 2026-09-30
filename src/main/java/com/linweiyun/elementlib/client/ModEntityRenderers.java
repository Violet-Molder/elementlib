package com.linweiyun.elementlib.client;

import com.linweiyun.elementlib.ElementLib;
import com.linweiyun.elementlib.core.entity.ModEntities;
import com.linweiyun.elementlib.core.entity.StellarVortexEntity;
import com.linweiyun.elementlib.core.entity.ThunderCloudEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** 示范实体的客户端渲染注册：两者共用一个不画几何体的渲染器。 */
@EventBusSubscriber(value = Dist.CLIENT, modid = ElementLib.MOD_ID)
public final class ModEntityRenderers {

    private ModEntityRenderers() {
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
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
    private static final class NoopRenderer<T extends Entity> extends EntityRenderer<T, EntityRenderState> {

        private NoopRenderer(EntityRendererProvider.Context context) {
            super(context);
            this.shadowRadius = 0.0F;
            this.shadowStrength = 0.0F;
        }

        @Override
        public EntityRenderState createRenderState() {
            return new EntityRenderState();
        }

        @Override
        public void submit(EntityRenderState state, PoseStack poseStack,
                           SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        }
    }
}
