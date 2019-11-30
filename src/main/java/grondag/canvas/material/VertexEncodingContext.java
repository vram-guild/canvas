package grondag.canvas.material;

import net.minecraft.util.math.BlockPos;

import grondag.canvas.apiimpl.RenderMaterialImpl;

public final class VertexEncodingContext {
	RenderMaterialImpl.Value mat;
	ShaderContext context;
	BlockPos pos;
	float[] aoData;
	int shaderFlags;

	public VertexEncodingContext prepare(RenderMaterialImpl.Value mat, ShaderContext context, BlockPos pos, float[] aoData, int shaderFlags) {
		this.mat = mat;
		this.context = context;
		this.pos = pos;
		this.aoData = aoData;
		this.shaderFlags = shaderFlags;
		return this;
	}
}
