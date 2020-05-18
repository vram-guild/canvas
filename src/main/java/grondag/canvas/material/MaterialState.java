package grondag.canvas.material;

import java.util.Arrays;

import net.minecraft.util.math.MathHelper;

import grondag.canvas.Configurator;
import grondag.canvas.apiimpl.RenderMaterialImpl.CompositeMaterial.DrawableMaterial;
import grondag.canvas.draw.DrawHandler;
import grondag.canvas.draw.DrawHandlers;
import grondag.canvas.shader.ShaderContext;
import grondag.fermion.varia.Useful;

public class MaterialState {
	// vertices with the same target can share the same buffer
	public final MaterialContext context;

	// sets up gl state, updates uniforms and does draw.  For shaders, handles vertex attributes and same handler implies same shader.
	// input format must match output format of draw handler
	public final DrawHandler drawHandler;

	public final MaterialVertexFormat bufferFormat;

	public final int index;

	public final long sortIndex;

	public final ShaderContext.Type shaderType;

	private MaterialState(MaterialContext context, DrawHandler drawHandler, int index) {
		this.context = context;
		bufferFormat = drawHandler.format;
		this.drawHandler = drawHandler;
		this.index = index;
		shaderType = drawHandler.shaderType;
		sortIndex = (bufferFormat.vertexStrideBytes << 24) | index;
	}

	private static final int DRAW_HANDLER_SHIFT = Useful.bitLength(MathHelper.smallestEncompassingPowerOfTwo(MaterialContext.values().length));

	public static int MAX_MATERIAL_STATES = Configurator.maxMaterialStates;

	private static final MaterialState[] VALUES = new MaterialState[MAX_MATERIAL_STATES];

	// UGLY: decal probably doesn't belong here
	public static MaterialState get(MaterialContext context, DrawableMaterial mat) {
		return get(context, DrawHandlers.get(context, mat));
	}

	public static MaterialState get(MaterialContext context, DrawHandler drawHandler) {
		final int index = index(context, drawHandler);
		MaterialState result = VALUES[index];

		if (result == null) {
			synchronized(VALUES) {
				result = VALUES[index];

				if (result == null) {
					result = new MaterialState(context, drawHandler, index);
					VALUES[index] = result;
				}
			}
		}

		return result;
	}

	public static MaterialState get(int index) {
		return  VALUES[index];
	}

	private static int index(MaterialContext context, DrawHandler drawHandler) {
		return context.ordinal() | (drawHandler.index << DRAW_HANDLER_SHIFT);
	}

	public static void reload() {
		Arrays.fill(VALUES, null);
	}
}
