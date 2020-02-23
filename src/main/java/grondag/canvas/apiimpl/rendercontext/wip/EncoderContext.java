package grondag.canvas.apiimpl.rendercontext.wip;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.Matrix3f;
import net.minecraft.client.util.math.Matrix4f;
import net.minecraft.client.util.math.Vector3f;

import grondag.canvas.apiimpl.rendercontext.BlockRenderInfo;
import grondag.canvas.light.AoCalculator;

public interface EncoderContext {
	VertexConsumer consumer(RenderLayer layer);

	BlockRenderInfo blockInfo();

	AoCalculator aoCalc();

	Vector3f normalVec();

	Matrix4f matrix();

	Matrix3f normalMatrix();

	int overlay();
}