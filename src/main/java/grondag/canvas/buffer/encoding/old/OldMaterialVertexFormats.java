package grondag.canvas.buffer.encoding.old;

import static grondag.canvas.buffer.encoding.old.OldMaterialVertextFormatElement.BASE_RGBA_4UB;
import static grondag.canvas.buffer.encoding.old.OldMaterialVertextFormatElement.BASE_TEX_2F;
import static grondag.canvas.buffer.encoding.old.OldMaterialVertextFormatElement.HD_AO_SHADEMAP_2US;
import static grondag.canvas.buffer.encoding.old.OldMaterialVertextFormatElement.HD_BLOCK_LIGHTMAP_2US;
import static grondag.canvas.buffer.encoding.old.OldMaterialVertextFormatElement.HD_SKY_LIGHTMAP_2US;
import static grondag.canvas.buffer.encoding.old.OldMaterialVertextFormatElement.LIGHTMAPS_4UB;
import static grondag.canvas.buffer.encoding.old.OldMaterialVertextFormatElement.NORMAL_AO_4UB;
import static grondag.canvas.buffer.encoding.old.OldMaterialVertextFormatElement.POSITION_3F;
import static grondag.canvas.buffer.encoding.old.OldMaterialVertextFormatElement.SECONDARY_RGBA_4UB;
import static grondag.canvas.buffer.encoding.old.OldMaterialVertextFormatElement.SECONDARY_TEX_2F;
import static grondag.canvas.buffer.encoding.old.OldMaterialVertextFormatElement.TERTIARY_RGBA_4UB;
import static grondag.canvas.buffer.encoding.old.OldMaterialVertextFormatElement.TERTIARY_TEX_2F;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import grondag.canvas.shader.old.OldShaderProps;

public final class OldMaterialVertexFormats {
	private static final Int2ObjectOpenHashMap<OldMaterialVertexFormat> MAP = new Int2ObjectOpenHashMap<>();
	private static final ObjectArrayList<OldMaterialVertexFormat> LIST = new ObjectArrayList<>();

	// Note that all logic for what is in or out is in ShaderProps
	// so that if compact is disabled we'll never see those options here
	// This keeps the key space compact.

	public static OldMaterialVertexFormat fromShaderProps(int shaderProps) {
		OldMaterialVertexFormat result = MAP.get(shaderProps);
		if(result == null) {
			synchronized(MAP) {
				result = MAP.get(shaderProps);
				if(result == null) {
					result = buildFormat(shaderProps);
					MAP.put(shaderProps, result);
					LIST.add(result);
				}
			}
		}
		return result;
	}

	public static OldMaterialVertexFormat fromIndex(int index) {
		return LIST.get(index);
	}

	public static void forceReload() {
		synchronized(MAP) {
			MAP.clear();
			LIST.clear();
		}
	}

	private static OldMaterialVertexFormat buildFormat(int shaderProps) {
		final int spriteDepth = OldShaderProps.spriteDepth(shaderProps);

		final ObjectArrayList<OldMaterialVertextFormatElement> elements = new ObjectArrayList<>();
		elements.add(POSITION_3F);

		if((shaderProps & OldShaderProps.WHITE_0) == 0) {
			elements.add(BASE_RGBA_4UB);
		}

		elements.add(BASE_TEX_2F);
		elements.add(LIGHTMAPS_4UB);

		if((shaderProps & OldShaderProps.SMOOTH_LIGHTMAPS) == OldShaderProps.SMOOTH_LIGHTMAPS) {
			elements.add(HD_BLOCK_LIGHTMAP_2US);
			elements.add(HD_SKY_LIGHTMAP_2US);
			elements.add(HD_AO_SHADEMAP_2US);
		}

		elements.add(NORMAL_AO_4UB);

		if(spriteDepth > 1) {
			elements.add(SECONDARY_RGBA_4UB);
			elements.add(SECONDARY_TEX_2F);
			if(spriteDepth == 3) {
				elements.add(TERTIARY_RGBA_4UB);
				elements.add(TERTIARY_TEX_2F);
			}
		}

		return new OldMaterialVertexFormat(LIST.size(), elements);
	}
}
