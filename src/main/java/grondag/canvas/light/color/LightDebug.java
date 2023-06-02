package grondag.canvas.light.color;

import static grondag.canvas.light.color.LightSectionData.Const.WIDTH;

import com.mojang.blaze3d.systems.RenderSystem;

import grondag.canvas.CanvasMod;

public class LightDebug {
	public static LightSectionData debugData;

	public static void initialize() {
		RenderSystem.assertOnRenderThread();

		if (debugData != null) {
			return;
		}

		debugData = new LightSectionData();
		// drawDummy(debugData);
		// debugData.upload();

		CanvasMod.LOG.info("Light debug render initialized.");
	}

	public static int getTexture(String imageName) {
		if (imageName.equals("_cv_debug_light_data") && debugData != null && !debugData.isClosed()) {
			return debugData.getTexId();
		}

		return -1;
	}

	static void drawDummy(LightSectionData data) {
		for (int x = 0; x < WIDTH; x ++) {
			for (int y = 0; y < WIDTH; y ++) {
				for (int z = 0; z < WIDTH; z ++) {
					data.draw(LightSectionData.Encoding.encodeLight(x % 16, y % 16, z % 16, false, false));
				}
			}
		}
	}
}
