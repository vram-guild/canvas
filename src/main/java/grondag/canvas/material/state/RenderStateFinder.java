package grondag.canvas.material.state;


public class RenderStateFinder extends AbstractStateFinder<RenderStateFinder, RenderState>{
	@Override
	public synchronized RenderState findInner() {
		RenderState result = RenderState.MAP.get(bits);

		if (result == null) {
			result = new RenderState(bits);
			RenderState.MAP.put(bits, result);
			RenderState.STATES[result.index] = result;
		}

		return result;
	}

	@Override
	protected RenderState missing() {
		return RenderState.MISSING;
	}

	private static ThreadLocal<RenderStateFinder> FINDER = ThreadLocal.withInitial(RenderStateFinder::new);

	public static RenderStateFinder threadLocal() {
		final RenderStateFinder result = FINDER.get();
		result.clear();
		return result;
	}
}