package grondag.canvas.material;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import grondag.canvas.apiimpl.MaterialConditionImpl;
import grondag.canvas.apiimpl.MaterialShaderImpl;
import grondag.canvas.apiimpl.RenderMaterialImpl.CompositeMaterial.DrawableMaterial;
import grondag.canvas.shader.ShaderManager;
import grondag.canvas.shader.ShaderPass;
import grondag.fermion.bits.BitPacker32;

public class MaterialState {
	// vertices with the same target can share the same buffer
	public final MaterialContext context;

	public final int index;

	public final ShaderPass shaderPass;

	public final MaterialShaderImpl shader;

	public final MaterialConditionImpl condition;

	public final MaterialVertexFormat format;

	private static int nextIndex = 0;

	private MaterialState(MaterialContext context, MaterialShaderImpl shader, MaterialConditionImpl condition, ShaderPass shaderType) {
		this.context = context;
		this.shader = shader;
		this.condition = condition;
		index = nextIndex++;
		this.shaderPass = shaderType;
		format = MaterialVertexFormats.get(context, shaderType == ShaderPass.TRANSLUCENT);
	}

	private static final Int2ObjectOpenHashMap<MaterialState> MAP = new Int2ObjectOpenHashMap<>(4096);
	private static final ObjectArrayList<MaterialState> LIST = new ObjectArrayList<>(4096);

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

	public static MaterialState get(MaterialContext context, MaterialShaderImpl shader, MaterialConditionImpl condition, ShaderPass shaderType) {
		// translucent must be done with ubershader
		if (shaderType == ShaderPass.TRANSLUCENT) {
			shader = ShaderManager.INSTANCE.getDefault();
			condition = MaterialConditionImpl.ALWAYS;
		}

		final int lookupIndex = CONTEXT_PACKER.getBits(context) | SHADER_TYPE_PACKER.getBits(shaderType)
				| CONDITION_PACKER.getBits(condition.index) | SHADER_PACKER.getBits(shader.getIndex());

		MaterialState result = MAP.get(lookupIndex);

		if (result == null) {
			synchronized(MAP) {
				result = MAP.get(lookupIndex);

				if (result == null) {
					result = new MaterialState(context, shader, condition, shaderType);
					MAP.put(lookupIndex, result);
					LIST.add(result);
				}
			}
		}

		return result;
	}

	public static MaterialState getDefault(MaterialContext context, ShaderPass shaderType) {
		return get(context, ShaderManager.INSTANCE.getDefault(), MaterialConditionImpl.ALWAYS, shaderType);
	}

	public static MaterialState get(int index) {
		return  LIST.get(index);
	}

	public static void reload() {
		nextIndex = 0;
		MAP.clear();
		LIST.clear();
	}
}
