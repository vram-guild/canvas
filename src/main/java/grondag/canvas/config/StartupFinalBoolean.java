package grondag.canvas.config;

import java.util.function.Supplier;

/**
 * Config values that shouldn't change until the next MC restart.
 */
public class StartupFinalBoolean implements Supplier<Boolean> {
	private boolean startedUp = false;
	private boolean effective;

	/**
	 * Package-private variable represents current stored value.
	 */
	boolean current;

	public StartupFinalBoolean(boolean currentValue) {
		effective = current = currentValue;
	}

	/**
	 * Get effective value NOT current value.
	 *
	 * @return effective value.
	 */
	@Override
	public Boolean get() {
		assert startedUp : "Accessed startup persistent boolean before initialization.";
		return effective;
	}

	@SuppressWarnings("ConstantConditions")
	public void set(boolean currentValue, boolean isStartup) {
		assert (!isStartup || !startedUp) : "Initializing a startup persistent boolean more than once.";
		effective = (isStartup && !startedUp) ? currentValue : effective;
		startedUp |= isStartup;
		current = currentValue;
	}
}
