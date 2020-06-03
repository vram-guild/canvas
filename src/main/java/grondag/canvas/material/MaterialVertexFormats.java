package grondag.canvas.material;

import static grondag.canvas.material.MaterialVertextFormatElement.BASE_RGBA_4UB;
import static grondag.canvas.material.MaterialVertextFormatElement.BASE_TEX_2F;
import static grondag.canvas.material.MaterialVertextFormatElement.HD_LIGHTMAP_2US;
import static grondag.canvas.material.MaterialVertextFormatElement.LIGHTMAPS_4UB;
import static grondag.canvas.material.MaterialVertextFormatElement.NORMAL_AO_4UB;
import static grondag.canvas.material.MaterialVertextFormatElement.POSITION_3F;

import grondag.canvas.Configurator;
import grondag.canvas.apiimpl.RenderMaterialImpl.CompositeMaterial;
import grondag.canvas.apiimpl.mesh.MutableQuadViewImpl;

public final class MaterialVertexFormats {
	public static final MaterialVertexFormat VANILLA_BLOCKS_AND_ITEMS = new MaterialVertexFormat(
			POSITION_3F,
			BASE_RGBA_4UB,
			BASE_TEX_2F,
			LIGHTMAPS_4UB,
			NORMAL_AO_4UB);

	public static final MaterialVertexFormat HD_TERRAIN = new MaterialVertexFormat(
			POSITION_3F,
			BASE_RGBA_4UB,
			BASE_TEX_2F,
			LIGHTMAPS_4UB, // PERF: remove and bundle flags with normal
			HD_LIGHTMAP_2US,
			NORMAL_AO_4UB);

	// UGLY: derive this from formats
	public static final int MAX_QUAD_INT_STRIDE = 128;

	public static MaterialVertexFormat get(MaterialContext context, CompositeMaterial mat, MutableQuadViewImpl quad) {
		return context == MaterialContext.TERRAIN && Configurator.hdLightmaps ? HD_TERRAIN : VANILLA_BLOCKS_AND_ITEMS;
	}
}
