package grondag.canvas.buffer.encoding;

import static grondag.canvas.buffer.encoding.MaterialVertextFormatElement.BASE_RGBA_4UB;
import static grondag.canvas.buffer.encoding.MaterialVertextFormatElement.BASE_TEX_2F;
import static grondag.canvas.buffer.encoding.MaterialVertextFormatElement.HD_AO_SHADEMAP_2US;
import static grondag.canvas.buffer.encoding.MaterialVertextFormatElement.HD_BLOCK_LIGHTMAP_2US;
import static grondag.canvas.buffer.encoding.MaterialVertextFormatElement.HD_SKY_LIGHTMAP_2US;
import static grondag.canvas.buffer.encoding.MaterialVertextFormatElement.LIGHTMAPS_4UB;
import static grondag.canvas.buffer.encoding.MaterialVertextFormatElement.NORMAL_AO_4UB;
import static grondag.canvas.buffer.encoding.MaterialVertextFormatElement.POSITION_3F;
import static grondag.canvas.buffer.encoding.MaterialVertextFormatElement.SECONDARY_RGBA_4UB;
import static grondag.canvas.buffer.encoding.MaterialVertextFormatElement.SECONDARY_TEX_2F;
import static grondag.canvas.buffer.encoding.MaterialVertextFormatElement.TERTIARY_RGBA_4UB;
import static grondag.canvas.buffer.encoding.MaterialVertextFormatElement.TERTIARY_TEX_2F;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import grondag.canvas.shader.ShaderProps;

public final class MaterialVertexFormats {
	private static final Int2ObjectOpenHashMap<MaterialVertexFormat> MAP = new Int2ObjectOpenHashMap<>();
	private static final ObjectArrayList<MaterialVertexFormat> LIST = new ObjectArrayList<>();

	// Note that all logic for what is in or out is in ShaderProps
	// so that if compact is disabled we'll never see those options here
	// This keeps the key space compact.

	public static MaterialVertexFormat fromShaderProps(int shaderProps) {
		MaterialVertexFormat result = MAP.get(shaderProps);
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

	public static MaterialVertexFormat fromIndex(int index) {
		return LIST.get(index);
	}

	public static void forceReload() {
		synchronized(MAP) {
			MAP.clear();
			LIST.clear();
		}
	}

	private static MaterialVertexFormat buildFormat(int shaderProps) {
		final int spriteDepth = ShaderProps.spriteDepth(shaderProps);

		final ObjectArrayList<MaterialVertextFormatElement> elements = new ObjectArrayList<>();
		elements.add(POSITION_3F);

		if((shaderProps & ShaderProps.WHITE_0) == 0) {
			elements.add(BASE_RGBA_4UB);
		}

		elements.add(BASE_TEX_2F);
		elements.add(LIGHTMAPS_4UB);

		if((shaderProps & ShaderProps.SMOOTH_LIGHTMAPS) == ShaderProps.SMOOTH_LIGHTMAPS) {
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

		return new MaterialVertexFormat(LIST.size(), elements);
	}
}
