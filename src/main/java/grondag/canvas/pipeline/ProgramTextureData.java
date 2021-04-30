package grondag.canvas.pipeline;

import grondag.canvas.pipeline.Image;
import grondag.canvas.pipeline.Pipeline;
import grondag.canvas.pipeline.config.ImageConfig;
import grondag.canvas.pipeline.config.util.NamedDependency;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL46;

public class ProgramTextureData {
	final public int[] texIds;
	final public int[] texTargets;

	public ProgramTextureData(NamedDependency<ImageConfig>[] samplerImages) {
		texIds = new int[samplerImages.length];
		texTargets = new int[samplerImages.length];

		for (int i = 0; i < samplerImages.length; ++i) {
			final String imageName = samplerImages[i].name;

			int imageBind = 0;
			int bindTarget = GL46.GL_TEXTURE_2D;

			if (imageName.contains(":")) {
				final AbstractTexture tex = MinecraftClient.getInstance().getTextureManager().getTexture(new Identifier(imageName));

				if (tex != null) {
					imageBind = tex.getGlId();
				}
			} else {
				final Image img = Pipeline.getImage(imageName);

				if (img != null) {
					imageBind = img.glId();
					bindTarget = img.config.target;
				}
			}

			texIds[i] = imageBind;
			texTargets[i] = bindTarget;
		}
	}
}
