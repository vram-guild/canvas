package grondag.canvas.material;

import static grondag.canvas.material.MaterialVertextFormatElement.BASE_RGBA_4UB;
import static grondag.canvas.material.MaterialVertextFormatElement.BASE_TEX_2F;
import static grondag.canvas.material.MaterialVertextFormatElement.LIGHTMAPS_4UB;
import static grondag.canvas.material.MaterialVertextFormatElement.NORMAL_AO_4UB;
import static grondag.canvas.material.MaterialVertextFormatElement.POSITION_3F;

import grondag.canvas.apiimpl.RenderMaterialImpl.Value;
import grondag.canvas.apiimpl.mesh.MutableQuadViewImpl;

public final class MaterialVertexFormats {
	public static final MaterialVertexFormat VANILLA_BLOCKS_AND_ITEMS = new MaterialVertexFormat(
			POSITION_3F,
			BASE_RGBA_4UB,
			BASE_TEX_2F,
			LIGHTMAPS_4UB,
			NORMAL_AO_4UB);

	// UGLY: derive this from formats
	public static final int MAX_QUAD_INT_STRIDE = 128;

	public static MaterialVertexFormat get(MaterialContext context, Value mat, MutableQuadViewImpl quad) {
		return VANILLA_BLOCKS_AND_ITEMS;
	}
}
