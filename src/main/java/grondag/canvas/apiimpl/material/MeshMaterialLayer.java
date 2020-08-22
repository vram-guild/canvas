package grondag.canvas.apiimpl.material;

import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;

import grondag.canvas.apiimpl.MaterialConditionImpl;
import grondag.canvas.shader.MaterialShaderManager;
import grondag.canvas.shader.ShaderPass;

/**
 * Describes a single layer of a mesh material and all of the information
 * needed to buffer and draw that layer.  Analogous to vanilla RenderLayer.
 *
 */
public class MeshMaterialLayer {
	private final CompositeMaterial compositeMaterial;
	public final int shaderFlags;
	public final ShaderPass shaderType;
	private final MaterialShaderImpl shader;

	public MeshMaterialLayer(CompositeMaterial compositeMaterial, int depth) {
		this.compositeMaterial = compositeMaterial;

		// determine how to buffer
		if (depth == 0) {
			shaderType = this.compositeMaterial.blendMode() == BlendMode.TRANSLUCENT ? ShaderPass.TRANSLUCENT : ShaderPass.SOLID;
		} else {
			// +1 layers with cutout are expected to not share pixels with lower layers! Otherwise Z-fighting over overwrite will happen
			// anything other than cutout handled as non-sorting, no-depth translucent decal
			shaderType = this.compositeMaterial.blendMode() == BlendMode.CUTOUT || this.compositeMaterial.blendMode() == BlendMode.CUTOUT_MIPPED ? ShaderPass.SOLID : ShaderPass.DECAL;
		}

		shader = MaterialShaderManager.INSTANCE.get(CompositeMaterial.SHADERS[depth].getValue(this.compositeMaterial.bits1));
		int flags = this.compositeMaterial.emissive(depth) ? 1 : 0;

		if (this.compositeMaterial.disableDiffuse(depth)) {
			flags |= 2;
		}

		if (this.compositeMaterial.disableAo(depth)) {
			flags |= 4;
		}

		switch(this.compositeMaterial.blendMode()) {
		case CUTOUT:
			flags |= 16; // disable LOD
			//$FALL-THROUGH$
		case CUTOUT_MIPPED:
			flags |= 8; // cutout
			break;
		default:
			break;
		}

		shaderFlags = flags;
	}

	public MaterialShaderImpl shader() {
		return shader;
	}

	public MaterialConditionImpl condition() {
		return compositeMaterial.condition;
	}
}