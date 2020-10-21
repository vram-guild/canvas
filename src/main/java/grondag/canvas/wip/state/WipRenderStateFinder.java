package grondag.canvas.wip.state;


public class WipRenderStateFinder extends AbstractStateFinder<WipRenderStateFinder, WipRenderState>{
	@Override
	public synchronized WipRenderState findInner() {
		WipRenderState result = WipRenderState.MAP.get(bits);

		if (result == null) {
			result = new WipRenderState(bits);
			WipRenderState.MAP.put(bits, result);
			WipRenderState.STATES[result.index] = result;
		}

		return result;
	}

	@Override
	protected WipRenderState missing() {
		return WipRenderState.MISSING;
	}

	private static ThreadLocal<WipRenderStateFinder> FINDER = ThreadLocal.withInitial(WipRenderStateFinder::new);

	public static WipRenderStateFinder threadLocal() {
		final WipRenderStateFinder result = FINDER.get();
		result.reset();
		return result;
	}
}