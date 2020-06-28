package grondag.canvas.buffer.encoding;

import net.minecraft.client.render.VertexConsumer;

public interface VertexCollector extends VertexConsumer {
	void addi(int i);

	void addf(float f);

	void addf(float u, float v);

	void addf(float x, float y, float z);

	void addf(float... f);

	void add(int[] appendData, int length);
}
