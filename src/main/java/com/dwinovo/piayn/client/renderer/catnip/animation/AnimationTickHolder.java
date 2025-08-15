package com.dwinovo.piayn.client.renderer.catnip.animation;


import net.minecraft.client.Minecraft;


public class AnimationTickHolder {


	public static float getPartialTicks() {
		Minecraft mc = Minecraft.getInstance();
		return mc.getTimer().getGameTimeDeltaPartialTick(false);
	}

	
}
