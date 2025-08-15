package com.dwinovo.piayn.client.renderer.catnip.render;

import java.util.function.BiFunction;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.Util;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import com.dwinovo.piayn.PIAYN;

public abstract class PonderRenderTypes extends RenderType {

	private static final RenderType OUTLINE_SOLID =
		create("outline_solid", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, false, false, CompositeState.builder()
			.setShaderState(RENDERTYPE_ENTITY_SOLID_SHADER)
			.setTextureState(new TextureStateShard(ResourceLocation.fromNamespaceAndPath("minecraft", "textures/misc/white.png"), false, false))
			.setCullState(CULL)
			.setLightmapState(LIGHTMAP)
			.setOverlayState(OVERLAY)
			.createCompositeState(false));

	private static final BiFunction<ResourceLocation, Boolean, RenderType> OUTLINE_TRANSLUCENT = Util.memoize((texture, cull) ->
		create("outline_translucent" + (cull ? "_cull" : ""), DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, false, true, CompositeState.builder()
			.setShaderState(cull ? RENDERTYPE_ENTITY_TRANSLUCENT_CULL_SHADER : RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
			.setTextureState(new TextureStateShard(texture, false, false))
			.setTransparencyState(TRANSLUCENT_TRANSPARENCY)
			.setCullState(cull ? CULL : NO_CULL)
			.setLightmapState(LIGHTMAP)
			.setOverlayState(OVERLAY)
			.setWriteMaskState(COLOR_WRITE)
			.createCompositeState(false)));

	public static RenderType outlineSolid() {
		return OUTLINE_SOLID;
	}

	public static RenderType outlineTranslucent(ResourceLocation texture, boolean cull) {
		return OUTLINE_TRANSLUCENT.apply(texture, cull);
	}

	private static String createLayerName(String name) {
		return PIAYN.MOD_ID + ":" + name;
	}

	private PonderRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState, Runnable clearState) {
		super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
	}
}
