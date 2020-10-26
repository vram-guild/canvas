package grondag.canvas.material.property;

public class BinaryMaterialState {
	private final Runnable enableAction;
	private final Runnable disableAction;

	public BinaryMaterialState(Runnable enableAction, Runnable disableAction) {
		this.enableAction = enableAction;
		this.disableAction = disableAction;
	}

	public void setEnabled(boolean enabled) {
		if (isActive && enabled == activeState) {
			return;
		}

		if (enabled) {
			enableAction.run();
		} else {
			disableAction.run();
		}

		isActive = true;
		activeState = enabled;
	}

	public void disable() {
		if (isActive) {
			if (activeState) {
				disableAction.run();
			}

			isActive = false;
		}
	}

	private boolean activeState;
	private boolean isActive;
}
