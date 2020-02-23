package grondag.canvas.buffer.encoding;

import net.minecraft.util.math.BlockPos;

import grondag.canvas.apiimpl.RenderMaterialImpl;
import grondag.canvas.shader.old.ShaderContext;

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
