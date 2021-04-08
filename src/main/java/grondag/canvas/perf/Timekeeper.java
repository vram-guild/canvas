package grondag.canvas.perf;

import grondag.canvas.pipeline.PipelineManager;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.system.CallbackI;

import java.util.HashMap;

public class Timekeeper {
	// TODO: these should be configurable
	private static boolean enabled = true;
//	private static long overlayUpdatePeriod = 1000;

	private static long lastOverlayUpdate;
	private static long shadowStart;
	private static long shadowEnd;
	private static long worldStart;
	private static long worldEnd;
	private static int beforeWorldi;
	private static String[] beforeWorldName;
	private static long[] beforeWorldStart;
	private static long beforeWorldEnd;
	private static int fabulousi;
	private static String[] fabulousName;
	private static long[] fabulousStart;
	private static long fabulousEnd;
	private static int afterHandi;
	private static String[] afterHandName;
	private static long[] afterHandStart;
	private static long afterHandEnd;

	public static void clearShadow() {
		// Important as it affects world time record
		shadowStart = shadowEnd = 0;
	}

	public static void startShadow() {
		shadowStart = System.nanoTime();
	}

	public static void endShadow() {
		shadowEnd = System.nanoTime();
	}

	public static void startWorld() {
		worldStart = System.nanoTime();
	}

	public static void endWorld() {
		worldEnd = System.nanoTime();
	}

	public static void prepareBeforeWorld(int length) {
		if(!enabled) return;
		if(beforeWorldStart == null || beforeWorldStart.length != length) {
			beforeWorldStart = new long[length];
			beforeWorldName = new String[length];
		}
		beforeWorldi = 0;
	}

	public static void timeBeforeWorld(String passName) {
		if(!enabled) return;
		beforeWorldName[beforeWorldi] = passName;
		beforeWorldStart[beforeWorldi] = System.nanoTime();
		beforeWorldi++;
	}

	public static void endBeforeWorld() {
		if(!enabled) return;
		beforeWorldEnd = System.nanoTime();
	}

	public static void prepareFabulous(int length) {
		if(!enabled) return;
		if(fabulousStart == null || fabulousStart.length != length) {
			fabulousStart = new long[length];
			fabulousName = new String[length];
		}
		fabulousi = 0;
	}

	public static void timeFabulous(String passName) {
		if(!enabled) return;
		fabulousName[fabulousi] = passName;
		fabulousStart[fabulousi] = System.nanoTime();
		fabulousi++;
	}

	public static void endFabulous() {
		if(!enabled) return;
		fabulousEnd = System.nanoTime();
	}

	public static void prepareAfterHand(int length) {
		if(!enabled) return;
		if(afterHandStart == null || afterHandStart.length != length) {
			afterHandStart = new long[length];
			afterHandName = new String[length];
		}
		afterHandi = 0;
	}

	public static void timeAfterHand(String passName) {
		if(!enabled) return;
		afterHandName[afterHandi] = passName;
		afterHandStart[afterHandi] = System.nanoTime();
		afterHandi++;
	}

	public static void endAfterHand() {
		if(!enabled) return;
		afterHandEnd = System.nanoTime();
	}

	public static long getShadowTime() {
		return shadowEnd - shadowStart;
	}

	public static long getWorldTime() {
		return worldEnd - worldStart - getShadowTime();
	}

	public static int getBeforeWorldLength() {
		if (beforeWorldStart == null) return 0;
		return beforeWorldStart.length;
	}

	public static long getBeforeWorldTime(int i) {
		if (beforeWorldStart == null || beforeWorldStart.length == 0) return 0;
		if (i == beforeWorldStart.length - 1) {
			return beforeWorldEnd - beforeWorldStart[i];
		} else {
			return beforeWorldStart[i+1] - beforeWorldStart[i];
		}
	}

	public static long getBeforeWorldTotalTime() {
		if (beforeWorldStart == null || beforeWorldStart.length == 0) return 0;
		return beforeWorldEnd - beforeWorldStart[0];
	}

	public static int getFabulousLength() {
		if (fabulousStart == null) return 0;
		return fabulousStart.length;
	}

	public static long getFabulousTime(int i) {
		if (fabulousStart == null || fabulousStart.length == 0) return 0;
		if (i == fabulousStart.length - 1) {
			return fabulousEnd - fabulousStart[i];
		} else {
			return fabulousStart[i+1] - fabulousStart[i];
		}
	}

	public static long getFabulousTotalTime() {
		if (fabulousStart == null || fabulousStart.length == 0) return 0;
		return fabulousEnd - fabulousStart[0];
	}

	public static int getAfterHandLength() {
		if (afterHandStart == null) return 0;
		return afterHandStart.length;
	}

	public static long getAfterHandTime(int i) {
		if (afterHandStart == null || afterHandStart.length == 0) return 0;
		if (i == afterHandStart.length - 1) {
			return afterHandEnd - afterHandStart[i];
		} else {
			return afterHandStart[i+1] - afterHandStart[i];
		}
	}

	public static long getAfterHandTotalTime() {
		if (afterHandStart == null || afterHandStart.length == 0) return 0;
		return afterHandEnd - afterHandStart[0];
	}


	public static void renderOverlay(MatrixStack matrices, TextRenderer fontRenderer) {
		if(!enabled) return;
//		int totalLines = 5 + getBeforeWorldLength() + getFabulousLength() + getAfterHandLength();

		int i = 0;

		matrices.push();
		matrices.scale(0.5f, 0.5f, 0.5f);

		final long shadowTime = getShadowTime();
		if (shadowTime != 0) {
			renderTime("shadow", shadowTime, i++, matrices, fontRenderer);
		}
		renderTime("world", getWorldTime(), i++, matrices, fontRenderer);
		if (getBeforeWorldLength() > 0) {
			renderTime("beforeWorld total", getBeforeWorldTotalTime(), i++, matrices, fontRenderer);
			for (int j = 0; j < getBeforeWorldLength(); j++) {
				renderTime(beforeWorldName[j], getBeforeWorldTime(j), i++, matrices, fontRenderer);
			}
		}
		if (getFabulousLength() > 0) {
			renderTime("fabulous total", getFabulousTotalTime(), i++, matrices, fontRenderer);
			for (int j = 0; j < getFabulousLength(); j++) {
				renderTime(fabulousName[j], getFabulousTime(j), i++, matrices, fontRenderer);
			}
		}
		if (getAfterHandLength() > 0) {
			renderTime("afterHand total", getAfterHandTotalTime(), i++, matrices, fontRenderer);
			for (int j = 0; j < getAfterHandLength(); j++) {
				renderTime(afterHandName[j], getAfterHandTime(j), i++, matrices, fontRenderer);
			}
		}

		matrices.pop();
	}

	final static private int render_forecolor = 0xC0C0C0;
	final static private int render_backcolor = 0x60606060;
	private static void renderTime(String label, long time, int i, MatrixStack matrices, TextRenderer fontRenderer) {
		final String s = String.format("%s: %f ms", label, (float)time/1000000f);
		final int k = fontRenderer.getWidth(s);
		final int m = 100 + 12 * i;
		DrawableHelper.fill(matrices, 20, m - 1, 22 + k + 1, m + 9, render_backcolor);
		fontRenderer.draw(matrices, s, 21, m, render_forecolor);
	}
}
