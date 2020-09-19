package grondag.canvas.shader.wip.encoding;

import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Matrix3f;
import net.minecraft.util.math.Matrix4f;

public class PaintingHelper {
	public static void bufferPainting(MatrixStack matrixStack, WipVertexCollectorImpl vertexConsumer, PaintingEntity paintingEntity, int width, int height, Sprite paintSprite, Sprite frameSprite) {
		final MatrixStack.Entry entry = matrixStack.peek();
		final Matrix4f modelMatrix = entry.getModel();
		final Matrix3f normalMatrix = entry.getNormal();

		final float hHalf = (-width) / 2.0F;
		final float vHalf = (-height) / 2.0F;

		final float fu0 = frameSprite.getMinU();
		final float fv0 = frameSprite.getMinV();
		final float fu1 = frameSprite.getMaxU();
		final float fv1 = frameSprite.getMaxV();

		final float uSpan = frameSprite.getFrameU(1.0D);
		final float vSpan = frameSprite.getFrameV(1.0D);

		final int w = width / 16;
		final int h = height / 16;

		final double wDiv = 16.0D / w;
		final double hDiv = 16.0D / h;

		for(int y = 0; y < w; ++y) {
			for(int xz = 0; xz < h; ++xz) {
				final float y1 = hHalf + (y + 1) * 16;
				final float y0 = hHalf + y * 16;
				final float xz1 = vHalf + (xz + 1) * 16;
				final float xz0 = vHalf + xz * 16;

				int posX = MathHelper.floor(paintingEntity.getX());
				final int posY = MathHelper.floor(paintingEntity.getY() + (xz1 + xz0) / 2.0F / 16.0F);
				int posZ = MathHelper.floor(paintingEntity.getZ());

				final Direction direction = paintingEntity.getHorizontalFacing();

				if (direction == Direction.NORTH) {
					posX = MathHelper.floor(paintingEntity.getX() + (y1 + y0) / 2.0F / 16.0F);
				} else if (direction == Direction.WEST) {
					posZ = MathHelper.floor(paintingEntity.getZ() - (y1 + y0) / 2.0F / 16.0F);
				} else if (direction == Direction.SOUTH) {
					posX = MathHelper.floor(paintingEntity.getX() - (y1 + y0) / 2.0F / 16.0F);
				} else if (direction == Direction.EAST) {
					posZ = MathHelper.floor(paintingEntity.getZ() + (y1 + y0) / 2.0F / 16.0F);
				}

				final int light = WorldRenderer.getLightmapCoordinates(paintingEntity.world, new BlockPos(posX, posY, posZ));

				final float pu0 = paintSprite.getFrameU(wDiv * (w - y));
				final float pu1 = paintSprite.getFrameU(wDiv * (w - (y + 1)));
				final float pv0 = paintSprite.getFrameV(hDiv * (h - xz));
				final float pv1 = paintSprite.getFrameV(hDiv * (h - (xz + 1)));

				vertexConsumer.sprite(paintSprite);

				vertexConsumer.vertex(modelMatrix, y1, xz0, -0.5F).color(-1).texture(pu1, pv0).light(light).normal(normalMatrix, 0, 0, -1).next();
				vertexConsumer.vertex(modelMatrix, y0, xz0, -0.5F).color(-1).texture(pu0, pv0).light(light).normal(normalMatrix, 0, 0, -1).next();
				vertexConsumer.vertex(modelMatrix, y0, xz1, -0.5F).color(-1).texture(pu0, pv1).light(light).normal(normalMatrix, 0, 0, -1).next();
				vertexConsumer.vertex(modelMatrix, y1, xz1, -0.5F).color(-1).texture(pu1, pv1).light(light).normal(normalMatrix, 0, 0, -1).next();

				vertexConsumer.sprite(frameSprite);

				vertexConsumer.vertex(modelMatrix, y1, xz1, 0.5F).color(-1).texture(fu0, fv0).light(light).normal(normalMatrix, 0, 0, 1).next();
				vertexConsumer.vertex(modelMatrix, y0, xz1, 0.5F).color(-1).texture(fu1, fv0).light(light).normal(normalMatrix, 0, 0, 1).next();
				vertexConsumer.vertex(modelMatrix, y0, xz0, 0.5F).color(-1).texture(fu1, fv1).light(light).normal(normalMatrix, 0, 0, 1).next();
				vertexConsumer.vertex(modelMatrix, y1, xz0, 0.5F).color(-1).texture(fu0, fv1).light(light).normal(normalMatrix, 0, 0, 1).next();

				vertexConsumer.vertex(modelMatrix, y1, xz1, -0.5F).color(-1).texture(fu0, fv0).light(light).normal(normalMatrix, 0, 1, 0).next();
				vertexConsumer.vertex(modelMatrix, y0, xz1, -0.5F).color(-1).texture(fu1, fv0).light(light).normal(normalMatrix, 0, 1, 0).next();
				vertexConsumer.vertex(modelMatrix, y0, xz1, 0.5F).color(-1).texture(fu1, vSpan).light(light).normal(normalMatrix, 0, 1, 0).next();
				vertexConsumer.vertex(modelMatrix, y1, xz1, 0.5F).color(-1).texture(fu0, vSpan).light(light).normal(normalMatrix, 0, 1, 0).next();

				vertexConsumer.vertex(modelMatrix, y1, xz0, 0.5F).color(-1).texture(fu0, fv0).light(light).normal(normalMatrix, 0, -1, 0).next();
				vertexConsumer.vertex(modelMatrix, y0, xz0, 0.5F).color(-1).texture(fu1, fv0).light(light).normal(normalMatrix, 0, -1, 0).next();
				vertexConsumer.vertex(modelMatrix, y0, xz0, -0.5F).color(-1).texture(fu1, vSpan).light(light).normal(normalMatrix, 0, -1, 0).next();
				vertexConsumer.vertex(modelMatrix, y1, xz0, -0.5F).color(-1).texture(fu0, vSpan).light(light).normal(normalMatrix, 0, -1, 0).next();

				vertexConsumer.vertex(modelMatrix, y1, xz1, 0.5F).color(-1).texture(uSpan, fv0).light(light).normal(normalMatrix, -1, 0, 0).next();
				vertexConsumer.vertex(modelMatrix, y1, xz0, 0.5F).color(-1).texture(uSpan, fv1).light(light).normal(normalMatrix, -1, 0, 0).next();
				vertexConsumer.vertex(modelMatrix, y1, xz0, -0.5F).color(-1).texture(fu0, fv1).light(light).normal(normalMatrix, -1, 0, 0).next();
				vertexConsumer.vertex(modelMatrix, y1, xz1, -0.5F).color(-1).texture(fu0, fv0).light(light).normal(normalMatrix, -1, 0, 0).next();

				vertexConsumer.vertex(modelMatrix, y0, xz1, -0.5F).color(-1).texture(uSpan, fv0).light(light).normal(normalMatrix, 1, 0, 0).next();
				vertexConsumer.vertex(modelMatrix, y0, xz0, -0.5F).color(-1).texture(uSpan, fv1).light(light).normal(normalMatrix, 1, 0, 0).next();
				vertexConsumer.vertex(modelMatrix, y0, xz0, 0.5F).color(-1).texture(fu0, fv1).light(light).normal(normalMatrix, 1, 0, 0).next();
				vertexConsumer.vertex(modelMatrix, y0, xz1, 0.5F).color(-1).texture(fu0, fv0).light(light).normal(normalMatrix, 1, 0, 0).next();
			}
		}

	}
}
