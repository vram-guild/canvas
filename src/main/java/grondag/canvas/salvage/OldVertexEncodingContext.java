package grondag.canvas.salvage;

import net.minecraft.util.math.BlockPos;

import grondag.canvas.apiimpl.RenderMaterialImpl;

public final class OldVertexEncodingContext {
	RenderMaterialImpl.CompositeMaterial mat;
	OldShaderContext context;
	BlockPos pos;
	float[] aoData;
	int shaderFlags;

	public OldVertexEncodingContext prepare(RenderMaterialImpl.CompositeMaterial mat, OldShaderContext context, BlockPos pos, float[] aoData, int shaderFlags) {
		this.mat = mat;
		this.context = context;
		this.pos = pos;
		this.aoData = aoData;
		this.shaderFlags = shaderFlags;
		return this;
	}

	public int overlay() {
		// TODO Auto-generated method stub
		return 0;
	}
}
