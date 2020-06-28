package grondag.canvas.material;

import java.util.Arrays;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import grondag.canvas.apiimpl.MaterialConditionImpl;
import grondag.canvas.apiimpl.MaterialShaderImpl;
import grondag.canvas.apiimpl.RenderMaterialImpl.CompositeMaterial.DrawableMaterial;
import grondag.canvas.shader.MaterialShaderManager;
import grondag.canvas.shader.ShaderPass;
import grondag.fermion.bits.BitPacker32;

public class MaterialState {
	// vertices with the same target can share the same buffer
	public final MaterialContext context;

	/*
	 *  unique across all states within a context - for vertex collectors
	 *  position 0 is reserved for translucent
	 */
	public final int collectorIndex;

	public static final int TRANSLUCENT_INDEX = 0;

	public final ShaderPass shaderPass;

	public final MaterialShaderImpl shader;

	public final MaterialConditionImpl condition;

	public final MaterialVertexFormat format;

	private static final int[] nextCollectorIndex = new int[MaterialContext.values().length];

	private MaterialState(MaterialContext context, MaterialShaderImpl shader, MaterialConditionImpl condition, ShaderPass shaderPass) {
		assert shaderPass != ShaderPass.PROCESS;

		this.context = context;
		this.shader = shader;
		this.condition = condition;
		this.shaderPass = shaderPass;
		collectorIndex = shaderPass == ShaderPass.TRANSLUCENT ? TRANSLUCENT_INDEX : ++nextCollectorIndex[context.ordinal()];
		format = MaterialVertexFormats.get(context, shaderPass == ShaderPass.TRANSLUCENT);
	}

	private static final Int2ObjectOpenHashMap<MaterialState> MAP = new Int2ObjectOpenHashMap<>(4096);

	// UGLY: decal probably doesn't belong here
	public static MaterialState get(MaterialContext context, DrawableMaterial mat) {
		return get(context, mat.shader(), mat.condition(), mat.shaderType);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static final BitPacker32<Void> PACKER = new BitPacker32(null, null);
	private static final  BitPacker32<Void>.EnumElement<MaterialContext> CONTEXT_PACKER = PACKER.createEnumElement(MaterialContext.class);
	private static final  BitPacker32<Void>.EnumElement<ShaderPass> SHADER_TYPE_PACKER = PACKER.createEnumElement(ShaderPass.class);
	private static final  BitPacker32<Void>.IntElement CONDITION_PACKER = PACKER.createIntElement(MaterialConditionImpl.MAX_CONDITIONS);
	private static final  BitPacker32<Void>.IntElement SHADER_PACKER = PACKER.createIntElement(1 << (32 - PACKER.bitLength()));

	static {
		assert  PACKER.bitLength() == 32;
	}

	public static MaterialState get(MaterialContext context, MaterialShaderImpl shader, MaterialConditionImpl condition, ShaderPass pass) {
		assert pass != ShaderPass.PROCESS;

		// translucent must be done with ubershader
		if (pass == ShaderPass.TRANSLUCENT) {
			shader = MaterialShaderManager.INSTANCE.getDefault();
			condition = MaterialConditionImpl.ALWAYS;
		}

		final int lookupIndex = CONTEXT_PACKER.getBits(context) | SHADER_TYPE_PACKER.getBits(pass)
				| CONDITION_PACKER.getBits(condition.index) | SHADER_PACKER.getBits(shader.getIndex());

		MaterialState result = MAP.get(lookupIndex);

		if (result == null) {
			synchronized(MAP) {
				result = MAP.get(lookupIndex);

				if (result == null) {
					result = new MaterialState(context, shader, condition, pass);
					MAP.put(lookupIndex, result);
				}
			}
		}

		return result;
	}

	public static MaterialState getDefault(MaterialContext context, ShaderPass pass) {
		return get(context, MaterialShaderManager.INSTANCE.getDefault(), MaterialConditionImpl.ALWAYS, pass);
	}

	public static void reload() {
		Arrays.fill(nextCollectorIndex, 0);
		MAP.clear();
	}
}
