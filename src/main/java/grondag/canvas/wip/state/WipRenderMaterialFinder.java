package grondag.canvas.wip.state;


public class WipRenderMaterialFinder extends AbstractStateFinder<WipRenderMaterialFinder, WipRenderMaterial> {
	@Override
	protected synchronized WipRenderMaterial findInner() {
		WipRenderMaterial result = WipRenderMaterial.MAP.get(bits);

		if (result == null) {
			result = new WipRenderMaterial(bits);
			WipRenderMaterial.MAP.put(bits, result);
			WipRenderMaterial.LIST.add(result);
		}

		return result;
	}

	@Override
	protected WipRenderMaterial missing() {
		return WipRenderMaterial.MISSING;
	}

	private static ThreadLocal<WipRenderMaterialFinder> FINDER = ThreadLocal.withInitial(WipRenderMaterialFinder::new);

	public static WipRenderMaterialFinder threadLocal() {
		final WipRenderMaterialFinder result = FINDER.get();
		result.reset();
		return result;
	}


}