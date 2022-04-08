package grondag.canvas.config.widget;

import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import dev.lambdaurora.spruceui.option.SpruceOption;
import dev.lambdaurora.spruceui.screen.SpruceScreen;

import net.minecraft.network.chat.Component;

public abstract class ConfigSession extends SpruceScreen {
	private final ArrayList<ResettableOption> resettableOptions = new ArrayList<>();
	private boolean closed = false;

	protected ConfigSession(Component title) {
		super(title);
	}

	@Override
	protected void init() {
		super.init();

		if (closed) {
			throw new IllegalStateException("A closed config session was reopened.");
		}
	}

	protected <T> Consumer<T> createSetter(Consumer<T> setter) {
		final int index = resettableOptions.size();
		return t -> {
			setter.accept(t);
			onSet(index);
		};
	}

	protected SpruceOption booleanOption(String key, Supplier<Boolean> getter, Consumer<Boolean> setter, boolean defaultVal, @Nullable Component tooltip) {
		final var expandedSetter = createSetter(setter);
		final var created = new ResettableCheckbox(key, getter, expandedSetter, defaultVal, tooltip);
		resettableOptions.add(created);
		return created;
	}

	protected <T extends Enum<T>> SpruceOption enumOption(String key, Supplier<T> getter, Consumer<T> setter, T defaultVal, Class<T> enumType, @Nullable Component tooltip) {
		final var expandedSetter = createSetter(setter);
		final var created = new ResettableEnum(key, getter, expandedSetter, defaultVal, enumType, tooltip);
		resettableOptions.add(created);
		return created;
	}

	protected SpruceOption intOption(String key, int min, int max, int step, Supplier<Integer> getter, Consumer<Integer> setter, int defaultVal, @Nullable Component tooltip) {
		final var expandedSetter = createSetter(setter);
		final var created = new ResettableSlider.IntSlider(key, min, max, step, getter, expandedSetter, defaultVal, tooltip);
		resettableOptions.add(created);
		return created;
	}

	protected SpruceOption floatOption(String key, float min, float max, float step, Supplier<Float> getter, Consumer<Float> setter, float defaultVal, @Nullable Component tooltip) {
		final var expandedSetter = createSetter(setter);
		final var created = new ResettableSlider.FloatSlider(key, min, max, step, getter, expandedSetter, defaultVal, tooltip);
		resettableOptions.add(created);
		return created;
	}

	private void onSet(int index) {
		resettableOptions.get(index).refreshResetButton();
	}

	@Override
	protected void clearWidgets() {
		super.clearWidgets();
		resettableOptions.clear();
	}

	protected final void close() {
		closed = true;
		// is this necessary?
		resettableOptions.clear();
	}
}
