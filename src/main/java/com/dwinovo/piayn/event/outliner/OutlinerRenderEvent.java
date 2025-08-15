package com.dwinovo.piayn.event.outliner;

import com.dwinovo.piayn.client.renderer.catnip.animation.AnimationTickHolder;
import com.dwinovo.piayn.client.renderer.catnip.outliner.Outliner;
import com.dwinovo.piayn.client.renderer.catnip.render.DefaultSuperRenderTypeBuffer;
import com.dwinovo.piayn.client.renderer.catnip.render.SuperRenderTypeBuffer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.api.distmarker.Dist;
import com.dwinovo.piayn.PIAYN;

@EventBusSubscriber(modid = PIAYN.MOD_ID,value = Dist.CLIENT)
public class OutlinerRenderEvent {
    @SubscribeEvent
    /**
     * 统一的 Outliner 渲染：在 AFTER_PARTICLES 阶段从全局缓冲中绘制追踪的轮廓与高亮面。
     */
    public static void onRenderWorld(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        PoseStack ms = event.getPoseStack();
		Vec3 cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
		float partialTicks = AnimationTickHolder.getPartialTicks();

		ms.pushPose();
		SuperRenderTypeBuffer buffer = DefaultSuperRenderTypeBuffer.getInstance();
        
		Outliner.getInstance().renderOutlines(ms, buffer, cameraPos, partialTicks);

		buffer.draw();
		ms.popPose();
	}
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        Outliner.getInstance().tickOutlines();
    }

}
