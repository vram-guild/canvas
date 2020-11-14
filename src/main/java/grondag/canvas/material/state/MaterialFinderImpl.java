package grondag.canvas.material.state;

import grondag.frex.api.material.MaterialFinder;

// WIP: implement proper decal layers in JMX, RenderBender and XB/XM to improve performance for multi-layer blocks
public class MaterialFinderImpl extends AbstractStateFinder<MaterialFinderImpl, RenderMaterialImpl> implements MaterialFinder {

	private String renderLayerName = CANVAS_MATERIAL_NAME;

	@Override
	public MaterialFinderImpl clear() {
		renderLayerName = CANVAS_MATERIAL_NAME;
		return super.clear();
	}

	public MaterialFinderImpl renderlayerName(String name) {
		renderLayerName = name;
		return this;
	}

	@Override
	protected synchronized RenderMaterialImpl findInner() {
		RenderMaterialImpl result = RenderMaterialImpl.MAP.get(bits);

		if (result == null) {
			result = new RenderMaterialImpl(bits, renderLayerName);
			RenderMaterialImpl.MAP.put(bits, result);
			RenderMaterialImpl.LIST.add(result);
		}

		return result;
	}

	@Override
	protected RenderMaterialImpl missing() {
		return RenderMaterialImpl.MISSING;
	}

	private static ThreadLocal<MaterialFinderImpl> FINDER = ThreadLocal.withInitial(MaterialFinderImpl::new);

	public static MaterialFinderImpl threadLocal() {
		final MaterialFinderImpl result = FINDER.get();
		result.clear();
		return result;
	}

	public static final String CANVAS_MATERIAL_NAME = "<canvas custom material>";
}