package grondag.canvas.apiimpl.material;

import javax.annotation.Nullable;

import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;

import grondag.canvas.apiimpl.MaterialConditionImpl;

/**
 * Mesh material with a specific blend mode and one or more layers.
 * This class controls overall quad buffer prep and encoding, but individual
 * layers control which buffers are targeted and individual layers.
 *
 * WIP: make sure can handle "dual" render layers and similar vanilla constructs.
 *
 */
public class MeshMaterial extends AbstractMeshMaterial {
	final MaterialConditionImpl condition;

	/**
	 * True if base layer is translucent.
	 */
	public final boolean isTranslucent;

	private final MeshMaterialLayer[] layers = new MeshMaterialLayer[MAX_SPRITE_DEPTH];

	protected MeshMaterial(MeshMaterialLocator locator) {
		bits0 = locator.bits0;
		bits1 = locator.bits1;
		condition = MaterialConditionImpl.fromIndex(CONDITION.getValue(bits0));
		isTranslucent = (blendMode() == BlendMode.TRANSLUCENT);

		layers[0] = new MeshMaterialLayer(this, 0);
		final int depth = spriteDepth();

		if (depth > 1) {
			layers[1] = new MeshMaterialLayer(this, 1);

			if (depth > 2) {
				layers[2] = new MeshMaterialLayer(this, 2);
			}
		}
	}

	/**
	 * Returns a single-layer material appropriate for the base layer or overlay/decal layer given.
	 */
	public @Nullable MeshMaterialLayer getLayer(int layerIndex) {
		assert layerIndex < spriteDepth();
		return layers[layerIndex];
	}
}