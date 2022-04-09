package grondag.canvas.config.widget;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import dev.lambdaurora.spruceui.option.SpruceOption;

import net.minecraft.network.chat.Component;

public class StandardOption {
	private static class ExpandedSetter<T> implements Consumer<T> {
		private ResettableOption<T> option;
		private final Consumer<T> setter;

		public ExpandedSetter(Consumer<T> setter) {
			this.setter = setter;
		}

		void attach(ResettableOption<T> option) {
			this.option = option;
		}

		@Override
		public void accept(T t) {
			setter.accept(t);
			option.refreshResetButton();
		}
	}

	public static SpruceOption booleanOption(String key, Supplier<Boolean> getter, Consumer<Boolean> setter, boolean defaultVal, @Nullable Component tooltip) {
		final var expandedSetter = new ExpandedSetter<>(setter);
		final var created = new ResettableCheckbox(key, getter, expandedSetter, defaultVal, tooltip);
		expandedSetter.attach(created);
		return created;
	}

	public static <T extends Enum<T>> SpruceOption enumOption(String key, Supplier<T> getter, Consumer<T> setter, T defaultVal, Class<T> enumType, @Nullable Component tooltip) {
		final var expandedSetter = new ExpandedSetter<>(setter);
		final var created = new ResettableEnum<T>(key, getter, expandedSetter, defaultVal, enumType.getEnumConstants(), tooltip);
		expandedSetter.attach(created);
		return created;
	}

	public static <T> SpruceOption enumOption(String key, Supplier<T> getter, Consumer<T> setter, T defaultVal, T[] values, @Nullable Component tooltip) {
		final var expandedSetter = new ExpandedSetter<>(setter);
		final var created = new ResettableEnum<T>(key, getter, expandedSetter, defaultVal, values, tooltip);
		expandedSetter.attach(created);
		return created;
	}

	public static SpruceOption intOption(String key, int min, int max, int step, Supplier<Integer> getter, Consumer<Integer> setter, int defaultVal, @Nullable Component tooltip) {
		final var expandedSetter = new ExpandedSetter<>(setter);
		final var created = new ResettableSlider.IntSlider(key, min, max, step, getter, expandedSetter, defaultVal, tooltip);
		expandedSetter.attach(created);
		return created;
	}

	public static SpruceOption floatOption(String key, float min, float max, float step, Supplier<Float> getter, Consumer<Float> setter, float defaultVal, @Nullable Component tooltip) {
		final var expandedSetter = new ExpandedSetter<>(setter);
		final var created = new ResettableSlider.FloatSlider(key, min, max, step, getter, expandedSetter, defaultVal, tooltip);
		expandedSetter.attach(created);
		return created;
	}
}
