package grondag.canvas.buffer.encoding;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.Matrix3f;
import net.minecraft.client.util.math.Matrix4f;
import net.minecraft.client.util.math.Vector3f;

import grondag.canvas.apiimpl.mesh.MutableQuadViewImpl;

public interface VertexEncodingContext {
	void computeLighting(MutableQuadViewImpl quad);

	void applyLighting(MutableQuadViewImpl quad);

	Vector3f normalVec();

	Matrix4f matrix();

	Matrix3f normalMatrix();

	int overlay();

	VertexConsumer consumer(MutableQuadViewImpl quad);

	int indexedColor(int colorIndex);
}