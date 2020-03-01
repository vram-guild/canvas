package grondag.canvas.material;

import net.minecraft.util.math.BlockPos;

import grondag.canvas.apiimpl.RenderMaterialImpl;
import grondag.canvas.shader.old.OldShaderContext;

public final class OldVertexEncodingContext {
	RenderMaterialImpl.Value mat;
	OldShaderContext context;
	BlockPos pos;
	float[] aoData;
	int shaderFlags;

	public OldVertexEncodingContext prepare(RenderMaterialImpl.Value mat, OldShaderContext context, BlockPos pos, float[] aoData, int shaderFlags) {
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
