package grondag.canvas.light;

import it.unimi.dsi.fastutil.ints.Int2IntFunction;

import grondag.canvas.Configurator;

public final class LightmapSizer {
	public static final int texSize = Configurator.moreLightmap ? 8192 : 4096;
	public static final int lightmapSize = 4;
	public static final int aoSize = lightmapSize + 1;
	public static final int paddedSize = lightmapSize + 2;
	public static final int radius = lightmapSize / 2;
	public static final int lightmapPixels = paddedSize * paddedSize;
	public static final int mapsPerAxis = texSize / paddedSize;
	public static final int maxCount = mapsPerAxis * mapsPerAxis;
	// UGLY - consider making this a full unsigned short
	// for initial pass didn't want to worry about signed value mistakes
	/** Scale of texture units sent to shader. Shader should divide by this. */
	public static final int bufferScale = 0x8000;
	public static final float textureToBuffer = (float) bufferScale / texSize;

	/** converts zero-based distance from center to u/v index - use for top/left */
	public static final Int2IntFunction NEG = i -> radius - i;
	/** converts zero-based distance from center to u/v index - use for bottom/right */
	public static final Int2IntFunction POS = i -> radius + 1 + i;
}
