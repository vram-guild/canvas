package grondag.canvas.apiimpl;

import javax.annotation.Nullable;

import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;

/**
 * Container for ambiguous blend mode variant OR (when blend mod not ambiguous)
 * specific-depth "drawable" materials.
 *
 * WIP: Ugly AF - ditch this somehow
 *
 */
public class CompositeMaterial extends RenderMaterialImpl implements RenderMaterial {
	final int index;

	final MaterialConditionImpl condition;

	/**
	 * True if base layer is translucent.
	 */
	public final boolean isTranslucent;

	private final CompositeMaterial[] blendModeVariants = new CompositeMaterial[4];

	private final DrawableMaterial[] drawables = new DrawableMaterial[MAX_SPRITE_DEPTH];

	protected CompositeMaterial(int index, long bits0, long bits1) {
		this.index = index;
		this.bits0 = bits0;
		this.bits1 = bits1;
		condition = MaterialConditionImpl.fromIndex(CONDITION.getValue(bits0));

		final BlendMode baseLayer = blendMode();

		if(baseLayer == BlendMode.SOLID) {
			isTranslucent = false;
		} else {
			isTranslucent = (baseLayer == BlendMode.TRANSLUCENT);
		}
	}

	private static final ThreadLocal<Finder> variantFinder = ThreadLocal.withInitial(Finder::new);

	void setupVariants() {
		final boolean needsBlendModeVariant = blendMode() == BlendMode.DEFAULT;

		final int depth = spriteDepth();

		final Finder finder = variantFinder.get();

		if(needsBlendModeVariant) {
			for(int i = 0; i < 4; i++) {
				final BlendMode layer = LAYERS[i];

				assert layer != BlendMode.DEFAULT;

				finder.bits0 = bits0;
				finder.bits1 = bits1;

				if(finder.blendMode() == BlendMode.DEFAULT) {
					finder.blendMode(layer);
				}

				blendModeVariants[i] = finder.findInternal(true);

				assert blendModeVariants[i].blendMode() !=  BlendMode.DEFAULT;
			}
		} else {
			// we are a renderable material, so set up control flags needed by shader
			for(int i = 0; i < 4; i++) {
				blendModeVariants[i] = this;
			}

			drawables[0] = new DrawableMaterial(this, 0);

			if (depth > 1) {
				drawables[1] = new DrawableMaterial(this, 1);

				if (depth > 2) {
					drawables[2] = new DrawableMaterial(this, 2);
				}
			}
		}
	}

	/**
	 * If this material has one or more null blend modes, this returns
	 * a material with any such blend modes set to the given input. Typically
	 * this is only used for vanilla default materials that derive their
	 * blend mode from the block render layer, but it is also possible to
	 * specify materials with null blend modes to achieve the same behavior.<p>
	 *
	 * If a non-null blend mode is specified for every sprite layer, this
	 * will always return the current instance.<p>
	 *
	 * We need shader flags to accurately reflect the effective blend mode
	 * and we need that to be fast, and we also want the buffering logic to
	 * remain simple.  This solves all those problems.<p>
	 */
	public CompositeMaterial forBlendMode(int modeIndex) {
		assert blendModeVariants[modeIndex - 1].blendMode() != BlendMode.DEFAULT;
		return blendModeVariants[modeIndex - 1];
	}

	/**
	 * Returns a single-layer material appropriate for the base layer or overlay/decal layer given.
	 * @param spriteIndex
	 * @return
	 */
	public @Nullable DrawableMaterial forDepth(int spriteIndex) {
		assert spriteIndex < spriteDepth();
		return drawables[spriteIndex];
	}

	public int index() {
		return index;
	}
}