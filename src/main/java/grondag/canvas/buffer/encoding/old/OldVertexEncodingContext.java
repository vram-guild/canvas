package grondag.canvas.buffer.encoding.old;

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
}
