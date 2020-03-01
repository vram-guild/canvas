package grondag.canvas.material;

import static grondag.canvas.material.MaterialVertextFormatElement.BASE_RGBA_4UB;
import static grondag.canvas.material.MaterialVertextFormatElement.BASE_TEX_2F;
import static grondag.canvas.material.MaterialVertextFormatElement.HD_AO_SHADEMAP_2US;
import static grondag.canvas.material.MaterialVertextFormatElement.HD_BLOCK_LIGHTMAP_2US;
import static grondag.canvas.material.MaterialVertextFormatElement.HD_SKY_LIGHTMAP_2US;
import static grondag.canvas.material.MaterialVertextFormatElement.LIGHTMAPS_4UB;
import static grondag.canvas.material.MaterialVertextFormatElement.NORMAL_AO_4UB;
import static grondag.canvas.material.MaterialVertextFormatElement.POSITION_3F;
import static grondag.canvas.material.MaterialVertextFormatElement.SECONDARY_RGBA_4UB;
import static grondag.canvas.material.MaterialVertextFormatElement.SECONDARY_TEX_2F;
import static grondag.canvas.material.MaterialVertextFormatElement.TERTIARY_RGBA_4UB;
import static grondag.canvas.material.MaterialVertextFormatElement.TERTIARY_TEX_2F;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import grondag.canvas.apiimpl.RenderMaterialImpl.Value;
import grondag.canvas.apiimpl.mesh.MutableQuadViewImpl;
import grondag.canvas.shader.old.OldShaderProps;

public final class MaterialVertexFormats {
	public static final MaterialVertexFormat VANILLA_BLOCKS_AND_ITEMS = new MaterialVertexFormat(
			POSITION_3F,
			BASE_RGBA_4UB,
			BASE_TEX_2F,
			LIGHTMAPS_4UB,
			NORMAL_AO_4UB);

	public static MaterialVertexFormat get(MaterialContext context, Value mat, MutableQuadViewImpl quad) {
		return VANILLA_BLOCKS_AND_ITEMS;
	}

	// TODO: use or remove
	@SuppressWarnings("unused")
	private static MaterialVertexFormat buildFormat(int shaderProps) {
		final int spriteDepth = OldShaderProps.spriteDepth(shaderProps);

		final ObjectArrayList<MaterialVertextFormatElement> elements = new ObjectArrayList<>();
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

		return new MaterialVertexFormat(elements.toArray(new MaterialVertextFormatElement[elements.size()]));
	}
}
