package grondag.canvas.config;

/**
 * Config values that shouldn't change until the next MC restart.
 */
public class StartupFinalBoolean {
	private boolean startedUp = false;
	private boolean effective;

	boolean current;

	public StartupFinalBoolean(boolean currentValue) {
		effective = current = currentValue;
	}

	public boolean effective() {
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
